package net.neoforged.neoforge.items;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public class ItemStackHandler implements IItemHandler, IItemHandlerModifiable, ValueIOSerializable {
   private static final Logger LOGGER = LoggerFactory.getLogger(ItemStackHandler.class);
   protected NonNullList<ItemStack> stacks;

   public ItemStackHandler() {
      this(1);
   }

   public ItemStackHandler(int size) {
      this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
   }

   public ItemStackHandler(NonNullList<ItemStack> stacks) {
      this.stacks = stacks;
   }

   public void setSize(int size) {
      this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
   }

   @Override
   public void setStackInSlot(int slot, ItemStack stack) {
      this.validateSlotIndex(slot);
      this.stacks.set(slot, stack);
      this.onContentsChanged(slot);
   }

   @Override
   public int getSlots() {
      return this.stacks.size();
   }

   @Override
   public ItemStack getStackInSlot(int slot) {
      this.validateSlotIndex(slot);
      return this.stacks.get(slot);
   }

   @Override
   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      if (stack.isEmpty()) {
         return ItemStack.EMPTY;
      } else if (!this.isItemValid(slot, stack)) {
         return stack;
      } else {
         this.validateSlotIndex(slot);
         ItemStack existing = this.stacks.get(slot);
         int limit = this.getStackLimit(slot, stack);
         if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(stack, existing)) {
               return stack;
            }

            limit -= existing.getCount();
         }

         if (limit <= 0) {
            return stack;
         } else {
            boolean reachedLimit = stack.getCount() > limit;
            if (!simulate) {
               if (existing.isEmpty()) {
                  this.stacks.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
               } else {
                  existing.grow(reachedLimit ? limit : stack.getCount());
               }

               this.onContentsChanged(slot);
            }

            return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
         }
      }
   }

   @Override
   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      if (amount == 0) {
         return ItemStack.EMPTY;
      } else {
         this.validateSlotIndex(slot);
         ItemStack existing = this.stacks.get(slot);
         if (existing.isEmpty()) {
            return ItemStack.EMPTY;
         } else {
            int toExtract = Math.min(amount, existing.getMaxStackSize());
            if (existing.getCount() <= toExtract) {
               if (!simulate) {
                  this.stacks.set(slot, ItemStack.EMPTY);
                  this.onContentsChanged(slot);
                  return existing;
               } else {
                  return existing.copy();
               }
            } else {
               if (!simulate) {
                  this.stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
                  this.onContentsChanged(slot);
               }

               return existing.copyWithCount(toExtract);
            }
         }
      }
   }

   @Override
   public int getSlotLimit(int slot) {
      return 99;
   }

   protected int getStackLimit(int slot, ItemStack stack) {
      return Math.min(this.getSlotLimit(slot), stack.getMaxStackSize());
   }

   @Override
   public boolean isItemValid(int slot, ItemStack stack) {
      return true;
   }

   @Override
   public void serialize(ValueOutput output) {
      ValueOutput.TypedOutputList<ItemStackWithSlot> itemList = output.list("Items", ItemStackWithSlot.CODEC);

      for (int i = 0; i < this.stacks.size(); i++) {
         ItemStack stack = this.stacks.get(i);
         if (!stack.isEmpty()) {
            itemList.add(new ItemStackWithSlot(i, stack));
         }
      }

      output.putInt("Size", this.stacks.size());
   }

   @Override
   public void deserialize(ValueInput input) {
      this.setSize(input.getIntOr("Size", this.stacks.size()));
      input.<ItemStackWithSlot>listOrEmpty("Items", ItemStackWithSlot.CODEC).forEach(slot -> {
         if (slot.isValidInContainer(this.stacks.size())) {
            this.stacks.set(slot.slot(), slot.stack());
         }
      });
   }

   protected void validateSlotIndex(int slot) {
      if (slot < 0 || slot >= this.stacks.size()) {
         throw new RuntimeException("Slot " + slot + " not in valid range - [0," + this.stacks.size() + ")");
      }
   }

   protected void onLoad() {
   }

   protected void onContentsChanged(int slot) {
   }
}
