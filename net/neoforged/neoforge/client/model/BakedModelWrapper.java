package net.neoforged.neoforge.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

public abstract class BakedModelWrapper<T extends BakedModel> implements BakedModel {
   protected final T originalModel;

   public BakedModelWrapper(T originalModel) {
      this.originalModel = originalModel;
   }

   @Override
   public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
      return this.originalModel.getQuads(state, side, rand);
   }

   @Override
   public boolean useAmbientOcclusion() {
      return this.originalModel.useAmbientOcclusion();
   }

   @Override
   public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
      return this.originalModel.useAmbientOcclusion(state, data, renderType);
   }

   @Override
   public boolean isGui3d() {
      return this.originalModel.isGui3d();
   }

   @Override
   public boolean usesBlockLight() {
      return this.originalModel.usesBlockLight();
   }

   @Override
   public boolean isCustomRenderer() {
      return this.originalModel.isCustomRenderer();
   }

   @Override
   public TextureAtlasSprite getParticleIcon() {
      return this.originalModel.getParticleIcon();
   }

   @Override
   public ItemTransforms getTransforms() {
      return this.originalModel.getTransforms();
   }

   @Override
   public ItemOverrides getOverrides() {
      return this.originalModel.getOverrides();
   }

   @Override
   public BakedModel applyTransform(ItemDisplayContext cameraTransformType, PoseStack poseStack, boolean applyLeftHandTransform) {
      return this.originalModel.applyTransform(cameraTransformType, poseStack, applyLeftHandTransform);
   }

   @Override
   public TextureAtlasSprite getParticleIcon(ModelData data) {
      return this.originalModel.getParticleIcon(data);
   }

   @Override
   public List<BakedQuad> getQuads(
      @Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType
   ) {
      return this.originalModel.getQuads(state, side, rand, extraData, renderType);
   }

   @Override
   public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
      return this.originalModel.getModelData(level, pos, state, modelData);
   }

   @Override
   public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
      return this.originalModel.getRenderTypes(state, rand, data);
   }

   @Override
   public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
      return this.originalModel.getRenderTypes(itemStack, fabulous);
   }

   @Override
   public List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous) {
      return this.originalModel.getRenderPasses(itemStack, fabulous);
   }
}
