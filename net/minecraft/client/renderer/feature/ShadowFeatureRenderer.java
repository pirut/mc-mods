package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ShadowFeatureRenderer {
   private static final RenderType SHADOW_RENDER_TYPE = RenderTypes.entityShadow(Identifier.withDefaultNamespace("textures/misc/shadow.png"));

   public void render(SubmitNodeCollection p_439570_, MultiBufferSource.BufferSource p_434940_) {
      VertexConsumer vertexconsumer = p_434940_.getBuffer(SHADOW_RENDER_TYPE);

      for (SubmitNodeStorage.ShadowSubmit submitnodestorage$shadowsubmit : p_439570_.getShadowSubmits()) {
         for (EntityRenderState.ShadowPiece entityrenderstate$shadowpiece : submitnodestorage$shadowsubmit.pieces()) {
            AABB aabb = entityrenderstate$shadowpiece.shapeBelow().bounds();
            float f = entityrenderstate$shadowpiece.relativeX() + (float)aabb.minX;
            float f1 = entityrenderstate$shadowpiece.relativeX() + (float)aabb.maxX;
            float f2 = entityrenderstate$shadowpiece.relativeY() + (float)aabb.minY;
            float f3 = entityrenderstate$shadowpiece.relativeZ() + (float)aabb.minZ;
            float f4 = entityrenderstate$shadowpiece.relativeZ() + (float)aabb.maxZ;
            float f5 = submitnodestorage$shadowsubmit.radius();
            float f6 = -f / 2.0F / f5 + 0.5F;
            float f7 = -f1 / 2.0F / f5 + 0.5F;
            float f8 = -f3 / 2.0F / f5 + 0.5F;
            float f9 = -f4 / 2.0F / f5 + 0.5F;
            int i = ARGB.white(entityrenderstate$shadowpiece.alpha());
            shadowVertex(submitnodestorage$shadowsubmit.pose(), vertexconsumer, i, f, f2, f3, f6, f8);
            shadowVertex(submitnodestorage$shadowsubmit.pose(), vertexconsumer, i, f, f2, f4, f6, f9);
            shadowVertex(submitnodestorage$shadowsubmit.pose(), vertexconsumer, i, f1, f2, f4, f7, f9);
            shadowVertex(submitnodestorage$shadowsubmit.pose(), vertexconsumer, i, f1, f2, f3, f7, f8);
         }
      }
   }

   private static void shadowVertex(
      Matrix4f p_435044_, VertexConsumer p_434139_, int p_434403_, float p_436039_, float p_434768_, float p_433368_, float p_435348_, float p_433719_
   ) {
      Vector3f vector3f = p_435044_.transformPosition(p_436039_, p_434768_, p_433368_, new Vector3f());
      p_434139_.addVertex(vector3f.x(), vector3f.y(), vector3f.z(), p_434403_, p_435348_, p_433719_, OverlayTexture.NO_OVERLAY, 15728880, 0.0F, 1.0F, 0.0F);
   }
}
