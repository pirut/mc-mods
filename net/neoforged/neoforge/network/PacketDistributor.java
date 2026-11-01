package net.neoforged.neoforge.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

public final class PacketDistributor {
   private PacketDistributor() {
   }

   public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... payloads) {
      player.connection.send(makeClientboundPacket(payload, payloads));
   }

   public static void sendToPlayersInDimension(ServerLevel level, CustomPacketPayload payload, CustomPacketPayload... payloads) {
      level.getServer().getPlayerList().broadcastAll(makeClientboundPacket(payload, payloads), level.dimension());
   }

   public static void sendToPlayersNear(
      ServerLevel level,
      @Nullable ServerPlayer excluded,
      double x,
      double y,
      double z,
      double radius,
      CustomPacketPayload payload,
      CustomPacketPayload... payloads
   ) {
      Packet<?> packet = makeClientboundPacket(payload, payloads);
      level.getServer().getPlayerList().broadcast(excluded, x, y, z, radius, level.dimension(), packet);
   }

   public static void sendToAllPlayers(CustomPacketPayload payload, CustomPacketPayload... payloads) {
      MinecraftServer server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Cannot send clientbound payloads on the client");
      server.getPlayerList().broadcastAll(makeClientboundPacket(payload, payloads));
   }

   public static void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads) {
      if (entity.level().isClientSide()) {
         throw new IllegalStateException("Cannot send clientbound payloads on the client");
      } else {
         if (entity.level().getChunkSource() instanceof ServerChunkCache chunkCache) {
            chunkCache.sendToTrackingPlayers(entity, makeClientboundPacket(payload, payloads));
         }
      }
   }

   public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads) {
      if (entity.level().isClientSide()) {
         throw new IllegalStateException("Cannot send clientbound payloads on the client");
      } else {
         if (entity.level().getChunkSource() instanceof ServerChunkCache chunkCache) {
            chunkCache.sendToTrackingPlayersAndSelf(entity, makeClientboundPacket(payload, payloads));
         }
      }
   }

   public static void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, CustomPacketPayload payload, CustomPacketPayload... payloads) {
      Packet<?> packet = makeClientboundPacket(payload, payloads);

      for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(chunkPos, false)) {
         player.connection.send(packet);
      }
   }

   private static Packet<? super ClientGamePacketListener> makeClientboundPacket(CustomPacketPayload payload, CustomPacketPayload... payloads) {
      Objects.requireNonNull(payload, "Cannot send null payload");
      if (payloads.length <= 0) {
         return new ClientboundCustomPayloadPacket(payload);
      } else {
         List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
         packets.add(new ClientboundCustomPayloadPacket(payload));

         for (CustomPacketPayload otherPayload : payloads) {
            Objects.requireNonNull(otherPayload, "Cannot send null payload");
            packets.add(new ClientboundCustomPayloadPacket(otherPayload));
         }

         return new ClientboundBundlePacket(packets);
      }
   }
}
