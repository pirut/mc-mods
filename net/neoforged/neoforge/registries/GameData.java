package net.neoforged.neoforge.registries;

import com.mojang.logging.LogUtils;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMapper;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@Internal
public class GameData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Marker REGISTRIES = MarkerFactory.getMarker("REGISTRIES");

   public static Map<Block, Item> getBlockItemMap() {
      return NeoForgeRegistryCallbacks.ItemCallbacks.BLOCK_TO_ITEM_MAP;
   }

   public static IdMapper<BlockState> getBlockStateIDMap() {
      return NeoForgeRegistryCallbacks.BlockCallbacks.BLOCKSTATE_TO_ID_MAP;
   }

   public static Map<BlockState, Holder<PoiType>> getBlockStatePointOfInterestTypeMap() {
      return NeoForgeRegistryCallbacks.PoiTypeCallbacks.BLOCKSTATE_TO_POI_TYPE_MAP;
   }

   public static void vanillaSnapshot() {
      LOGGER.debug(REGISTRIES, "Creating vanilla freeze snapshot");
      RegistryManager.takeVanillaSnapshot();
      LOGGER.debug(REGISTRIES, "Vanilla freeze snapshot created");
   }

   public static void unfreezeData() {
      LOGGER.debug(REGISTRIES, "Unfreezing registries");
      BuiltInRegistries.REGISTRY.stream().filter(r -> r instanceof BaseMappedRegistry).forEach(r -> ((BaseMappedRegistry)r).unfreeze(true));
   }

   public static void freezeData() {
      LOGGER.debug(REGISTRIES, "Freezing registries");
      BuiltInRegistries.REGISTRY.stream().filter(r -> r instanceof MappedRegistry).forEach(r -> {
         ((MappedRegistry)r).bindAllTagsToEmpty();
         ((MappedRegistry)r).freeze();
      });
      RegistryManager.takeFrozenSnapshot();
      LOGGER.debug(REGISTRIES, "All registries frozen");
   }

   public static void postRegisterEvents() {
      Set<Identifier> ordered = getRegistrationOrder();
      RuntimeException aggregate = new RuntimeException();

      for (Identifier rootRegistryName : ordered) {
         try {
            ResourceKey<? extends Registry<?>> registryKey = ResourceKey.createRegistryKey(rootRegistryName);
            Registry<?> registry = Objects.requireNonNull((Registry<?>)BuiltInRegistries.REGISTRY.getValue(rootRegistryName));
            RegisterEvent registerEvent = new RegisterEvent(registryKey, registry);
            StartupNotificationManager.modLoaderMessage("REGISTERING " + registryKey.identifier());
            ModLoader.postEventWrapContainerInModOrder(registerEvent);
         } catch (Throwable var7) {
            aggregate.addSuppressed(var7);
         }
      }

      if (aggregate.getSuppressed().length > 0) {
         LOGGER.error("Failed to register some entries, see suppressed exceptions for details", aggregate);
         LOGGER.error("Rolling back to VANILLA state");
         RegistryManager.revertToVanilla();
         throw aggregate;
      } else {
         CommonHooks.modifyAttributes();
         SpawnPlacements.fireSpawnPlacementEvent();
         ModLoader.postEvent(new BlockEntityTypeAddBlocksEvent());
         CreativeModeTabRegistry.sortTabs();
         GameRuleCategory.registerModdedCategories();
      }
   }

   public static Set<Identifier> getRegistrationOrder() {
      Set<Identifier> ordered = new LinkedHashSet<>();
      ordered.add(Registries.ATTRIBUTE.identifier());
      ordered.add(Registries.DATA_COMPONENT_TYPE.identifier());
      ordered.add(Registries.PARTICLE_TYPE.identifier());
      ordered.addAll(BuiltInRegistries.getVanillaRegistrationOrder());
      ordered.addAll(BuiltInRegistries.REGISTRY.keySet().stream().sorted(Identifier::compareNamespaced).toList());
      return ordered;
   }
}
