package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class LeashFeatureRenderer {
   private static final int LEASH_RENDER_STEPS = 24;
   private static final float LEASH_WIDTH = 0.05F;

   public void render(SubmitNodeCollection nodeCollection, MultiBufferSource.BufferSource bufferSource) {
      for (SubmitNodeStorage.LeashSubmit leashSubmit : nodeCollection.getLeashSubmits()) {
         renderLeash(leashSubmit.pose(), bufferSource, leashSubmit.leashState());
      }
   }

   private static void renderLeash(Matrix4f pose, MultiBufferSource bufferSource, EntityRenderState.LeashState leashState) {
      float f = (float)(leashState.end.x - leashState.start.x);
      float g = (float)(leashState.end.y - leashState.start.y);
      float h = (float)(leashState.end.z - leashState.start.z);
      float i = Mth.invSqrt(f * f + h * h) * 0.05F / 2.0F;
      float j = h * i;
      float k = f * i;
      pose.translate((float)leashState.offset.x, (float)leashState.offset.y, (float)leashState.offset.z);
      VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderTypes.leash());

      for (int l = 0; l <= 24; l++) {
         addVertexPair(vertexConsumer, pose, f, g, h, 0.05F, j, k, l, false, leashState);
      }

      for (int l = 24; l >= 0; l--) {
         addVertexPair(vertexConsumer, pose, f, g, h, 0.0F, j, k, l, true, leashState);
      }
   }

   private static void addVertexPair(
      VertexConsumer consumer,
      Matrix4f pose,
      float startX,
      float startY,
      float startZ,
      float yOffset,
      float dx,
      float dz,
      int index,
      boolean reverse,
      EntityRenderState.LeashState leashState
   ) {
      float f = index / 24.0F;
      int i = (int)Mth.lerp(f, (float)leashState.startBlockLight, (float)leashState.endBlockLight);
      int j = (int)Mth.lerp(f, (float)leashState.startSkyLight, (float)leashState.endSkyLight);
      int k = LightTexture.pack(i, j);
      float g = index % 2 == (reverse ? 1 : 0) ? 0.7F : 1.0F;
      float h = 0.5F * g;
      float l = 0.4F * g;
      float m = 0.3F * g;
      float n = startX * f;
      float o;
      if (leashState.slack) {
         o = startY > 0.0F ? startY * f * f : startY - startY * (1.0F - f) * (1.0F - f);
      } else {
         o = startY * f;
      }

      float p = startZ * f;
      consumer.addVertex(pose, n - dx, o + yOffset, p + dz).setColor(h, l, m, 1.0F).setLight(k);
      consumer.addVertex(pose, n + dx, o + 0.05F - yOffset, p - dz).setColor(h, l, m, 1.0F).setLight(k);
   }
}
