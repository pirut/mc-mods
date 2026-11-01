package net.neoforged.neoforge.fluids.capability.templates;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public class VoidFluidHandler implements IFluidHandler {
   public static final VoidFluidHandler INSTANCE = new VoidFluidHandler();

   @Override
   public int getTanks() {
      return 1;
   }

   @Override
   public FluidStack getFluidInTank(int tank) {
      return FluidStack.EMPTY;
   }

   @Override
   public int getTankCapacity(int tank) {
      return Integer.MAX_VALUE;
   }

   @Override
   public boolean isFluidValid(int tank, FluidStack stack) {
      return true;
   }

   @Override
   public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
      return resource.getAmount();
   }

   @Override
   public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
      return FluidStack.EMPTY;
   }

   @Override
   public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
      return FluidStack.EMPTY;
   }
}
