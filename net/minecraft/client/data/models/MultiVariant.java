package net.minecraft.client.data.models;

import java.util.List;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.client.resources.model.WeightedVariants;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder;
import net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator;

@OnlyIn(Dist.CLIENT)
public record MultiVariant(WeightedList<Variant> variants, WeightedList<CustomBlockStateModelBuilder> customBlockStateModels) {
   public MultiVariant(WeightedList<Variant> variants, WeightedList<CustomBlockStateModelBuilder> customBlockStateModels) {
      if (!customBlockStateModels.isEmpty()) {
         if (!variants.isEmpty()) {
            throw new IllegalArgumentException("Cannot specify both a variant list and a custom blockstate model");
         }

         this.variants = variants;
         this.customBlockStateModels = customBlockStateModels;
      } else {
         if (variants.isEmpty()) {
            throw new IllegalArgumentException("Variant list must contain at least one element");
         }

         this.variants = variants;
         this.customBlockStateModels = customBlockStateModels;
      }
   }

   public MultiVariant(WeightedList<Variant> variants) {
      this(variants, WeightedList.of());
   }

   public static MultiVariant of(WeightedList<CustomBlockStateModelBuilder> customBlockStateModels) {
      return new MultiVariant(WeightedList.of(), customBlockStateModels);
   }

   public static MultiVariant of(CustomBlockStateModelBuilder customBlockStateModel) {
      return new MultiVariant(WeightedList.of(), WeightedList.of(customBlockStateModel));
   }

   public MultiVariant with(VariantMutator p_409928_) {
      return !this.customBlockStateModels.isEmpty()
         ? new MultiVariant(this.variants, this.customBlockStateModels.map(model -> model.with(p_409928_)))
         : new MultiVariant(this.variants.map(p_409928_));
   }

   public MultiVariant with(UnbakedMutator mutator) {
      return !this.customBlockStateModels.isEmpty()
         ? new MultiVariant(this.variants, this.customBlockStateModels.map(model -> model.with(mutator)))
         : new MultiVariant(this.variants.map(v -> mutator.apply(new SingleVariant.Unbaked(v)).variant()));
   }

   public BlockStateModel.Unbaked toUnbaked() {
      if (!this.customBlockStateModels.isEmpty()) {
         List<Weighted<CustomBlockStateModelBuilder>> builders = this.customBlockStateModels.unwrap();
         return (BlockStateModel.Unbaked)(builders.size() == 1
            ? builders.getFirst().value().toUnbaked()
            : new WeightedVariants.Unbaked(this.customBlockStateModels.map(CustomBlockStateModelBuilder::toUnbaked)));
      } else {
         List<Weighted<Variant>> list = this.variants.unwrap();
         return (BlockStateModel.Unbaked)(
            list.size() == 1 ? new SingleVariant.Unbaked(list.getFirst().value()) : new WeightedVariants.Unbaked(this.variants.map(SingleVariant.Unbaked::new))
         );
      }
   }
}
