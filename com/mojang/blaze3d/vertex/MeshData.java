package com.mojang.blaze3d.vertex;

import it.unimi.dsi.fastutil.ints.IntConsumer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableLong;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class MeshData implements AutoCloseable {
   private final ByteBufferBuilder.Result vertexBuffer;
   @Nullable
   private ByteBufferBuilder.Result indexBuffer;
   private final MeshData.DrawState drawState;

   public MeshData(ByteBufferBuilder.Result p_350637_, MeshData.DrawState p_350712_) {
      this.vertexBuffer = p_350637_;
      this.drawState = p_350712_;
   }

   private static Vector3f[] unpackQuadCentroids(ByteBuffer p_350488_, int p_350671_, VertexFormat p_350665_) {
      int $$3 = p_350665_.getOffset(VertexFormatElement.POSITION);
      if ($$3 == -1) {
         throw new IllegalArgumentException("Cannot identify quad centers with no position element");
      } else {
         FloatBuffer $$4 = p_350488_.asFloatBuffer();
         int $$5 = p_350665_.getVertexSize() / 4;
         int $$6 = $$5 * 4;
         int $$7 = p_350671_ / 4;
         Vector3f[] $$8 = new Vector3f[$$7];

         for (int $$9 = 0; $$9 < $$7; $$9++) {
            int $$10 = $$9 * $$6 + $$3;
            int $$11 = $$10 + $$5 * 2;
            float $$12 = $$4.get($$10 + 0);
            float $$13 = $$4.get($$10 + 1);
            float $$14 = $$4.get($$10 + 2);
            float $$15 = $$4.get($$11 + 0);
            float $$16 = $$4.get($$11 + 1);
            float $$17 = $$4.get($$11 + 2);
            $$8[$$9] = new Vector3f(($$12 + $$15) / 2.0F, ($$13 + $$16) / 2.0F, ($$14 + $$17) / 2.0F);
         }

         return $$8;
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
   public MeshData.SortState sortQuads(ByteBufferBuilder p_350540_, VertexSorting p_350720_) {
      if (this.drawState.mode() != VertexFormat.Mode.QUADS) {
         return null;
      } else {
         Vector3f[] $$2 = unpackQuadCentroids(this.vertexBuffer.byteBuffer(), this.drawState.vertexCount(), this.drawState.format());
         MeshData.SortState $$3 = new MeshData.SortState($$2, this.drawState.indexType());
         this.indexBuffer = $$3.buildSortedIndexBuffer(p_350540_, p_350720_);
         return $$3;
      }
   }

   @Override
   public void close() {
      this.vertexBuffer.close();
      if (this.indexBuffer != null) {
         this.indexBuffer.close();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record DrawState(VertexFormat format, int vertexCount, int indexCount, VertexFormat.Mode mode, VertexFormat.IndexType indexType) {
   }

   @OnlyIn(Dist.CLIENT)
   public record SortState(Vector3f[] centroids, VertexFormat.IndexType indexType) {
      @Nullable
      public ByteBufferBuilder.Result buildSortedIndexBuffer(ByteBufferBuilder p_351024_, VertexSorting p_350537_) {
         int[] $$2 = p_350537_.sort(this.centroids);
         long $$3 = p_351024_.reserve($$2.length * 6 * this.indexType.bytes);
         IntConsumer $$4 = this.indexWriter($$3, this.indexType);

         for (int $$5 : $$2) {
            $$4.accept($$5 * 4 + 0);
            $$4.accept($$5 * 4 + 1);
            $$4.accept($$5 * 4 + 2);
            $$4.accept($$5 * 4 + 2);
            $$4.accept($$5 * 4 + 3);
            $$4.accept($$5 * 4 + 0);
         }

         return p_351024_.build();
      }

      private IntConsumer indexWriter(long p_350877_, VertexFormat.IndexType p_350389_) {
         MutableLong $$2 = new MutableLong(p_350877_);

         return switch (p_350389_) {
            case SHORT -> p_350656_ -> MemoryUtil.memPutShort($$2.getAndAdd(2L), (short)p_350656_);
            case INT -> p_350913_ -> MemoryUtil.memPutInt($$2.getAndAdd(4L), p_350913_);
         };
      }
   }
}
