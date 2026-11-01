package net.minecraft.world.level.block.entity;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import java.util.Collections;
import java.util.SequencedSet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public class FuelValues {
   private final Object2IntSortedMap<Item> values;

   FuelValues(Object2IntSortedMap<Item> p_362184_) {
      this.values = p_362184_;
   }

   public boolean isFuel(ItemStack p_361943_) {
      return this.values.containsKey(p_361943_.getItem());
   }

   public SequencedSet<Item> fuelItems() {
      return Collections.unmodifiableSequencedSet(this.values.keySet());
   }

   @Deprecated
   public int burnDuration(ItemStack p_362816_) {
      return p_362816_.isEmpty() ? 0 : this.values.getInt(p_362816_.getItem());
   }

   public static FuelValues vanillaBurnTimes(HolderLookup.Provider p_360841_, FeatureFlagSet p_365144_) {
      return vanillaBurnTimes(p_360841_, p_365144_, 200);
   }

   public static FuelValues vanillaBurnTimes(HolderLookup.Provider p_362290_, FeatureFlagSet p_364291_, int p_363365_) {
      return vanillaBurnTimes(new FuelValues.Builder(p_362290_, p_364291_), p_363365_);
   }

   public static FuelValues vanillaBurnTimes(FuelValues.Builder builder, int p_363365_) {
      return builder.add(Items.LAVA_BUCKET, p_363365_ * 100)
         .add(Blocks.COAL_BLOCK, p_363365_ * 8 * 10)
         .add(Items.BLAZE_ROD, p_363365_ * 12)
         .add(Items.COAL, p_363365_ * 8)
         .add(Items.CHARCOAL, p_363365_ * 8)
         .add(ItemTags.LOGS, p_363365_ * 3 / 2)
         .add(ItemTags.BAMBOO_BLOCKS, p_363365_ * 3 / 2)
         .add(ItemTags.PLANKS, p_363365_ * 3 / 2)
         .add(Blocks.BAMBOO_MOSAIC, p_363365_ * 3 / 2)
         .add(ItemTags.WOODEN_STAIRS, p_363365_ * 3 / 2)
         .add(Blocks.BAMBOO_MOSAIC_STAIRS, p_363365_ * 3 / 2)
         .add(ItemTags.WOODEN_SLABS, p_363365_ * 3 / 4)
         .add(Blocks.BAMBOO_MOSAIC_SLAB, p_363365_ * 3 / 4)
         .add(ItemTags.WOODEN_TRAPDOORS, p_363365_ * 3 / 2)
         .add(ItemTags.WOODEN_PRESSURE_PLATES, p_363365_ * 3 / 2)
         .add(ItemTags.WOODEN_SHELVES, p_363365_ * 3 / 2)
         .add(ItemTags.WOODEN_FENCES, p_363365_ * 3 / 2)
         .add(ItemTags.FENCE_GATES, p_363365_ * 3 / 2)
         .add(Blocks.NOTE_BLOCK, p_363365_ * 3 / 2)
         .add(Blocks.BOOKSHELF, p_363365_ * 3 / 2)
         .add(Blocks.CHISELED_BOOKSHELF, p_363365_ * 3 / 2)
         .add(Blocks.LECTERN, p_363365_ * 3 / 2)
         .add(Blocks.JUKEBOX, p_363365_ * 3 / 2)
         .add(Blocks.CHEST, p_363365_ * 3 / 2)
         .add(Blocks.TRAPPED_CHEST, p_363365_ * 3 / 2)
         .add(Blocks.CRAFTING_TABLE, p_363365_ * 3 / 2)
         .add(Blocks.DAYLIGHT_DETECTOR, p_363365_ * 3 / 2)
         .add(ItemTags.BANNERS, p_363365_ * 3 / 2)
         .add(Items.BOW, p_363365_ * 3 / 2)
         .add(Items.FISHING_ROD, p_363365_ * 3 / 2)
         .add(Blocks.LADDER, p_363365_ * 3 / 2)
         .add(ItemTags.SIGNS, p_363365_)
         .add(ItemTags.HANGING_SIGNS, p_363365_ * 4)
         .add(Items.WOODEN_SHOVEL, p_363365_)
         .add(Items.WOODEN_SWORD, p_363365_)
         .add(Items.WOODEN_SPEAR, p_363365_)
         .add(Items.WOODEN_HOE, p_363365_)
         .add(Items.WOODEN_AXE, p_363365_)
         .add(Items.WOODEN_PICKAXE, p_363365_)
         .add(ItemTags.WOODEN_DOORS, p_363365_)
         .add(ItemTags.BOATS, p_363365_ * 6)
         .add(ItemTags.WOOL, p_363365_ / 2)
         .add(ItemTags.WOODEN_BUTTONS, p_363365_ / 2)
         .add(Items.STICK, p_363365_ / 2)
         .add(ItemTags.SAPLINGS, p_363365_ / 2)
         .add(Items.BOWL, p_363365_ / 2)
         .add(ItemTags.WOOL_CARPETS, 1 + p_363365_ / 3)
         .add(Blocks.DRIED_KELP_BLOCK, 1 + p_363365_ * 20)
         .add(Items.CROSSBOW, p_363365_ * 3 / 2)
         .add(Blocks.BAMBOO, p_363365_ / 4)
         .add(Blocks.DEAD_BUSH, p_363365_ / 2)
         .add(Blocks.SHORT_DRY_GRASS, p_363365_ / 2)
         .add(Blocks.TALL_DRY_GRASS, p_363365_ / 2)
         .add(Blocks.SCAFFOLDING, p_363365_ / 4)
         .add(Blocks.LOOM, p_363365_ * 3 / 2)
         .add(Blocks.BARREL, p_363365_ * 3 / 2)
         .add(Blocks.CARTOGRAPHY_TABLE, p_363365_ * 3 / 2)
         .add(Blocks.FLETCHING_TABLE, p_363365_ * 3 / 2)
         .add(Blocks.SMITHING_TABLE, p_363365_ * 3 / 2)
         .add(Blocks.COMPOSTER, p_363365_ * 3 / 2)
         .add(Blocks.AZALEA, p_363365_ / 2)
         .add(Blocks.FLOWERING_AZALEA, p_363365_ / 2)
         .add(Blocks.MANGROVE_ROOTS, p_363365_ * 3 / 2)
         .add(Blocks.LEAF_LITTER, p_363365_ / 2)
         .remove(ItemTags.NON_FLAMMABLE_WOOD)
         .build();
   }

   public static class Builder {
      private final HolderLookup<Item> items;
      private final FeatureFlagSet enabledFeatures;
      private final Object2IntSortedMap<Item> values = new Object2IntLinkedOpenHashMap();

      public Builder(HolderLookup.Provider p_364498_, FeatureFlagSet p_363815_) {
         this.items = p_364498_.lookupOrThrow(Registries.ITEM);
         this.enabledFeatures = p_363815_;
      }

      public FuelValues build() {
         return new FuelValues(this.values);
      }

      public FuelValues.Builder remove(TagKey<Item> p_360569_) {
         this.values.keySet().removeIf(p_363176_ -> p_363176_.builtInRegistryHolder().is(p_360569_));
         return this;
      }

      public FuelValues.Builder add(TagKey<Item> p_363594_, int p_361965_) {
         this.items.get(p_363594_).ifPresent(p_364314_ -> {
            for (Holder<Item> holder : p_364314_) {
               this.putInternal(p_361965_, holder.value());
            }
         });
         return this;
      }

      public FuelValues.Builder add(ItemLike p_361713_, int p_362628_) {
         Item item = p_361713_.asItem();
         this.putInternal(p_362628_, item);
         return this;
      }

      private void putInternal(int p_360649_, Item p_363984_) {
         if (p_363984_.isEnabled(this.enabledFeatures)) {
            this.values.put(p_363984_, p_360649_);
         }
      }
   }
}
