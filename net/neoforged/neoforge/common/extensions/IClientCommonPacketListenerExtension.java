package net.neoforged.neoforge.common.extensions;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface IClientCommonPacketListenerExtension extends ICommonPacketListener {
   @Override
   default void send(CustomPacketPayload payload) {
      this.send(new ServerboundCustomPayloadPacket(payload));
   }

   @Override
   default void disconnect(Component reason) {
      this.getConnection().disconnect(reason);
   }
}
