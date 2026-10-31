package io.github.pirut.ccagent;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Properties;

/** config/ccagent.properties. Made with a fresh random token the first time a world opens. */
public final class AgentConfig {
    public boolean enabled = true;
    public boolean dedicatedServer = false;
    public String host = "127.0.0.1";
    public int port = 25599;
    public String token = "";
    public boolean allowShellAndLua = true;
    public boolean allowFileWrites = true;
    public boolean launchAgents = true;
    public String claudeCommand = "claude";
    public String codexCommand = "codex";
    public String claudeArgs = "";
    public String codexArgs = "";
    public boolean relay = true;
    public boolean relayOpsOnly = false;
    public int relayRange = 128;
    public int relayCallsPerMinute = 240;

    private final Path file;

    private AgentConfig(Path file) {
        this.file = file;
    }

    public static AgentConfig load(Path configDir) {
        var config = new AgentConfig(configDir.resolve("ccagent.properties"));
        var props = new Properties();
        if (Files.exists(config.file)) {
            try (Reader in = Files.newBufferedReader(config.file, StandardCharsets.UTF_8)) {
                props.load(in);
            } catch (IOException e) {
                CCAgent.LOG.warn("Couldn't read {}, using defaults", config.file, e);
            }
        }
        config.enabled = bool(props, "enabled", config.enabled);
        config.dedicatedServer = bool(props, "dedicatedServer", config.dedicatedServer);
        config.host = props.getProperty("host", config.host).trim();
        try {
            config.port = Integer.parseInt(props.getProperty("port", Integer.toString(config.port)).trim());
        } catch (NumberFormatException e) {
            CCAgent.LOG.warn("Bad port in {}, using {}", config.file, config.port);
        }
        config.token = props.getProperty("token", "").trim();
        config.allowShellAndLua = bool(props, "allowShellAndLua", config.allowShellAndLua);
        config.allowFileWrites = bool(props, "allowFileWrites", config.allowFileWrites);
        config.launchAgents = bool(props, "launchAgents", config.launchAgents);
        config.claudeCommand = props.getProperty("claudeCommand", config.claudeCommand).trim();
        config.codexCommand = props.getProperty("codexCommand", config.codexCommand).trim();
        config.claudeArgs = props.getProperty("claudeArgs", config.claudeArgs).trim();
        config.codexArgs = props.getProperty("codexArgs", config.codexArgs).trim();
        config.relay = bool(props, "relay", config.relay);
        config.relayOpsOnly = bool(props, "relayOpsOnly", config.relayOpsOnly);
        config.relayRange = integer(props, "relayRange", config.relayRange);
        config.relayCallsPerMinute = integer(props, "relayCallsPerMinute", config.relayCallsPerMinute);
        if (config.token.length() < 16) config.token = newToken();
        config.save();
        return config;
    }

    public static String newToken() {
        var bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String url() {
        var shown = host.equals("0.0.0.0") ? "127.0.0.1" : host;
        return "http://" + shown + ":" + port + "/mcp";
    }

    public void save() {
        var text = """
            # CC: Agent. A local MCP server that lets Claude Code, Codex or any MCP client use CC: Tweaked computers.
            # Changes apply the next time a world opens. Run /ccagent in game for setup commands.

            # Run the server while a singleplayer world is open.
            enabled=%s
            # Also run it on a dedicated server. Anyone with the token can control every loaded computer.
            dedicatedServer=%s
            # 127.0.0.1 keeps it to this machine. Don't open it to a network you don't trust.
            host=%s
            port=%d
            # Agents send this as "Authorization: Bearer <token>". Delete it to make a new one.
            token=%s
            # Let agents run Lua and shell commands (most tools need this).
            allowShellAndLua=%s
            # Let agents write, move and delete files on computers.
            allowFileWrites=%s

            # Let the in-game claude and codex programs start Claude Code / Codex on this machine, signed in as
            # you. Singleplayer only. They may only use CC: Agent's tools (and web search for docs).
            launchAgents=%s
            # The commands to start them. A full path works too, if the game can't find them on its own.
            claudeCommand=%s
            codexCommand=%s
            # Extra arguments, e.g. claudeArgs=--model sonnet or codexArgs=-m gpt-5-codex
            claudeArgs=%s
            codexArgs=%s

            # Multiplayer (server side). Players with CC: Agent in their own game run agents on their own PC;
            # their tool calls come to the server, which runs them with these limits.
            relay=%s
            # Only operators may use it.
            relayOpsOnly=%s
            # Players' agents can only reach computers and blocks this close to them (0 = anywhere loaded).
            # Operators have no limit. Command computers are operators only.
            relayRange=%d
            relayCallsPerMinute=%d
            """.formatted(enabled, dedicatedServer, host, port, token, allowShellAndLua, allowFileWrites,
            launchAgents, esc(claudeCommand), esc(codexCommand), esc(claudeArgs), esc(codexArgs),
            relay, relayOpsOnly, relayRange, relayCallsPerMinute);
        try {
            Files.createDirectories(file.getParent());
            try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                out.write(text);
            }
        } catch (IOException e) {
            CCAgent.LOG.warn("Couldn't write {}", file, e);
        }
    }

    /** Properties files treat backslashes as escapes (Windows paths). */
    private static String esc(String value) {
        return value.replace("\\", "\\\\");
    }

    private static int integer(Properties props, String key, int fallback) {
        try {
            return Integer.parseInt(props.getProperty(key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean bool(Properties props, String key, boolean fallback) {
        var value = props.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }
}
