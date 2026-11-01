package net.neoforged.neoforge.fluids.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;

public final class IntersectionFluidIngredient extends FluidIngredient {
   public static final MapCodec<IntersectionFluidIngredient> CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(FluidIngredient.LIST_CODEC_NON_EMPTY.fieldOf("children").forGetter(IntersectionFluidIngredient::children))
         .apply(builder, IntersectionFluidIngredient::new)
   );
   private final List<FluidIngredient> children;

   public IntersectionFluidIngredient(List<FluidIngredient> children) {
      if (children.isEmpty()) {
         throw new IllegalArgumentException(
            "Cannot create an IntersectionFluidIngredient with no children, use FluidIngredient.of() to create an empty ingredient"
         );
      } else {
         this.children = children;
      }
   }

   public static FluidIngredient of(FluidIngredient... ingredients) {
      if (ingredients.length == 0) {
         throw new IllegalArgumentException(
            "Cannot create an IntersectionFluidIngredient with no children, use FluidIngredient.of() to create an empty ingredient"
         );
      } else {
         return (FluidIngredient)(ingredients.length == 1 ? ingredients[0] : new IntersectionFluidIngredient(Arrays.asList(ingredients)));
      }
   }

   @Override
   public boolean test(FluidStack stack) {
      for (FluidIngredient child : this.children) {
         if (!child.test(stack)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public Stream<FluidStack> generateStacks() {
      return this.children.stream().flatMap(FluidIngredient::generateStacks).filter(this);
   }

   @Override
   public boolean isSimple() {
      for (FluidIngredient child : this.children) {
         if (!child.isSimple()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public FluidIngredientType<?> getType() {
      return NeoForgeMod.INTERSECTION_FLUID_INGREDIENT_TYPE.get();
   }

   public List<FluidIngredient> children() {
      return this.children;
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.children);
   }

   @Override
   public boolean equals(Object obj) {
      return this == obj ? true : obj instanceof IntersectionFluidIngredient other && this.children.equals(other.children);
   }
}
