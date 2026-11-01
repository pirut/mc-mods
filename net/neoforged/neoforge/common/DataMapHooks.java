package net.neoforged.neoforge.common;

import com.google.common.collect.UnmodifiableIterator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.Oxidizable;
import net.neoforged.neoforge.registries.datamaps.builtin.Waxable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public class DataMapHooks {
   @Internal
   public static boolean didHaveToFallbackToVanillaMaps = false;
   private static final Map<Block, Block> INVERSE_OXIDIZABLES_DATAMAP_INTERNAL = new HashMap<>();
   private static final Map<Block, Block> INVERSE_WAXABLES_DATAMAP_INTERNAL = new HashMap<>();
   public static final Map<Block, Block> INVERSE_OXIDIZABLES_DATAMAP = Collections.unmodifiableMap(INVERSE_OXIDIZABLES_DATAMAP_INTERNAL);
   public static final Map<Block, Block> INVERSE_WAXABLES_DATAMAP = Collections.unmodifiableMap(INVERSE_WAXABLES_DATAMAP_INTERNAL);

   @Nullable
   public static Block getNextOxidizedStage(Block block) {
      Oxidizable oxidizable = block.builtInRegistryHolder().getData(NeoForgeDataMaps.OXIDIZABLES);
      return oxidizable != null ? oxidizable.nextOxidationStage() : (Block)WeatheringCopper.NEXT_BY_BLOCK.get().get(block);
   }

   @Nullable
   public static Block getPreviousOxidizedStage(Block block) {
      return INVERSE_OXIDIZABLES_DATAMAP.containsKey(block)
         ? INVERSE_OXIDIZABLES_DATAMAP.get(block)
         : (Block)WeatheringCopper.PREVIOUS_BY_BLOCK.get().get(block);
   }

   @Nullable
   public static Block getBlockWaxed(Block block) {
      Waxable waxable = block.builtInRegistryHolder().getData(NeoForgeDataMaps.WAXABLES);
      return waxable != null ? waxable.waxed() : (Block)HoneycombItem.WAXABLES.get().get(block);
   }

   @Nullable
   public static Block getBlockUnwaxed(Block block) {
      return INVERSE_WAXABLES_DATAMAP.containsKey(block) ? INVERSE_WAXABLES_DATAMAP.get(block) : (Block)HoneycombItem.WAX_OFF_BY_BLOCK.get().get(block);
   }

   @Internal
   public static FuelValues populateFuelValues(RegistryAccess lookupProvider, FeatureFlagSet features) {
      FuelValues.Builder builder = new FuelValues.Builder(lookupProvider, features);
      Registry<Item> registry = lookupProvider.lookupOrThrow(Registries.ITEM);
      registry.getDataMap(NeoForgeDataMaps.FURNACE_FUELS).forEach((key, fuel) -> builder.add(registry.getValue((ResourceKey<Item>)key), fuel.burnTime()));
      return builder.build();
   }

   @SubscribeEvent
   static void onDataMapsUpdated(DataMapsUpdatedEvent event) {
      event.ifRegistry(
         Registries.BLOCK,
         registry -> {
            INVERSE_OXIDIZABLES_DATAMAP_INTERNAL.clear();
            INVERSE_WAXABLES_DATAMAP_INTERNAL.clear();
            registry.getDataMap(NeoForgeDataMaps.OXIDIZABLES).forEach((resourceKey, oxidizable) -> {
               Block block = BuiltInRegistries.BLOCK.getValue((ResourceKey<Block>)resourceKey);
               INVERSE_OXIDIZABLES_DATAMAP_INTERNAL.put(oxidizable.nextOxidationStage(), block);
               UnmodifiableIterator var3 = block.getStateDefinition().getPossibleStates().iterator();

               while (var3.hasNext()) {
                  BlockState state = (BlockState)var3.next();
                  state.initCache();
               }
            });
            WeatheringCopper.PREVIOUS_BY_BLOCK.get().forEach((after, before) -> {
               if (!INVERSE_OXIDIZABLES_DATAMAP_INTERNAL.containsKey(after)) {
                  INVERSE_OXIDIZABLES_DATAMAP_INTERNAL.put(after, before);
                  didHaveToFallbackToVanillaMaps = true;
               }
            });
            registry.getDataMap(NeoForgeDataMaps.WAXABLES)
               .forEach(
                  (resourceKey, waxable) -> INVERSE_WAXABLES_DATAMAP_INTERNAL.put(
                     waxable.waxed(), BuiltInRegistries.BLOCK.getValue((ResourceKey<Block>)resourceKey)
                  )
               );
            HoneycombItem.WAX_OFF_BY_BLOCK.get().forEach((after, before) -> {
               if (!INVERSE_WAXABLES_DATAMAP_INTERNAL.containsKey(after)) {
                  INVERSE_OXIDIZABLES_DATAMAP_INTERNAL.put(after, before);
                  didHaveToFallbackToVanillaMaps = true;
               }
            });
         }
      );
   }
}
