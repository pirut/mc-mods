package net.neoforged.neoforge.network.handling;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MainThreadPayloadHandler<T extends CustomPacketPayload>(IPayloadHandler<T> handler) implements IPayloadHandler<T> {
   @Override
   public void handle(T payload, IPayloadContext context) {
      context.enqueueWork(() -> this.handler().handle(payload, context));
   }
}
