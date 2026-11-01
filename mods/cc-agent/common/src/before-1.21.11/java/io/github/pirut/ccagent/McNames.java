package io.github.pirut.ccagent;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Names, players and permissions, for Minecraft before 1.21.11 (ResourceLocation, GameProfile, op levels). */
final class McNames {
    private McNames() {
    }

    static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    static ResourceLocation parseId(String id) {
        return ResourceLocation.parse(id);
    }

    static String dimension(ServerLevel level) {
        return level.dimension().location().toString();
    }

    static String name(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    /** The singleplayer host (or LAN host) of this game. */
    static boolean isHost(MinecraftServer server, ServerPlayer player) {
        return server.isSingleplayerOwner(player.getGameProfile());
    }

    /** An operator who may use cheats (level 2). */
    static boolean isOp(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    /** The server's owner level (4). */
    static boolean isOwner(CommandSourceStack source) {
        return source.hasPermission(4);
    }

    static BlockPos spawn(ServerLevel level) {
        return level.getSharedSpawnPos();
    }
}
