package net.neoforged.neoforge.network.handling;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DirectionalPayloadHandler<T extends CustomPacketPayload>(IPayloadHandler<T> clientSide, IPayloadHandler<T> serverSide)
   implements IPayloadHandler<T> {
   @Override
   public void handle(T payload, IPayloadContext context) {
      if (context.flow().isClientbound()) {
         this.clientSide.handle(payload, context);
      } else if (context.flow().isServerbound()) {
         this.serverSide.handle(payload, context);
      }
   }
}
