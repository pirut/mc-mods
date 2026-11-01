package net.neoforged.neoforge.client.model.generators.blockstate;

import net.minecraft.client.renderer.block.model.VariantMutator;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

public abstract class CustomBlockStateModelBuilder {
   protected CustomBlockStateModelBuilder() {
   }

   public abstract CustomBlockStateModelBuilder with(VariantMutator var1);

   public abstract CustomBlockStateModelBuilder with(UnbakedMutator var1);

   public abstract CustomUnbakedBlockStateModel toUnbaked();

   public static final class Simple extends CustomBlockStateModelBuilder {
      private final CustomUnbakedBlockStateModel blockStateModel;

      public Simple(CustomUnbakedBlockStateModel blockStateModel) {
         this.blockStateModel = blockStateModel;
      }

      public CustomBlockStateModelBuilder.Simple with(VariantMutator variantMutator) {
         return this;
      }

      @Override
      public CustomBlockStateModelBuilder with(UnbakedMutator variantMutator) {
         return new CustomBlockStateModelBuilder.Simple(variantMutator.apply(this.blockStateModel));
      }

      @Override
      public CustomUnbakedBlockStateModel toUnbaked() {
         return this.blockStateModel;
      }
   }
}
