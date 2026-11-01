package net.minecraft.world.item.component;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.apache.commons.lang3.math.Fraction;
import org.jspecify.annotations.Nullable;

public final class BundleContents implements TooltipComponent {
   public static final BundleContents EMPTY = new BundleContents(List.of());
   public static final Codec<BundleContents> CODEC = ItemStack.CODEC
      .listOf()
      .flatXmap(BundleContents::checkAndCreate, p_381696_ -> DataResult.success(p_381696_.items));
   public static final StreamCodec<RegistryFriendlyByteBuf, BundleContents> STREAM_CODEC = ItemStack.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(BundleContents::new, p_331551_ -> p_331551_.items);
   private static final Fraction BUNDLE_IN_BUNDLE_WEIGHT = Fraction.getFraction(1, 16);
   private static final int NO_STACK_INDEX = -1;
   public static final int NO_SELECTED_ITEM_INDEX = -1;
   final List<ItemStack> items;
   final Fraction weight;
   final int selectedItem;

   BundleContents(List<ItemStack> p_331277_, Fraction p_339622_, int p_361490_) {
      this.items = p_331277_;
      this.weight = p_339622_;
      this.selectedItem = p_361490_;
   }

   private static DataResult<BundleContents> checkAndCreate(List<ItemStack> p_381706_) {
      try {
         Fraction fraction = computeContentWeight(p_381706_);
         return DataResult.success(new BundleContents(p_381706_, fraction, -1));
      } catch (ArithmeticException var2) {
         return DataResult.error(() -> "Excessive total bundle weight");
      }
   }

   public BundleContents(List<ItemStack> p_331417_) {
      this(p_331417_, computeContentWeight(p_331417_), -1);
   }

   private static Fraction computeContentWeight(List<ItemStack> p_331148_) {
      Fraction fraction = Fraction.ZERO;

      for (ItemStack itemstack : p_331148_) {
         fraction = fraction.add(getWeight(itemstack).multiplyBy(Fraction.getFraction(itemstack.getCount(), 1)));
      }

      return fraction;
   }

   static Fraction getWeight(ItemStack p_332084_) {
      BundleContents bundlecontents = p_332084_.get(DataComponents.BUNDLE_CONTENTS);
      if (bundlecontents != null) {
         return BUNDLE_IN_BUNDLE_WEIGHT.add(bundlecontents.weight());
      } else {
         List<BeehiveBlockEntity.Occupant> list = p_332084_.getOrDefault(DataComponents.BEES, Bees.EMPTY).bees();
         return !list.isEmpty() ? Fraction.ONE : Fraction.getFraction(1, p_332084_.getMaxStackSize());
      }
   }

   public static boolean canItemBeInBundle(ItemStack p_362945_) {
      return !p_362945_.isEmpty() && p_362945_.canFitInsideContainerItems();
   }

   public int getNumberOfItemsToShow() {
      int i = this.size();
      int j = i > 12 ? 11 : 12;
      int k = i % 4;
      int l = k == 0 ? 0 : 4 - k;
      return Math.min(i, j - l);
   }

   public ItemStack getItemUnsafe(int p_330802_) {
      return this.items.get(p_330802_);
   }

   public Stream<ItemStack> itemCopyStream() {
      return this.items.stream().map(ItemStack::copy);
   }

   public Iterable<ItemStack> items() {
      return this.items;
   }

   public Iterable<ItemStack> itemsCopy() {
      return Lists.transform(this.items, ItemStack::copy);
   }

   public int size() {
      return this.items.size();
   }

   public Fraction weight() {
      return this.weight;
   }

   public boolean isEmpty() {
      return this.items.isEmpty();
   }

   public int getSelectedItem() {
      return this.selectedItem;
   }

   public boolean hasSelectedItem() {
      return this.selectedItem != -1;
   }

   @Override
   public boolean equals(Object p_331898_) {
      if (this == p_331898_) {
         return true;
      } else {
         return p_331898_ instanceof BundleContents bundlecontents
            ? this.weight.equals(bundlecontents.weight) && ItemStack.listMatches(this.items, bundlecontents.items)
            : false;
      }
   }

   @Override
   public int hashCode() {
      return ItemStack.hashStackList(this.items);
   }

   @Override
   public String toString() {
      return "BundleContents" + this.items;
   }

   public static class Mutable {
      private final List<ItemStack> items;
      private Fraction weight;
      private int selectedItem;

      public Mutable(BundleContents p_332039_) {
         this.items = new ArrayList<>(p_332039_.items);
         this.weight = p_332039_.weight;
         this.selectedItem = p_332039_.selectedItem;
      }

      public BundleContents.Mutable clearItems() {
         this.items.clear();
         this.weight = Fraction.ZERO;
         this.selectedItem = -1;
         return this;
      }

      private int findStackIndex(ItemStack p_331941_) {
         if (!p_331941_.isStackable()) {
            return -1;
         } else {
            for (int i = 0; i < this.items.size(); i++) {
               if (ItemStack.isSameItemSameComponents(this.items.get(i), p_331941_)) {
                  return i;
               }
            }

            return -1;
         }
      }

      private int getMaxAmountToAdd(ItemStack p_330527_) {
         Fraction fraction = Fraction.ONE.subtract(this.weight);
         return Math.max(fraction.divideBy(BundleContents.getWeight(p_330527_)).intValue(), 0);
      }

      public int tryInsert(ItemStack p_331789_) {
         if (!BundleContents.canItemBeInBundle(p_331789_)) {
            return 0;
         } else {
            int i = Math.min(p_331789_.getCount(), this.getMaxAmountToAdd(p_331789_));
            if (i == 0) {
               return 0;
            } else {
               this.weight = this.weight.add(BundleContents.getWeight(p_331789_).multiplyBy(Fraction.getFraction(i, 1)));
               int j = this.findStackIndex(p_331789_);
               if (j != -1) {
                  ItemStack itemstack = this.items.remove(j);
                  ItemStack itemstack1 = itemstack.copyWithCount(itemstack.getCount() + i);
                  p_331789_.shrink(i);
                  this.items.add(0, itemstack1);
               } else {
                  this.items.add(0, p_331789_.split(i));
               }

               return i;
            }
         }
      }

      public int tryTransfer(Slot p_330834_, Player p_331924_) {
         ItemStack itemstack = p_330834_.getItem();
         int i = this.getMaxAmountToAdd(itemstack);
         return BundleContents.canItemBeInBundle(itemstack) ? this.tryInsert(p_330834_.safeTake(itemstack.getCount(), i, p_331924_)) : 0;
      }

      public void toggleSelectedItem(int p_360571_) {
         this.selectedItem = this.selectedItem != p_360571_ && !this.indexIsOutsideAllowedBounds(p_360571_) ? p_360571_ : -1;
      }

      private boolean indexIsOutsideAllowedBounds(int p_393944_) {
         return p_393944_ < 0 || p_393944_ >= this.items.size();
      }

      @Nullable
      public ItemStack removeOne() {
         if (this.items.isEmpty()) {
            return null;
         } else {
            int i = this.indexIsOutsideAllowedBounds(this.selectedItem) ? 0 : this.selectedItem;
            ItemStack itemstack = this.items.remove(i).copy();
            this.weight = this.weight.subtract(BundleContents.getWeight(itemstack).multiplyBy(Fraction.getFraction(itemstack.getCount(), 1)));
            this.toggleSelectedItem(-1);
            return itemstack;
         }
      }

      public Fraction weight() {
         return this.weight;
      }

      public BundleContents toImmutable() {
         return new BundleContents(List.copyOf(this.items), this.weight, this.selectedItem);
      }
   }
}
