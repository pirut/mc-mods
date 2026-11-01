package net.neoforged.neoforge.network.handling;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record ClientPayloadContext(ClientCommonPacketListener listener, ResourceLocation payloadId) implements IPayloadContext {
   @Override
   public void handle(CustomPacketPayload payload) {
      this.handle(new ClientboundCustomPayloadPacket(payload));
   }

   @Override
   public CompletableFuture<Void> enqueueWork(Runnable task) {
      if (this.listener.getMainThreadEventLoop().isSameThread()) {
         task.run();
         return CompletableFuture.completedFuture(null);
      } else {
         return NetworkRegistry.guard(this.listener.getMainThreadEventLoop().submit(task), this.payloadId);
      }
   }

   @Override
   public <T> CompletableFuture<T> enqueueWork(Supplier<T> task) {
      return this.listener.getMainThreadEventLoop().isSameThread()
         ? CompletableFuture.completedFuture(task.get())
         : NetworkRegistry.guard(this.listener.getMainThreadEventLoop().submit(task), this.payloadId);
   }

   @Override
   public void finishCurrentTask(ConfigurationTask.Type type) {
      throw new UnsupportedOperationException("Attempted to complete a configuration task on the client!");
   }

   @Override
   public PacketFlow flow() {
      return PacketFlow.CLIENTBOUND;
   }

   @Override
   public Player player() {
      if (Minecraft.getInstance().player != null) {
         return Minecraft.getInstance().player;
      } else {
         throw new UnsupportedOperationException("Cannot retrieve the client player during the configuration phase.");
      }
   }
}
