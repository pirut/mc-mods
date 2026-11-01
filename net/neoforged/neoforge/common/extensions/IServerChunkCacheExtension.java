package net.neoforged.neoforge.common.extensions;

import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.entity.Entity;

public interface IServerChunkCacheExtension {
   default ServerChunkCache self() {
      return (ServerChunkCache)this;
   }

   default void broadcastAndSend(Entity entity, CustomPacketPayload payload) {
      this.self().broadcastAndSend(entity, new ClientboundCustomPayloadPacket(payload));
   }

   default void broadcast(Entity entity, CustomPacketPayload payload) {
      this.self().broadcast(entity, new ClientboundCustomPayloadPacket(payload));
   }
}
