package net.neoforged.neoforge.items.wrapper;

import com.google.common.base.Preconditions;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public class RangedWrapper implements IItemHandlerModifiable {
   private final IItemHandlerModifiable compose;
   private final int minSlot;
   private final int maxSlot;

   public RangedWrapper(IItemHandlerModifiable compose, int minSlot, int maxSlotExclusive) {
      Preconditions.checkArgument(maxSlotExclusive > minSlot, "Max slot must be greater than min slot");
      this.compose = compose;
      this.minSlot = minSlot;
      this.maxSlot = maxSlotExclusive;
   }

   @Override
   public int getSlots() {
      return this.maxSlot - this.minSlot;
   }

   @Override
   public ItemStack getStackInSlot(int slot) {
      return this.checkSlot(slot) ? this.compose.getStackInSlot(slot + this.minSlot) : ItemStack.EMPTY;
   }

   @Override
   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      return this.checkSlot(slot) ? this.compose.insertItem(slot + this.minSlot, stack, simulate) : stack;
   }

   @Override
   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return this.checkSlot(slot) ? this.compose.extractItem(slot + this.minSlot, amount, simulate) : ItemStack.EMPTY;
   }

   @Override
   public void setStackInSlot(int slot, ItemStack stack) {
      if (this.checkSlot(slot)) {
         this.compose.setStackInSlot(slot + this.minSlot, stack);
      }
   }

   @Override
   public int getSlotLimit(int slot) {
      return this.checkSlot(slot) ? this.compose.getSlotLimit(slot + this.minSlot) : 0;
   }

   @Override
   public boolean isItemValid(int slot, ItemStack stack) {
      return this.checkSlot(slot) ? this.compose.isItemValid(slot + this.minSlot, stack) : false;
   }

   private boolean checkSlot(int localSlot) {
      return localSlot + this.minSlot < this.maxSlot;
   }
}
