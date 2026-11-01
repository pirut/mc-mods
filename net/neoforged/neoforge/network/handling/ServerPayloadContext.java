package net.neoforged.neoforge.network.handling;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerPlayerConnection;
import net.neoforged.neoforge.common.extensions.IServerConfigurationPacketListenerExtension;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public record ServerPayloadContext(ServerCommonPacketListener listener, ResourceLocation payloadId) implements IPayloadContext {
   @Override
   public void handle(CustomPacketPayload payload) {
      this.handle(new ServerboundCustomPayloadPacket(payload));
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
      if (this.listener instanceof IServerConfigurationPacketListenerExtension ext) {
         ext.finishCurrentTask(type);
      } else {
         throw new UnsupportedOperationException("Attempted to complete a configuration task outside of the configuration phase!");
      }
   }

   @Override
   public PacketFlow flow() {
      return PacketFlow.SERVERBOUND;
   }

   public ServerPlayer player() {
      if (this.listener instanceof ServerPlayerConnection spc) {
         return spc.getPlayer();
      } else {
         throw new UnsupportedOperationException("Cannot retrieve the sending player during the configuration phase.");
      }
   }
}
