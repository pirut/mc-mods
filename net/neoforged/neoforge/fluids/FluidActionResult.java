package net.neoforged.neoforge.fluids;

import net.minecraft.world.item.ItemStack;

public class FluidActionResult {
   public static final FluidActionResult FAILURE = new FluidActionResult(false, ItemStack.EMPTY);
   public final boolean success;
   public final ItemStack result;

   public FluidActionResult(ItemStack result) {
      this(true, result);
   }

   private FluidActionResult(boolean success, ItemStack result) {
      this.success = success;
      this.result = result;
   }

   public boolean isSuccess() {
      return this.success;
   }

   public ItemStack getResult() {
      return this.result;
   }
}
