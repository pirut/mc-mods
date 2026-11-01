package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.ModelBakery;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class FlameFeatureRenderer {
   public void render(SubmitNodeCollection p_440254_, MultiBufferSource.BufferSource p_433616_, AtlasManager p_433168_) {
      for (SubmitNodeStorage.FlameSubmit submitnodestorage$flamesubmit : p_440254_.getFlameSubmits()) {
         this.renderFlame(
            submitnodestorage$flamesubmit.pose(),
            p_433616_,
            submitnodestorage$flamesubmit.entityRenderState(),
            submitnodestorage$flamesubmit.rotation(),
            p_433168_
         );
      }
   }

   private void renderFlame(PoseStack.Pose p_435970_, MultiBufferSource p_434226_, EntityRenderState p_432856_, Quaternionf p_434582_, AtlasManager p_433838_) {
      TextureAtlasSprite textureatlassprite = p_433838_.get(ModelBakery.FIRE_0);
      TextureAtlasSprite textureatlassprite1 = p_433838_.get(ModelBakery.FIRE_1);
      float f = p_432856_.boundingBoxWidth * 1.4F;
      p_435970_.scale(f, f, f);
      float f1 = 0.5F;
      float f2 = 0.0F;
      float f3 = p_432856_.boundingBoxHeight / f;
      float f4 = 0.0F;
      p_435970_.rotate(p_434582_);
      p_435970_.translate(0.0F, 0.0F, 0.3F - (int)f3 * 0.02F);
      float f5 = 0.0F;
      int i = 0;

      for (VertexConsumer vertexconsumer = p_434226_.getBuffer(Sheets.cutoutBlockSheet()); f3 > 0.0F; i++) {
         TextureAtlasSprite textureatlassprite2 = i % 2 == 0 ? textureatlassprite : textureatlassprite1;
         float f6 = textureatlassprite2.getU0();
         float f7 = textureatlassprite2.getV0();
         float f8 = textureatlassprite2.getU1();
         float f9 = textureatlassprite2.getV1();
         if (i / 2 % 2 == 0) {
            float f10 = f8;
            f8 = f6;
            f6 = f10;
         }

         fireVertex(p_435970_, vertexconsumer, -f1 - 0.0F, 0.0F - f4, f5, f8, f9);
         fireVertex(p_435970_, vertexconsumer, f1 - 0.0F, 0.0F - f4, f5, f6, f9);
         fireVertex(p_435970_, vertexconsumer, f1 - 0.0F, 1.4F - f4, f5, f6, f7);
         fireVertex(p_435970_, vertexconsumer, -f1 - 0.0F, 1.4F - f4, f5, f8, f7);
         f3 -= 0.45F;
         f4 -= 0.45F;
         f1 *= 0.9F;
         f5 -= 0.03F;
      }
   }

   private static void fireVertex(
      PoseStack.Pose p_435718_, VertexConsumer p_435929_, float p_434852_, float p_435412_, float p_433404_, float p_433090_, float p_435664_
   ) {
      p_435929_.addVertex(p_435718_, p_434852_, p_435412_, p_433404_)
         .setColor(-1)
         .setUv(p_433090_, p_435664_)
         .setUv1(0, 10)
         .setLight(240)
         .setNormal(p_435718_, 0.0F, 1.0F, 0.0F);
   }
}
