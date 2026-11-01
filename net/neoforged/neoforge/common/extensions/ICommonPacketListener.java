package net.neoforged.neoforge.common.extensions;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

public interface ICommonPacketListener extends PacketListener {
   void send(Packet<?> var1);

   void send(CustomPacketPayload var1);

   void disconnect(Component var1);

   Connection getConnection();

   PacketProcessor getPacketProcessor();

   default boolean hasChannel(Identifier payloadId) {
      return NetworkRegistry.hasChannel(this.getConnection(), this.protocol(), payloadId);
   }

   default boolean hasChannel(CustomPacketPayload.Type<?> type) {
      return this.hasChannel(type.id());
   }

   default boolean hasChannel(CustomPacketPayload payload) {
      return this.hasChannel(payload.type());
   }

   ConnectionType getConnectionType();
}
