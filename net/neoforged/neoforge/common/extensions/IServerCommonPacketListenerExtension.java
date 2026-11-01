package net.neoforged.neoforge.common.extensions;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.Nullable;

public interface IServerCommonPacketListenerExtension extends ICommonPacketListener {
   @Override
   default void send(CustomPacketPayload payload) {
      this.send(new ClientboundCustomPayloadPacket(payload));
   }

   void send(Packet<?> var1, @Nullable ChannelFutureListener var2);

   default void send(CustomPacketPayload payload, @Nullable ChannelFutureListener listener) {
      this.send(new ClientboundCustomPayloadPacket(payload), listener);
   }
}
