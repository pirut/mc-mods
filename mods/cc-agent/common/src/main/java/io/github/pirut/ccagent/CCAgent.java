package io.github.pirut.ccagent;

import com.mojang.brigadier.CommandDispatcher;
import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CCAgent {
    public static final String MOD_ID = "ccagent";
    public static final String VERSION = "0.5.1";
    public static final Logger LOG = LoggerFactory.getLogger("CC: Agent");

    private static Platform platform;
    private static AgentConfig config;
    private static McpServer mcp;
    private static String startError;
    private static MinecraftServer server;
    private static Tools tools;

    private CCAgent() {
    }

    public static void init(Platform platform) {
        CCAgent.platform = platform;
        ComputerCraftAPI.registerAPIFactory(AgentApi::new);
    }

    public static void serverStarted(MinecraftServer server) {
        CCAgent.server = server;
        config = AgentConfig.load(platform.configDir());
        tools = new Tools(server);
        startError = null;
        if (!config.enabled) {
            LOG.info("MCP server is off (enabled=false in ccagent.properties)");
            return;
        }
        if (server.isDedicatedServer() && !config.dedicatedServer) {
            LOG.info("MCP server is off on dedicated servers (dedicatedServer=false in ccagent.properties)");
            return;
        }
        try {
            mcp = McpServer.start(tools, config);
            LOG.info("MCP server listening on {} (run /ccagent in game for setup)", config.url());
        } catch (Exception e) {
            startError = e.toString();
            LOG.error("Couldn't start the MCP server on {}:{}", config.host, config.port, e);
        }
    }

    public static void serverStopping() {
        AgentRunner.stopAll();
        TuiSession.stopAll();
        Questions.cancelAll();
        Relay.serverStopped();
        server = null;
        tools = null;
        if (mcp != null) {
            mcp.stop();
            mcp = null;
        }
        Jobs.clear();
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        AgentCommand.register(dispatcher);
    }

    public static AgentConfig config() {
        return config;
    }

    public static boolean running() {
        return mcp != null;
    }

    public static String startError() {
        return startError;
    }

    public static Platform platform() {
        return platform;
    }

    public static Tools tools() {
        return tools;
    }

    public static MinecraftServer server() {
        return server;
    }

    public static Platform.ModMeta mod(String modId) {
        return platform == null ? null : platform.mod(modId);
    }

    public static java.nio.file.Path gameDir() {
        return platform.gameDir();
    }

    public static String loader() {
        return platform == null ? "?" : platform.loader();
    }
}
