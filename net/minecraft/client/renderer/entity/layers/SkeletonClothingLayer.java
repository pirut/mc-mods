package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkeletonClothingLayer<S extends SkeletonRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
   private final SkeletonModel<S> layerModel;
   private final Identifier clothesLocation;

   public SkeletonClothingLayer(RenderLayerParent<S, M> p_326918_, EntityModelSet p_326830_, ModelLayerLocation p_326794_, Identifier p_467856_) {
      super(p_326918_);
      this.clothesLocation = p_467856_;
      this.layerModel = new SkeletonModel<>(p_326830_.bakeLayer(p_326794_));
   }

   public void submit(PoseStack p_435743_, SubmitNodeCollector p_435711_, int p_434500_, S p_436040_, float p_434917_, float p_435872_) {
      coloredCutoutModelCopyLayerRender(this.layerModel, this.clothesLocation, p_435743_, p_435711_, p_434500_, p_436040_, -1, 1);
   }
}
