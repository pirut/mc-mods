package io.github.pirut.ccagent;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.pirut.ccagent.Json.obj;

/**
 * A Model Context Protocol server over Streamable HTTP (plain JSON responses, no SSE stream),
 * on the JDK's built-in HTTP server. One endpoint, /mcp, behind a bearer token.
 */
public final class McpServer {
    private static final List<String> PROTOCOLS = List.of("2025-11-25", "2025-06-18", "2025-03-26", "2024-11-05");

    static final String INSTRUCTIONS = """
        Tools for CC: Tweaked (ComputerCraft) computers and turtles in the player's Minecraft world.
        Start with list_computers. Write programs with write_file (or edit_file), run them with run_shell \
        ("myprog args") or run_lua, and read what they print in the result. run_lua returns an expression's value, \
        so it's also the quickest way to query any CC API (fs, peripheral, rednet, gps, redstone, turtle, textutils...).
        Programs that wait for keyboard input or draw full-screen UIs: start them visibly with type_text (it types \
        into the real shell), then use read_screen and press_keys.
        Lua is CC: Tweaked's (Lua 5.2 style). Long loops must yield (sleep, os.pullEvent) or CC kills them after \
        about 10 s. Turtle moves need fuel. Positions: +x east, +y up, +z south.
        Use cc_help for CC's own API docs. Files written here land on the computer at once; nothing restarts \
        unless you reboot it.""";

    private final HttpServer http;
    private final ExecutorService pool;
    private final Tools tools;
    private final AgentConfig config;

    private McpServer(HttpServer http, ExecutorService pool, Tools tools, AgentConfig config) {
        this.http = http;
        this.pool = pool;
        this.tools = tools;
        this.config = config;
    }

    public static McpServer start(MinecraftServer server, AgentConfig config) throws IOException {
        var http = HttpServer.create(new InetSocketAddress(config.host, config.port), 16);
        var count = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(8, r -> {
            var t = new Thread(r, "CC: Agent MCP " + count.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        var mcp = new McpServer(http, pool, new Tools(server), config);
        http.setExecutor(pool);
        http.createContext("/mcp", mcp::handle);
        http.createContext("/", exchange -> mcp.send(exchange, 404, "text/plain", "CC: Agent: the MCP endpoint is /mcp\n"));
        http.start();
        return mcp;
    }

    public void stop() {
        http.stop(0);
        pool.shutdownNow();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if (!localOrigin(exchange)) {
                send(exchange, 403, "text/plain", "forbidden origin\n");
                return;
            }
            if (!authorised(exchange)) {
                exchange.getResponseHeaders().add("WWW-Authenticate", "Bearer");
                send(exchange, 401, "text/plain", "missing or wrong bearer token (see config/ccagent.properties or /ccagent in game)\n");
                return;
            }
            switch (exchange.getRequestMethod().toUpperCase(Locale.ROOT)) {
                case "POST" -> post(exchange);
                case "DELETE" -> send(exchange, 200, "text/plain", "");
                default -> {
                    exchange.getResponseHeaders().add("Allow", "POST, DELETE");
                    send(exchange, 405, "text/plain", "this server doesn't offer an SSE stream; POST JSON-RPC to /mcp\n");
                }
            }
        } catch (Exception e) {
            CCAgent.LOG.warn("MCP request failed", e);
            send(exchange, 500, "text/plain", "internal error\n");
        } finally {
            exchange.close();
        }
    }

    /** Browsers send Origin; refuse any page that isn't on this machine (DNS rebinding). */
    private static boolean localOrigin(HttpExchange exchange) {
        var origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin == null || origin.equals("null")) return origin == null;
        try {
            var host = URI.create(origin).getHost();
            return host != null && (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("[::1]") || host.equals("::1"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean authorised(HttpExchange exchange) {
        var header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) return false;
        var given = header.substring(7).trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(given, config.token.getBytes(StandardCharsets.UTF_8));
    }

    private void post(HttpExchange exchange) throws IOException {
        JsonElement request;
        try {
            request = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            sendJson(exchange, error(null, -32700, "parse error"));
            return;
        }
        if (request.isJsonArray()) {
            var replies = new JsonArray();
            for (var item : request.getAsJsonArray()) {
                var reply = dispatch(item);
                if (reply != null) replies.add(reply);
            }
            if (replies.isEmpty()) send(exchange, 202, null, "");
            else sendJson(exchange, replies);
            return;
        }
        var reply = dispatch(request);
        if (reply == null) send(exchange, 202, null, "");
        else sendJson(exchange, reply);
    }

    private JsonObject dispatch(JsonElement element) {
        if (!element.isJsonObject()) return error(null, -32600, "invalid request");
        var message = element.getAsJsonObject();
        var id = message.get("id");
        var method = message.has("method") ? message.get("method").getAsString() : null;
        // Responses and notifications from the client need no answer.
        if (method == null || id == null || id.isJsonNull()) return null;
        var params = message.has("params") && message.get("params").isJsonObject() ? message.getAsJsonObject("params") : new JsonObject();
        try {
            return switch (method) {
                case "initialize" -> result(id, initialize(params));
                case "ping" -> result(id, new JsonObject());
                case "tools/list" -> result(id, toolsList());
                case "tools/call" -> result(id, call(params));
                case "resources/list" -> result(id, obj("resources", new JsonArray()));
                case "prompts/list" -> result(id, obj("prompts", new JsonArray()));
                default -> error(id, -32601, "method not found: " + method);
            };
        } catch (RuntimeException e) {
            CCAgent.LOG.warn("MCP {} failed", method, e);
            return error(id, -32603, e.toString());
        }
    }

    private static JsonObject initialize(JsonObject params) {
        var asked = params.has("protocolVersion") ? params.get("protocolVersion").getAsString() : PROTOCOLS.get(1);
        var version = PROTOCOLS.contains(asked) ? asked : PROTOCOLS.get(1);
        return obj(
            "protocolVersion", version,
            "capabilities", obj("tools", obj("listChanged", false)),
            "serverInfo", obj("name", "cc-agent", "title", "CC: Agent", "version", CCAgent.VERSION),
            "instructions", INSTRUCTIONS);
    }

    private JsonObject toolsList() {
        var list = new JsonArray();
        for (var tool : tools.all()) list.add(tool.listing());
        return obj("tools", list);
    }

    private JsonObject call(JsonObject params) {
        var name = params.has("name") ? params.get("name").getAsString() : "";
        var tool = tools.get(name);
        if (tool == null) return toolResult("unknown tool " + name, true);
        var args = params.has("arguments") && params.get("arguments").isJsonObject() ? params.getAsJsonObject("arguments") : new JsonObject();
        try {
            var value = tool.handler().call(args);
            if (value instanceof Tools.ToolResult r) return toolResult(r.text(), r.error());
            if (value instanceof JsonElement json) return toolResult(Json.PRETTY.toJson(json), false);
            return toolResult(String.valueOf(value), false);
        } catch (ToolException e) {
            return toolResult(e.getMessage(), true);
        } catch (Exception e) {
            CCAgent.LOG.warn("Tool {} failed", name, e);
            return toolResult(name + " failed: " + e, true);
        }
    }

    private static JsonObject toolResult(String text, boolean isError) {
        var content = new JsonArray();
        content.add(obj("type", "text", "text", text));
        return obj("content", content, "isError", isError);
    }

    private static JsonObject result(JsonElement id, JsonObject result) {
        return obj("jsonrpc", "2.0", "id", id, "result", result);
    }

    private static JsonObject error(JsonElement id, int code, String message) {
        var out = obj("jsonrpc", "2.0", "error", obj("code", code, "message", message));
        out.add("id", id == null ? com.google.gson.JsonNull.INSTANCE : id);
        return out;
    }

    private void sendJson(HttpExchange exchange, JsonElement body) throws IOException {
        send(exchange, 200, "application/json", Json.COMPACT.toJson(body));
    }

    private void send(HttpExchange exchange, int status, String type, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        if (type != null) exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }
}
