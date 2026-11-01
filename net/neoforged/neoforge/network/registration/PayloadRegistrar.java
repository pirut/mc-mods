package net.neoforged.neoforge.network.registration;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.handling.MainThreadPayloadHandler;
import org.jspecify.annotations.Nullable;

public class PayloadRegistrar {
   private String version;
   private boolean optional = false;
   private HandlerThread thread = HandlerThread.MAIN;

   public PayloadRegistrar(String version) {
      this.version = version;
   }

   private PayloadRegistrar(PayloadRegistrar source) {
      this.version = source.version;
      this.optional = source.optional;
      this.thread = source.thread;
   }

   public <T extends CustomPacketPayload> PayloadRegistrar playToClient(
      CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler
   ) {
      this.register(type, codec, null, handler, List.of(ConnectionProtocol.PLAY), Optional.of(PacketFlow.CLIENTBOUND), this.version, this.optional);
      return this;
   }

   public <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
      this.register(type, codec, null, null, List.of(ConnectionProtocol.PLAY), Optional.of(PacketFlow.CLIENTBOUND), this.version, this.optional);
      return this;
   }

   public <T extends CustomPacketPayload> PayloadRegistrar playToServer(
      CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler
   ) {
      this.register(type, codec, handler, null, List.of(ConnectionProtocol.PLAY), Optional.of(PacketFlow.SERVERBOUND), this.version, this.optional);
      return this;
   }

   public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(
      CustomPacketPayload.Type<T> type,
      StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
      IPayloadHandler<T> serverHandler,
      @Nullable IPayloadHandler<T> clientHandler
   ) {
      this.register(type, codec, serverHandler, clientHandler, List.of(ConnectionProtocol.PLAY), Optional.empty(), this.version, this.optional);
      return this;
   }

   public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(
      CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> serverHandler
   ) {
      return this.playBidirectional(type, codec, serverHandler, null);
   }

   public <T extends CustomPacketPayload> PayloadRegistrar configurationToClient(
      CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec, IPayloadHandler<T> handler
   ) {
      this.register(type, codec, null, handler, List.of(ConnectionProtocol.CONFIGURATION), Optional.of(PacketFlow.CLIENTBOUND), this.version, this.optional);
      return this;
   }

   public <T extends CustomPacketPayload> PayloadRegistrar configurationToClient(
      CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec
   ) {
      this.register(type, codec, null, null, List.of(ConnectionProtocol.CONFIGURATION), Optional.of(PacketFlow.CLIENTBOUND), this.version, this.optional);
      return this;
   }

   public <T extends CustomPacketPayload> PayloadRegistrar configurationToServer(
      CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec, IPayloadHandler<T> handler
   ) {
      this.register(type, codec, handler, null, List.of(ConnectionProtocol.CONFIGURATION), Optional.of(PacketFlow.SERVERBOUND), this.version, this.optional);
      return this;
   }

   public <T extends CustomPacketPayload> PayloadRegistrar configurationBidirectional(
      CustomPacketPayload.Type<T> type,
      StreamCodec<? super FriendlyByteBuf, T> codec,
      IPayloadHandler<T> serverHandler,
      @Nullable IPayloadHandler<T> clientHandler
   ) {
      this.register(type, codec, serverHandler, clientHandler, List.of(ConnectionProtocol.CONFIGURATION), Optional.empty(), this.version, this.optional);
      return this;
   }

   public <T extends CustomPacketPayload> PayloadRegistrar configurationBidirectional(
      CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec, IPayloadHandler<T> serverHandler
   ) {
      return this.configurationBidirectional(type, codec, serverHandler, null);
   }

   public <T extends CustomPacketPayload> PayloadRegistrar commonToClient(
      CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec, IPayloadHandler<T> handler
   ) {
      this.register(
         type,
         codec,
         null,
         handler,
         List.of(ConnectionProtocol.PLAY, ConnectionProtocol.CONFIGURATION),
         Optional.of(PacketFlow.CLIENTBOUND),
         this.version,
         this.optional
      );
      return this;
   }

   public <T extends CustomPacketPayload> PayloadRegistrar commonToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec) {
      this.register(
         type,
         codec,
         null,
         null,
         List.of(ConnectionProtocol.PLAY, ConnectionProtocol.CONFIGURATION),
         Optional.of(PacketFlow.CLIENTBOUND),
         this.version,
         this.optional
      );
      return this;
   }

   public <T extends CustomPacketPayload> PayloadRegistrar commonToServer(
      CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec, IPayloadHandler<T> handler
   ) {
      this.register(
         type,
         codec,
         handler,
         null,
         List.of(ConnectionProtocol.PLAY, ConnectionProtocol.CONFIGURATION),
         Optional.of(PacketFlow.SERVERBOUND),
         this.version,
         this.optional
      );
      return this;
   }

   public <T extends CustomPacketPayload> PayloadRegistrar commonBidirectional(
      CustomPacketPayload.Type<T> type,
      StreamCodec<? super FriendlyByteBuf, T> codec,
      IPayloadHandler<T> serverHandler,
      @Nullable IPayloadHandler<T> clientHandler
   ) {
      this.register(
         type,
         codec,
         serverHandler,
         clientHandler,
         List.of(ConnectionProtocol.PLAY, ConnectionProtocol.CONFIGURATION),
         Optional.empty(),
         this.version,
         this.optional
      );
      return this;
   }

   public <T extends CustomPacketPayload> PayloadRegistrar commonBidirectional(
      CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec, IPayloadHandler<T> serverHandler
   ) {
      return this.commonBidirectional(type, codec, serverHandler, null);
   }

   public PayloadRegistrar executesOn(HandlerThread thread) {
      PayloadRegistrar clone = new PayloadRegistrar(this);
      clone.thread = thread;
      return clone;
   }

   public PayloadRegistrar versioned(String version) {
      PayloadRegistrar clone = new PayloadRegistrar(this);
      clone.version = version;
      return clone;
   }

   public PayloadRegistrar optional() {
      PayloadRegistrar clone = new PayloadRegistrar(this);
      clone.optional = true;
      return clone;
   }

   private <T extends CustomPacketPayload, B extends FriendlyByteBuf> void register(
      CustomPacketPayload.Type<T> type,
      StreamCodec<? super B, T> codec,
      @Nullable IPayloadHandler<T> serverHandler,
      @Nullable IPayloadHandler<T> clientHandler,
      List<ConnectionProtocol> protocols,
      Optional<PacketFlow> flow,
      String version,
      boolean optional
   ) {
      if (this.thread == HandlerThread.MAIN) {
         if (serverHandler != null) {
            serverHandler = new MainThreadPayloadHandler<>(serverHandler);
         }

         if (clientHandler != null) {
            clientHandler = new MainThreadPayloadHandler<>(clientHandler);
         }
      }

      NetworkRegistry.register(type, codec, serverHandler, clientHandler, protocols, flow, version, optional);
   }
}
