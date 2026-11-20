package io.github.pirut.ccagent;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** /ccagent: is the server running, and the commands that connect Claude Code and Codex to it. */
final class AgentCommand {
    private AgentCommand() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ccagent")
            .requires(AgentCommand::allowed)
            .executes(ctx -> status(ctx.getSource()))
            .then(Commands.literal("claude").executes(ctx -> claude(ctx.getSource())))
            .then(Commands.literal("codex").executes(ctx -> codex(ctx.getSource()))));
    }

    /** The token controls every computer, so only the singleplayer host or a level 4 op sees it. */
    private static boolean allowed(CommandSourceStack source) {
        if (McNames.isOwner(source)) return true;
        var player = source.getPlayer();
        return player != null && McNames.isHost(source.getServer(), player);
    }

    /** Says why there's nothing to connect to, if the MCP server isn't running. */
    private static boolean notRunning(CommandSourceStack source) {
        var config = CCAgent.config();
        if (config == null) {
            source.sendFailure(Component.literal("CC: Agent hasn't loaded its config yet."));
            return true;
        }
        if (CCAgent.running()) return false;
        var why = !config.enabled ? "enabled=false in config/ccagent.properties"
            : CCAgent.startError() != null ? CCAgent.startError()
            : "it's off on dedicated servers unless dedicatedServer=true";
        source.sendFailure(Component.literal("CC: Agent's MCP server isn't running: " + why));
        return true;
    }

    private static int status(CommandSourceStack source) {
        if (notRunning(source)) return 0;
        var config = CCAgent.config();
        source.sendSystemMessage(Component.literal("CC: Agent MCP server: ").withStyle(ChatFormatting.GOLD)
            .append(copyable(config.url(), config.url())));
        source.sendSystemMessage(Component.literal("Claude Code: ").append(link("/ccagent claude")));
        source.sendSystemMessage(Component.literal("Codex: ").append(link("/ccagent codex")));
        return 1;
    }

    private static int claude(CommandSourceStack source) {
        if (notRunning(source)) return 0;
        var config = CCAgent.config();
        var cmd = "claude mcp add --scope user --transport http cc-agent " + config.url()
            + " --header \"Authorization: Bearer " + config.token + "\"";
        source.sendSystemMessage(Component.literal("Run this once in a terminal (click to copy):"));
        source.sendSystemMessage(copyable("[copy Claude Code command]", cmd));
        if (source.getPlayer() == null) source.sendSystemMessage(Component.literal(cmd));
        return 1;
    }

    private static int codex(CommandSourceStack source) {
        if (notRunning(source)) return 0;
        var config = CCAgent.config();
        var toml = "[mcp_servers.cc-agent]\nurl = \"" + config.url() + "\"\nhttp_headers = { \"Authorization\" = \"Bearer "
            + config.token + "\" }\ndefault_tools_approval_mode = \"approve\"\n";
        source.sendSystemMessage(Component.literal("Add this to ~/.codex/config.toml (click to copy):"));
        source.sendSystemMessage(copyable("[copy Codex config]", toml));
        if (source.getPlayer() == null) source.sendSystemMessage(Component.literal(toml));
        return 1;
    }

    private static MutableComponent copyable(String label, String text) {
        return Component.literal(label).withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
            .withClickEvent(McChat.copy(text))
            .withHoverEvent(McChat.tooltip(Component.literal("Click to copy"))));
    }

    private static MutableComponent link(String command) {
        return Component.literal(command).withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
            .withClickEvent(McChat.run(command)));
    }
}
