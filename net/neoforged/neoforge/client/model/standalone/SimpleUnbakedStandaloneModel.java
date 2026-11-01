package net.neoforged.neoforge.client.model.standalone;

import java.util.function.BiFunction;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;

public final class SimpleUnbakedStandaloneModel<T> implements UnbakedStandaloneModel<T> {
   private final Identifier modelId;
   private final BiFunction<ResolvedModel, ModelBaker, T> bake;

   public SimpleUnbakedStandaloneModel(Identifier modelId, BiFunction<ResolvedModel, ModelBaker, T> bake) {
      this.modelId = modelId;
      this.bake = bake;
   }

   @Override
   public T bake(ModelBaker baker) {
      return this.bake.apply(baker.getModel(this.modelId), baker);
   }

   @Override
   public void resolveDependencies(ResolvableModel.Resolver resolver) {
      resolver.markDependency(this.modelId);
   }

   public static SimpleUnbakedStandaloneModel<BlockModelPart> simpleModelWrapper(Identifier modelId) {
      return new SimpleUnbakedStandaloneModel<>(modelId, (model, baker) -> SimpleModelWrapper.bake(baker, model, BlockModelRotation.IDENTITY));
   }

   public static SimpleUnbakedStandaloneModel<BlockModelPart> simpleModelWrapper(Identifier modelId, ModelState modelState) {
      return new SimpleUnbakedStandaloneModel<>(modelId, (model, baker) -> SimpleModelWrapper.bake(baker, model, modelState));
   }

   public static SimpleUnbakedStandaloneModel<BlockStateModel> blockStateModel(Identifier modelId) {
      return new SimpleUnbakedStandaloneModel<>(
         modelId, (model, baker) -> new SingleVariant(SimpleModelWrapper.bake(baker, model, BlockModelRotation.IDENTITY))
      );
   }

   public static SimpleUnbakedStandaloneModel<BlockStateModel> blockStateModel(Identifier modelId, ModelState modelState) {
      return new SimpleUnbakedStandaloneModel<>(modelId, (model, baker) -> new SingleVariant(SimpleModelWrapper.bake(baker, model, modelState)));
   }

   public static SimpleUnbakedStandaloneModel<QuadCollection> quadCollection(Identifier modelId) {
      return new SimpleUnbakedStandaloneModel<>(
         modelId, (model, baker) -> model.bakeTopGeometry(model.getTopTextureSlots(), baker, BlockModelRotation.IDENTITY)
      );
   }

   public static SimpleUnbakedStandaloneModel<QuadCollection> quadCollection(Identifier modelId, ModelState modelState) {
      return new SimpleUnbakedStandaloneModel<>(modelId, (model, baker) -> model.bakeTopGeometry(model.getTopTextureSlots(), baker, modelState));
   }
}
