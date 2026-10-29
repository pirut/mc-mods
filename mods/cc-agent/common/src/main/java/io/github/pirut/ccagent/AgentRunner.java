package io.github.pirut.ccagent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dan200.computercraft.api.lua.IComputerSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs Claude Code or Codex on the player's machine for the in-game claude/codex programs, signed in
 * as the player, and streams what it says and does back to the computer as ccagent_chat events.
 * The agents only get CC: Agent's MCP tools (plus web search to look up docs).
 */
public final class AgentRunner {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static final Map<Integer, Run> RUNS = new ConcurrentHashMap<>();
    private static final String TOOL_PREFIX = "mcp__cc-agent__";

    private record Run(int id, int computerId, Process process) {
    }

    private AgentRunner() {
    }

    /** Start a turn. Returns the run id; events follow on the computer. */
    public static int start(IComputerSystem computer, String agent, String prompt, String session, int width) {
        var config = CCAgent.config();
        var server = CCAgent.server();
        if (config == null || server == null) throw new ToolException("no world is open");
        if (server.isDedicatedServer()) throw new ToolException("the claude and codex programs only work in singleplayer");
        if (!config.launchAgents) throw new ToolException("launchAgents=false in config/ccagent.properties");
        if (!CCAgent.running()) throw new ToolException("CC: Agent's MCP server isn't running (see /ccagent)");
        for (var run : RUNS.values()) {
            if (run.computerId == computer.getID()) throw new ToolException("this computer already has an agent working");
        }

        boolean claude = agent.equals("claude");
        if (!claude && !agent.equals("codex")) throw new ToolException("agent must be claude or codex");
        var exe = find(claude ? config.claudeCommand : config.codexCommand);
        if (exe == null) {
            throw new ToolException("couldn't find " + (claude ? config.claudeCommand : config.codexCommand)
                + ". Install it, or set " + agent + "Command in config/ccagent.properties to its full path.");
        }

        var workDir = CCAgent.gameDir().resolve("ccagent").resolve("workspace");
        List<String> cmd = new ArrayList<>();
        try {
            Files.createDirectories(workDir);
            if (claude) {
                var mcp = workDir.resolve("mcp.json");
                var servers = new JsonObject();
                servers.add("cc-agent", Json.obj("type", "http", "url", config.url(),
                    "headers", Json.obj("Authorization", "Bearer " + config.token)));
                Files.writeString(mcp, Json.PRETTY.toJson(Json.obj("mcpServers", servers)));
                cmd.addAll(List.of("-p", "--output-format", "stream-json", "--verbose", "--strict-mcp-config",
                    "--mcp-config", mcp.toString(), "--allowedTools", TOOL_PREFIX + "*,WebSearch,WebFetch"));
                if (session != null) cmd.addAll(List.of("--resume", session));
                cmd.addAll(split(config.claudeArgs));
            } else {
                cmd.addAll(List.of("exec", "--json", "--skip-git-repo-check", "--sandbox", "read-only",
                    "-c", "approval_policy=never",
                    "-c", "mcp_servers.cc-agent.url=" + config.url(),
                    "-c", "mcp_servers.cc-agent.bearer_token_env_var=CC_AGENT_TOKEN"));
                cmd.addAll(split(config.codexArgs));
                if (session != null) cmd.addAll(List.of("resume", session));
                cmd.add("-");
            }
        } catch (IOException e) {
            throw new ToolException("couldn't set up the agent folder: " + e.getMessage());
        }

        var full = launcher(exe);
        full.addAll(cmd);
        var builder = new ProcessBuilder(full).directory(workDir.toFile()).redirectErrorStream(false);
        builder.environment().put("CC_AGENT_TOKEN", config.token);
        builder.environment().put("PATH", searchPath(exe));
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new ToolException("couldn't start " + exe + ": " + e.getMessage());
        }

        var run = new Run(NEXT_ID.getAndIncrement(), computer.getID(), process);
        RUNS.put(run.id, run);
        var text = session == null ? context(computer, width) + prompt : prompt;
        var thread = new Thread(() -> pump(run, computer, claude, text), "CC: Agent " + agent + " " + run.id);
        thread.setDaemon(true);
        thread.start();
        return run.id;
    }

    public static void stop(int id) {
        var run = RUNS.remove(id);
        if (run != null) kill(run.process);
    }

    static void stopAll() {
        for (var id : List.copyOf(RUNS.keySet())) stop(id);
    }

    private static void kill(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private static String context(IComputerSystem computer, int width) {
        var label = computer.getLabel() == null ? "" : " (label \"" + computer.getLabel() + "\")";
        return "[The player is talking to you from inside Minecraft, through the chat program on CC: Tweaked computer #"
            + computer.getID() + label + ". Act in the world with the cc-agent MCP tools. That computer's screen is "
            + "showing this chat: don't use type_text or press_keys on it, and don't reboot or shut it down unless "
            + "asked; run things on it headless with run_lua/run_shell, or use other computers. When a peripheral "
            + "comes from an addon mod, call describe_peripheral and look up that mod's docs online. Your replies "
            + "show on a " + width + "-column screen: keep them short, plain text, no tables or markdown headings.]\n\n";
    }

    private static void pump(Run run, IComputerSystem computer, boolean claude, String prompt) {
        var process = run.process;
        try (OutputStream in = process.getOutputStream()) {
            in.write(prompt.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            send(computer, run, "error", "couldn't send the prompt: " + e.getMessage());
        }
        // Keep the last lines of stderr for error messages.
        var errors = new StringBuilder();
        var errThread = new Thread(() -> {
            try (var err = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = err.readLine()) != null) {
                    synchronized (errors) {
                        errors.append(line).append('\n');
                        if (errors.length() > 4000) errors.delete(0, errors.length() - 4000);
                    }
                }
            } catch (IOException ignored) {
            }
        });
        errThread.setDaemon(true);
        errThread.start();

        var state = new State();
        try (var out = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = out.readLine()) != null) {
                JsonElement json;
                try {
                    json = JsonParser.parseString(line);
                } catch (RuntimeException e) {
                    continue;
                }
                if (!json.isJsonObject()) continue;
                if (claude) claudeEvent(computer, run, json.getAsJsonObject(), state);
                else codexEvent(computer, run, json.getAsJsonObject(), state);
            }
            int code = process.waitFor();
            errThread.join(1000);
            if (!RUNS.containsKey(run.id)) {
                send(computer, run, "done", "stopped");
                return;
            }
            if (code != 0 && !state.failed) {
                String tail;
                synchronized (errors) {
                    tail = errors.toString().trim();
                }
                var lines = tail.split("\n");
                var last = lines.length == 0 ? "" : lines[lines.length - 1];
                send(computer, run, "error", "exited with code " + code + (last.isEmpty() ? "" : ": " + last)
                    + (tail.toLowerCase(Locale.ROOT).contains("login") || tail.toLowerCase(Locale.ROOT).contains("auth")
                    ? " (sign in once in a terminal first)" : ""));
            }
            send(computer, run, "done", state.footer());
        } catch (IOException | InterruptedException e) {
            send(computer, run, "error", e.toString());
            send(computer, run, "done", "");
        } finally {
            RUNS.remove(run.id);
            if (process.isAlive()) kill(process);
        }
    }

    private static final class State {
        String session;
        boolean failed;

        String footer() {
            return "";
        }
    }

    private static void claudeEvent(IComputerSystem computer, Run run, JsonObject event, State state) {
        var type = str(event, "type");
        switch (type) {
            case "system" -> {
                if ("init".equals(str(event, "subtype")) && event.has("session_id")) {
                    state.session = str(event, "session_id");
                    send(computer, run, "session", state.session);
                }
            }
            case "assistant" -> {
                var message = event.getAsJsonObject("message");
                if (message == null || !message.has("content")) return;
                for (var part : message.getAsJsonArray("content")) {
                    var p = part.getAsJsonObject();
                    switch (str(p, "type")) {
                        case "text" -> {
                            var text = str(p, "text").trim();
                            if (!text.isEmpty()) send(computer, run, "text", text);
                        }
                        case "tool_use" -> {
                            var name = str(p, "name");
                            if (name.equals("ToolSearch")) continue;
                            var args = p.has("input") && p.get("input").isJsonObject() ? p.getAsJsonObject("input") : new JsonObject();
                            send(computer, run, "tool", toolLine(name, args));
                        }
                        default -> {
                        }
                    }
                }
            }
            case "user" -> {
                var message = event.getAsJsonObject("message");
                if (message == null || !message.has("content") || !message.get("content").isJsonArray()) return;
                for (var part : message.getAsJsonArray("content")) {
                    if (!part.isJsonObject()) continue;
                    var p = part.getAsJsonObject();
                    if ("tool_result".equals(str(p, "type")) && p.has("is_error") && p.get("is_error").getAsBoolean()) {
                        send(computer, run, "tool_error", firstLine(contentText(p.get("content"))));
                    }
                }
            }
            case "result" -> {
                if (event.has("session_id")) {
                    state.session = str(event, "session_id");
                    send(computer, run, "session", state.session);
                }
                if (event.has("is_error") && event.get("is_error").getAsBoolean()) {
                    state.failed = true;
                    send(computer, run, "error", firstLine(str(event, "result")));
                }
            }
            default -> {
            }
        }
    }

    private static void codexEvent(IComputerSystem computer, Run run, JsonObject event, State state) {
        var type = str(event, "type");
        switch (type) {
            case "thread.started" -> {
                state.session = str(event, "thread_id");
                send(computer, run, "session", state.session);
            }
            case "item.started", "item.completed" -> {
                var item = event.getAsJsonObject("item");
                if (item == null) return;
                var kind = str(item, "type");
                boolean done = type.equals("item.completed");
                switch (kind) {
                    case "agent_message" -> {
                        if (done && !str(item, "text").isBlank()) send(computer, run, "text", str(item, "text").trim());
                    }
                    case "mcp_tool_call" -> {
                        if (!done) {
                            var args = item.has("arguments") && item.get("arguments").isJsonObject() ? item.getAsJsonObject("arguments") : new JsonObject();
                            send(computer, run, "tool", toolLine(TOOL_PREFIX + str(item, "tool"), args));
                        } else if ("failed".equals(str(item, "status"))) {
                            send(computer, run, "tool_error", firstLine(item.has("error") ? item.get("error").toString() : "failed"));
                        }
                    }
                    case "command_execution" -> {
                        if (!done) send(computer, run, "tool", "$ " + shorten(str(item, "command"), 80));
                    }
                    case "web_search" -> {
                        if (!done) send(computer, run, "tool", "search " + shorten(str(item, "query"), 80));
                    }
                    case "error" -> send(computer, run, "error", firstLine(str(item, "message")));
                    default -> {
                    }
                }
            }
            case "turn.failed" -> {
                state.failed = true;
                var error = event.getAsJsonObject("error");
                send(computer, run, "error", firstLine(error == null ? "turn failed" : str(error, "message")));
            }
            case "error" -> {
                state.failed = true;
                send(computer, run, "error", firstLine(str(event, "message")));
            }
            default -> {
            }
        }
    }

    /** "write_file /dashboard.lua" and the like, for the chat screen. */
    static String toolLine(String name, JsonObject args) {
        if (!name.startsWith(TOOL_PREFIX)) {
            var detail = name.equals("WebSearch") ? Json.str(args, "query", "") : name.equals("WebFetch") ? Json.str(args, "url", "") : "";
            return shorten((name.equals("WebSearch") ? "search " : name.equals("WebFetch") ? "read " : name + " ") + detail, 120);
        }
        var tool = name.substring(TOOL_PREFIX.length());
        var computer = Json.str(args, "computer", null);
        var summary = Tools.summary(tool, args);
        return tool + (computer == null ? "" : " #" + computer) + (summary.isEmpty() ? "" : " " + summary);
    }

    private static String contentText(JsonElement content) {
        if (content == null) return "";
        if (content.isJsonPrimitive()) return content.getAsString();
        if (content.isJsonArray()) {
            for (var part : content.getAsJsonArray()) {
                if (part.isJsonObject() && part.getAsJsonObject().has("text")) return part.getAsJsonObject().get("text").getAsString();
            }
        }
        return content.toString();
    }

    private static void send(IComputerSystem computer, Run run, String kind, String text) {
        computer.queueEvent("ccagent_chat", run.id, kind, LuaStrings.toLua(text == null ? "" : text));
    }

    private static String str(JsonObject o, String key) {
        var v = o.get(key);
        return v == null || v.isJsonNull() ? "" : v.isJsonPrimitive() ? v.getAsString() : v.toString();
    }

    private static String firstLine(String text) {
        if (text == null) return "";
        var t = text.trim();
        int nl = t.indexOf('\n');
        return shorten(nl >= 0 ? t.substring(0, nl) : t, 200);
    }

    private static String shorten(String text, int max) {
        var t = text.replaceAll("\\s+", " ").trim();
        return t.length() > max ? t.substring(0, max - 3) + "..." : t;
    }

    private static List<String> split(String args) {
        var out = new ArrayList<String>();
        if (args == null || args.isBlank()) return out;
        var m = java.util.regex.Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(args);
        while (m.find()) out.add(m.group(1) != null ? m.group(1) : m.group(2));
        return out;
    }

    private static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /** npm installs .cmd shims on Windows, which only cmd.exe can start. */
    private static List<String> launcher(Path exe) {
        var name = exe.getFileName().toString().toLowerCase(Locale.ROOT);
        var out = new ArrayList<String>();
        if (windows() && (name.endsWith(".cmd") || name.endsWith(".bat"))) out.addAll(List.of("cmd.exe", "/c"));
        out.add(exe.toString());
        return out;
    }

    /**
     * Where to look for the CLIs. Games started from a launcher often get a bare PATH (macOS apps
     * especially), so add the usual install folders too.
     */
    private static List<Path> searchDirs() {
        Set<Path> dirs = new LinkedHashSet<>();
        var path = System.getenv("PATH");
        if (path != null) {
            for (var part : path.split(java.io.File.pathSeparator)) {
                if (!part.isBlank()) dirs.add(Path.of(part));
            }
        }
        var home = Path.of(System.getProperty("user.home"));
        dirs.add(home.resolve(".local/bin"));
        dirs.add(home.resolve(".claude/local"));
        dirs.add(home.resolve(".npm-global/bin"));
        dirs.add(home.resolve(".bun/bin"));
        dirs.add(home.resolve(".volta/bin"));
        dirs.add(home.resolve(".cargo/bin"));
        dirs.add(Path.of("/opt/homebrew/bin"));
        dirs.add(Path.of("/usr/local/bin"));
        dirs.add(Path.of("/usr/bin"));
        var appData = System.getenv("APPDATA");
        if (appData != null) dirs.add(Path.of(appData, "npm"));
        var nvm = home.resolve(".nvm/versions/node");
        if (Files.isDirectory(nvm)) {
            try (var versions = Files.list(nvm)) {
                versions.sorted(java.util.Comparator.reverseOrder()).forEach(v -> dirs.add(v.resolve("bin")));
            } catch (IOException ignored) {
            }
        }
        return new ArrayList<>(dirs);
    }

    private static Path find(String command) {
        if (command.isBlank()) return null;
        var direct = Path.of(command);
        if (direct.isAbsolute()) return Files.isRegularFile(direct) ? direct : null;
        var names = windows() ? List.of(command + ".exe", command + ".cmd", command + ".bat", command) : List.of(command);
        for (var dir : searchDirs()) {
            for (var name : names) {
                var candidate = dir.resolve(name);
                if (Files.isRegularFile(candidate) && (windows() || Files.isExecutable(candidate))) return candidate;
            }
        }
        return null;
    }

    /** PATH for the child: its own folder first (node shims sit next to node), then the usual folders. */
    private static String searchPath(Path exe) {
        var parts = new ArrayList<String>();
        if (exe.getParent() != null) parts.add(exe.getParent().toString());
        for (var dir : searchDirs()) parts.add(dir.toString());
        return String.join(java.io.File.pathSeparator, new LinkedHashSet<>(parts));
    }
}
