package net.neoforged.neoforge.client.model.pipeline;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public abstract class VertexConsumerWrapper implements VertexConsumer {
   protected final VertexConsumer parent;

   public VertexConsumerWrapper(VertexConsumer parent) {
      this.parent = parent;
   }

   @Override
   public VertexConsumer addVertex(float x, float y, float z) {
      this.parent.addVertex(x, y, z);
      return this;
   }

   @Override
   public VertexConsumer setColor(int packedColor) {
      this.parent.setColor(packedColor);
      return this;
   }

   @Override
   public VertexConsumer setColor(int r, int g, int b, int a) {
      this.parent.setColor(r, g, b, a);
      return this;
   }

   @Override
   public VertexConsumer setUv(float u, float v) {
      this.parent.setUv(u, v);
      return this;
   }

   @Override
   public VertexConsumer setUv1(int u, int v) {
      this.parent.setUv1(u, v);
      return this;
   }

   @Override
   public VertexConsumer setUv2(int u, int v) {
      this.parent.setUv2(u, v);
      return this;
   }

   @Override
   public VertexConsumer setNormal(float x, float y, float z) {
      this.parent.setNormal(x, y, z);
      return this;
   }

   @Override
   public VertexConsumer setLineWidth(float lineWidth) {
      this.parent.setLineWidth(lineWidth);
      return this;
   }

   @Override
   public VertexConsumer misc(VertexFormatElement element, int... values) {
      this.parent.misc(element, values);
      return this;
   }
}
