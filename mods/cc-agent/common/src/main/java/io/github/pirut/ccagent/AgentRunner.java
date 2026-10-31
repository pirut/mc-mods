package io.github.pirut.ccagent;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    /** Chat settings: model and effort (null for the CLI's own default) and the permission mode. */
    public record Options(String model, String effort, String mode) {
        public static final Options DEFAULT = new Options(null, null, "auto");

        static String clean(Object value) {
            if (value == null) return null;
            var text = value.toString().trim();
            return text.isEmpty() || text.equals("default") ? null : text;
        }

        public static Options of(Object model, Object effort, Object mode) {
            var m = clean(mode);
            return new Options(clean(model), clean(effort), m == null ? "auto" : m);
        }

        public static Options fromJson(com.google.gson.JsonObject json) {
            if (json == null) return DEFAULT;
            return of(Json.str(json, "model", null), Json.str(json, "effort", null), Json.str(json, "mode", null));
        }

        public com.google.gson.JsonObject toJson() {
            var out = new com.google.gson.JsonObject();
            if (model != null) out.addProperty("model", model);
            if (effort != null) out.addProperty("effort", effort);
            out.addProperty("mode", mode);
            return out;
        }
    }

    /** CC: Agent tools that only look: allowed without asking in the chat's "ask" and "plan" modes. */
    private static final List<String> LOOK_TOOLS = List.of("list_computers", "computer_info", "read_screen", "get_job",
        "list_files", "read_file", "list_peripherals", "describe_peripheral", "turtle_inventory", "inspect_block",
        "scan_area", "watch_events", "cc_help", "ask_player");

    /** Where a run's chat lines go: straight to the computer, or back over the relay. */
    public interface Sink {
        void send(int run, String kind, String text);
    }

    private record Run(int id, int computerId, Process process, Sink sink) {
    }

    private AgentRunner() {
    }

    /**
     * Start a turn on this machine. Returns the run id; chat lines follow on the sink.
     * The caller decides whether this machine may run agents at all.
     */
    public static int start(Sink sink, int computerId, String label, String agent, String prompt, String session, int width,
                            Options options, AgentConfig config, String mcpUrl) {
        if (options == null) options = Options.DEFAULT;
        if (!config.launchAgents) throw new ToolException("launchAgents=false in config/ccagent.properties");
        for (var run : RUNS.values()) {
            if (run.computerId == computerId) throw new ToolException("this computer already has an agent working");
        }

        boolean claude = agent.equals("claude");
        if (!claude && !agent.equals("codex")) throw new ToolException("agent must be claude or codex");
        // These come from the computer (any Lua program can set them) or, in multiplayer, from the server, and
        // end up on the command line. On Windows the npm shims go through cmd.exe, which would run "&calc".
        checkArg("model", options.model());
        checkArg("effort", options.effort());
        checkArg("session", session);
        var exe = find(claude ? config.claudeCommand : config.codexCommand);
        if (exe == null) {
            throw new ToolException("couldn't find " + (claude ? config.claudeCommand : config.codexCommand)
                + ". Run \"" + (windows() ? "where " : "which ") + agent + "\" in a terminal and put that path in "
                + agent + "Command= in config/ccagent.properties (forward slashes), then reopen the world.");
        }

        var workDir = CCAgent.gameDir().resolve("ccagent").resolve("workspace");
        List<String> cmd = new ArrayList<>();
        try {
            Files.createDirectories(workDir);
            if (claude) {
                var mcp = workDir.resolve("mcp-" + computerId + ".json");
                var servers = new JsonObject();
                servers.add("cc-agent", Json.obj("type", "http", "url", mcpUrl,
                    "headers", Json.obj("Authorization", "Bearer " + config.token, Questions.CHAT_HEADER, String.valueOf(computerId))));
                Files.writeString(mcp, Json.PRETTY.toJson(Json.obj("mcpServers", servers)));
                // Only web search among Claude Code's own tools; anything not allowed up front asks the player
                // through permission_prompt, which shows Allow/Deny buttons in the chat.
                String allowed = switch (options.mode()) {
                    case "ask", "plan" -> String.join(",", LOOK_TOOLS.stream().map(t -> TOOL_PREFIX + t).toList()) + ",WebSearch,WebFetch";
                    default -> TOOL_PREFIX + "*,WebSearch,WebFetch";
                };
                cmd.addAll(List.of("-p", "--output-format", "stream-json", "--verbose", "--strict-mcp-config",
                    "--mcp-config", mcp.toString(),
                    // Plan mode writes its plan to a file in ~/.claude/plans, then asks with ExitPlanMode.
                    "--tools", "WebSearch,WebFetch,TodoWrite" + (options.mode().equals("plan") ? ",Write,ExitPlanMode" : ""),
                    "--allowedTools", allowed + ",TodoWrite",
                    "--permission-prompt-tool", TOOL_PREFIX + "permission_prompt"));
                if (options.mode().equals("plan")) cmd.addAll(List.of("--permission-mode", "plan"));
                if (options.model() != null) cmd.addAll(List.of("--model", options.model()));
                if (options.effort() != null) cmd.addAll(List.of("--effort", options.effort()));
                if (session != null) cmd.addAll(List.of("--resume", session));
                cmd.addAll(split(config.claudeArgs));
            } else {
                cmd.addAll(List.of("exec", "--json", "--skip-git-repo-check", "--sandbox", "read-only",
                    "-c", "approval_policy=never",
                    "-c", "mcp_servers.cc-agent.url=" + mcpUrl,
                    "-c", "mcp_servers.cc-agent.bearer_token_env_var=CC_AGENT_TOKEN",
                    // With approval_policy=never Codex refuses MCP calls that need approval: approve ours up front.
                    "-c", "mcp_servers.cc-agent.default_tools_approval_mode=approve",
                    "-c", "mcp_servers.cc-agent.http_headers={\"" + Questions.CHAT_HEADER + "\"=\"" + computerId + "\"}"));
                if (options.model() != null) cmd.addAll(List.of("-m", options.model()));
                if (options.effort() != null) cmd.addAll(List.of("-c", "model_reasoning_effort=" + options.effort()));
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

        var run = new Run(NEXT_ID.getAndIncrement(), computerId, process, sink);
        RUNS.put(run.id, run);
        var text = session == null ? context(computerId, label, width) + prompt : prompt;
        var thread = new Thread(() -> pump(run, claude, text), "CC: Agent " + agent + " " + run.id);
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

    private static String context(int computerId, String computerLabel, int width) {
        var label = computerLabel == null ? "" : " (label \"" + computerLabel + "\")";
        return "[The player is talking to you from inside Minecraft, through the chat program on CC: Tweaked computer #"
            + computerId + label + ". Act in the world with the cc-agent MCP tools. That computer's screen is "
            + "showing this chat: don't use type_text or press_keys on it, and don't reboot or shut it down unless "
            + "asked; run things on it headless with run_lua/run_shell, or use other computers. To ask the player something, "
            + "use ask_player: it shows your question with buttons on that computer. When a peripheral "
            + "comes from an addon mod, call describe_peripheral and look up that mod's docs online. Your replies "
            + "show on a " + width + "-column screen: keep them short, plain text, no tables or markdown headings.]\n\n";
    }

    private static void pump(Run run, boolean claude, String prompt) {
        var process = run.process;
        try (OutputStream in = process.getOutputStream()) {
            in.write(prompt.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            send(run, "error", "couldn't send the prompt: " + e.getMessage());
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
                if (claude) claudeEvent(run, json.getAsJsonObject(), state);
                else codexEvent(run, json.getAsJsonObject(), state);
            }
            int code = process.waitFor();
            errThread.join(1000);
            if (!RUNS.containsKey(run.id)) {
                send(run, "done", "stopped");
                return;
            }
            if (code != 0 && !state.failed) {
                String tail;
                synchronized (errors) {
                    tail = errors.toString().trim();
                }
                var lines = tail.split("\n");
                var last = lines.length == 0 ? "" : lines[lines.length - 1];
                send(run, "error", "exited with code " + code + (last.isEmpty() ? "" : ": " + last)
                    + (tail.toLowerCase(Locale.ROOT).contains("login") || tail.toLowerCase(Locale.ROOT).contains("auth")
                    ? " (sign in once in a terminal first)" : ""));
            }
            send(run, "done", state.footer());
        } catch (IOException | InterruptedException e) {
            send(run, "error", e.toString());
            send(run, "done", "");
        } finally {
            RUNS.remove(run.id);
            if (process.isAlive()) kill(process);
        }
    }

    private static final class State {
        String session;
        boolean failed;
        boolean sawText;
        String model;
        long context;

        /** "sonnet-5-5, 31k context" for the chat's line after a reply. */
        String footer() {
            var parts = new ArrayList<String>();
            if (model != null) parts.add(model.replaceFirst("^claude-", ""));
            if (context > 0) parts.add(context >= 1000 ? (context + 500) / 1000 + "k context" : context + " context");
            return String.join(", ", parts);
        }
    }

    /** A todo list as chat lines: "[x] done", "[>] doing", "[ ] to do". */
    private static String todos(JsonArray items, String textKey, String statusKey) {
        var out = new StringBuilder();
        for (var item : items) {
            if (!item.isJsonObject()) continue;
            var o = item.getAsJsonObject();
            String mark;
            if (statusKey.equals("completed")) mark = o.has("completed") && o.get("completed").getAsBoolean() ? "x" : " ";
            else mark = switch (str(o, statusKey)) {
                case "completed" -> "x";
                case "in_progress" -> ">";
                default -> " ";
            };
            if (out.length() > 0) out.append('\n');
            out.append('[').append(mark).append("] ").append(str(o, textKey));
        }
        return out.toString();
    }

    private static void claudeEvent(Run run, JsonObject event, State state) {
        var type = str(event, "type");
        switch (type) {
            case "system" -> {
                if ("init".equals(str(event, "subtype")) && event.has("session_id")) {
                    state.session = str(event, "session_id");
                    send(run, "session", state.session);
                    if (event.has("model")) state.model = str(event, "model");
                }
            }
            case "assistant" -> {
                var message = event.getAsJsonObject("message");
                if (message == null || !message.has("content")) return;
                if (message.has("usage") && message.get("usage").isJsonObject()) {
                    var usage = message.getAsJsonObject("usage");
                    long context = 0;
                    for (var key : new String[]{"input_tokens", "cache_read_input_tokens", "cache_creation_input_tokens", "output_tokens"}) {
                        if (usage.has(key) && usage.get(key).isJsonPrimitive()) context += usage.get(key).getAsLong();
                    }
                    if (context > 0) state.context = context;
                }
                for (var part : message.getAsJsonArray("content")) {
                    var p = part.getAsJsonObject();
                    switch (str(p, "type")) {
                        case "text" -> {
                            var text = str(p, "text").trim();
                            if (!text.isEmpty()) {
                                state.sawText = true;
                                send(run, "text", text);
                            }
                        }
                        case "thinking" -> {
                            var text = str(p, "thinking").trim();
                            if (!text.isEmpty()) send(run, "thinking", shorten(firstLine(text), 160));
                        }
                        case "tool_use" -> {
                            var name = str(p, "name");
                            if (name.equals("ToolSearch")) continue;
                            var args = p.has("input") && p.get("input").isJsonObject() ? p.getAsJsonObject("input") : new JsonObject();
                            var path = Json.str(args, "file_path", "").replace('\\', '/');
                            if (name.equals("Write") && path.contains("/.claude/plans/")) {
                                send(run, "plan", Json.str(args, "content", "").trim());
                            } else if (name.equals("ExitPlanMode") && !Json.str(args, "plan", "").isBlank()) {
                                send(run, "plan", Json.str(args, "plan", "").trim());
                            } else if (name.equals("TodoWrite") && args.has("todos") && args.get("todos").isJsonArray()) {
                                send(run, "todos", todos(args.getAsJsonArray("todos"), "content", "status"));
                            } else if (!name.equals("ExitPlanMode")) {
                                send(run, "tool", toolLine(name, args));
                            }
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
                        send(run, "tool_error", firstLine(contentText(p.get("content"))));
                    }
                }
            }
            case "result" -> {
                if (event.has("session_id")) {
                    state.session = str(event, "session_id");
                    send(run, "session", state.session);
                }
                if (event.has("is_error") && event.get("is_error").getAsBoolean()) {
                    state.failed = true;
                    send(run, "error", firstLine(str(event, "result")));
                } else if (!state.sawText && !str(event, "result").isBlank()) {
                    // Slash commands like /context answer only in the result.
                    send(run, "text", str(event, "result").trim());
                }
            }
            default -> {
            }
        }
    }

    private static void codexEvent(Run run, JsonObject event, State state) {
        var type = str(event, "type");
        switch (type) {
            case "thread.started" -> {
                state.session = str(event, "thread_id");
                send(run, "session", state.session);
            }
            case "item.started", "item.completed" -> {
                var item = event.getAsJsonObject("item");
                if (item == null) return;
                var kind = str(item, "type");
                boolean done = type.equals("item.completed");
                switch (kind) {
                    case "agent_message" -> {
                        if (done && !str(item, "text").isBlank()) send(run, "text", str(item, "text").trim());
                    }
                    case "mcp_tool_call" -> {
                        if (!done) {
                            var args = item.has("arguments") && item.get("arguments").isJsonObject() ? item.getAsJsonObject("arguments") : new JsonObject();
                            send(run, "tool", toolLine(TOOL_PREFIX + str(item, "tool"), args));
                        } else if ("failed".equals(str(item, "status"))) {
                            send(run, "tool_error", firstLine(item.has("error") ? item.get("error").toString() : "failed"));
                        }
                    }
                    case "command_execution" -> {
                        if (!done) send(run, "tool", "$ " + shorten(str(item, "command"), 80));
                    }
                    case "web_search" -> {
                        if (!done) send(run, "tool", "search " + shorten(str(item, "query"), 80));
                    }
                    case "reasoning" -> {
                        if (done && !str(item, "text").isBlank()) send(run, "thinking", shorten(firstLine(str(item, "text").trim()), 160));
                    }
                    case "todo_list" -> {
                        if (item.has("items") && item.get("items").isJsonArray()) {
                            send(run, "todos", todos(item.getAsJsonArray("items"), "text", "completed"));
                        }
                    }
                    case "error" -> send(run, "error", firstLine(str(item, "message")));
                    default -> {
                    }
                }
            }
            case "turn.failed" -> {
                state.failed = true;
                var error = event.getAsJsonObject("error");
                send(run, "error", firstLine(error == null ? "turn failed" : str(error, "message")));
            }
            case "error" -> {
                state.failed = true;
                send(run, "error", firstLine(str(event, "message")));
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

    private static void send(Run run, String kind, String text) {
        run.sink.send(run.id, kind, text == null ? "" : text);
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

    /** Model names, effort levels and session ids: letters, digits and . _ - : @ / [ ] only. */
    private static final java.util.regex.Pattern SAFE_ARG = java.util.regex.Pattern.compile("[A-Za-z0-9._:@/\\[\\]-]{1,128}");

    static void checkArg(String what, String value) {
        if (value != null && !SAFE_ARG.matcher(value).matches()) throw new ToolException("that " + what + " isn't valid: " + value);
    }

    static List<String> split(String args) {
        var out = new ArrayList<String>();
        if (args == null || args.isBlank()) return out;
        var m = java.util.regex.Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(args);
        while (m.find()) out.add(m.group(1) != null ? m.group(1) : m.group(2));
        return out;
    }

    static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /** npm installs .cmd shims on Windows, which only cmd.exe can start. */
    static List<String> launcher(Path exe) {
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
        dirs.add(Path.of("/Applications/Codex.app/Contents/Resources"));
        dirs.add(home.resolve("Applications/Codex.app/Contents/Resources"));
        var appData = System.getenv("APPDATA");
        if (appData != null) dirs.add(Path.of(appData, "npm"));
        var localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            dirs.add(Path.of(localAppData, "Microsoft", "WinGet", "Links"));
            dirs.add(Path.of(localAppData, "Programs", "claude"));
            dirs.add(Path.of(localAppData, "pnpm"));
        }
        var nvmSymlink = System.getenv("NVM_SYMLINK");
        if (nvmSymlink != null) dirs.add(Path.of(nvmSymlink));
        dirs.add(Path.of("C:/nvm4w/nodejs"));
        var programFiles = System.getenv("ProgramFiles");
        if (programFiles != null) dirs.add(Path.of(programFiles, "nodejs"));
        dirs.add(home.resolve("scoop/shims"));
        dirs.add(home.resolve("AppData/Local/Volta/bin"));
        dirs.addAll(SHELL_PATH.get());
        var nvm = home.resolve(".nvm/versions/node");
        if (Files.isDirectory(nvm)) {
            try (var versions = Files.list(nvm)) {
                versions.sorted(java.util.Comparator.reverseOrder()).forEach(v -> dirs.add(v.resolve("bin")));
            } catch (IOException ignored) {
            }
        }
        return new ArrayList<>(dirs);
    }

    /**
     * The PATH a login shell would give (macOS and Linux): apps started from the Dock or a launcher
     * don't get the PATH set up in .zshrc or .bashrc, which is where node version managers live.
     */
    private static final java.util.function.Supplier<List<Path>> SHELL_PATH = memoize(() -> {
        var out = new ArrayList<Path>();
        if (windows()) return out;
        var shell = System.getenv("SHELL");
        if (shell == null || shell.isBlank()) shell = Files.isExecutable(Path.of("/bin/zsh")) ? "/bin/zsh" : "/bin/bash";
        try {
            var process = new ProcessBuilder(shell, "-ilc", "echo __CCAGENT__$PATH").redirectErrorStream(true).start();
            process.getOutputStream().close();
            // A shell that hangs (a prompt asking for input) must not hang the game.
            if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return out;
            }
            var text = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int at = text.lastIndexOf("__CCAGENT__");
            if (at >= 0) {
                var line = text.substring(at + "__CCAGENT__".length()).split("\\R", 2)[0];
                for (var part : line.split(":")) if (!part.isBlank()) out.add(Path.of(part.trim()));
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            CCAgent.LOG.debug("Couldn't ask the login shell for PATH", e);
        }
        return out;
    });

    private static <T> java.util.function.Supplier<T> memoize(java.util.function.Supplier<T> supplier) {
        return new java.util.function.Supplier<>() {
            private volatile T value;

            @Override
            public synchronized T get() {
                if (value == null) value = supplier.get();
                return value;
            }
        };
    }

    static Path find(String command) {
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
    static String searchPath(Path exe) {
        var parts = new ArrayList<String>();
        if (exe.getParent() != null) parts.add(exe.getParent().toString());
        for (var dir : searchDirs()) parts.add(dir.toString());
        return String.join(java.io.File.pathSeparator, new LinkedHashSet<>(parts));
    }
}
