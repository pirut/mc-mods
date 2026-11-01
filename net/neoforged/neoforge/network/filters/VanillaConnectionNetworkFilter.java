package net.neoforged.neoforge.network.filters;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandler.Sharable;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagNetworkSerialization;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.registries.RegistryManager;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;

@Sharable
@Internal
public class VanillaConnectionNetworkFilter extends VanillaPacketFilter {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ConnectionType connectionType;

   public VanillaConnectionNetworkFilter(ConnectionType connectionType) {
      super(
         ImmutableMap.builder()
            .put(handler(ClientboundUpdateAttributesPacket.class, VanillaConnectionNetworkFilter::filterEntityProperties))
            .put(handler(ClientboundCommandsPacket.class, VanillaConnectionNetworkFilter::filterCommandList))
            .put(handler(ClientboundUpdateTagsPacket.class, VanillaConnectionNetworkFilter::filterCustomTagTypes))
            .build()
      );
      this.connectionType = connectionType;
   }

   @Override
   public boolean isNecessary(Connection manager) {
      return !this.connectionType.isNeoForge();
   }

   private static ClientboundUpdateAttributesPacket filterEntityProperties(ClientboundUpdateAttributesPacket msg) {
      ClientboundUpdateAttributesPacket newPacket = new ClientboundUpdateAttributesPacket(msg.getEntityId(), Collections.emptyList());
      msg.getValues().stream().filter(snapshot -> {
         Identifier key = snapshot.attribute().unwrapKey().map(ResourceKey::identifier).orElse(null);
         return key != null && key.getNamespace().equals("minecraft");
      }).forEach(snapshot -> newPacket.getValues().add(snapshot));
      return newPacket;
   }

   private static ClientboundCommandsPacket filterCommandList(ClientboundCommandsPacket packet) {
      CommandBuildContext commandBuildContext = Commands.createValidationContext(VanillaRegistries.createLookup());
      RootCommandNode<SharedSuggestionProvider> root = packet.getRoot(commandBuildContext, CommandTreeCleaner.COMMAND_NODE_BUILDER);
      RootCommandNode<SharedSuggestionProvider> newRoot = CommandTreeCleaner.cleanArgumentTypes(root, argType -> {
         ArgumentTypeInfo<?, ?> info = ArgumentTypeInfos.byClass(argType);
         Identifier id = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(info);
         return id != null && (id.getNamespace().equals("minecraft") || id.getNamespace().equals("brigadier"));
      });
      return new ClientboundCommandsPacket(newRoot, CommandTreeCleaner.COMMAND_NODE_INSPECTOR);
   }

   private static ClientboundUpdateTagsPacket filterCustomTagTypes(ClientboundUpdateTagsPacket packet) {
      Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags = packet.getTags()
         .entrySet()
         .stream()
         .filter(e -> isVanillaRegistry(e.getKey().identifier()))
         .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      return new ClientboundUpdateTagsPacket(tags);
   }

   private static boolean isVanillaRegistry(Identifier location) {
      return RegistryManager.getVanillaRegistryKeys().contains(location)
         || VanillaRegistries.DATAPACK_REGISTRY_KEYS.stream().anyMatch(k -> k.identifier().equals(location));
   }
}
