package net.neoforged.neoforge.fluids.crafting.display;

import net.minecraft.core.Holder;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public interface ForFluidStacks<T> extends DisplayContentsFactory<T> {
   default T forStack(Holder<Fluid> fluid) {
      return this.forStack(new FluidStack(fluid, 1000));
   }

   default T forStack(Fluid fluid) {
      return this.forStack(fluid.builtInRegistryHolder());
   }

   T forStack(FluidStack var1);
}
