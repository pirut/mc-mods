package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>> {
   private final RenderLayerParent<S, M> renderer;

   public RenderLayer(RenderLayerParent<S, M> p_117346_) {
      this.renderer = p_117346_;
   }

   protected static <S extends LivingEntityRenderState> void coloredCutoutModelCopyLayerRender(
      Model<? super S> p_433009_,
      Identifier p_468079_,
      PoseStack p_117363_,
      SubmitNodeCollector p_434929_,
      int p_117365_,
      S p_364841_,
      int p_350559_,
      int p_435193_
   ) {
      if (!p_364841_.isInvisible) {
         renderColoredCutoutModel(p_433009_, p_468079_, p_117363_, p_434929_, p_117365_, p_364841_, p_350559_, p_435193_);
      }
   }

   protected static <S extends LivingEntityRenderState> void renderColoredCutoutModel(
      Model<? super S> p_433267_,
      Identifier p_468501_,
      PoseStack p_117379_,
      SubmitNodeCollector p_432795_,
      int p_117381_,
      S p_360300_,
      int p_350384_,
      int p_434176_
   ) {
      p_432795_.order(p_434176_)
         .submitModel(
            p_433267_,
            p_360300_,
            p_117379_,
            RenderTypes.entityCutoutNoCull(p_468501_),
            p_117381_,
            LivingEntityRenderer.getOverlayCoords(p_360300_, 0.0F),
            p_350384_,
            null,
            p_360300_.outlineColor,
            null
         );
   }

   public M getParentModel() {
      return this.renderer.getModel();
   }

   public abstract void submit(PoseStack var1, SubmitNodeCollector var2, int var3, S var4, float var5, float var6);
}
