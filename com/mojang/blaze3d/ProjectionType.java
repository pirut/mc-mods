package com.mojang.blaze3d;

import com.mojang.blaze3d.vertex.VertexSorting;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public enum ProjectionType {
   PERSPECTIVE(VertexSorting.DISTANCE_TO_ORIGIN, (p_381644_, p_381621_) -> p_381644_.scale(1.0F - p_381621_ / 4096.0F)),
   ORTHOGRAPHIC(VertexSorting.ORTHOGRAPHIC_Z, (p_381625_, p_381627_) -> p_381625_.translate(0.0F, 0.0F, p_381627_ / 512.0F));

   private final VertexSorting vertexSorting;
   private final ProjectionType.LayeringTransform layeringTransform;

   private ProjectionType(VertexSorting p_381626_, ProjectionType.LayeringTransform p_381649_) {
      this.vertexSorting = p_381626_;
      this.layeringTransform = p_381649_;
   }

   public VertexSorting vertexSorting() {
      return this.vertexSorting;
   }

   public void applyLayeringTransform(Matrix4f p_381648_, float p_381620_) {
      this.layeringTransform.apply(p_381648_, p_381620_);
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface LayeringTransform {
      void apply(Matrix4f var1, float var2);
   }
}
