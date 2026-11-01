package net.neoforged.neoforge.fluids.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;

public class DataComponentFluidIngredient extends FluidIngredient {
   public static final MapCodec<DataComponentFluidIngredient> CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(
            HolderSetCodec.create(Registries.FLUID, BuiltInRegistries.FLUID.holderByNameCodec(), false)
               .fieldOf("fluids")
               .forGetter(DataComponentFluidIngredient::fluids),
            DataComponentPredicate.CODEC.fieldOf("components").forGetter(DataComponentFluidIngredient::components),
            Codec.BOOL.optionalFieldOf("strict", false).forGetter(DataComponentFluidIngredient::isStrict)
         )
         .apply(builder, DataComponentFluidIngredient::new)
   );
   private final HolderSet<Fluid> fluids;
   private final DataComponentPredicate components;
   private final boolean strict;
   private final FluidStack[] stacks;

   public DataComponentFluidIngredient(HolderSet<Fluid> fluids, DataComponentPredicate components, boolean strict) {
      this.fluids = fluids;
      this.components = components;
      this.strict = strict;
      this.stacks = fluids.stream().map(i -> new FluidStack((Holder<Fluid>)i, 1000, components.asPatch())).toArray(FluidStack[]::new);
   }

   @Override
   public boolean test(FluidStack stack) {
      if (this.strict) {
         for (FluidStack stack2 : this.stacks) {
            if (FluidStack.isSameFluidSameComponents(stack, stack2)) {
               return true;
            }
         }

         return false;
      } else {
         return this.fluids.contains(stack.getFluidHolder()) && this.components.test((DataComponentHolder)stack);
      }
   }

   @Override
   public Stream<FluidStack> generateStacks() {
      return Stream.of(this.stacks);
   }

   @Override
   public boolean isSimple() {
      return false;
   }

   @Override
   public FluidIngredientType<?> getType() {
      return NeoForgeMod.DATA_COMPONENT_FLUID_INGREDIENT_TYPE.get();
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.fluids, this.components, this.strict);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else {
         return !(obj instanceof DataComponentFluidIngredient other)
            ? false
            : other.fluids.equals(this.fluids) && other.components.equals(this.components) && other.strict == this.strict;
      }
   }

   public HolderSet<Fluid> fluids() {
      return this.fluids;
   }

   public DataComponentPredicate components() {
      return this.components;
   }

   public boolean isStrict() {
      return this.strict;
   }

   public static FluidIngredient of(boolean strict, FluidStack stack) {
      return of(strict, stack.getComponents(), stack.getFluid());
   }

   public static <T> FluidIngredient of(boolean strict, DataComponentType<? super T> type, T value, Fluid... fluids) {
      return of(strict, DataComponentPredicate.builder().expect(type, value).build(), fluids);
   }

   public static <T> FluidIngredient of(boolean strict, Supplier<? extends DataComponentType<? super T>> type, T value, Fluid... fluids) {
      return of(strict, (DataComponentType<? super T>)type.get(), value, fluids);
   }

   public static FluidIngredient of(boolean strict, DataComponentMap map, Fluid... fluids) {
      return of(strict, DataComponentPredicate.allOf(map), fluids);
   }

   @SafeVarargs
   public static FluidIngredient of(boolean strict, DataComponentMap map, Holder<Fluid>... fluids) {
      return of(strict, DataComponentPredicate.allOf(map), fluids);
   }

   public static FluidIngredient of(boolean strict, DataComponentMap map, HolderSet<Fluid> fluids) {
      return of(strict, DataComponentPredicate.allOf(map), fluids);
   }

   @SafeVarargs
   public static FluidIngredient of(boolean strict, DataComponentPredicate predicate, Holder<Fluid>... fluids) {
      return of(strict, predicate, HolderSet.direct(fluids));
   }

   public static FluidIngredient of(boolean strict, DataComponentPredicate predicate, Fluid... fluids) {
      return of(strict, predicate, HolderSet.direct(Arrays.stream(fluids).map(Fluid::builtInRegistryHolder).toList()));
   }

   public static FluidIngredient of(boolean strict, DataComponentPredicate predicate, HolderSet<Fluid> fluids) {
      return new DataComponentFluidIngredient(fluids, predicate, strict);
   }
}
