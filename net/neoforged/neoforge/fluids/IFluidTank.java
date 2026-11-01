package net.neoforged.neoforge.fluids;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public interface IFluidTank {
   FluidStack getFluid();

   int getFluidAmount();

   int getCapacity();

   boolean isFluidValid(FluidStack var1);

   int fill(FluidStack var1, IFluidHandler.FluidAction var2);

   FluidStack drain(int var1, IFluidHandler.FluidAction var2);

   FluidStack drain(FluidStack var1, IFluidHandler.FluidAction var2);
}
