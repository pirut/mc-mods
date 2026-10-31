package io.github.pirut.ccagent;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;
import io.github.pirut.ccagent.tui.Vt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Claude Code's or Codex's own interactive TUI, running in a pseudo-terminal on this machine. A
 * small terminal emulator keeps the screen; changed rows go to the CC computer as frames, and the
 * computer's keys come back as the bytes a terminal would send.
 */
public final class TuiSession {
    /**
     * Where a session's screen goes. Rows are CC blit strings: text, text colors, background colors.
     * The palette holds the slots (0-15) whose color changed, as RGB.
     */
    public interface Sink {
        void frame(int id, Map<Integer, String[]> rows, Map<Integer, Integer> palette, int cursorX, int cursorY,
                   boolean cursorVisible);

        void exit(int id, int code);
    }

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static final Map<Integer, TuiSession> SESSIONS = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService FLUSHER = Executors.newSingleThreadScheduledExecutor(r -> {
        var thread = new Thread(r, "CC: Agent TUI frames");
        thread.setDaemon(true);
        return thread;
    });
    private static final long FRAME_MILLIS = 50;

    private static final int LEFT_SHIFT = 340, LEFT_CTRL = 341, LEFT_ALT = 342;
    private static final int RIGHT_SHIFT = 344, RIGHT_CTRL = 345, RIGHT_ALT = 346;
    private static final int PAGE_UP = 266, PAGE_DOWN = 267;

    public final int id;
    public final int computerId;
    private final PtyProcess process;
    private final OutputStream input;
    private final Vt vt;
    private final Sink sink;
    private int lastX = -1, lastY = -1;
    private boolean lastVisible;
    private boolean ctrl, alt, shift;
    private volatile boolean fullPalette = true;
    private volatile boolean closed;

    static {
        FLUSHER.scheduleAtFixedRate(TuiSession::flushAll, FRAME_MILLIS, FRAME_MILLIS, TimeUnit.MILLISECONDS);
    }

    private TuiSession(int id, int computerId, PtyProcess process, Vt vt, Sink sink) {
        this.id = id;
        this.computerId = computerId;
        this.process = process;
        this.input = process.getOutputStream();
        this.vt = vt;
        this.sink = sink;
    }

    /** Start an agent's TUI for a computer, sized cols x rows. Returns the session id. */
    public static int start(Sink sink, int computerId, String label, String agent, int cols, int rows, String monitor,
                            AgentConfig config, String mcpUrl) {
        if (!config.launchAgents) throw new ToolException("launchAgents=false in config/ccagent.properties");
        boolean claude = agent.equals("claude");
        if (!claude && !agent.equals("codex")) throw new ToolException("agent must be claude or codex");
        // The same bounds as resize: the size can come from a server over the relay.
        cols = Math.max(20, Math.min(cols, 400));
        rows = Math.max(5, Math.min(rows, 200));
        stopComputer(computerId);

        var exe = AgentRunner.find(claude ? config.claudeCommand : config.codexCommand);
        if (exe == null) {
            throw new ToolException("couldn't find " + (claude ? config.claudeCommand : config.codexCommand)
                + ". Run \"" + (AgentRunner.windows() ? "where " : "which ") + agent + "\" in a terminal and put that "
                + "path in " + agent + "Command= in config/ccagent.properties (forward slashes), then reopen the world.");
        }

        var workDir = CCAgent.gameDir().resolve("ccagent").resolve("tui");
        List<String> cmd = AgentRunner.launcher(exe);
        try {
            Files.createDirectories(workDir);
            // Both CLIs read project instructions from their working folder.
            var notes = context(computerId, label, cols, monitor);
            Files.writeString(workDir.resolve("CLAUDE.md"), notes);
            Files.writeString(workDir.resolve("AGENTS.md"), notes);
            if (claude) {
                var mcp = workDir.resolve("mcp.json");
                var servers = new com.google.gson.JsonObject();
                servers.add("cc-agent", Json.obj("type", "http", "url", mcpUrl,
                    "headers", Json.obj("Authorization", "Bearer " + config.token)));
                Files.writeString(mcp, Json.PRETTY.toJson(Json.obj("mcpServers", servers)));
                cmd.addAll(List.of("--strict-mcp-config", "--mcp-config", mcp.toString(),
                    "--allowedTools", "mcp__cc-agent__*,WebSearch,WebFetch"));
                cmd.addAll(CliArgs.split(config.claudeArgs));
            } else {
                cmd.addAll(List.of("--sandbox", "read-only",
                    "-c", "mcp_servers.cc-agent.url=" + mcpUrl,
                    "-c", "mcp_servers.cc-agent.bearer_token_env_var=CC_AGENT_TOKEN",
                    "-c", "mcp_servers.cc-agent.default_tools_approval_mode=approve"));
                cmd.addAll(CliArgs.split(config.codexArgs));
            }
        } catch (IOException e) {
            throw new ToolException("couldn't set up the agent folder: " + e.getMessage());
        }

        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("TERM", "xterm-256color");
        env.remove("COLORTERM");
        env.put("CC_AGENT_TOKEN", config.token);
        env.put("PATH", AgentRunner.searchPath(exe));
        env.putIfAbsent("LANG", "en_US.UTF-8");
        // A game started from inside a Claude Code session shouldn't make this one a child of it.
        env.keySet().removeIf(key -> key.equals("CLAUDECODE") || key.equals("CLAUDE_CODE_ENTRYPOINT")
            || key.contains("CHILD_SESSION"));

        PtyProcess process;
        try {
            process = new PtyProcessBuilder(cmd.toArray(String[]::new))
                .setEnvironment(env)
                .setDirectory(workDir.toString())
                .setInitialColumns(cols)
                .setInitialRows(rows)
                .setConsole(false)
                .setUseWinConPty(true)
                .start();
        } catch (IOException | RuntimeException | LinkageError e) {
            CCAgent.LOG.warn("Couldn't start {} in a terminal", exe, e);
            throw new ToolException("couldn't start " + exe + " in a terminal: " + e);
        }

        int id = NEXT_ID.getAndIncrement();
        var holder = new TuiSession[1];
        var vt = new Vt(cols, rows, bytes -> {
            if (holder[0] != null) holder[0].write(bytes);
        });
        var session = new TuiSession(id, computerId, process, vt, sink);
        holder[0] = session;
        SESSIONS.put(id, session);
        var reader = new Thread(session::pump, "CC: Agent TUI " + agent + " " + id);
        reader.setDaemon(true);
        reader.start();
        return id;
    }

    private static String context(int computerId, String computerLabel, int cols, String monitor) {
        var label = computerLabel == null ? "" : " (label \"" + computerLabel + "\")";
        var where = monitor == null ? "the screen of CC: Tweaked computer #" + computerId + label
            : "the monitor \"" + monitor + "\" of CC: Tweaked computer #" + computerId + label
            + " (don't draw on, clear or resize that monitor: it would wipe this session)";
        return "# Minecraft: CC: Tweaked computer #" + computerId + label + "\n\n"
            + "The player is using you from inside Minecraft: this terminal is shown on " + where + ", " + cols
            + " columns wide. Act in the world with the cc-agent MCP tools. That computer is running this "
            + "session, so don't use type_text or press_keys on it, and don't reboot or shut it down unless "
            + "asked; run things on it headless with "
            + "run_lua/run_shell, or use other computers. When a peripheral comes from an addon mod, call "
            + "describe_peripheral and look up that mod's docs online. This folder is only a scratch folder; "
            + "the computers' files are reached through the tools.\n";
    }

    public static TuiSession get(int id) {
        return SESSIONS.get(id);
    }

    public static void stop(int id) {
        var session = SESSIONS.remove(id);
        if (session != null) session.close();
    }

    public static void stopComputer(int computerId) {
        for (var session : List.copyOf(SESSIONS.values())) {
            if (session.computerId == computerId) stop(session.id);
        }
    }

    static void stopAll() {
        for (var id : List.copyOf(SESSIONS.keySet())) stop(id);
    }

    private void close() {
        closed = true;
        try {
            // PtyProcess doesn't support toHandle(), so find the children by pid.
            ProcessHandle.of(process.pid()).ifPresent(handle -> handle.descendants().forEach(ProcessHandle::destroyForcibly));
        } catch (RuntimeException e) {
            CCAgent.LOG.debug("Couldn't list the TUI's child processes", e);
        }
        process.destroyForcibly();
    }

    private void pump() {
        var buffer = new byte[8192];
        try (InputStream out = process.getInputStream()) {
            int n;
            while ((n = out.read(buffer)) > 0) vt.feed(buffer, n);
        } catch (IOException e) {
            // The terminal closes when the process ends.
        }
        int code;
        try {
            code = process.waitFor();
        } catch (InterruptedException e) {
            code = -1;
        }
        SESSIONS.remove(id);
        flush();
        sink.exit(id, code);
    }

    private synchronized void write(byte[] bytes) {
        if (closed || bytes == null || bytes.length == 0) return;
        try {
            input.write(bytes);
            input.flush();
        } catch (IOException e) {
            // Gone; the reader reports the exit.
        }
    }

    private static void flushAll() {
        for (var session : SESSIONS.values()) {
            try {
                session.flush();
            } catch (RuntimeException e) {
                CCAgent.LOG.warn("TUI frame failed", e);
            }
        }
    }

    private void flush() {
        var palette = new HashMap<Integer, Integer>();
        var rows = vt.takeDirty(palette);
        if (fullPalette) {
            fullPalette = false;
            palette.putAll(vt.palette());
        }
        int x = vt.cursorX(), y = vt.cursorY();
        boolean visible = vt.cursorVisible();
        if (rows.isEmpty() && palette.isEmpty() && x == lastX && y == lastY && visible == lastVisible) return;
        lastX = x;
        lastY = y;
        lastVisible = visible;
        sink.frame(id, rows, palette, x, y, visible);
    }

    public void resize(int cols, int rows) {
        cols = Math.max(20, Math.min(cols, 400));
        rows = Math.max(5, Math.min(rows, 200));
        vt.resize(cols, rows);
        vt.markAllDirty();
        process.setWinSize(new WinSize(cols, rows));
    }

    public void input(String kind, Object a) {
        input(kind, a, 0, 0);
    }

    /** Scroll back (positive lines) or forward through what scrolled off the top. */
    public void scroll(int lines) {
        vt.scrollView(lines);
    }

    public void redraw() {
        vt.markAllDirty();
        fullPalette = true;
        lastX = -1;
    }

    /**
     * A CC input event: char, key (a = code), key_up (a = code), paste (a = text), or from the mouse:
     * scroll (a = 1 down / -1 up) and click (a = button), at terminal cell x, y counted from 0.
     */
    public void input(String kind, Object a, int x, int y) {
        switch (kind) {
            case "scroll" -> {
                int dir = ((Number) a).intValue();
                // The program's own scrolling if it takes the mouse (Claude Code's full screen),
                // else the lines that scrolled off the top.
                var wheel = vt.mouse(dir < 0 ? 64 : 65, x, y, true);
                if (wheel != null) {
                    for (int i = 0; i < 3; i++) write(wheel);
                } else {
                    vt.scrollView(-dir * 3);
                }
            }
            case "click" -> {
                int button = Math.max(0, Math.min(2, ((Number) a).intValue() - 1)); // CC: 1 left, 2 right, 3 middle
                if (button == 1) button = 2;
                else if (button == 2) button = 1;
                var press = vt.mouse(button, x, y, true);
                if (press != null) {
                    write(press);
                    write(vt.mouse(button, x, y, false));
                }
            }
            case "char" -> {
                if (ctrl) return;
                vt.scrollToBottom();
                var text = String.valueOf(a);
                write((alt ? "\u001b" + text : text).getBytes(StandardCharsets.UTF_8));
            }
            case "paste" -> {
                vt.scrollToBottom();
                write(vt.paste(String.valueOf(a)));
            }
            case "key" -> {
                int code = ((Number) a).intValue();
                switch (code) {
                    case LEFT_CTRL, RIGHT_CTRL -> ctrl = true;
                    case LEFT_ALT, RIGHT_ALT -> alt = true;
                    case LEFT_SHIFT, RIGHT_SHIFT -> shift = true;
                    // Shift+Page Up/Down scroll back through what scrolled off, like a terminal.
                    case PAGE_UP, PAGE_DOWN -> {
                        if (shift) vt.scrollView((code == PAGE_UP ? 1 : -1) * Math.max(1, vt.rows() - 2));
                        else {
                            vt.scrollToBottom();
                            write(vt.key(code, ctrl, alt, shift));
                        }
                    }
                    default -> {
                        vt.scrollToBottom();
                        write(vt.key(code, ctrl, alt, shift));
                    }
                }
            }
            case "key_up" -> {
                int code = ((Number) a).intValue();
                switch (code) {
                    case LEFT_CTRL, RIGHT_CTRL -> ctrl = false;
                    case LEFT_ALT, RIGHT_ALT -> alt = false;
                    case LEFT_SHIFT, RIGHT_SHIFT -> shift = false;
                    default -> {
                    }
                }
            }
            default -> throw new ToolException("unknown input kind " + kind);
        }
    }

    /** Rows for a CC event: {[row] = {text, textColors, backgroundColors}}, rows counted from 1. */
    public static Map<Integer, List<String>> luaRows(Map<Integer, String[]> rows) {
        var out = new HashMap<Integer, List<String>>();
        for (var e : rows.entrySet()) out.put(e.getKey() + 1, new ArrayList<>(List.of(e.getValue())));
        return out;
    }
}
