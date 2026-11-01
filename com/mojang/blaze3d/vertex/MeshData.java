package com.mojang.blaze3d.vertex;

import it.unimi.dsi.fastutil.ints.IntConsumer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@Environment(EnvType.CLIENT)
public class MeshData implements AutoCloseable {
   private final ByteBufferBuilder.Result vertexBuffer;
   private ByteBufferBuilder.Result indexBuffer;
   private final MeshData.DrawState drawState;

   public MeshData(ByteBufferBuilder.Result vertexBuffer, MeshData.DrawState drawState) {
      this.vertexBuffer = vertexBuffer;
      this.drawState = drawState;
   }

   private static CompactVectorArray unpackQuadCentroids(ByteBuffer buffer, int vertexCount, VertexFormat format) {
      int i = format.getOffset(VertexFormatElement.POSITION);
      if (i == -1) {
         throw new IllegalArgumentException("Cannot identify quad centers with no position element");
      } else {
         FloatBuffer floatBuffer = buffer.asFloatBuffer();
         int j = format.getVertexSize() / 4;
         int k = j * 4;
         int l = vertexCount / 4;
         CompactVectorArray compactVectorArray = new CompactVectorArray(l);

         for (int m = 0; m < l; m++) {
            int n = m * k + i;
            int o = n + j * 2;
            float f = floatBuffer.get(n + 0);
            float g = floatBuffer.get(n + 1);
            float h = floatBuffer.get(n + 2);
            float p = floatBuffer.get(o + 0);
            float q = floatBuffer.get(o + 1);
            float r = floatBuffer.get(o + 2);
            float s = (f + p) / 2.0F;
            float t = (g + q) / 2.0F;
            float u = (h + r) / 2.0F;
            compactVectorArray.set(m, s, t, u);
         }

         return compactVectorArray;
      }
   }

   public ByteBuffer vertexBuffer() {
      return this.vertexBuffer.byteBuffer();
   }

   @Nullable
   public ByteBuffer indexBuffer() {
      return this.indexBuffer != null ? this.indexBuffer.byteBuffer() : null;
   }

   public MeshData.DrawState drawState() {
      return this.drawState;
   }

   @Nullable
   public MeshData.SortState sortQuads(ByteBufferBuilder bufferBuilder, VertexSorting sorting) {
      if (this.drawState.mode() != VertexFormat.Mode.QUADS) {
         return null;
      } else {
         CompactVectorArray compactVectorArray = unpackQuadCentroids(this.vertexBuffer.byteBuffer(), this.drawState.vertexCount(), this.drawState.format());
         MeshData.SortState sortState = new MeshData.SortState(compactVectorArray, this.drawState.indexType());
         this.indexBuffer = sortState.buildSortedIndexBuffer(bufferBuilder, sorting);
         return sortState;
      }
   }

   @Override
   public void close() {
      this.vertexBuffer.close();
      if (this.indexBuffer != null) {
         this.indexBuffer.close();
      }
   }

   @Environment(EnvType.CLIENT)
   public record DrawState(VertexFormat format, int vertexCount, int indexCount, VertexFormat.Mode mode, VertexFormat.IndexType indexType) {
   }

   @Environment(EnvType.CLIENT)
   public record SortState(CompactVectorArray centroids, VertexFormat.IndexType indexType) {
      public ByteBufferBuilder.Result buildSortedIndexBuffer(ByteBufferBuilder bufferBuilder, VertexSorting sorting) {
         int[] is = sorting.sort(this.centroids);
         long l = bufferBuilder.reserve(is.length * 6 * this.indexType.bytes);
         IntConsumer intConsumer = this.indexWriter(l, this.indexType);

         for (int i : is) {
            intConsumer.accept(i * 4 + 0);
            intConsumer.accept(i * 4 + 1);
            intConsumer.accept(i * 4 + 2);
            intConsumer.accept(i * 4 + 2);
            intConsumer.accept(i * 4 + 3);
            intConsumer.accept(i * 4 + 0);
         }

         return bufferBuilder.build();
      }

      private IntConsumer indexWriter(long index, VertexFormat.IndexType type) {
         MutableLong mutableLong = new MutableLong(index);

         return switch (type) {
            case SHORT -> i -> MemoryUtil.memPutShort(mutableLong.getAndAdd(2L), (short)i);
            case INT -> i -> MemoryUtil.memPutInt(mutableLong.getAndAdd(4L), i);
         };
      }
   }
}
