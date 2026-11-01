package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GlBuffer extends GpuBuffer {
   protected static final MemoryPool MEMORY_POOl = TracyClient.createMemoryPool("GPU Buffers");
   protected boolean closed;
   @Nullable
   protected final Supplier<String> label;
   private final DirectStateAccess dsa;
   protected final int handle;
   @Nullable
   protected ByteBuffer persistentBuffer;

   protected GlBuffer(
      @Nullable Supplier<String> p_410223_,
      DirectStateAccess p_418155_,
      @GpuBuffer.Usage int p_409846_,
      long p_482282_,
      int p_409922_,
      @Nullable ByteBuffer p_418525_
   ) {
      super(p_409846_, p_482282_);
      this.label = p_410223_;
      this.dsa = p_418155_;
      this.handle = p_409922_;
      this.persistentBuffer = p_418525_;
      int i = (int)Math.min(p_482282_, 2147483647L);
      MEMORY_POOl.malloc(p_409922_, i);
   }

   @Override
   public boolean isClosed() {
      return this.closed;
   }

   @Override
   public void close() {
      if (!this.closed) {
         this.closed = true;
         if (this.persistentBuffer != null) {
            this.dsa.unmapBuffer(this.handle, this.usage());
            this.persistentBuffer = null;
         }

         GlStateManager._glDeleteBuffers(this.handle);
         MEMORY_POOl.free(this.handle);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class GlMappedView implements GpuBuffer.MappedView {
      private final Runnable unmap;
      private final GlBuffer buffer;
      private final ByteBuffer data;
      private boolean closed;

      protected GlMappedView(Runnable p_418346_, GlBuffer p_418517_, ByteBuffer p_418416_) {
         this.unmap = p_418346_;
         this.buffer = p_418517_;
         this.data = p_418416_;
      }

      @Override
      public ByteBuffer data() {
         return this.data;
      }

      @Override
      public void close() {
         if (!this.closed) {
            this.closed = true;
            this.unmap.run();
         }
      }
   }
}
