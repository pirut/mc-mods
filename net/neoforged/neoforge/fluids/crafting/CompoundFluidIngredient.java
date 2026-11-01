package net.neoforged.neoforge.fluids.crafting;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.neoforged.neoforge.fluids.FluidStack;

public final class CompoundFluidIngredient extends FluidIngredient {
   public static final MapCodec<CompoundFluidIngredient> CODEC = NeoForgeExtraCodecs.aliasedFieldOf(
         FluidIngredient.LIST_CODEC_NON_EMPTY, "children", "ingredients"
      )
      .xmap(CompoundFluidIngredient::new, CompoundFluidIngredient::children);
   private final List<FluidIngredient> children;

   public CompoundFluidIngredient(List<? extends FluidIngredient> children) {
      if (children.isEmpty()) {
         throw new IllegalArgumentException("Compound fluid ingredient must have at least one child");
      } else {
         this.children = List.copyOf(children);
      }
   }

   public static FluidIngredient of(FluidIngredient... children) {
      if (children.length == 0) {
         return FluidIngredient.empty();
      } else {
         return (FluidIngredient)(children.length == 1 ? children[0] : new CompoundFluidIngredient(List.of(children)));
      }
   }

   public static FluidIngredient of(List<FluidIngredient> children) {
      if (children.isEmpty()) {
         return FluidIngredient.empty();
      } else {
         return (FluidIngredient)(children.size() == 1 ? children.getFirst() : new CompoundFluidIngredient(children));
      }
   }

   public static FluidIngredient of(Stream<FluidIngredient> stream) {
      return of(stream.toList());
   }

   @Override
   public Stream<FluidStack> generateStacks() {
      return this.children.stream().flatMap(FluidIngredient::generateStacks);
   }

   @Override
   public boolean test(FluidStack stack) {
      for (FluidIngredient child : this.children) {
         if (child.test(stack)) {
            return true;
         }
      }

      return false;
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
      return NeoForgeMod.COMPOUND_FLUID_INGREDIENT_TYPE.get();
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.children);
   }

   @Override
   public boolean equals(Object obj) {
      return this == obj ? true : obj instanceof CompoundFluidIngredient other && other.children.equals(this.children);
   }

   public List<FluidIngredient> children() {
      return this.children;
   }
}
