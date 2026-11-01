package net.neoforged.neoforge.items.wrapper;

import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public class ForwardingItemHandler implements IItemHandler {
   protected final Supplier<IItemHandler> delegate;

   public ForwardingItemHandler(IItemHandler delegate) {
      this.delegate = () -> delegate;
   }

   public ForwardingItemHandler(Supplier<IItemHandler> delegate) {
      this.delegate = delegate;
   }

   @Override
   public int getSlots() {
      return this.delegate.get().getSlots();
   }

   @Override
   public ItemStack getStackInSlot(int slot) {
      return this.delegate.get().getStackInSlot(slot);
   }

   @Override
   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      return this.delegate.get().insertItem(slot, stack, simulate);
   }

   @Override
   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return this.delegate.get().extractItem(slot, amount, simulate);
   }

   @Override
   public int getSlotLimit(int slot) {
      return this.delegate.get().getSlotLimit(slot);
   }

   @Override
   public boolean isItemValid(int slot, ItemStack stack) {
      return this.delegate.get().isItemValid(slot, stack);
   }
}
