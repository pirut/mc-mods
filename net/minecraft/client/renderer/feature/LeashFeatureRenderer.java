package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class LeashFeatureRenderer {
   private static final int LEASH_RENDER_STEPS = 24;
   private static final float LEASH_WIDTH = 0.05F;

   public void render(SubmitNodeCollection p_440704_, MultiBufferSource.BufferSource p_433570_) {
      for (SubmitNodeStorage.LeashSubmit submitnodestorage$leashsubmit : p_440704_.getLeashSubmits()) {
         renderLeash(submitnodestorage$leashsubmit.pose(), p_433570_, submitnodestorage$leashsubmit.leashState());
      }
   }

   private static void renderLeash(Matrix4f p_435977_, MultiBufferSource p_433388_, EntityRenderState.LeashState p_435610_) {
      float f = (float)(p_435610_.end.x - p_435610_.start.x);
      float f1 = (float)(p_435610_.end.y - p_435610_.start.y);
      float f2 = (float)(p_435610_.end.z - p_435610_.start.z);
      float f3 = Mth.invSqrt(f * f + f2 * f2) * 0.05F / 2.0F;
      float f4 = f2 * f3;
      float f5 = f * f3;
      p_435977_.translate((float)p_435610_.offset.x, (float)p_435610_.offset.y, (float)p_435610_.offset.z);
      VertexConsumer vertexconsumer = p_433388_.getBuffer(RenderTypes.leash());

      for (int i = 0; i <= 24; i++) {
         addVertexPair(vertexconsumer, p_435977_, f, f1, f2, 0.05F, f4, f5, i, false, p_435610_);
      }

      for (int j = 24; j >= 0; j--) {
         addVertexPair(vertexconsumer, p_435977_, f, f1, f2, 0.0F, f4, f5, j, true, p_435610_);
      }
   }

   private static void addVertexPair(
      VertexConsumer p_434388_,
      Matrix4f p_433395_,
      float p_433965_,
      float p_433396_,
      float p_433151_,
      float p_433167_,
      float p_433199_,
      float p_434961_,
      int p_436038_,
      boolean p_434670_,
      EntityRenderState.LeashState p_435559_
   ) {
      float f = p_436038_ / 24.0F;
      int i = (int)Mth.lerp(f, (float)p_435559_.startBlockLight, (float)p_435559_.endBlockLight);
      int j = (int)Mth.lerp(f, (float)p_435559_.startSkyLight, (float)p_435559_.endSkyLight);
      int k = LightTexture.pack(i, j);
      float f1 = p_436038_ % 2 == (p_434670_ ? 1 : 0) ? 0.7F : 1.0F;
      float f2 = 0.5F * f1;
      float f3 = 0.4F * f1;
      float f4 = 0.3F * f1;
      float f5 = p_433965_ * f;
      float f6;
      if (p_435559_.slack) {
         f6 = p_433396_ > 0.0F ? p_433396_ * f * f : p_433396_ - p_433396_ * (1.0F - f) * (1.0F - f);
      } else {
         f6 = p_433396_ * f;
      }

      float f7 = p_433151_ * f;
      p_434388_.addVertex(p_433395_, f5 - p_433199_, f6 + p_433167_, f7 + p_434961_).setColor(f2, f3, f4, 1.0F).setLight(k);
      p_434388_.addVertex(p_433395_, f5 + p_433199_, f6 + 0.05F - p_433167_, f7 - p_434961_).setColor(f2, f3, f4, 1.0F).setLight(k);
   }
}
