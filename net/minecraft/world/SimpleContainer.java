package net.minecraft.world;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SimpleContainer implements Container, StackedContentsCompatible {
   private final int size;
   private final NonNullList<ItemStack> items;
   @Nullable
   private List<ContainerListener> listeners;

   public SimpleContainer(int p_19150_) {
      this.size = p_19150_;
      this.items = NonNullList.withSize(p_19150_, ItemStack.EMPTY);
   }

   public SimpleContainer(ItemStack... p_19152_) {
      this.size = p_19152_.length;
      this.items = NonNullList.of(ItemStack.EMPTY, p_19152_);
   }

   public void addListener(ContainerListener p_19165_) {
      if (this.listeners == null) {
         this.listeners = Lists.newArrayList();
      }

      this.listeners.add(p_19165_);
   }

   public void removeListener(ContainerListener p_19182_) {
      if (this.listeners != null) {
         this.listeners.remove(p_19182_);
      }
   }

   @Override
   public ItemStack getItem(int p_19157_) {
      return p_19157_ >= 0 && p_19157_ < this.items.size() ? this.items.get(p_19157_) : ItemStack.EMPTY;
   }

   public List<ItemStack> removeAllItems() {
      List<ItemStack> $$0 = this.items.stream().filter(p_19197_ -> !p_19197_.isEmpty()).collect(Collectors.toList());
      this.clearContent();
      return $$0;
   }

   @Override
   public ItemStack removeItem(int p_19159_, int p_19160_) {
      ItemStack $$2 = ContainerHelper.removeItem(this.items, p_19159_, p_19160_);
      if (!$$2.isEmpty()) {
         this.setChanged();
      }

      return $$2;
   }

   public ItemStack removeItemType(Item p_19171_, int p_19172_) {
      ItemStack $$2 = new ItemStack(p_19171_, 0);

      for (int $$3 = this.size - 1; $$3 >= 0; $$3--) {
         ItemStack $$4 = this.getItem($$3);
         if ($$4.getItem().equals(p_19171_)) {
            int $$5 = p_19172_ - $$2.getCount();
            ItemStack $$6 = $$4.split($$5);
            $$2.grow($$6.getCount());
            if ($$2.getCount() == p_19172_) {
               break;
            }
         }
      }

      if (!$$2.isEmpty()) {
         this.setChanged();
      }

      return $$2;
   }

   public ItemStack addItem(ItemStack p_19174_) {
      if (p_19174_.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack $$1 = p_19174_.copy();
         this.moveItemToOccupiedSlotsWithSameType($$1);
         if ($$1.isEmpty()) {
            return ItemStack.EMPTY;
         } else {
            this.moveItemToEmptySlots($$1);
            return $$1.isEmpty() ? ItemStack.EMPTY : $$1;
         }
      }
   }

   public boolean canAddItem(ItemStack p_19184_) {
      boolean $$1 = false;

      for (ItemStack $$2 : this.items) {
         if ($$2.isEmpty() || ItemStack.isSameItemSameComponents($$2, p_19184_) && $$2.getCount() < $$2.getMaxStackSize()) {
            $$1 = true;
            break;
         }
      }

      return $$1;
   }

   @Override
   public ItemStack removeItemNoUpdate(int p_19180_) {
      ItemStack $$1 = this.items.get(p_19180_);
      if ($$1.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         this.items.set(p_19180_, ItemStack.EMPTY);
         return $$1;
      }
   }

   @Override
   public void setItem(int p_19162_, ItemStack p_19163_) {
      this.items.set(p_19162_, p_19163_);
      p_19163_.limitSize(this.getMaxStackSize(p_19163_));
      this.setChanged();
   }

   @Override
   public int getContainerSize() {
      return this.size;
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack $$0 : this.items) {
         if (!$$0.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public void setChanged() {
      if (this.listeners != null) {
         for (ContainerListener $$0 : this.listeners) {
            $$0.containerChanged(this);
         }
      }
   }

   @Override
   public boolean stillValid(Player p_19167_) {
      return true;
   }

   @Override
   public void clearContent() {
      this.items.clear();
      this.setChanged();
   }

   @Override
   public void fillStackedContents(StackedContents p_19169_) {
      for (ItemStack $$1 : this.items) {
         p_19169_.accountStack($$1);
      }
   }

   @Override
   public String toString() {
      return this.items.stream().filter(p_19194_ -> !p_19194_.isEmpty()).collect(Collectors.toList()).toString();
   }

   private void moveItemToEmptySlots(ItemStack p_19190_) {
      for (int $$1 = 0; $$1 < this.size; $$1++) {
         ItemStack $$2 = this.getItem($$1);
         if ($$2.isEmpty()) {
            this.setItem($$1, p_19190_.copyAndClear());
            return;
         }
      }
   }

   private void moveItemToOccupiedSlotsWithSameType(ItemStack p_19192_) {
      for (int $$1 = 0; $$1 < this.size; $$1++) {
         ItemStack $$2 = this.getItem($$1);
         if (ItemStack.isSameItemSameComponents($$2, p_19192_)) {
            this.moveItemsBetweenStacks(p_19192_, $$2);
            if (p_19192_.isEmpty()) {
               return;
            }
         }
      }
   }

   private void moveItemsBetweenStacks(ItemStack p_19186_, ItemStack p_19187_) {
      int $$2 = this.getMaxStackSize(p_19187_);
      int $$3 = Math.min(p_19186_.getCount(), $$2 - p_19187_.getCount());
      if ($$3 > 0) {
         p_19187_.grow($$3);
         p_19186_.shrink($$3);
         this.setChanged();
      }
   }

   public void fromTag(ListTag p_19178_, HolderLookup.Provider p_331527_) {
      this.clearContent();

      for (int $$2 = 0; $$2 < p_19178_.size(); $$2++) {
         ItemStack.parse(p_331527_, p_19178_.getCompound($$2)).ifPresent(this::addItem);
      }
   }

   public ListTag createTag(HolderLookup.Provider p_330840_) {
      ListTag $$1 = new ListTag();

      for (int $$2 = 0; $$2 < this.getContainerSize(); $$2++) {
         ItemStack $$3 = this.getItem($$2);
         if (!$$3.isEmpty()) {
            $$1.add($$3.save(p_330840_));
         }
      }

      return $$1;
   }

   public NonNullList<ItemStack> getItems() {
      return this.items;
   }
}
