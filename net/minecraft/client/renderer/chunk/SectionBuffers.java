package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class SectionBuffers implements AutoCloseable {
   private GpuBuffer vertexBuffer;
   @Nullable
   private GpuBuffer indexBuffer;
   private int indexCount;
   private VertexFormat.IndexType indexType;

   public SectionBuffers(GpuBuffer p_427237_, @Nullable GpuBuffer p_427387_, int p_427279_, VertexFormat.IndexType p_427338_) {
      this.vertexBuffer = p_427237_;
      this.indexBuffer = p_427387_;
      this.indexCount = p_427279_;
      this.indexType = p_427338_;
   }

   public GpuBuffer getVertexBuffer() {
      return this.vertexBuffer;
   }

   @Nullable
   public GpuBuffer getIndexBuffer() {
      return this.indexBuffer;
   }

   public void setIndexBuffer(@Nullable GpuBuffer p_427349_) {
      this.indexBuffer = p_427349_;
   }

   public int getIndexCount() {
      return this.indexCount;
   }

   public VertexFormat.IndexType getIndexType() {
      return this.indexType;
   }

   public void setIndexType(VertexFormat.IndexType p_427281_) {
      this.indexType = p_427281_;
   }

   public void setIndexCount(int p_427423_) {
      this.indexCount = p_427423_;
   }

   public void setVertexBuffer(GpuBuffer p_427453_) {
      this.vertexBuffer = p_427453_;
   }

   @Override
   public void close() {
      this.vertexBuffer.close();
      if (this.indexBuffer != null) {
         this.indexBuffer.close();
      }
   }
}
