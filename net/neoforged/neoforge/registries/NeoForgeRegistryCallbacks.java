package net.neoforged.neoforge.registries;

import com.google.common.collect.UnmodifiableIterator;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMapper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.neoforged.neoforge.registries.callback.AddCallback;
import net.neoforged.neoforge.registries.callback.BakeCallback;
import net.neoforged.neoforge.registries.callback.ClearCallback;

class NeoForgeRegistryCallbacks {
   static class AttributeCallbacks implements BakeCallback<Attribute> {
      static final NeoForgeRegistryCallbacks.AttributeCallbacks INSTANCE = new NeoForgeRegistryCallbacks.AttributeCallbacks();

      @Override
      public void onBake(Registry<Attribute> registry) {
         DefaultAttributes.validate();
      }
   }

   static class BlockCallbacks implements AddCallback<Block>, ClearCallback<Block>, BakeCallback<Block> {
      static final NeoForgeRegistryCallbacks.BlockCallbacks INSTANCE = new NeoForgeRegistryCallbacks.BlockCallbacks();
      static final NeoForgeRegistryCallbacks.BlockCallbacks.ClearableObjectIntIdentityMap<BlockState> BLOCKSTATE_TO_ID_MAP = new NeoForgeRegistryCallbacks.BlockCallbacks.ClearableObjectIntIdentityMap<>();
      private final Set<Block> addedBlocks = new ReferenceOpenHashSet();

      public void onAdd(Registry<Block> registry, int id, ResourceKey<Block> key, Block value) {
         this.addedBlocks.add(value);
      }

      @Override
      public void onClear(Registry<Block> registry, boolean full) {
         BLOCKSTATE_TO_ID_MAP.clear();
      }

      @Override
      public void onBake(Registry<Block> registry) {
         for (Block block : this.addedBlocks) {
            block.getStateDefinition().getPossibleStates().forEach(BlockBehaviour.BlockStateBase::initCache);
            block.getLootTable();
         }

         this.addedBlocks.clear();

         for (Block block : registry) {
            UnmodifiableIterator var4 = block.getStateDefinition().getPossibleStates().iterator();

            while (var4.hasNext()) {
               BlockState state = (BlockState)var4.next();
               BLOCKSTATE_TO_ID_MAP.add(state);
            }
         }

         DebugLevelSource.initValidStates();
      }

      private static class ClearableObjectIntIdentityMap<T> extends IdMapper<T> {
         void clear() {
            this.tToId.clear();
            this.idToT.clear();
            this.nextId = 0;
         }
      }
   }

   static class ItemCallbacks implements AddCallback<Item>, ClearCallback<Item> {
      static final NeoForgeRegistryCallbacks.ItemCallbacks INSTANCE = new NeoForgeRegistryCallbacks.ItemCallbacks();
      static final Map<Block, Item> BLOCK_TO_ITEM_MAP = new HashMap<>();

      public void onAdd(Registry<Item> registry, int id, ResourceKey<Item> key, Item item) {
         if (item instanceof BlockItem blockItem) {
            blockItem.registerBlocks(BLOCK_TO_ITEM_MAP, item);
         }
      }

      @Override
      public void onClear(Registry<Item> registry, boolean full) {
         if (full) {
            BLOCK_TO_ITEM_MAP.clear();
         }
      }
   }

   static class PoiTypeCallbacks implements AddCallback<PoiType>, ClearCallback<PoiType> {
      static final NeoForgeRegistryCallbacks.PoiTypeCallbacks INSTANCE = new NeoForgeRegistryCallbacks.PoiTypeCallbacks();
      static final Map<BlockState, Holder<PoiType>> BLOCKSTATE_TO_POI_TYPE_MAP = new HashMap<>();

      public void onAdd(Registry<PoiType> registry, int id, ResourceKey<PoiType> key, PoiType value) {
         value.matchingStates()
            .forEach(
               state -> {
                  Holder<PoiType> oldType = BLOCKSTATE_TO_POI_TYPE_MAP.put(state, registry.getHolderOrThrow(key));
                  if (oldType != null) {
                     throw new IllegalStateException(
                        String.format(
                           Locale.ENGLISH,
                           "Point of interest types %s and %s both list %s in their blockstates, this is not allowed. Blockstates can only have one point of interest type each.",
                           oldType,
                           value,
                           state
                        )
                     );
                  }
               }
            );
      }

      @Override
      public void onClear(Registry<PoiType> registry, boolean full) {
         if (full) {
            BLOCKSTATE_TO_POI_TYPE_MAP.clear();
         }
      }
   }
}
