package net.neoforged.neoforge.fluids.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.stream.Stream;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;

public final class DifferenceFluidIngredient extends FluidIngredient {
   public static final MapCodec<DifferenceFluidIngredient> CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(
            FluidIngredient.CODEC_NON_EMPTY.fieldOf("base").forGetter(DifferenceFluidIngredient::base),
            FluidIngredient.CODEC_NON_EMPTY.fieldOf("subtracted").forGetter(DifferenceFluidIngredient::subtracted)
         )
         .apply(builder, DifferenceFluidIngredient::new)
   );
   private final FluidIngredient base;
   private final FluidIngredient subtracted;

   public DifferenceFluidIngredient(FluidIngredient base, FluidIngredient subtracted) {
      this.base = base;
      this.subtracted = subtracted;
   }

   @Override
   public Stream<FluidStack> generateStacks() {
      return this.base.generateStacks().filter(this.subtracted.negate());
   }

   @Override
   public boolean test(FluidStack stack) {
      return this.base.test(stack) && !this.subtracted.test(stack);
   }

   @Override
   public boolean isSimple() {
      return this.base.isSimple() && this.subtracted.isSimple();
   }

   @Override
   public FluidIngredientType<?> getType() {
      return NeoForgeMod.DIFFERENCE_FLUID_INGREDIENT_TYPE.get();
   }

   public static FluidIngredient of(FluidIngredient base, FluidIngredient subtracted) {
      return new DifferenceFluidIngredient(base, subtracted);
   }

   public FluidIngredient base() {
      return this.base;
   }

   public FluidIngredient subtracted() {
      return this.subtracted;
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.base, this.subtracted);
   }

   @Override
   public boolean equals(Object obj) {
      return this == obj ? true : obj instanceof DifferenceFluidIngredient other && other.base.equals(this.base) && other.subtracted.equals(this.subtracted);
   }
}
