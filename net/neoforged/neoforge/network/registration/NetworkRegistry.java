package net.neoforged.neoforge.network.registration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableSet.Builder;
import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandlerContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.common.NeoForgeVersion;
import net.neoforged.neoforge.common.config.NeoForgeCommonConfig;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.configuration.CommonRegisterTask;
import net.neoforged.neoforge.network.configuration.CommonVersionTask;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.filters.NetworkFilters;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.handling.ServerPayloadContext;
import net.neoforged.neoforge.network.negotiation.NegotiableNetworkComponent;
import net.neoforged.neoforge.network.negotiation.NegotiationResult;
import net.neoforged.neoforge.network.negotiation.NetworkComponentNegotiator;
import net.neoforged.neoforge.network.payload.CommonRegisterPayload;
import net.neoforged.neoforge.network.payload.CommonVersionPayload;
import net.neoforged.neoforge.network.payload.MinecraftRegisterPayload;
import net.neoforged.neoforge.network.payload.MinecraftUnregisterPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryComponent;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkSetupFailedPayload;
import org.jetbrains.annotations.VisibleForTesting;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Internal
public class NetworkRegistry {
   public static final List<Integer> SUPPORTED_COMMON_NETWORKING_VERSIONS = List.of(1);
   private static final Logger LOGGER = LogUtils.getLogger();
   protected static final Map<Identifier, StreamCodec<FriendlyByteBuf, ? extends CustomPacketPayload>> BUILTIN_PAYLOADS = ImmutableMap.of(
      MinecraftRegisterPayload.ID,
      MinecraftRegisterPayload.STREAM_CODEC,
      MinecraftUnregisterPayload.ID,
      MinecraftUnregisterPayload.STREAM_CODEC,
      ModdedNetworkQueryPayload.ID,
      ModdedNetworkQueryPayload.STREAM_CODEC,
      ModdedNetworkPayload.ID,
      ModdedNetworkPayload.STREAM_CODEC,
      ModdedNetworkSetupFailedPayload.ID,
      ModdedNetworkSetupFailedPayload.STREAM_CODEC,
      CommonVersionPayload.ID,
      CommonVersionPayload.STREAM_CODEC,
      CommonRegisterPayload.ID,
      CommonRegisterPayload.STREAM_CODEC
   );
   protected static final Map<ConnectionProtocol, Map<Identifier, PayloadRegistration<?>>> PAYLOAD_REGISTRATIONS = ImmutableMap.of(
      ConnectionProtocol.CONFIGURATION, new HashMap(), ConnectionProtocol.PLAY, new HashMap()
   );
   protected static final Map<ConnectionProtocol, Map<Identifier, IPayloadHandler<?>>> SERVERBOUND_HANDLERS = ImmutableMap.of(
      ConnectionProtocol.CONFIGURATION, new HashMap(), ConnectionProtocol.PLAY, new HashMap()
   );
   protected static final Map<ConnectionProtocol, Map<Identifier, IPayloadHandler<?>>> CLIENTBOUND_HANDLERS = ImmutableMap.of(
      ConnectionProtocol.CONFIGURATION, new HashMap(), ConnectionProtocol.PLAY, new HashMap()
   );
   protected static boolean setup = false;

   protected NetworkRegistry() {
   }

   public static void setup() {
      if (setup) {
         throw new IllegalStateException("The network registry can only be setup once.");
      } else {
         ModLoader.postEvent(new RegisterPayloadHandlersEvent());
         setup = true;
      }
   }

   public static <T extends CustomPacketPayload, B extends FriendlyByteBuf> void register(
      CustomPacketPayload.Type<T> type,
      StreamCodec<? super B, T> codec,
      @Nullable IPayloadHandler<T> serverHandler,
      @Nullable IPayloadHandler<T> clientHandler,
      List<ConnectionProtocol> protocols,
      Optional<PacketFlow> flow,
      String version,
      boolean optional
   ) {
      if (setup) {
         throw new UnsupportedOperationException("Cannot register payload " + type.id() + " after registration phase.");
      } else if (protocols.isEmpty()) {
         throw new UnsupportedOperationException("Cannot register payload " + type.id() + " with no protocols.");
      } else if (version.isBlank()) {
         throw new UnsupportedOperationException("Cannot register payload " + type.id() + " with a blank version.");
      } else if ("minecraft".equals(type.id().getNamespace())) {
         throw new UnsupportedOperationException("Cannot register payload " + type.id() + " using the domain \"minecraft\".");
      } else {
         PayloadRegistration<T> reg = new PayloadRegistration<>(type, codec, protocols, flow, version.strip(), optional);
         if (reg.matchesFlow(PacketFlow.SERVERBOUND) && serverHandler == null) {
            throw new IllegalArgumentException("Cannot register serverbound payload " + type.id() + " without a server-side handler");
         } else {
            for (ConnectionProtocol protocol : protocols) {
               Map<Identifier, PayloadRegistration<?>> byProtocol = PAYLOAD_REGISTRATIONS.get(protocol);
               if (byProtocol == null) {
                  throw new UnsupportedOperationException("Cannot register payload " + type.id() + " for unsupported protocol: " + protocol.name());
               }

               if (byProtocol.containsKey(type.id())) {
                  throw new UnsupportedOperationException("Cannot register payload " + type.id() + " as it is already registered.");
               }

               byProtocol.put(type.id(), reg);
               if (serverHandler != null && reg.matchesFlow(PacketFlow.SERVERBOUND)) {
                  registerHandler(SERVERBOUND_HANDLERS, protocol, PacketFlow.SERVERBOUND, type, serverHandler);
               }

               if (clientHandler != null && reg.matchesFlow(PacketFlow.CLIENTBOUND)) {
                  registerHandler(CLIENTBOUND_HANDLERS, protocol, PacketFlow.CLIENTBOUND, type, clientHandler);
               }
            }
         }
      }
   }

   protected static <T extends CustomPacketPayload> void registerHandler(
      Map<ConnectionProtocol, Map<Identifier, IPayloadHandler<?>>> handlers,
      ConnectionProtocol protocol,
      PacketFlow flow,
      CustomPacketPayload.Type<T> type,
      IPayloadHandler<T> handler
   ) {
      Map<Identifier, IPayloadHandler<?>> byProtocol = handlers.get(protocol);
      if (byProtocol == null) {
         throw new UnsupportedOperationException("Cannot register handler for payload " + type.id() + " for unsupported protocol: " + protocol.name());
      } else if (byProtocol.containsKey(type.id())) {
         throw new UnsupportedOperationException("Duplicate " + flow.id() + " handler registration for payload " + type.id());
      } else {
         byProtocol.put(type.id(), handler);
      }
   }

   @Nullable
   public static StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload> getCodec(Identifier id, ConnectionProtocol protocol, PacketFlow flow) {
      if (!BUILTIN_PAYLOADS.containsKey(id)) {
         if (PAYLOAD_REGISTRATIONS.containsKey(protocol)) {
            PayloadRegistration<?> registration = PAYLOAD_REGISTRATIONS.get(protocol).get(id);
            if (registration == null) {
               LOGGER.warn("No registration for payload {}; refusing to decode.", id);
               return null;
            } else if (registration.flow().isPresent() && registration.flow().get() != flow) {
               LOGGER.warn(
                  "Received {} on the {}, expected to receive on the {}; refusing to decode.",
                  new Object[]{id, flow.getReceptionSide(), registration.flow().get().getReceptionSide()}
               );
               return null;
            } else {
               return (StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload>)registration.codec();
            }
         } else {
            LOGGER.error("A packet was received while not in the configuration or play phase.");
            dumpStackToLog();
            return null;
         }
      } else {
         return flow != PacketFlow.SERVERBOUND || !id.equals(ModdedNetworkPayload.ID) && !id.equals(ModdedNetworkSetupFailedPayload.ID)
            ? BUILTIN_PAYLOADS.get(id)
            : null;
      }
   }

   public static boolean isModdedPayload(CustomPacketPayload payload) {
      return !(payload instanceof DiscardedPayload) && !"minecraft".equals(payload.type().id().getNamespace());
   }

   public static void handleModdedPayload(ServerCommonPacketListener listener, ServerboundCustomPayloadPacket packet) {
      NetworkPayloadSetup payloadSetup = ChannelAttributes.getPayloadSetup(listener.getConnection());
      if (payloadSetup == null) {
         LOGGER.warn("Received a modded payload before channel negotiation; disconnecting.");
         listener.disconnect(
            Component.translatable("multiplayer.disconnect.incompatible", "NeoForge %s (No Payload Setup)".formatted(NeoForgeVersion.getVersion()))
         );
      } else {
         ServerPayloadContext context = new ServerPayloadContext(listener, packet.payload().type().id());
         if (SERVERBOUND_HANDLERS.containsKey(listener.protocol())) {
            NetworkChannel channel = payloadSetup.getChannel(listener.protocol(), context.payloadId());
            if (channel == null && !hasAdhocChannel(listener.protocol(), context.payloadId(), PacketFlow.SERVERBOUND)) {
               LOGGER.warn("Received a modded payload {} with an unknown or unaccepted channel; disconnecting.", context.payloadId());
               listener.disconnect(
                  Component.translatable(
                     "multiplayer.disconnect.incompatible",
                     "NeoForge %s (No Channel for %s)".formatted(NeoForgeVersion.getVersion(), context.payloadId().toString())
                  )
               );
               return;
            }

            IPayloadHandler handler = SERVERBOUND_HANDLERS.get(listener.protocol()).get(context.payloadId());
            if (handler == null) {
               LOGGER.error("Received a modded payload {} with no registration; disconnecting.", context.payloadId());
               listener.disconnect(
                  Component.translatable(
                     "multiplayer.disconnect.incompatible",
                     "NeoForge %s (No Handler for %s)".formatted(NeoForgeVersion.getVersion(), context.payloadId().toString())
                  )
               );
               dumpStackToLog();
               return;
            }

            handler.handle(packet.payload(), context);
         } else {
            LOGGER.error("Received a modded payload {} while not in the configuration or play phase; disconnecting.", context.payloadId());
            listener.disconnect(
               Component.translatable(
                  "multiplayer.disconnect.incompatible",
                  "NeoForge %s (Invalid Protocol %s)".formatted(NeoForgeVersion.getVersion(), listener.protocol().name())
               )
            );
         }
      }
   }

   public static void initializeNeoForgeConnection(
      ServerConfigurationPacketListener listener, Map<ConnectionProtocol, Set<ModdedNetworkQueryComponent>> clientChannels
   ) {
      ChannelAttributes.setPayloadSetup(listener.getConnection(), NetworkPayloadSetup.empty());
      ChannelAttributes.setConnectionType(listener.getConnection(), listener.getConnectionType());
      Map<ConnectionProtocol, NegotiationResult> results = new IdentityHashMap<>();

      for (ConnectionProtocol protocol : PAYLOAD_REGISTRATIONS.keySet()) {
         NegotiationResult negotiationResult = NetworkComponentNegotiator.negotiate(
            PAYLOAD_REGISTRATIONS.get(protocol).values().stream().map(NegotiableNetworkComponent::new).toList(),
            clientChannels.getOrDefault(protocol, Collections.emptySet()).stream().map(NegotiableNetworkComponent::new).toList()
         );
         if (!negotiationResult.success()) {
            if (!negotiationResult.failureReasons().isEmpty()) {
               listener.send(new ModdedNetworkSetupFailedPayload(negotiationResult.failureReasons()));
            }

            listener.disconnect(Component.translatable("multiplayer.disconnect.incompatible", "NeoForge %s".formatted(NeoForgeVersion.getVersion())));
            return;
         }

         results.put(protocol, negotiationResult);
      }

      NetworkPayloadSetup setup = NetworkPayloadSetup.from(results);
      ChannelAttributes.setPayloadSetup(listener.getConnection(), setup);
      NetworkFilters.injectIfNecessary(listener.getConnection());
      listener.send(new ModdedNetworkPayload(setup));
      Builder<Identifier> nowListeningOn = ImmutableSet.builder();
      nowListeningOn.addAll(getInitialListeningChannels(listener.flow()));
      nowListeningOn.addAll(setup.getChannels(ConnectionProtocol.CONFIGURATION).keySet());
      listener.send(new MinecraftRegisterPayload(nowListeningOn.build()));
   }

   public static boolean initializeOtherConnection(ServerConfigurationPacketListener listener) {
      NetworkFilters.cleanIfNecessary(listener.getConnection());
      ChannelAttributes.setPayloadSetup(listener.getConnection(), NetworkPayloadSetup.empty());
      ChannelAttributes.setConnectionType(listener.getConnection(), listener.getConnectionType());

      for (ConnectionProtocol protocol : PAYLOAD_REGISTRATIONS.keySet()) {
         NegotiationResult negotiationResult = NetworkComponentNegotiator.negotiate(
            PAYLOAD_REGISTRATIONS.get(protocol)
               .entrySet()
               .stream()
               .map(entry -> new NegotiableNetworkComponent(entry.getKey(), entry.getValue().version(), entry.getValue().flow(), entry.getValue().optional()))
               .toList(),
            List.of()
         );
         if (!negotiationResult.success()) {
            listener.disconnect(
               Component.translatableWithFallback(
                  "neoforge.network.negotiation.failure.vanilla.client.not_supported",
                  "You are trying to connect to a server that is running NeoForge, but you are not. Please install NeoForge Version: %s to connect to this server.",
                  NeoForgeVersion.getVersion()
               )
            );
            return false;
         }
      }

      NetworkFilters.injectIfNecessary(listener.getConnection());
      Builder<Identifier> nowListeningOn = ImmutableSet.builder();
      nowListeningOn.addAll(getInitialListeningChannels(listener.flow()));
      PAYLOAD_REGISTRATIONS.get(ConnectionProtocol.CONFIGURATION)
         .entrySet()
         .stream()
         .filter(registration -> registration.getValue().matchesFlow(listener.flow()))
         .filter(registration -> registration.getValue().optional())
         .forEach(registration -> nowListeningOn.add(registration.getKey()));
      listener.send(new MinecraftRegisterPayload(nowListeningOn.build()));
      return true;
   }

   public static void checkPacket(Packet<?> packet, ServerCommonPacketListener listener) {
      if (packet instanceof ClientboundCustomPayloadPacket customPayloadPacket) {
         Identifier id = customPayloadPacket.payload().type().id();
         if (!BUILTIN_PAYLOADS.containsKey(id) && !"minecraft".equals(id.getNamespace())) {
            if (!hasChannel(listener, customPayloadPacket.payload().type().id())) {
               throw new UnsupportedOperationException("Payload %s may not be sent to the client!".formatted(customPayloadPacket.payload().type().id()));
            }
         }
      }
   }

   public static void checkPacket(Packet<?> packet, ClientCommonPacketListener listener) {
      if (packet instanceof ServerboundCustomPayloadPacket customPayloadPacket) {
         Identifier id = customPayloadPacket.payload().type().id();
         if (!BUILTIN_PAYLOADS.containsKey(id) && !"minecraft".equals(id.getNamespace())) {
            if (!hasChannel(listener, customPayloadPacket.payload().type().id())) {
               throw new UnsupportedOperationException("Payload %s may not be sent to the server!".formatted(customPayloadPacket.payload().type().id()));
            }
         }
      }
   }

   protected static boolean hasAdhocChannel(ConnectionProtocol protocol, Identifier id, PacketFlow flow) {
      PayloadRegistration<?> reg = PAYLOAD_REGISTRATIONS.getOrDefault(protocol, Collections.emptyMap()).get(id);
      return reg != null && reg.optional() && reg.matchesFlow(flow);
   }

   public static boolean hasChannel(ICommonPacketListener listener, Identifier payloadId) {
      return hasChannel(listener.getConnection(), listener.protocol(), payloadId);
   }

   public static boolean hasChannel(Connection connection, @Nullable ConnectionProtocol protocol, Identifier payloadId) {
      NetworkPayloadSetup payloadSetup = ChannelAttributes.getPayloadSetup(connection);
      if (payloadSetup != null) {
         if (protocol != null && payloadSetup.getChannels(protocol).containsKey(payloadId)) {
            return true;
         }

         if (protocol == null && payloadSetup.channels().values().stream().anyMatch(map -> map.containsKey(payloadId))) {
            return true;
         }
      }

      return protocol != null && ChannelAttributes.getOrCreateCommonChannels(connection, protocol).contains(payloadId)
         ? true
         : ChannelAttributes.getOrCreateAdHocChannels(connection).contains(payloadId);
   }

   public static <T extends PacketListener> List<Packet<?>> filterGameBundlePackets(ChannelHandlerContext context, Iterable<Packet<? super T>> packets) {
      NetworkPayloadSetup payloadSetup = (NetworkPayloadSetup)context.channel().attr(ChannelAttributes.PAYLOAD_SETUP).get();
      if (payloadSetup == null) {
         LOGGER.trace("Somebody tried to filter bundled packets to a client that has not negotiated with the server. Not filtering.");
         return Lists.newArrayList(packets.iterator());
      } else {
         List<Packet<?>> toSend = new ArrayList<>();
         packets.forEach(
            packet -> {
               if (packet instanceof ClientboundCustomPayloadPacket customPayloadPacket) {
                  Identifier id = customPayloadPacket.payload().type().id();
                  if (!BUILTIN_PAYLOADS.containsKey(id) && !"minecraft".equals(id.getNamespace())) {
                     NetworkChannel channel = payloadSetup.getChannel(ConnectionProtocol.PLAY, customPayloadPacket.payload().type().id());
                     if (channel == null) {
                        LOGGER.trace(
                           "Somebody tried to send: {} to a client which cannot accept it. Not sending packet.", customPayloadPacket.payload().type().id()
                        );
                     } else {
                        toSend.add((Packet<?>)packet);
                     }
                  } else {
                     toSend.add((Packet<?>)packet);
                  }
               } else {
                  toSend.add((Packet<?>)packet);
               }
            }
         );
         return toSend;
      }
   }

   @VisibleForTesting
   public static void configureMockConnection(Connection connection) {
      ChannelAttributes.setPayloadSetup(connection, NetworkPayloadSetup.empty());
      ChannelAttributes.setConnectionType(connection, ConnectionType.NEOFORGE);
      NetworkPayloadSetup setup = new NetworkPayloadSetup(
         PAYLOAD_REGISTRATIONS.entrySet()
            .stream()
            .map(
               entry -> Map.entry(
                  entry.getKey(),
                  entry.getValue()
                     .values()
                     .stream()
                     .map(reg -> new NetworkChannel(reg.id(), reg.version()))
                     .collect(Collectors.toMap(NetworkChannel::id, Function.identity()))
               )
            )
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue))
      );
      ChannelAttributes.setPayloadSetup(connection, setup);
      NetworkFilters.injectIfNecessary(connection);
   }

   public static void onMinecraftRegister(Connection connection, Set<Identifier> resourceLocations) {
      Set<Identifier> channels = ChannelAttributes.getOrCreateAdHocChannels(connection);
      if (channels.size() + resourceLocations.size() > NeoForgeCommonConfig.INSTANCE.maxAdhocChannelLimit.getAsInt()) {
         if (connection.getPacketListener() instanceof ICommonPacketListener listener) {
            listener.disconnect(Component.translatable("neoforge.network.adhoc_channels.too_many"));
         } else {
            throw new RuntimeException("Too many ad-hoc channels.");
         }
      } else {
         channels.addAll(resourceLocations);
      }
   }

   public static void onMinecraftUnregister(Connection connection, Set<Identifier> resourceLocations) {
      ChannelAttributes.getOrCreateAdHocChannels(connection).removeAll(resourceLocations);
   }

   public static Set<Identifier> getInitialListeningChannels(PacketFlow flow) {
      return BUILTIN_PAYLOADS.keySet();
   }

   public static Set<Identifier> getInitialServerUnregisterChannels() {
      Builder<Identifier> nowForgottenChannels = ImmutableSet.builder();
      nowForgottenChannels.add(MinecraftRegisterPayload.ID);
      nowForgottenChannels.add(MinecraftUnregisterPayload.ID);
      PAYLOAD_REGISTRATIONS.get(ConnectionProtocol.PLAY)
         .entrySet()
         .stream()
         .filter(registration -> registration.getValue().flow().isEmpty() || registration.getValue().flow().get() == PacketFlow.SERVERBOUND)
         .filter(registration -> registration.getValue().optional())
         .forEach(registration -> nowForgottenChannels.add(registration.getKey()));
      return nowForgottenChannels.build();
   }

   public static void checkCommonVersion(ICommonPacketListener listener, CommonVersionPayload payload) {
      List<Integer> otherVersions = payload.versions();
      if (otherVersions.stream().noneMatch(SUPPORTED_COMMON_NETWORKING_VERSIONS::contains)) {
         String versions = String.join(", ", SUPPORTED_COMMON_NETWORKING_VERSIONS.stream().map(i -> i.toString()).toList());
         listener.disconnect(Component.literal("Unsupported common network version. This installation of NeoForge only supports: " + versions));
      }

      if (listener.protocol() == ConnectionProtocol.CONFIGURATION) {
         if (listener.flow() == PacketFlow.SERVERBOUND) {
            ((ServerConfigurationPacketListener)listener).finishCurrentTask(CommonVersionTask.TYPE);
         } else {
            listener.send(new CommonVersionPayload());
         }
      }
   }

   public static void onCommonRegister(ICommonPacketListener listener, CommonRegisterPayload payload) {
      Set<Identifier> channels = ChannelAttributes.getOrCreateCommonChannels(listener.getConnection(), payload.protocol());
      channels.clear();
      channels.addAll(payload.channels());
      if (listener.protocol() == ConnectionProtocol.CONFIGURATION) {
         if (listener.flow() == PacketFlow.SERVERBOUND) {
            ((ServerConfigurationPacketListener)listener).finishCurrentTask(CommonRegisterTask.TYPE);
         } else {
            listener.send(new CommonRegisterPayload(1, ConnectionProtocol.PLAY, getCommonPlayChannels(PacketFlow.CLIENTBOUND)));
         }
      }
   }

   public static Set<Identifier> getCommonPlayChannels(PacketFlow flow) {
      return PAYLOAD_REGISTRATIONS.get(ConnectionProtocol.PLAY)
         .entrySet()
         .stream()
         .filter(registration -> registration.getValue().matchesFlow(flow))
         .filter(registration -> registration.getValue().optional())
         .map(registration -> registration.getKey())
         .collect(Collectors.toSet());
   }

   public static void onConfigurationFinished(ICommonPacketListener listener) {
      NetworkPayloadSetup setup = ChannelAttributes.getPayloadSetup(listener.getConnection());
      if (setup == null) {
         LOGGER.error(
            "Somebody tried to finish the configuration phase of a connection that has not performed channel negotiation. Not finishing configuration."
         );
      } else {
         Builder<Identifier> notListeningAnymoreOn = ImmutableSet.builder();
         notListeningAnymoreOn.addAll(getInitialListeningChannels(listener.flow()));
         notListeningAnymoreOn.addAll(setup.getChannels(ConnectionProtocol.CONFIGURATION).keySet());
         listener.send(new MinecraftUnregisterPayload(notListeningAnymoreOn.build()));
         Builder<Identifier> nowListeningOn = ImmutableSet.builder();
         nowListeningOn.add(MinecraftRegisterPayload.ID);
         nowListeningOn.add(MinecraftUnregisterPayload.ID);
         if (listener.getConnectionType().isNeoForge()) {
            nowListeningOn.add(ModdedNetworkQueryPayload.ID);
         } else {
            nowListeningOn.addAll(getCommonPlayChannels(listener.flow()));
         }

         listener.send(new MinecraftRegisterPayload(nowListeningOn.build()));
      }
   }

   public static ConnectionType getConnectionType(Connection connection) {
      return Objects.requireNonNull(ChannelAttributes.getConnectionType(connection), "no connection type on connection!");
   }

   public static <T> CompletableFuture<T> guard(CompletableFuture<T> future, Identifier payloadId) {
      return future.exceptionally(ex -> {
         LOGGER.error("Failed to process a synchronized task of the payload: %s".formatted(payloadId), ex);
         return null;
      });
   }

   public static <T extends PacketListener> void handlePacketUnchecked(Packet<T> packet, PacketListener listener) {
      try {
         packet.handle((T)listener);
      } catch (ClassCastException var3) {
         throw new IllegalStateException("Attempted to handle a packet in a listener that does not support it.", var3);
      }
   }

   private static void dumpStackToLog() {
      LOGGER.error("", new Exception("Stack Trace"));
   }
}
