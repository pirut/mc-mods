package net.neoforged.neoforge.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public abstract class StackCopySlot extends Slot {
   private static final Container emptyInventory = new SimpleContainer(0);
   @Nullable
   private ItemStack cachedReturnedStack = null;

   public StackCopySlot(int x, int y) {
      super(emptyInventory, 0, x, y);
   }

   protected abstract ItemStack getStackCopy();

   protected abstract void setStackCopy(ItemStack var1);

   @Override
   public final ItemStack getItem() {
      return this.cachedReturnedStack = this.getStackCopy();
   }

   @Override
   public final void set(ItemStack stack) {
      this.setStackCopy(stack);
      this.cachedReturnedStack = stack;
   }

   @Override
   public final void setChanged() {
      if (this.cachedReturnedStack != null && !ItemStack.matches(this.cachedReturnedStack, this.getStackCopy())) {
         this.set(this.cachedReturnedStack);
      }
   }

   @Override
   public final ItemStack remove(int amount) {
      ItemStack stack = this.getStackCopy().copy();
      ItemStack ret = stack.split(amount);
      this.set(stack);
      this.cachedReturnedStack = null;
      return ret;
   }
}
