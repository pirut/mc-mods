package net.neoforged.neoforge.client.network.registration;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.network.handling.ClientPayloadContext;
import net.neoforged.neoforge.common.NeoForgeVersion;
import net.neoforged.neoforge.network.configuration.CheckExtensibleEnums;
import net.neoforged.neoforge.network.configuration.CheckFeatureFlags;
import net.neoforged.neoforge.network.filters.NetworkFilters;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.handling.MainThreadPayloadHandler;
import net.neoforged.neoforge.network.negotiation.NegotiableNetworkComponent;
import net.neoforged.neoforge.network.negotiation.NegotiationResult;
import net.neoforged.neoforge.network.negotiation.NetworkComponentNegotiator;
import net.neoforged.neoforge.network.payload.MinecraftRegisterPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.NetworkChannel;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;

@Internal
public final class ClientNetworkRegistry extends NetworkRegistry {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static boolean setupClient = false;

   private ClientNetworkRegistry() {
   }

   public static void setup() {
      if (!NetworkRegistry.setup) {
         throw new IllegalStateException("ClientNetworkRegistry cannot be set up before main NetworkRegistry");
      } else if (setupClient) {
         throw new IllegalStateException("The client network registry can only be set up once.");
      } else {
         ModLoader.postEvent(new RegisterClientPayloadHandlersEvent());
         List<Identifier> missingHandlers = PAYLOAD_REGISTRATIONS.values().stream().map(Map::values).flatMap(Collection::stream).filter(reg -> {
            if (!reg.matchesFlow(PacketFlow.CLIENTBOUND)) {
               return false;
            } else {
               for (ConnectionProtocol protocol : reg.protocols()) {
                  if (!CLIENTBOUND_HANDLERS.get(protocol).containsKey(reg.id())) {
                     return true;
                  }
               }

               return false;
            }
         }).map(PayloadRegistration::id).distinct().toList();
         if (!missingHandlers.isEmpty()) {
            throw new IllegalStateException("Some clientbound payloads are missing client-side handlers: " + missingHandlers);
         } else {
            setupClient = true;
         }
      }
   }

   public static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, HandlerThread thread, IPayloadHandler<T> handler) {
      if (setupClient) {
         throw new UnsupportedOperationException("Cannot register client-side handler for payload " + type.id() + " after registration phase.");
      } else {
         if (thread == HandlerThread.MAIN) {
            handler = new MainThreadPayloadHandler<>(handler);
         }

         boolean found = false;

         for (Entry<ConnectionProtocol, Map<Identifier, PayloadRegistration<?>>> entry : PAYLOAD_REGISTRATIONS.entrySet()) {
            ConnectionProtocol protocol = entry.getKey();
            Map<Identifier, PayloadRegistration<?>> registrations = entry.getValue();
            PayloadRegistration<T> registration = (PayloadRegistration<T>)registrations.get(type.id());
            if (registration != null) {
               if (!registration.matchesFlow(PacketFlow.CLIENTBOUND)) {
                  throw new IllegalArgumentException("Cannot register client handler for serverbound payload " + type);
               }

               found = true;
               registerHandler(CLIENTBOUND_HANDLERS, protocol, PacketFlow.CLIENTBOUND, type, handler);
            }
         }

         if (!found) {
            throw new IllegalArgumentException("Cannot register client handler for unknown payload type " + type);
         }
      }
   }

   public static void handleModdedPayload(ClientCommonPacketListener listener, ClientboundCustomPayloadPacket packet) {
      NetworkPayloadSetup payloadSetup = ChannelAttributes.getPayloadSetup(listener.getConnection());
      if (payloadSetup == null) {
         LOGGER.warn("Received a modded payload before channel negotiation; disconnecting.");
         listener.getConnection()
            .disconnect(Component.translatable("multiplayer.disconnect.incompatible", "NeoForge %s (No Payload Setup)".formatted(NeoForgeVersion.getVersion())));
      } else {
         Identifier payloadId = packet.payload().type().id();
         ClientPayloadContext context = new ClientPayloadContext(listener, payloadId);
         if (CLIENTBOUND_HANDLERS.containsKey(listener.protocol())) {
            NetworkChannel channel = payloadSetup.getChannel(listener.protocol(), payloadId);
            if (channel == null && !hasAdhocChannel(listener.protocol(), packet.payload().type().id(), PacketFlow.CLIENTBOUND)) {
               LOGGER.warn("Received a modded payload with an unknown or unaccepted channel; disconnecting.");
               listener.getConnection()
                  .disconnect(
                     Component.translatable(
                        "multiplayer.disconnect.incompatible", "NeoForge %s (No Channel for %s)".formatted(NeoForgeVersion.getVersion(), payloadId.toString())
                     )
                  );
               return;
            }

            IPayloadHandler handler = CLIENTBOUND_HANDLERS.get(listener.protocol()).get(payloadId);
            if (handler == null) {
               LOGGER.error("Received a modded payload with no registration; disconnecting.");
               listener.getConnection()
                  .disconnect(
                     Component.translatable(
                        "multiplayer.disconnect.incompatible", "NeoForge %s (No Handler for %s)".formatted(NeoForgeVersion.getVersion(), payloadId.toString())
                     )
                  );
               dumpStackToLog();
               return;
            }

            handler.handle(packet.payload(), context);
         } else {
            LOGGER.error("Received a modded payload while not in the configuration or play phase. Disconnecting.");
            listener.getConnection()
               .disconnect(
                  Component.translatable(
                     "multiplayer.disconnect.incompatible",
                     "NeoForge %s (Invalid Protocol %s)".formatted(NeoForgeVersion.getVersion(), listener.protocol().name())
                  )
               );
         }
      }
   }

   public static void onNetworkQuery(ClientConfigurationPacketListener listener) {
      listener.send(ModdedNetworkQueryPayload.fromRegistry(PAYLOAD_REGISTRATIONS));
   }

   public static void initializeNeoForgeConnection(ClientConfigurationPacketListener listener, NetworkPayloadSetup setup) {
      ChannelAttributes.setPayloadSetup(listener.getConnection(), setup);
      ChannelAttributes.setConnectionType(listener.getConnection(), listener.getConnectionType());
      NetworkFilters.injectIfNecessary(listener.getConnection());
      Builder<Identifier> nowListeningOn = ImmutableSet.builder();
      nowListeningOn.addAll(getInitialListeningChannels(listener.flow()));
      nowListeningOn.addAll(setup.getChannels(ConnectionProtocol.CONFIGURATION).keySet());
      listener.send(new MinecraftRegisterPayload(nowListeningOn.build()));
   }

   public static void initializeOtherConnection(ClientConfigurationPacketListener listener) {
      ChannelAttributes.setPayloadSetup(listener.getConnection(), NetworkPayloadSetup.empty());
      ChannelAttributes.setConnectionType(listener.getConnection(), listener.getConnectionType());

      for (ConnectionProtocol protocol : PAYLOAD_REGISTRATIONS.keySet()) {
         NegotiationResult negotiationResult = NetworkComponentNegotiator.negotiate(
            List.of(),
            PAYLOAD_REGISTRATIONS.get(protocol)
               .entrySet()
               .stream()
               .map(entry -> new NegotiableNetworkComponent(entry.getKey(), entry.getValue().version(), entry.getValue().flow(), entry.getValue().optional()))
               .toList()
         );
         if (!negotiationResult.success()) {
            listener.getConnection()
               .disconnect(
                  Component.translatableWithFallback(
                     "neoforge.network.negotiation.failure.vanilla.server.not_supported",
                     "You are trying to connect to a server that is not running NeoForge, but you have mods that require it. A connection could not be established.",
                     NeoForgeVersion.getVersion()
                  )
               );
            return;
         }
      }

      if (CheckExtensibleEnums.handleVanillaServerConnection(listener)) {
         if (CheckFeatureFlags.handleVanillaServerConnection(listener)) {
            ConfigTracker.INSTANCE.loadDefaultServerConfigs();
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
         }
      }
   }

   private static void dumpStackToLog() {
      LOGGER.error("", new Exception("Stack Trace"));
   }
}
