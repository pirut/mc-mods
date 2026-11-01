package net.neoforged.neoforge.fluids.crafting;

import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;

public class TagFluidIngredient extends FluidIngredient {
   public static final MapCodec<TagFluidIngredient> CODEC = TagKey.codec(Registries.FLUID)
      .xmap(TagFluidIngredient::new, TagFluidIngredient::tag)
      .fieldOf("tag");
   private final TagKey<Fluid> tag;

   public TagFluidIngredient(TagKey<Fluid> tag) {
      this.tag = tag;
   }

   @Override
   public boolean test(FluidStack fluidStack) {
      return fluidStack.is(this.tag);
   }

   @Override
   protected Stream<FluidStack> generateStacks() {
      return BuiltInRegistries.FLUID.getTag(this.tag).stream().flatMap(HolderSet::stream).map(fluid -> new FluidStack((Holder<Fluid>)fluid, 1000));
   }

   @Override
   public boolean isSimple() {
      return true;
   }

   @Override
   public FluidIngredientType<?> getType() {
      return NeoForgeMod.TAG_FLUID_INGREDIENT_TYPE.get();
   }

   @Override
   public int hashCode() {
      return this.tag.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      return this == obj ? true : obj instanceof TagFluidIngredient tag && tag.tag.equals(this.tag);
   }

   public TagKey<Fluid> tag() {
      return this.tag;
   }
}
