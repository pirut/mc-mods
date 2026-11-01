package io.github.pirut.ccagent;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/** Names, players and permissions, for Minecraft 1.21.11 and later (Identifier, NameAndId, permission sets). */
final class McNames {
    private McNames() {
    }

    static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    static Identifier parseId(String id) {
        return Identifier.parse(id);
    }

    static String dimension(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    static String name(ServerPlayer player) {
        return player.getGameProfile().name();
    }

    /** The singleplayer host (or LAN host) of this game. */
    static boolean isHost(MinecraftServer server, ServerPlayer player) {
        return server.isSingleplayerOwner(player.nameAndId());
    }

    /** An operator who may use cheats (level 2). */
    static boolean isOp(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /** The server's owner level (4). */
    static boolean isOwner(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_OWNER);
    }

    static BlockPos spawn(ServerLevel level) {
        return level.getRespawnData().pos();
    }
}
