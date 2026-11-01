package net.neoforged.neoforge.network.configuration;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.payload.FeatureFlagAcknowledgePayload;
import net.neoforged.neoforge.network.payload.FeatureFlagDataPayload;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;

@Internal
public record CheckFeatureFlags(ServerConfigurationPacketListener listener) implements ConfigurationTask {
   public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(Identifier.fromNamespaceAndPath("neoforge", "check_feature_flags"));
   private static final Logger LOGGER = LogUtils.getLogger();
   private static Set<Identifier> moddedFlags = null;

   @Override
   public void start(Consumer<Packet<?>> packetSender) {
      if (this.listener.getConnection().isMemoryConnection()) {
         this.listener.finishCurrentTask(TYPE);
      } else {
         Set<Identifier> moddedFlags = getModdedFeatureFlags();
         if (!this.listener.getConnectionType().isOther() && this.listener.hasChannel(FeatureFlagDataPayload.TYPE)) {
            packetSender.accept(new FeatureFlagDataPayload(moddedFlags).toVanillaClientbound());
         } else {
            if (!moddedFlags.isEmpty()) {
               this.listener.disconnect(Component.literal("This server does not support vanilla clients as it has custom FeatureFlags"));
            } else {
               this.listener.finishCurrentTask(TYPE);
            }
         }
      }
   }

   public static void handleClientboundPayload(FeatureFlagDataPayload payload, IPayloadContext context) {
      Set<Identifier> localFlags = getModdedFeatureFlags();
      Set<Identifier> remoteFlags = payload.moddedFlags();
      if (localFlags.equals(remoteFlags)) {
         context.reply(FeatureFlagAcknowledgePayload.INSTANCE);
      } else {
         context.disconnect(Component.translatable("neoforge.network.feature_flags.entry_mismatch"));
         StringBuilder message = new StringBuilder("The server and client have different sets of custom FeatureFlags");
         Set<Identifier> missingLocal = Sets.difference(remoteFlags, localFlags);
         if (!missingLocal.isEmpty()) {
            message.append("\n\tFlags missing on the client, but present on the server:");

            for (Identifier flag : missingLocal) {
               message.append("\n\t\t- ").append(flag);
            }
         }

         Set<Identifier> missingRemote = Sets.difference(localFlags, remoteFlags);
         if (!missingRemote.isEmpty()) {
            message.append("\n\tFlags missing on the server, but present on the client:");

            for (Identifier flag : missingRemote) {
               message.append("\n\t\t- ").append(flag);
            }
         }

         LOGGER.warn(message.toString());
      }
   }

   public static void handleServerboundPayload(FeatureFlagAcknowledgePayload payload, IPayloadContext context) {
      context.finishCurrentTask(TYPE);
   }

   public static boolean handleVanillaServerConnection(ClientConfigurationPacketListener listener) {
      if (!getModdedFeatureFlags().isEmpty()) {
         listener.disconnect(Component.translatable("neoforge.network.feature_flags.no_vanilla_server"));
         return false;
      } else {
         return true;
      }
   }

   private static Set<Identifier> getModdedFeatureFlags() {
      if (moddedFlags == null) {
         moddedFlags = FeatureFlags.REGISTRY
            .getAllFlags()
            .entrySet()
            .stream()
            .filter(e -> e.getValue().isModded())
            .map(Entry::getKey)
            .collect(Collectors.toSet());
      }

      return moddedFlags;
   }

   @Override
   public ConfigurationTask.Type type() {
      return TYPE;
   }
}
