package io.github.pirut.ccagent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.menu.ComputerMenu;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static io.github.pirut.ccagent.Json.obj;

/**
 * Multiplayer: a player's agent runs on their own PC. Their game client serves the same MCP tools
 * locally and forwards every call over the game connection; the server runs it with that player's
 * limits ({@link Access}) and sends the result back. The in-game claude/codex programs work the same
 * way: the server asks the player's client to start the CLI, and the chat lines come back to the computer.
 */
public final class Relay {
    static final int CHUNK = 30_000;
    private static final AtomicInteger NEXT_MESSAGE = new AtomicInteger(1);
    // Relay chat ids stay clear of this machine's own agent run ids, which share the chat program.
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1_000_000);
    private static final Map<String, Partial> PARTIALS = new ConcurrentHashMap<>();
    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        var t = new Thread(r, "CC: Agent relay");
        t.setDaemon(true);
        return t;
    });

    private Relay() {
    }

    // ---- Splitting messages into payloads ----

    private static final class Partial {
        final byte[][] parts;
        int received;

        Partial(int count) {
            parts = new byte[count][];
        }
    }

    static List<RelayPayload> encode(JsonObject message) {
        var bytes = Json.COMPACT.toJson(message).getBytes(StandardCharsets.UTF_8);
        int id = NEXT_MESSAGE.getAndIncrement();
        int parts = Math.max(1, (bytes.length + CHUNK - 1) / CHUNK);
        var out = new ArrayList<RelayPayload>(parts);
        for (int i = 0; i < parts; i++) {
            out.add(new RelayPayload(id, i, parts, Arrays.copyOfRange(bytes, i * CHUNK, Math.min(bytes.length, (i + 1) * CHUNK))));
        }
        return out;
    }

    /** Collect a payload; returns the whole message once every part is in. */
    private static JsonObject assemble(String from, RelayPayload payload) {
        if (payload.parts() <= 0 || payload.parts() > 2048 || payload.part() < 0 || payload.part() >= payload.parts()) return null;
        if (payload.parts() == 1) return parse(payload.data());
        var key = from + "/" + payload.message();
        // A sender can't pile up half-sent messages without end.
        if (!PARTIALS.containsKey(key) && pendingFrom(from) >= MAX_PARTIALS) return null;
        var partial = PARTIALS.computeIfAbsent(key, k -> new Partial(payload.parts()));
        synchronized (partial) {
            if (partial.parts.length != payload.parts() || partial.parts[payload.part()] != null) return null;
            partial.parts[payload.part()] = payload.data();
            if (++partial.received < partial.parts.length) return null;
        }
        PARTIALS.remove(key);
        var all = new ByteArrayOutputStream();
        for (var part : partial.parts) all.writeBytes(part);
        return parse(all.toByteArray());
    }

    private static final int MAX_PARTIALS = 16;

    private static long pendingFrom(String from) {
        var prefix = from + "/";
        return PARTIALS.keySet().stream().filter(k -> k.startsWith(prefix)).count();
    }

    private static JsonObject parse(byte[] bytes) {
        try {
            var json = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            return json.isJsonObject() ? json.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---- Server side ----

    private record Chat(int id, IComputerSystem computer, UUID player) {
    }

    private static final Map<Integer, Chat> CHATS = new ConcurrentHashMap<>();
    private static final Map<UUID, ArrayDeque<Long>> CALLS = new ConcurrentHashMap<>();

    private static boolean serverEnabled() {
        var config = CCAgent.config();
        return config != null && config.relay && CCAgent.server() != null;
    }

    private static void send(ServerPlayer player, JsonObject message) {
        for (var payload : encode(message)) CCAgent.platform().sendToPlayer(player, payload);
    }

    /** Tell a joining player's client the server takes relayed tool calls. */
    public static void playerJoined(ServerPlayer player) {
        var server = CCAgent.server();
        if (!serverEnabled() || !CCAgent.platform().hasRelay(player)) return;
        // The singleplayer host already has the local MCP server.
        if (McNames.isHost(server, player)) return;
        send(player, obj("type", "hello", "version", CCAgent.version()));
    }

    public static void playerLeft(ServerPlayer player) {
        // Their PC ran the agent, so its chats end here: tell the computers instead of leaving them waiting.
        for (var chat : List.copyOf(CHATS.values())) {
            if (chat.player.equals(player.getUUID())) {
                CHATS.remove(chat.id);
                chat.computer.queueEvent("ccagent_chat", chat.id, "error", LuaStrings.toLua(
                    McNames.name(player) + " left the server, and the agent ran on their PC"));
                chat.computer.queueEvent("ccagent_chat", chat.id, "done", "");
            }
        }
        PARTIALS.keySet().removeIf(k -> k.startsWith(player.getUUID() + "/"));
        for (var tui : List.copyOf(TUIS.values())) {
            if (tui.player.equals(player.getUUID())) {
                TUIS.remove(tui.id);
                tui.computer.queueEvent("ccagent_tui", tui.id, "exit", -1);
            }
        }
        CALLS.remove(player.getUUID());
    }

    public static void serverReceive(ServerPlayer player, RelayPayload payload) {
        var message = assemble(player.getUUID().toString(), payload);
        if (message == null) return;
        // Frames must stay in order, and only queue an event: handle them here.
        if (Json.str(message, "type", "").startsWith("tui_")) {
            try {
                serverTui(player, message);
            } catch (RuntimeException e) {
                CCAgent.LOG.warn("Relay TUI message from {} failed", McNames.name(player), e);
            }
            return;
        }
        POOL.execute(() -> {
            try {
                serverHandle(player, message);
            } catch (RuntimeException e) {
                CCAgent.LOG.warn("Relay message from {} failed", McNames.name(player), e);
            }
        });
    }

    private static void serverHandle(ServerPlayer player, JsonObject message) {
        var type = Json.str(message, "type", "");
        if (type.equals("call")) {
            var id = Json.integer(message, "id", 0);
            var name = Json.str(message, "tool", "");
            var args = message.has("args") && message.get("args").isJsonObject() ? message.getAsJsonObject("args") : new JsonObject();
            JsonObject result;
            var config = CCAgent.config();
            if (!serverEnabled()) {
                result = McpServer.toolResult("this server has CC: Agent's relay turned off (relay=false)", true);
            } else if (config.relayOpsOnly && !McNames.isOp(player)) {
                result = McpServer.toolResult("on this server only operators can use CC: Agent", true);
            } else if (name.equals("__list")) {
                result = obj("tools", CCAgent.tools().tools());
            } else if (!allowCall(player.getUUID(), config.relayCallsPerMinute)) {
                result = McpServer.toolResult("slow down: this server allows " + config.relayCallsPerMinute
                    + " tool calls a minute per player (relayCallsPerMinute)", true);
            } else {
                var access = new Access(player, McNames.isOp(player), config.relayRange);
                result = Access.with(access, () -> CCAgent.tools().call(name, args));
            }
            send(player, obj("type", "result", "id", id, "result", result));
        } else if (type.equals("chat")) {
            var chat = CHATS.get(Json.integer(message, "chat", -1));
            if (chat == null || !chat.player.equals(player.getUUID())) return;
            var kind = Json.str(message, "kind", "text");
            chat.computer.queueEvent("ccagent_chat", chat.id, kind, LuaStrings.toLua(Json.str(message, "text", "")));
            if (kind.equals("done")) CHATS.remove(chat.id);
        }
    }

    private static boolean allowCall(UUID player, int perMinute) {
        if (perMinute <= 0) return true;
        var now = System.currentTimeMillis();
        var times = CALLS.computeIfAbsent(player, p -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > 60_000) times.removeFirst();
            if (times.size() >= perMinute) return false;
            times.addLast(now);
            return true;
        }
    }

    /** The player with this computer's screen open, if any. */
    public static ServerPlayer interactingPlayer(int computerId) {
        var server = CCAgent.server();
        if (server == null) return null;
        for (var player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof ComputerMenu menu && menu.getComputer() != null && menu.getComputer().getID() == computerId) {
                return player;
            }
        }
        return null;
    }

    /** Ask a player's client to run Claude Code or Codex for this computer. Returns the chat id. */
    public static int launch(ServerPlayer player, IComputerSystem computer, String agent, String prompt, String session, int width,
                             AgentRunner.Options options) {
        checkLaunch(player);
        int id = NEXT_ID.getAndIncrement();
        CHATS.put(id, new Chat(id, computer, player.getUUID()));
        var message = obj("type", "launch", "chat", id, "agent", agent, "prompt", prompt, "width", width,
            "computer", computer.getID(), "options", options.toJson());
        if (session != null) message.addProperty("session", session);
        if (computer.getLabel() != null) message.addProperty("label", computer.getLabel());
        send(player, message);
        return id;
    }

    // ---- Server side: full-screen TUIs ----

    private record Tui(int id, IComputerSystem computer, UUID player) {
    }

    private static final Map<Integer, Tui> TUIS = new ConcurrentHashMap<>();

    /** Ask a player's client to run an agent's TUI for this computer. Returns the session id. */
    public static int launchTui(ServerPlayer player, IComputerSystem computer, String agent, int cols, int rows, String monitor) {
        checkLaunch(player);
        int id = NEXT_ID.getAndIncrement();
        TUIS.put(id, new Tui(id, computer, player.getUUID()));
        var message = obj("type", "tui_launch", "tui", id, "agent", agent, "cols", cols, "rows", rows,
            "computer", computer.getID());
        if (monitor != null) message.addProperty("monitor", monitor);
        if (computer.getLabel() != null) message.addProperty("label", computer.getLabel());
        send(player, message);
        return id;
    }

    /** Whether this is a relayed TUI session running for that computer. */
    public static boolean isRelayTui(int id, int computerId) {
        var tui = TUIS.get(id);
        return tui != null && tui.computer.getID() == computerId;
    }

    /** Pass keys, a resize, a redraw or a stop on to the player running the TUI. */
    public static void tuiControl(int id, JsonObject message) {
        var tui = TUIS.get(id);
        var server = CCAgent.server();
        if (tui == null || server == null) return;
        if (Json.str(message, "action", "").equals("stop")) TUIS.remove(id);
        var player = server.getPlayerList().getPlayer(tui.player);
        if (player == null) return;
        message.addProperty("type", "tui_ctl");
        message.addProperty("tui", id);
        send(player, message);
    }

    public static void stopTuis(int computerId) {
        for (var tui : List.copyOf(TUIS.values())) {
            if (tui.computer.getID() == computerId) tuiControl(tui.id, obj("action", "stop"));
        }
    }

    private static void serverTui(ServerPlayer player, JsonObject message) {
        var tui = TUIS.get(Json.integer(message, "tui", -1));
        if (tui == null || !tui.player.equals(player.getUUID())) return;
        switch (Json.str(message, "type", "")) {
            case "tui_frame" -> {
                var rows = new java.util.HashMap<Integer, List<String>>();
                if (message.has("rows") && message.get("rows").isJsonObject()) {
                    for (var e : message.getAsJsonObject("rows").entrySet()) {
                        if (!e.getValue().isJsonArray() || e.getValue().getAsJsonArray().size() != 3) continue;
                        var line = e.getValue().getAsJsonArray();
                        rows.put(Integer.parseInt(e.getKey()) + 1, new ArrayList<>(List.of(
                            line.get(0).getAsString(), line.get(1).getAsString(), line.get(2).getAsString())));
                    }
                }
                var palette = new java.util.HashMap<Integer, Integer>();
                if (message.has("palette") && message.get("palette").isJsonObject()) {
                    for (var e : message.getAsJsonObject("palette").entrySet()) {
                        int slot = Integer.parseInt(e.getKey());
                        if (slot >= 0 && slot < 16) palette.put(slot, e.getValue().getAsInt() & 0xFFFFFF);
                    }
                }
                tui.computer.queueEvent("ccagent_tui", tui.id, "frame", rows, palette,
                    Json.integer(message, "x", 0) + 1, Json.integer(message, "y", 0) + 1,
                    message.has("visible") && message.get("visible").getAsBoolean());
            }
            case "tui_exit" -> {
                TUIS.remove(tui.id);
                tui.computer.queueEvent("ccagent_tui", tui.id, "exit", Json.integer(message, "code", 0),
                    message.has("error") ? LuaStrings.toLua(Json.str(message, "error", "")) : null);
            }
            default -> {
            }
        }
    }

    private static void checkLaunch(ServerPlayer player) {
        var config = CCAgent.config();
        if (!serverEnabled()) throw new ToolException("this server has CC: Agent's relay turned off (relay=false)");
        if (config.relayOpsOnly && !McNames.isOp(player)) throw new ToolException("on this server only operators can use CC: Agent");
        if (!CCAgent.platform().hasRelay(player)) {
            throw new ToolException("install CC: Agent in your own game too: the agent runs on your PC, not the server");
        }
        var serverComputer = interactingComputer(player);
        if (serverComputer != null && serverComputer.getFamily() == ComputerFamily.COMMAND && !McNames.isOp(player)) {
            throw new ToolException("only operators can use agents on command computers");
        }
    }

    private static dan200.computercraft.shared.computer.core.ServerComputer interactingComputer(ServerPlayer player) {
        return player.containerMenu instanceof ComputerMenu menu ? menu.getComputer() : null;
    }

    public static boolean isRelayChat(int id) {
        return CHATS.containsKey(id);
    }

    public static void stopChat(int id) {
        var chat = CHATS.get(id);
        var server = CCAgent.server();
        if (chat == null || server == null) return;
        var player = server.getPlayerList().getPlayer(chat.player);
        if (player != null) send(player, obj("type", "stop", "chat", id));
    }

    static void serverStopped() {
        CHATS.clear();
        TUIS.clear();
        CALLS.clear();
    }

    // ---- Client side ----

    private static volatile Consumer<RelayPayload> clientSender;
    private static final Map<Integer, CompletableFuture<JsonObject>> PENDING = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> CLIENT_RUNS = new ConcurrentHashMap<>();
    private static volatile McpServer clientMcp;
    private static volatile AgentConfig clientConfig;
    private static volatile JsonArray toolCache;

    /** The loader's client glue hands over how to send a payload to the server. */
    public static void setClientSender(Consumer<RelayPayload> sender) {
        clientSender = sender;
    }

    private static void sendToServer(JsonObject message) {
        var sender = clientSender;
        if (sender == null) throw new ToolException("not connected to a server");
        for (var payload : encode(message)) sender.accept(payload);
    }

    public static void clientReceive(RelayPayload payload) {
        var message = assemble("server", payload);
        if (message == null) return;
        // Keys must reach the TUI in order.
        if (Json.str(message, "type", "").equals("tui_ctl")) {
            try {
                tuiControlHere(message);
            } catch (RuntimeException e) {
                CCAgent.LOG.warn("Relay TUI input from the server failed", e);
            }
            return;
        }
        POOL.execute(() -> {
            try {
                clientHandle(message);
            } catch (RuntimeException e) {
                CCAgent.LOG.warn("Relay message from the server failed", e);
            }
        });
    }

    private static void clientHandle(JsonObject message) {
        switch (Json.str(message, "type", "")) {
            case "hello" -> startClientMcp();
            case "result" -> {
                var future = PENDING.remove(Json.integer(message, "id", -1));
                if (future != null && message.has("result")) future.complete(message.getAsJsonObject("result"));
            }
            case "launch" -> launchHere(message);
            case "tui_launch" -> launchTuiHere(message);
            case "stop" -> {
                var run = CLIENT_RUNS.remove(Json.integer(message, "chat", -1));
                if (run != null) AgentRunner.stop(run);
            }
            default -> {
            }
        }
    }

    private static synchronized void startClientMcp() {
        if (clientMcp != null) return;
        clientConfig = AgentConfig.load(CCAgent.platform().configDir());
        toolCache = null;
        if (!clientConfig.enabled) return;
        try {
            clientMcp = McpServer.start(new RelayBackend(), clientConfig);
            CCAgent.LOG.info("Relaying MCP calls to this server; agents connect to {}", clientConfig.url());
        } catch (Exception e) {
            CCAgent.LOG.error("Couldn't start the MCP server on {}:{} for the relay", clientConfig.host, clientConfig.port, e);
        }
    }

    private static void launchHere(JsonObject message) {
        int chat = Json.integer(message, "chat", -1);
        AgentRunner.Sink sink = (run, kind, text) -> {
            try {
                sendToServer(obj("type", "chat", "chat", chat, "kind", kind, "text", text));
            } catch (ToolException ignored) {
            }
            if (kind.equals("done")) CLIENT_RUNS.remove(chat);
        };
        try {
            startClientMcp();
            if (clientMcp == null) throw new ToolException("CC: Agent's MCP server isn't running on your PC (port "
                + (clientConfig == null ? "?" : clientConfig.port) + " busy, or enabled=false)");
            int run = AgentRunner.start(sink, Json.integer(message, "computer", 0), Json.str(message, "label", null),
                Json.str(message, "agent", "claude"), Json.str(message, "prompt", ""), Json.str(message, "session", null),
                Json.integer(message, "width", 51),
                AgentRunner.Options.fromJson(message.has("options") && message.get("options").isJsonObject()
                    ? message.getAsJsonObject("options") : null), clientConfig, clientConfig.url());
            CLIENT_RUNS.put(chat, run);
        } catch (ToolException e) {
            sink.send(0, "error", e.getMessage());
            sink.send(0, "done", "");
        }
    }

    private static final Map<Integer, Integer> CLIENT_TUIS = new ConcurrentHashMap<>();

    private static void launchTuiHere(JsonObject message) {
        int tui = Json.integer(message, "tui", -1);
        TuiSession.Sink sink = new TuiSession.Sink() {
            @Override
            public void frame(int id, Map<Integer, String[]> rows, Map<Integer, Integer> palette, int x, int y, boolean visible) {
                var lines = new JsonObject();
                for (var e : rows.entrySet()) {
                    var line = new JsonArray();
                    for (var part : e.getValue()) line.add(part);
                    lines.add(String.valueOf(e.getKey()), line);
                }
                var colors = new JsonObject();
                for (var e : palette.entrySet()) colors.addProperty(String.valueOf(e.getKey()), e.getValue());
                trySend(obj("type", "tui_frame", "tui", tui, "rows", lines, "palette", colors, "x", x, "y", y,
                    "visible", visible));
            }

            @Override
            public void exit(int id, int code) {
                CLIENT_TUIS.remove(tui);
                trySend(obj("type", "tui_exit", "tui", tui, "code", code));
            }
        };
        try {
            startClientMcp();
            if (clientMcp == null) throw new ToolException("CC: Agent's MCP server isn't running on your PC (port "
                + (clientConfig == null ? "?" : clientConfig.port) + " busy, or enabled=false)");
            int session = TuiSession.start(sink, Json.integer(message, "computer", 0), Json.str(message, "label", null),
                Json.str(message, "agent", "claude"), Json.integer(message, "cols", 80), Json.integer(message, "rows", 24),
                Json.str(message, "monitor", null), clientConfig, clientConfig.url());
            CLIENT_TUIS.put(tui, session);
        } catch (ToolException e) {
            CCAgent.LOG.warn("Couldn't start a TUI for the server: {}", e.getMessage());
            trySend(obj("type", "tui_exit", "tui", tui, "code", -1, "error", e.getMessage()));
        }
    }

    private static void tuiControlHere(JsonObject message) {
        int tui = Json.integer(message, "tui", -1);
        var local = CLIENT_TUIS.get(tui);
        if (local == null) return;
        var session = TuiSession.get(local);
        switch (Json.str(message, "action", "")) {
            case "input" -> {
                if (session == null) return;
                var kind = Json.str(message, "kind", "");
                Object value = kind.equals("char") || kind.equals("paste") ? Json.str(message, "value", "")
                    : (Object) Json.integer(message, "value", 0);
                session.input(kind, value, Json.integer(message, "x", 0), Json.integer(message, "y", 0));
            }
            case "resize" -> {
                if (session != null) session.resize(Json.integer(message, "cols", 80), Json.integer(message, "rows", 24));
            }
            case "redraw" -> {
                if (session != null) session.redraw();
            }
            case "scroll" -> {
                if (session != null) session.scroll(Json.integer(message, "lines", 0));
            }
            case "stop" -> {
                CLIENT_TUIS.remove(tui);
                TuiSession.stop(local);
            }
            default -> {
            }
        }
    }

    private static void trySend(JsonObject message) {
        try {
            sendToServer(message);
        } catch (ToolException ignored) {
            // Disconnected; the TUI is stopped with the connection.
        }
    }

    public static synchronized void clientDisconnected() {
        clientSender = null;
        for (var session : CLIENT_TUIS.values()) TuiSession.stop(session);
        CLIENT_TUIS.clear();
        for (var run : CLIENT_RUNS.values()) AgentRunner.stop(run);
        CLIENT_RUNS.clear();
        for (var future : PENDING.values()) future.complete(McpServer.toolResult("disconnected from the server", true));
        PENDING.clear();
        if (clientMcp != null) {
            clientMcp.stop();
            clientMcp = null;
        }
        PARTIALS.clear();
    }

    /** The client's MCP backend: every call goes to the server. */
    private static final class RelayBackend implements McpServer.Backend {
        @Override
        public JsonArray tools() {
            var cached = toolCache;
            if (cached != null) return cached;
            var result = ask("__list", new JsonObject(), 30);
            if (result.has("tools")) {
                toolCache = result.getAsJsonArray("tools");
                return toolCache;
            }
            return new JsonArray();
        }

        @Override
        public JsonObject call(String name, JsonObject args) {
            // The server answers questions on the computer whose chat this agent runs in.
            var chat = Questions.CHAT.get();
            if (chat != null) {
                args = args.deepCopy();
                args.addProperty("__chat", chat);
            }
            return ask(name, args, 660);
        }

        private JsonObject ask(String name, JsonObject args, int timeout) {
            int id = NEXT_ID.getAndIncrement();
            var future = new CompletableFuture<JsonObject>();
            PENDING.put(id, future);
            try {
                sendToServer(obj("type", "call", "id", id, "tool", name, "args", args));
                return future.get(timeout, TimeUnit.SECONDS);
            } catch (ToolException e) {
                return McpServer.toolResult(e.getMessage(), true);
            } catch (Exception e) {
                return McpServer.toolResult("the server didn't answer: " + e, true);
            } finally {
                PENDING.remove(id);
            }
        }
    }
}
