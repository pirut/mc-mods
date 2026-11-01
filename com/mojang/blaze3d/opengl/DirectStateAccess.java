package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.buffers.GpuBuffer;
import java.nio.ByteBuffer;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GLCapabilities;

@Environment(EnvType.CLIENT)
public abstract class DirectStateAccess {
   public static DirectStateAccess create(GLCapabilities capabilities, Set<String> enabledExtensions, GraphicsWorkarounds workarounds) {
      if (capabilities.GL_ARB_direct_state_access && GlDevice.USE_GL_ARB_direct_state_access && !workarounds.isGlOnDx12()) {
         enabledExtensions.add("GL_ARB_direct_state_access");
         return new DirectStateAccess.Core();
      } else {
         return new DirectStateAccess.Emulated();
      }
   }

   abstract int createBuffer();

   abstract void bufferData(int buffer, long size, @GpuBuffer.Usage int usage);

   abstract void bufferData(int buffer, ByteBuffer data, @GpuBuffer.Usage int usage);

   abstract void bufferSubData(int buffer, long offset, ByteBuffer data, @GpuBuffer.Usage int usage);

   abstract void bufferStorage(int buffer, long size, @GpuBuffer.Usage int usage);

   abstract void bufferStorage(int buffer, ByteBuffer data, @GpuBuffer.Usage int usage);

   @Nullable
   abstract ByteBuffer mapBufferRange(int buffer, long offset, long length, int access, @GpuBuffer.Usage int usage);

   abstract void unmapBuffer(int buffer, @GpuBuffer.Usage int usage);

   abstract int createFrameBufferObject();

   abstract void bindFrameBufferTextures(int frameBuffer, int colorTexture, int depthTexture, int level, int target);

   abstract void blitFrameBuffers(
      int readFrameBuffer,
      int drawFrameBuffer,
      int srcX0,
      int srcY0,
      int srcX1,
      int srcY1,
      int destX0,
      int destY0,
      int destX1,
      int destY1,
      int mask,
      int filter
   );

   abstract void flushMappedBufferRange(int buffer, long offset, long length, @GpuBuffer.Usage int usage);

   abstract void copyBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset, long size);

   @Environment(EnvType.CLIENT)
   static class Core extends DirectStateAccess {
      @Override
      int createBuffer() {
         GlStateManager.incrementTrackedBuffers();
         return ARBDirectStateAccess.glCreateBuffers();
      }

      @Override
      void bufferData(int buffer, long size, @GpuBuffer.Usage int usage) {
         ARBDirectStateAccess.glNamedBufferData(buffer, size, GlConst.bufferUsageToGlEnum(usage));
      }

      @Override
      void bufferData(int buffer, ByteBuffer data, @GpuBuffer.Usage int usage) {
         ARBDirectStateAccess.glNamedBufferData(buffer, data, GlConst.bufferUsageToGlEnum(usage));
      }

      @Override
      void bufferSubData(int buffer, long offset, ByteBuffer data, @GpuBuffer.Usage int usage) {
         ARBDirectStateAccess.glNamedBufferSubData(buffer, offset, data);
      }

      @Override
      void bufferStorage(int buffer, long size, @GpuBuffer.Usage int usage) {
         ARBDirectStateAccess.glNamedBufferStorage(buffer, size, GlConst.bufferUsageToGlFlag(usage));
      }

      @Override
      void bufferStorage(int buffer, ByteBuffer data, @GpuBuffer.Usage int usage) {
         ARBDirectStateAccess.glNamedBufferStorage(buffer, data, GlConst.bufferUsageToGlFlag(usage));
      }

      @Nullable
      @Override
      ByteBuffer mapBufferRange(int buffer, long offset, long length, int access, @GpuBuffer.Usage int usage) {
         return ARBDirectStateAccess.glMapNamedBufferRange(buffer, offset, length, access);
      }

      @Override
      void unmapBuffer(int buffer, int usage) {
         ARBDirectStateAccess.glUnmapNamedBuffer(buffer);
      }

      @Override
      public int createFrameBufferObject() {
         return ARBDirectStateAccess.glCreateFramebuffers();
      }

      @Override
      public void bindFrameBufferTextures(int frameBuffer, int colorTexture, int depthTexture, int level, @GpuBuffer.Usage int target) {
         ARBDirectStateAccess.glNamedFramebufferTexture(frameBuffer, 36064, colorTexture, level);
         ARBDirectStateAccess.glNamedFramebufferTexture(frameBuffer, 36096, depthTexture, level);
         if (target != 0) {
            GlStateManager._glBindFramebuffer(target, frameBuffer);
         }
      }

      @Override
      public void blitFrameBuffers(
         int readFrameBuffer,
         int drawFrameBuffer,
         int srcX0,
         int srcY0,
         int srcX1,
         int srcY1,
         int destX0,
         int destY0,
         int destX1,
         int destY1,
         int mask,
         int filter
      ) {
         ARBDirectStateAccess.glBlitNamedFramebuffer(readFrameBuffer, drawFrameBuffer, srcX0, srcY0, srcX1, srcY1, destX0, destY0, destX1, destY1, mask, filter);
      }

      @Override
      void flushMappedBufferRange(int buffer, long offset, long length, @GpuBuffer.Usage int usage) {
         ARBDirectStateAccess.glFlushMappedNamedBufferRange(buffer, offset, length);
      }

      @Override
      void copyBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset, long size) {
         ARBDirectStateAccess.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
      }
   }

   @Environment(EnvType.CLIENT)
   static class Emulated extends DirectStateAccess {
      private int selectBufferBindTarget(@GpuBuffer.Usage int usage) {
         if ((usage & 32) != 0) {
            return 34962;
         } else if ((usage & 64) != 0) {
            return 34963;
         } else {
            return (usage & 128) != 0 ? 35345 : 36663;
         }
      }

      @Override
      int createBuffer() {
         return GlStateManager._glGenBuffers();
      }

      @Override
      void bufferData(int buffer, long size, @GpuBuffer.Usage int usage) {
         int i = this.selectBufferBindTarget(usage);
         GlStateManager._glBindBuffer(i, buffer);
         GlStateManager._glBufferData(i, size, GlConst.bufferUsageToGlEnum(usage));
         GlStateManager._glBindBuffer(i, 0);
      }

      @Override
      void bufferData(int buffer, ByteBuffer data, @GpuBuffer.Usage int usage) {
         int i = this.selectBufferBindTarget(usage);
         GlStateManager._glBindBuffer(i, buffer);
         GlStateManager._glBufferData(i, data, GlConst.bufferUsageToGlEnum(usage));
         GlStateManager._glBindBuffer(i, 0);
      }

      @Override
      void bufferSubData(int buffer, long offset, ByteBuffer data, @GpuBuffer.Usage int usage) {
         int i = this.selectBufferBindTarget(usage);
         GlStateManager._glBindBuffer(i, buffer);
         GlStateManager._glBufferSubData(i, offset, data);
         GlStateManager._glBindBuffer(i, 0);
      }

      @Override
      void bufferStorage(int buffer, long size, @GpuBuffer.Usage int usage) {
         int i = this.selectBufferBindTarget(usage);
         GlStateManager._glBindBuffer(i, buffer);
         ARBBufferStorage.glBufferStorage(i, size, GlConst.bufferUsageToGlFlag(usage));
         GlStateManager._glBindBuffer(i, 0);
      }

      @Override
      void bufferStorage(int buffer, ByteBuffer data, @GpuBuffer.Usage int usage) {
         int i = this.selectBufferBindTarget(usage);
         GlStateManager._glBindBuffer(i, buffer);
         ARBBufferStorage.glBufferStorage(i, data, GlConst.bufferUsageToGlFlag(usage));
         GlStateManager._glBindBuffer(i, 0);
      }

      @Nullable
      @Override
      ByteBuffer mapBufferRange(int buffer, long offset, long length, int access, @GpuBuffer.Usage int usage) {
         int i = this.selectBufferBindTarget(usage);
         GlStateManager._glBindBuffer(i, buffer);
         ByteBuffer byteBuffer = GlStateManager._glMapBufferRange(i, offset, length, access);
         GlStateManager._glBindBuffer(i, 0);
         return byteBuffer;
      }

      @Override
      void unmapBuffer(int buffer, @GpuBuffer.Usage int usage) {
         int i = this.selectBufferBindTarget(usage);
         GlStateManager._glBindBuffer(i, buffer);
         GlStateManager._glUnmapBuffer(i);
         GlStateManager._glBindBuffer(i, 0);
      }

      @Override
      void flushMappedBufferRange(int buffer, long offset, long length, @GpuBuffer.Usage int usage) {
         int i = this.selectBufferBindTarget(usage);
         GlStateManager._glBindBuffer(i, buffer);
         GL30.glFlushMappedBufferRange(i, offset, length);
         GlStateManager._glBindBuffer(i, 0);
      }

      @Override
      void copyBufferSubData(int readBuffer, int writeBuffer, long readOffset, long writeOffset, long size) {
         GlStateManager._glBindBuffer(36662, readBuffer);
         GlStateManager._glBindBuffer(36663, writeBuffer);
         GL31.glCopyBufferSubData(36662, 36663, readOffset, writeOffset, size);
         GlStateManager._glBindBuffer(36662, 0);
         GlStateManager._glBindBuffer(36663, 0);
      }

      @Override
      public int createFrameBufferObject() {
         return GlStateManager.glGenFramebuffers();
      }

      @Override
      public void bindFrameBufferTextures(int frameBuffer, int colorTexture, int depthTexture, int level, int target) {
         int i = target == 0 ? '販' : target;
         int j = GlStateManager.getFrameBuffer(i);
         GlStateManager._glBindFramebuffer(i, frameBuffer);
         GlStateManager._glFramebufferTexture2D(i, 36064, 3553, colorTexture, level);
         GlStateManager._glFramebufferTexture2D(i, 36096, 3553, depthTexture, level);
         if (target == 0) {
            GlStateManager._glBindFramebuffer(i, j);
         }
      }

      @Override
      public void blitFrameBuffers(
         int readFrameBuffer,
         int drawFrameBuffer,
         int srcX0,
         int srcY0,
         int srcX1,
         int srcY1,
         int destX0,
         int destY0,
         int destX1,
         int destY1,
         int mask,
         int filter
      ) {
         int i = GlStateManager.getFrameBuffer(36008);
         int j = GlStateManager.getFrameBuffer(36009);
         GlStateManager._glBindFramebuffer(36008, readFrameBuffer);
         GlStateManager._glBindFramebuffer(36009, drawFrameBuffer);
         GlStateManager._glBlitFrameBuffer(srcX0, srcY0, srcX1, srcY1, destX0, destY0, destX1, destY1, mask, filter);
         GlStateManager._glBindFramebuffer(36008, i);
         GlStateManager._glBindFramebuffer(36009, j);
      }
   }
}
