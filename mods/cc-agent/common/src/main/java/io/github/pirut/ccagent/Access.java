package io.github.pirut.ccagent;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * Limits on a tool call that came from a player's agent over the multiplayer relay: which computers
 * and blocks it may touch. Calls from this machine's own MCP server have no limits.
 */
public record Access(ServerPlayer player, boolean op, int range) {
    private static final ThreadLocal<Access> CURRENT = new ThreadLocal<>();

    public static Access current() {
        return CURRENT.get();
    }

    public static <T> T with(Access access, Supplier<T> task) {
        var previous = CURRENT.get();
        CURRENT.set(access);
        try {
            return task.get();
        } finally {
            CURRENT.set(previous);
        }
    }

    public static boolean allows(ServerComputer computer) {
        var access = CURRENT.get();
        if (access == null || access.op) return true;
        // A command computer runs server commands: ops only.
        if (computer.getFamily() == ComputerFamily.COMMAND) return false;
        return access.near(computer.getLevel(), computer.getPosition());
    }

    public static boolean allows(ServerLevel level, BlockPos pos) {
        var access = CURRENT.get();
        return access == null || access.op || access.near(level, pos);
    }

    public static void check(ServerLevel level, BlockPos pos) {
        if (!allows(level, pos)) {
            throw new ToolException("that's more than " + CURRENT.get().range + " blocks from you (the server's relayRange)");
        }
    }

    private boolean near(ServerLevel level, BlockPos pos) {
        if (range <= 0) return true;
        if (player.level() != level) return false;
        return player.blockPosition().distSqr(pos) <= (double) range * range;
    }
}
