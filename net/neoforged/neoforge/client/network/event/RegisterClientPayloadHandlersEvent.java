package net.neoforged.neoforge.client.network.event;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.network.registration.ClientNetworkRegistry;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.HandlerThread;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterClientPayloadHandlersEvent extends Event implements IModBusEvent {
   public <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
      this.register(type, HandlerThread.MAIN, handler);
   }

   public <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, HandlerThread thread, IPayloadHandler<T> handler) {
      ClientNetworkRegistry.register(type, thread, handler);
   }
}
