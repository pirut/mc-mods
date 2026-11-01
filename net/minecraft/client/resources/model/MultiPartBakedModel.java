package net.minecraft.client.resources.model;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.MultipartModelData;
import net.neoforged.neoforge.common.util.ConcatenatedListView;
import net.neoforged.neoforge.common.util.TriState;
import org.apache.commons.lang3.tuple.Pair;

@OnlyIn(Dist.CLIENT)
public class MultiPartBakedModel implements BakedModel, IDynamicBakedModel {
   private final List<Pair<Predicate<BlockState>, BakedModel>> selectors;
   protected final boolean hasAmbientOcclusion;
   protected final boolean isGui3d;
   protected final boolean usesBlockLight;
   protected final TextureAtlasSprite particleIcon;
   protected final ItemTransforms transforms;
   protected final ItemOverrides overrides;
   private final Map<BlockState, BitSet> selectorCache = new Reference2ObjectOpenHashMap();
   private final BakedModel defaultModel;

   public MultiPartBakedModel(List<Pair<Predicate<BlockState>, BakedModel>> p_119462_) {
      this.selectors = p_119462_;
      BakedModel bakedmodel = (BakedModel)p_119462_.iterator().next().getRight();
      this.defaultModel = bakedmodel;
      this.hasAmbientOcclusion = bakedmodel.useAmbientOcclusion();
      this.isGui3d = bakedmodel.isGui3d();
      this.usesBlockLight = bakedmodel.usesBlockLight();
      this.particleIcon = bakedmodel.getParticleIcon();
      this.transforms = bakedmodel.getTransforms();
      this.overrides = bakedmodel.getOverrides();
   }

   public BitSet getSelectors(@Nullable BlockState p_235050_) {
      BitSet bitset = this.selectorCache.get(p_235050_);
      if (bitset == null) {
         bitset = new BitSet();

         for (int i = 0; i < this.selectors.size(); i++) {
            Pair<Predicate<BlockState>, BakedModel> pair = this.selectors.get(i);
            if (((Predicate)pair.getLeft()).test(p_235050_)) {
               bitset.set(i);
            }
         }

         this.selectorCache.put(p_235050_, bitset);
      }

      return bitset;
   }

   @Override
   public List<BakedQuad> getQuads(
      @Nullable BlockState p_235050_,
      @Nullable Direction p_235051_,
      RandomSource p_235052_,
      ModelData modelData,
      @org.jetbrains.annotations.Nullable RenderType renderType
   ) {
      if (p_235050_ == null) {
         return Collections.emptyList();
      } else {
         BitSet bitset = this.getSelectors(p_235050_);
         List<List<BakedQuad>> list = Lists.newArrayList();
         long k = p_235052_.nextLong();

         for (int j = 0; j < bitset.length(); j++) {
            if (bitset.get(j)) {
               BakedModel model = (BakedModel)this.selectors.get(j).getRight();
               if (renderType == null || model.getRenderTypes(p_235050_, p_235052_, modelData).contains(renderType)) {
                  list.add(model.getQuads(p_235050_, p_235051_, RandomSource.create(k), MultipartModelData.resolve(modelData, model), renderType));
               }
            }
         }

         return ConcatenatedListView.of(list);
      }
   }

   @Override
   public boolean useAmbientOcclusion() {
      return this.hasAmbientOcclusion;
   }

   @Override
   public TriState useAmbientOcclusion(BlockState state, ModelData modelData, RenderType renderType) {
      return this.defaultModel.useAmbientOcclusion(state, modelData, renderType);
   }

   @Override
   public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
      return MultipartModelData.create(this.selectors, this.getSelectors(state), level, pos, state, modelData);
   }

   @Override
   public boolean isGui3d() {
      return this.isGui3d;
   }

   @Override
   public boolean usesBlockLight() {
      return this.usesBlockLight;
   }

   @Override
   public boolean isCustomRenderer() {
      return false;
   }

   @Deprecated
   @Override
   public TextureAtlasSprite getParticleIcon() {
      return this.particleIcon;
   }

   @Override
   public TextureAtlasSprite getParticleIcon(ModelData modelData) {
      return this.defaultModel.getParticleIcon(modelData);
   }

   @Deprecated
   @Override
   public ItemTransforms getTransforms() {
      return this.transforms;
   }

   @Override
   public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
      return this.defaultModel.applyTransform(transformType, poseStack, applyLeftHandTransform);
   }

   @Override
   public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
      LinkedList<ChunkRenderTypeSet> renderTypeSets = new LinkedList<>();
      BitSet selectors = this.getSelectors(state);

      for (int i = 0; i < selectors.length(); i++) {
         if (selectors.get(i)) {
            renderTypeSets.add(((BakedModel)this.selectors.get(i).getRight()).getRenderTypes(state, rand, data));
         }
      }

      return ChunkRenderTypeSet.union(renderTypeSets);
   }

   @Override
   public ItemOverrides getOverrides() {
      return this.overrides;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final List<Pair<Predicate<BlockState>, BakedModel>> selectors = Lists.newArrayList();

      public void add(Predicate<BlockState> p_119478_, BakedModel p_119479_) {
         this.selectors.add(Pair.of(p_119478_, p_119479_));
      }

      public BakedModel build() {
         return new MultiPartBakedModel(this.selectors);
      }
   }
}
