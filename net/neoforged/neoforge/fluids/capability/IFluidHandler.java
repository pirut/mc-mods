package net.neoforged.neoforge.fluids.capability;

import java.util.Objects;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

@Deprecated(
   since = "1.21.9",
   forRemoval = true
)
public interface IFluidHandler {
   static IFluidHandler of(ResourceHandler<FluidResource> handler) {
      Objects.requireNonNull(handler);
      return new FluidResourceHandlerAdapter(handler);
   }

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   int getTanks();

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   FluidStack getFluidInTank(int var1);

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   int getTankCapacity(int var1);

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   boolean isFluidValid(int var1, FluidStack var2);

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   int fill(FluidStack var1, IFluidHandler.FluidAction var2);

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   FluidStack drain(FluidStack var1, IFluidHandler.FluidAction var2);

   @Deprecated(
      since = "1.21.9",
      forRemoval = true
   )
   FluidStack drain(int var1, IFluidHandler.FluidAction var2);

   public static enum FluidAction {
      EXECUTE,
      SIMULATE;

      public boolean execute() {
         return this == EXECUTE;
      }

      public boolean simulate() {
         return this == SIMULATE;
      }
   }
}
