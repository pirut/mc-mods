package net.neoforged.neoforge.network.handling;

import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface IPayloadContext {
   ICommonPacketListener listener();

   default Connection connection() {
      return this.listener().getConnection();
   }

   Player player();

   default void reply(CustomPacketPayload payload) {
      this.listener().send(payload);
   }

   default void disconnect(Component reason) {
      this.listener().disconnect(reason);
   }

   CompletableFuture<Void> enqueueWork(Runnable var1);

   <T> CompletableFuture<T> enqueueWork(Supplier<T> var1);

   PacketFlow flow();

   default ConnectionProtocol protocol() {
      return this.listener().protocol();
   }

   default void handle(Packet<?> packet) {
      NetworkRegistry.handlePacketUnchecked(packet, this.listener());
   }

   void handle(CustomPacketPayload var1);

   void finishCurrentTask(ConfigurationTask.Type var1);

   default ChannelHandlerContext channelHandlerContext() {
      return this.connection().channel().pipeline().lastContext();
   }
}
