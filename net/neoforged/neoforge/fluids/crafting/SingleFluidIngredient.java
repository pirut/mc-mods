package net.neoforged.neoforge.fluids.crafting;

import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;

public class SingleFluidIngredient extends FluidIngredient {
   public static final MapCodec<SingleFluidIngredient> CODEC = FluidStack.FLUID_NON_EMPTY_CODEC
      .xmap(SingleFluidIngredient::new, SingleFluidIngredient::fluid)
      .fieldOf("fluid");
   private final Holder<Fluid> fluid;

   public SingleFluidIngredient(Holder<Fluid> fluid) {
      if (fluid.is(Fluids.EMPTY.builtInRegistryHolder())) {
         throw new IllegalStateException("SingleFluidIngredient must not be constructed with minecraft:empty, use FluidIngredient.empty() instead!");
      } else {
         this.fluid = fluid;
      }
   }

   @Override
   public boolean test(FluidStack fluidStack) {
      return fluidStack.is(this.fluid);
   }

   @Override
   protected Stream<FluidStack> generateStacks() {
      return Stream.of(new FluidStack(this.fluid, 1000));
   }

   @Override
   public boolean isSimple() {
      return true;
   }

   @Override
   public FluidIngredientType<?> getType() {
      return NeoForgeMod.SINGLE_FLUID_INGREDIENT_TYPE.get();
   }

   @Override
   public int hashCode() {
      return this.fluid().value().hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      return this == obj ? true : obj instanceof SingleFluidIngredient other && other.fluid.is(this.fluid);
   }

   public Holder<Fluid> fluid() {
      return this.fluid;
   }
}
