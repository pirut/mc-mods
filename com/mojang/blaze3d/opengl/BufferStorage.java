package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

@Environment(EnvType.CLIENT)
public abstract class BufferStorage {
   public static BufferStorage create(GLCapabilities capabilities, Set<String> enabledExtensions) {
      if (capabilities.GL_ARB_buffer_storage && GlDevice.USE_GL_ARB_buffer_storage) {
         enabledExtensions.add("GL_ARB_buffer_storage");
         return new BufferStorage.Immutable();
      } else {
         return new BufferStorage.Mutable();
      }
   }

   public abstract GlBuffer createBuffer(DirectStateAccess directStateAccess, @Nullable Supplier<String> label, @GpuBuffer.Usage int usage, long size);

   public abstract GlBuffer createBuffer(DirectStateAccess directStateAccess, @Nullable Supplier<String> label, @GpuBuffer.Usage int usage, ByteBuffer data);

   public abstract GlBuffer.GlMappedView mapBuffer(DirectStateAccess directStateAccess, GlBuffer buffer, long offset, long length, int access);

   @Environment(EnvType.CLIENT)
   static class Immutable extends BufferStorage {
      @Override
      public GlBuffer createBuffer(DirectStateAccess directStateAccess, @Nullable Supplier<String> label, @GpuBuffer.Usage int usage, long size) {
         int i = directStateAccess.createBuffer();
         directStateAccess.bufferStorage(i, size, usage);
         ByteBuffer byteBuffer = this.tryMapBufferPersistent(directStateAccess, usage, i, size);
         return new GlBuffer(label, directStateAccess, usage, size, i, byteBuffer);
      }

      @Override
      public GlBuffer createBuffer(DirectStateAccess directStateAccess, @Nullable Supplier<String> label, @GpuBuffer.Usage int usage, ByteBuffer data) {
         int i = directStateAccess.createBuffer();
         int j = data.remaining();
         directStateAccess.bufferStorage(i, data, usage);
         ByteBuffer byteBuffer = this.tryMapBufferPersistent(directStateAccess, usage, i, j);
         return new GlBuffer(label, directStateAccess, usage, j, i, byteBuffer);
      }

      @Nullable
      private ByteBuffer tryMapBufferPersistent(DirectStateAccess directStateAccess, @GpuBuffer.Usage int usage, int buffer, long length) {
         int i = 0;
         if ((usage & 1) != 0) {
            i |= 1;
         }

         if ((usage & 2) != 0) {
            i |= 18;
         }

         ByteBuffer byteBuffer;
         if (i != 0) {
            GlStateManager.clearGlErrors();
            byteBuffer = directStateAccess.mapBufferRange(buffer, 0L, length, i | 64, usage);
            if (byteBuffer == null) {
               throw new IllegalStateException("Can't persistently map buffer, opengl error " + GlStateManager._getError());
            }
         } else {
            byteBuffer = null;
         }

         return byteBuffer;
      }

      @Override
      public GlBuffer.GlMappedView mapBuffer(DirectStateAccess directStateAccess, GlBuffer buffer, long offset, long length, int access) {
         if (buffer.persistentBuffer == null) {
            throw new IllegalStateException("Somehow trying to map an unmappable buffer");
         } else if (offset > 2147483647L || length > 2147483647L) {
            throw new IllegalArgumentException("Mapping buffers larger than 2GB is not supported");
         } else if (offset >= 0L && length >= 0L) {
            return new GlBuffer.GlMappedView(() -> {
               if ((access & 2) != 0) {
                  directStateAccess.flushMappedBufferRange(buffer.handle, offset, length, buffer.usage());
               }
            }, buffer, MemoryUtil.memSlice(buffer.persistentBuffer, (int)offset, (int)length));
         } else {
            throw new IllegalArgumentException("Offset or length must be positive integer values");
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static class Mutable extends BufferStorage {
      @Override
      public GlBuffer createBuffer(DirectStateAccess directStateAccess, @Nullable Supplier<String> label, @GpuBuffer.Usage int usage, long size) {
         int i = directStateAccess.createBuffer();
         directStateAccess.bufferData(i, size, usage);
         return new GlBuffer(label, directStateAccess, usage, size, i, null);
      }

      @Override
      public GlBuffer createBuffer(DirectStateAccess directStateAccess, @Nullable Supplier<String> label, @GpuBuffer.Usage int usage, ByteBuffer data) {
         int i = directStateAccess.createBuffer();
         int j = data.remaining();
         directStateAccess.bufferData(i, data, usage);
         return new GlBuffer(label, directStateAccess, usage, j, i, null);
      }

      @Override
      public GlBuffer.GlMappedView mapBuffer(DirectStateAccess directStateAccess, GlBuffer buffer, long offset, long length, int access) {
         GlStateManager.clearGlErrors();
         ByteBuffer byteBuffer = directStateAccess.mapBufferRange(buffer.handle, offset, length, access, buffer.usage());
         if (byteBuffer == null) {
            throw new IllegalStateException("Can't map buffer, opengl error " + GlStateManager._getError());
         } else {
            return new GlBuffer.GlMappedView(() -> directStateAccess.unmapBuffer(buffer.handle, buffer.usage()), buffer, byteBuffer);
         }
      }
   }
}
