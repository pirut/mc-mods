package net.neoforged.neoforge.client.model.generators.blockstate;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.neoforged.neoforge.client.model.block.CompositeBlockModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

public class CompositeBlockStateModelBuilder extends CustomBlockStateModelBuilder {
   private final List<BlockStateModel.Unbaked> models = new ArrayList<>();

   public void addPartModel(BlockStateModel.Unbaked partModel) {
      this.models.add(partModel);
   }

   public CompositeBlockStateModelBuilder with(VariantMutator variantMutator) {
      return this;
   }

   @Override
   public CustomBlockStateModelBuilder with(UnbakedMutator variantMutator) {
      CompositeBlockStateModelBuilder result = new CompositeBlockStateModelBuilder();

      for (BlockStateModel.Unbaked model : this.models) {
         result.models.add(variantMutator.apply(model));
      }

      return result;
   }

   @Override
   public CustomUnbakedBlockStateModel toUnbaked() {
      return new CompositeBlockModel.Unbaked(this.models);
   }
}
