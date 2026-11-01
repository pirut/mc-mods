package com.mojang.blaze3d.vertex;

import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.MemoryUtil.MemoryAllocator;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ByteBufferBuilder implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);
   private static final int MAX_GROWTH_SIZE = 2097152;
   private static final int BUFFER_FREED_GENERATION = -1;
   long pointer;
   private int capacity;
   private int writeOffset;
   private int nextResultOffset;
   private int resultCount;
   private int generation;

   public ByteBufferBuilder(int p_350446_) {
      this.capacity = p_350446_;
      this.pointer = ALLOCATOR.malloc(p_350446_);
      if (this.pointer == 0L) {
         throw new OutOfMemoryError("Failed to allocate " + p_350446_ + " bytes");
      }
   }

   public long reserve(int p_350289_) {
      int $$1 = this.writeOffset;
      int $$2 = $$1 + p_350289_;
      this.ensureCapacity($$2);
      this.writeOffset = $$2;
      return this.pointer + $$1;
   }

   private void ensureCapacity(int p_350893_) {
      if (p_350893_ > this.capacity) {
         int $$1 = Math.min(this.capacity, 2097152);
         int $$2 = Math.max(this.capacity + $$1, p_350893_);
         this.resize($$2);
      }
   }

   private void resize(int p_350909_) {
      this.pointer = ALLOCATOR.realloc(this.pointer, p_350909_);
      LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", this.capacity, p_350909_);
      if (this.pointer == 0L) {
         throw new OutOfMemoryError("Failed to resize buffer from " + this.capacity + " bytes to " + p_350909_ + " bytes");
      } else {
         this.capacity = p_350909_;
      }
   }

   @Nullable
   public ByteBufferBuilder.Result build() {
      this.checkOpen();
      int $$0 = this.nextResultOffset;
      int $$1 = this.writeOffset - $$0;
      if ($$1 == 0) {
         return null;
      } else {
         this.nextResultOffset = this.writeOffset;
         this.resultCount++;
         return new ByteBufferBuilder.Result($$0, $$1, this.generation);
      }
   }

   public void clear() {
      if (this.resultCount > 0) {
         LOGGER.warn("Clearing BufferBuilder with unused batches");
      }

      this.discard();
   }

   public void discard() {
      this.checkOpen();
      if (this.resultCount > 0) {
         this.discardResults();
         this.resultCount = 0;
      }
   }

   boolean isValid(int p_350321_) {
      return p_350321_ == this.generation;
   }

   void freeResult() {
      if (--this.resultCount <= 0) {
         this.discardResults();
      }
   }

   private void discardResults() {
      int $$0 = this.writeOffset - this.nextResultOffset;
      if ($$0 > 0) {
         MemoryUtil.memCopy(this.pointer + this.nextResultOffset, this.pointer, $$0);
      }

      this.writeOffset = $$0;
      this.nextResultOffset = 0;
      this.generation++;
   }

   @Override
   public void close() {
      if (this.pointer != 0L) {
         ALLOCATOR.free(this.pointer);
         this.pointer = 0L;
         this.generation = -1;
      }
   }

   private void checkOpen() {
      if (this.pointer == 0L) {
         throw new IllegalStateException("Buffer has been freed");
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class Result implements AutoCloseable {
      private final int offset;
      private final int capacity;
      private final int generation;
      private boolean closed;

      Result(int p_350632_, int p_350856_, int p_350616_) {
         this.offset = p_350632_;
         this.capacity = p_350856_;
         this.generation = p_350616_;
      }

      public ByteBuffer byteBuffer() {
         if (!ByteBufferBuilder.this.isValid(this.generation)) {
            throw new IllegalStateException("Buffer is no longer valid");
         } else {
            return MemoryUtil.memByteBuffer(ByteBufferBuilder.this.pointer + this.offset, this.capacity);
         }
      }

      @Override
      public void close() {
         if (!this.closed) {
            this.closed = true;
            if (ByteBufferBuilder.this.isValid(this.generation)) {
               ByteBufferBuilder.this.freeResult();
            }
         }
      }
   }
}
