package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PowerableMob;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class EnergySwirlLayer<T extends Entity & PowerableMob, M extends EntityModel<T>> extends RenderLayer<T, M> {
   public EnergySwirlLayer(RenderLayerParent<T, M> p_116967_) {
      super(p_116967_);
   }

   @Override
   public void render(
      PoseStack p_116970_,
      MultiBufferSource p_116971_,
      int p_116972_,
      T p_116973_,
      float p_116974_,
      float p_116975_,
      float p_116976_,
      float p_116977_,
      float p_116978_,
      float p_116979_
   ) {
      if (p_116973_.isPowered()) {
         float $$10 = p_116973_.tickCount + p_116976_;
         EntityModel<T> $$11 = this.model();
         $$11.prepareMobModel(p_116973_, p_116974_, p_116975_, p_116976_);
         this.getParentModel().copyPropertiesTo($$11);
         VertexConsumer $$12 = p_116971_.getBuffer(RenderType.energySwirl(this.getTextureLocation(), this.xOffset($$10) % 1.0F, $$10 * 0.01F % 1.0F));
         $$11.setupAnim(p_116973_, p_116974_, p_116975_, p_116977_, p_116978_, p_116979_);
         $$11.renderToBuffer(p_116970_, $$12, p_116972_, OverlayTexture.NO_OVERLAY, -8355712);
      }
   }

   protected abstract float xOffset(float var1);

   protected abstract ResourceLocation getTextureLocation();

   protected abstract EntityModel<T> model();
}
