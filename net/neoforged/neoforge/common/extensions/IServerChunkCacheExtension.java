package net.neoforged.neoforge.common.extensions;

import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.entity.Entity;

public interface IServerChunkCacheExtension {
   default ServerChunkCache self() {
      return (ServerChunkCache)this;
   }

   default void sendToTrackingPlayersAndSelf(Entity entity, CustomPacketPayload payload) {
      this.self().sendToTrackingPlayersAndSelf(entity, new ClientboundCustomPayloadPacket(payload));
   }

   default void sendToTrackingPlayers(Entity entity, CustomPacketPayload payload) {
      this.self().sendToTrackingPlayers(entity, new ClientboundCustomPayloadPacket(payload));
   }
}
