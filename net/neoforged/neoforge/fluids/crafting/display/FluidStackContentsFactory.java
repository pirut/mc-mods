package net.neoforged.neoforge.fluids.crafting.display;

import net.neoforged.neoforge.fluids.FluidStack;

public class FluidStackContentsFactory implements ForFluidStacks<FluidStack> {
   public static final FluidStackContentsFactory INSTANCE = new FluidStackContentsFactory();

   public FluidStack forStack(FluidStack fluid) {
      return fluid;
   }
}
