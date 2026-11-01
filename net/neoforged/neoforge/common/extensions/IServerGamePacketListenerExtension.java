package net.neoforged.neoforge.common.extensions;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;

public interface IServerGamePacketListenerExtension extends IServerCommonPacketListenerExtension {
   private ServerGamePacketListener self() {
      return (ServerGamePacketListener)this;
   }

   default void sendBundled(CustomPacketPayload... payloads) {
      this.sendBundled(List.of(payloads));
   }

   default void sendBundled(Iterable<CustomPacketPayload> payloads) {
      List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();

      for (CustomPacketPayload payload : payloads) {
         packets.add(new ClientboundCustomPayloadPacket(payload));
      }

      this.self().send(new ClientboundBundlePacket(packets));
   }
}
