package net.minecraft.world;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class ContainerHelper {
   public static final String TAG_ITEMS = "Items";

   public static ItemStack removeItem(List<ItemStack> p_18970_, int p_18971_, int p_18972_) {
      return p_18971_ >= 0 && p_18971_ < p_18970_.size() && !p_18970_.get(p_18971_).isEmpty() && p_18972_ > 0
         ? p_18970_.get(p_18971_).split(p_18972_)
         : ItemStack.EMPTY;
   }

   public static ItemStack takeItem(List<ItemStack> p_18967_, int p_18968_) {
      return p_18968_ >= 0 && p_18968_ < p_18967_.size() ? p_18967_.set(p_18968_, ItemStack.EMPTY) : ItemStack.EMPTY;
   }

   public static CompoundTag saveAllItems(CompoundTag p_18974_, NonNullList<ItemStack> p_18975_, HolderLookup.Provider p_331375_) {
      return saveAllItems(p_18974_, p_18975_, true, p_331375_);
   }

   public static CompoundTag saveAllItems(CompoundTag p_18977_, NonNullList<ItemStack> p_18978_, boolean p_18979_, HolderLookup.Provider p_330957_) {
      ListTag $$4 = new ListTag();

      for (int $$5 = 0; $$5 < p_18978_.size(); $$5++) {
         ItemStack $$6 = p_18978_.get($$5);
         if (!$$6.isEmpty()) {
            CompoundTag $$7 = new CompoundTag();
            $$7.putByte("Slot", (byte)$$5);
            $$4.add($$6.save(p_330957_, $$7));
         }
      }

      if (!$$4.isEmpty() || p_18979_) {
         p_18977_.put("Items", $$4);
      }

      return p_18977_;
   }

   public static void loadAllItems(CompoundTag p_18981_, NonNullList<ItemStack> p_18982_, HolderLookup.Provider p_332027_) {
      ListTag $$3 = p_18981_.getList("Items", 10);

      for (int $$4 = 0; $$4 < $$3.size(); $$4++) {
         CompoundTag $$5 = $$3.getCompound($$4);
         int $$6 = $$5.getByte("Slot") & 255;
         if ($$6 >= 0 && $$6 < p_18982_.size()) {
            p_18982_.set($$6, ItemStack.parse(p_332027_, $$5).orElse(ItemStack.EMPTY));
         }
      }
   }

   public static int clearOrCountMatchingItems(Container p_18957_, Predicate<ItemStack> p_18958_, int p_18959_, boolean p_18960_) {
      int $$4 = 0;

      for (int $$5 = 0; $$5 < p_18957_.getContainerSize(); $$5++) {
         ItemStack $$6 = p_18957_.getItem($$5);
         int $$7 = clearOrCountMatchingItems($$6, p_18958_, p_18959_ - $$4, p_18960_);
         if ($$7 > 0 && !p_18960_ && $$6.isEmpty()) {
            p_18957_.setItem($$5, ItemStack.EMPTY);
         }

         $$4 += $$7;
      }

      return $$4;
   }

   public static int clearOrCountMatchingItems(ItemStack p_18962_, Predicate<ItemStack> p_18963_, int p_18964_, boolean p_18965_) {
      if (p_18962_.isEmpty() || !p_18963_.test(p_18962_)) {
         return 0;
      } else if (p_18965_) {
         return p_18962_.getCount();
      } else {
         int $$4 = p_18964_ < 0 ? p_18962_.getCount() : Math.min(p_18964_, p_18962_.getCount());
         p_18962_.shrink($$4);
         return $$4;
      }
   }
}
