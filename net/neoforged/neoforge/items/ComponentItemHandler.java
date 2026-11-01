package net.neoforged.neoforge.items;

import com.google.common.base.Preconditions;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

public class ComponentItemHandler implements IItemHandlerModifiable {
   protected final MutableDataComponentHolder parent;
   protected final DataComponentType<ItemContainerContents> component;
   protected final int size;

   public ComponentItemHandler(MutableDataComponentHolder parent, DataComponentType<ItemContainerContents> component, int size) {
      this.parent = parent;
      this.component = component;
      this.size = size;
      Preconditions.checkArgument(size <= 256, "The max size of ItemContainerContents is 256 slots.");
   }

   @Override
   public int getSlots() {
      return this.size;
   }

   @Override
   public ItemStack getStackInSlot(int slot) {
      ItemContainerContents contents = this.getContents();
      return this.getStackFromContents(contents, slot);
   }

   @Override
   public void setStackInSlot(int slot, ItemStack stack) {
      this.validateSlotIndex(slot);
      if (!this.isItemValid(slot, stack)) {
         throw new RuntimeException("Invalid stack " + stack + " for slot " + slot + ")");
      } else {
         ItemContainerContents contents = this.getContents();
         ItemStack existing = this.getStackFromContents(contents, slot);
         if (!ItemStack.matches(stack, existing)) {
            this.updateContents(contents, stack, slot);
         }
      }
   }

   @Override
   public ItemStack insertItem(int slot, ItemStack toInsert, boolean simulate) {
      this.validateSlotIndex(slot);
      if (toInsert.isEmpty()) {
         return ItemStack.EMPTY;
      } else if (!this.isItemValid(slot, toInsert)) {
         return toInsert;
      } else {
         ItemContainerContents contents = this.getContents();
         ItemStack existing = this.getStackFromContents(contents, slot);
         int insertLimit = Math.min(this.getSlotLimit(slot), toInsert.getMaxStackSize());
         if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(toInsert, existing)) {
               return toInsert;
            }

            insertLimit -= existing.getCount();
         }

         if (insertLimit <= 0) {
            return toInsert;
         } else {
            int inserted = Math.min(insertLimit, toInsert.getCount());
            if (!simulate) {
               this.updateContents(contents, toInsert.copyWithCount(existing.getCount() + inserted), slot);
            }

            return toInsert.copyWithCount(toInsert.getCount() - inserted);
         }
      }
   }

   @Override
   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      this.validateSlotIndex(slot);
      if (amount == 0) {
         return ItemStack.EMPTY;
      } else {
         ItemContainerContents contents = this.getContents();
         ItemStack existing = this.getStackFromContents(contents, slot);
         if (existing.isEmpty()) {
            return ItemStack.EMPTY;
         } else {
            int toExtract = Math.min(amount, existing.getCount());
            if (!simulate) {
               this.updateContents(contents, existing.copyWithCount(existing.getCount() - toExtract), slot);
            }

            return existing.copyWithCount(toExtract);
         }
      }
   }

   @Override
   public int getSlotLimit(int slot) {
      return 99;
   }

   @Override
   public boolean isItemValid(int slot, ItemStack stack) {
      return stack.canFitInsideContainerItems();
   }

   protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {
   }

   protected ItemContainerContents getContents() {
      return this.parent.getOrDefault(this.component, ItemContainerContents.EMPTY);
   }

   protected ItemStack getStackFromContents(ItemContainerContents contents, int slot) {
      this.validateSlotIndex(slot);
      return contents.getSlots() <= slot ? ItemStack.EMPTY : contents.getStackInSlot(slot);
   }

   protected void updateContents(ItemContainerContents contents, ItemStack stack, int slot) {
      this.validateSlotIndex(slot);
      NonNullList<ItemStack> list = NonNullList.withSize(Math.max(contents.getSlots(), this.getSlots()), ItemStack.EMPTY);
      contents.copyInto(list);
      ItemStack oldStack = list.get(slot);
      list.set(slot, stack);
      this.parent.set(this.component, ItemContainerContents.fromItems(list));
      this.onContentsChanged(slot, oldStack, stack);
   }

   protected final void validateSlotIndex(int slot) {
      if (slot < 0 || slot >= this.getSlots()) {
         throw new RuntimeException("Slot " + slot + " not in valid range - [0," + this.getSlots() + ")");
      }
   }
}
