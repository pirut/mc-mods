package net.neoforged.neoforge.network.handlers;

import net.neoforged.neoforge.network.configuration.SyncRegistries;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.payload.FrozenRegistrySyncCompletedPayload;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class ServerPayloadHandler {
   private ServerPayloadHandler() {
   }

   public static void handle(FrozenRegistrySyncCompletedPayload payload, IPayloadContext context) {
      context.finishCurrentTask(SyncRegistries.TYPE);
   }
}
