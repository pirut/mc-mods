package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;

@Environment(EnvType.CLIENT)
public class OutlineBufferSource implements MultiBufferSource {
   private final MultiBufferSource.BufferSource outlineBufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
   private int outlineColor = -1;

   @Override
   public VertexConsumer getBuffer(RenderType renderType) {
      if (renderType.isOutline()) {
         VertexConsumer vertexConsumer = this.outlineBufferSource.getBuffer(renderType);
         return new OutlineBufferSource.EntityOutlineGenerator(vertexConsumer, this.outlineColor);
      } else {
         Optional<RenderType> optional = renderType.outline();
         if (optional.isPresent()) {
            VertexConsumer vertexConsumer2 = this.outlineBufferSource.getBuffer(optional.get());
            return new OutlineBufferSource.EntityOutlineGenerator(vertexConsumer2, this.outlineColor);
         } else {
            throw new IllegalStateException("Can't render an outline for this rendertype!");
         }
      }
   }

   public void setColor(int color) {
      this.outlineColor = color;
   }

   public void endOutlineBatch() {
      this.outlineBufferSource.endBatch();
   }

   @Environment(EnvType.CLIENT)
   record EntityOutlineGenerator(VertexConsumer delegate, int color) implements VertexConsumer {
      @Override
      public VertexConsumer addVertex(float x, float y, float z) {
         this.delegate.addVertex(x, y, z).setColor(this.color);
         return this;
      }

      @Override
      public VertexConsumer setColor(int red, int green, int blue, int alpha) {
         return this;
      }

      @Override
      public VertexConsumer setColor(int color) {
         return this;
      }

      @Override
      public VertexConsumer setUv(float u, float v) {
         this.delegate.setUv(u, v);
         return this;
      }

      @Override
      public VertexConsumer setUv1(int u, int v) {
         return this;
      }

      @Override
      public VertexConsumer setUv2(int u, int v) {
         return this;
      }

      @Override
      public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
         return this;
      }

      @Override
      public VertexConsumer setLineWidth(float lineWidth) {
         return this;
      }
   }
}
