package net.neoforged.neoforge.transfer.fluid;

import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.StacksResourceHandler;

public class FluidStacksResourceHandler extends StacksResourceHandler<FluidStack, FluidResource> {
   protected int capacity;

   public FluidStacksResourceHandler(int size, int capacity) {
      super(size, FluidStack.EMPTY, FluidStack.OPTIONAL_CODEC);
      this.capacity = capacity;
   }

   public FluidStacksResourceHandler(NonNullList<FluidStack> stacks, int capacity) {
      super(stacks, FluidStack.EMPTY, FluidStack.OPTIONAL_CODEC);
      this.capacity = capacity;
   }

   public FluidResource getResourceFrom(FluidStack stack) {
      return FluidResource.of(stack);
   }

   public int getAmountFrom(FluidStack stack) {
      return stack.getAmount();
   }

   protected FluidStack getStackFrom(FluidResource resource, int amount) {
      return resource.toStack(amount);
   }

   protected int getCapacity(int index, FluidResource resource) {
      return this.capacity;
   }

   protected FluidStack copyOf(FluidStack stack) {
      return stack.copy();
   }

   public boolean matches(FluidStack stack, FluidResource resource) {
      return resource.matches(stack);
   }
}
