package net.neoforged.neoforge.fluids;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public interface IFluidTank {
   FluidStack getFluid();

   int getFluidAmount();

   int getCapacity();

   boolean isFluidValid(FluidStack var1);

   int fill(FluidStack var1, IFluidHandler.FluidAction var2);

   FluidStack drain(int var1, IFluidHandler.FluidAction var2);

   FluidStack drain(FluidStack var1, IFluidHandler.FluidAction var2);
}
