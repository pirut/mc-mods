package io.github.pirut.chunks3.fabric;

import io.github.pirut.chunks3.fabric.mixin.ConnectionAccess;
import io.github.pirut.chunks3.fabric.mixin.PacketListenerAccess;
import io.github.pirut.chunks3.platform.Platform;
import io.netty.channel.Channel;
import java.util.Collection;
import javax.annotation.Nullable;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@link Platform} on Fabric. Fabric leaves vanilla's block code as it is, so NeoForge's block
 * hooks are vanilla's behaviour here: no snapshots, no load and unload calls. Fabric API's block
 * entity load and unload events do fire, as its LevelChunk hooks fire them for vanilla chunks.
 */
public final class FabricPlatform implements Platform {
    /** The running server, kept from Fabric's server lifecycle events. */
    @Nullable static volatile MinecraftServer server;

    @Override @Nullable public MinecraftServer currentServer() { return server; }

    @Override public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) { ServerPlayNetworking.send(player, payload); }

    @Override public void sendToServer(CustomPacketPayload payload) { ClientPlayNetworking.send(payload); }

    @Override @Nullable public Channel channel(ServerPlayer player) {
        if (player.connection == null) return null;
        Connection connection = ((PacketListenerAccess) player.connection).chunks3$connection();
        return connection == null || connection.isMemoryConnection() ? null : ((ConnectionAccess) connection).chunks3$channel();
    }

    @Override public boolean capturingSnapshots(Level level) { return false; }

    @Override @Nullable public Object captureSnapshot(Level level, BlockPos pos, int flags) { return null; }

    @Override public void dropSnapshot(Level level, Object snapshot) {}

    @Override public void onBlockStateChange(BlockState state, Level level, BlockPos pos, BlockState old) {}

    /**
     * Fabric API fires its block entity load event from LevelChunk, which never holds the cubic
     * level's block entities. Its API lookup caches (the peripherals CC: Tweaked's computers see,
     * other mods' energy and item transfer) are cleared by that event and its unload twin, so
     * without them a computer kept seeing the empty space beside it from before a modem was placed.
     */
    @Override public void addFreshBlockEntities(Level level, Collection<BlockEntity> entities) {
        if (level instanceof ServerLevel server) for (BlockEntity entity : entities) ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.invoker().onLoad(entity, server);
    }

    @Override public void onChunkUnloaded(BlockEntity entity) { unloaded(entity); }

    @Override public void removedBlockEntity(BlockEntity entity) { unloaded(entity); }

    private static void unloaded(BlockEntity entity) {
        if (entity.getLevel() instanceof ServerLevel server) ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(entity, server);
    }

    /** What vanilla's block entity data packet does with the tag. */
    //#if MC >= 1.21.6
    //$$ @Override public void handleUpdateTag(BlockEntity entity, CompoundTag tag, HolderLookup.Provider registries) {
    //$$     entity.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, registries, tag));
    //$$ }
    //#else
    @Override public void handleUpdateTag(BlockEntity entity, CompoundTag tag, HolderLookup.Provider registries) { entity.loadWithComponents(tag, registries); }
    //#endif

    @Override public CompoundTag persistentData(Entity entity) { return new CompoundTag(); }

    @Override @Nullable public long[] tickTimes(MinecraftServer server, ResourceKey<Level> dimension) { return null; }
}
