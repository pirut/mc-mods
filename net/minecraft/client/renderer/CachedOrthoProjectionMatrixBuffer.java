package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

@Environment(EnvType.CLIENT)
public class CachedOrthoProjectionMatrixBuffer implements AutoCloseable {
   private final GpuBuffer buffer;
   private final GpuBufferSlice bufferSlice;
   private final float zNear;
   private final float zFar;
   private final boolean invertY;
   private float width;
   private float height;

   public CachedOrthoProjectionMatrixBuffer(String label, float zNear, float zFar, boolean invertY) {
      this.zNear = zNear;
      this.zFar = zFar;
      this.invertY = invertY;
      GpuDevice gpuDevice = RenderSystem.getDevice();
      this.buffer = gpuDevice.createBuffer(() -> "Projection matrix UBO " + label, 136, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
      this.bufferSlice = this.buffer.slice(0L, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
   }

   public GpuBufferSlice getBuffer(float width, float height) {
      if (this.width != width || this.height != height) {
         Matrix4f matrix4f = this.createProjectionMatrix(width, height);
         MemoryStack memoryStack = MemoryStack.stackPush();

         try {
            ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE).putMat4f(matrix4f).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), byteBuffer);
         } catch (Throwable var8) {
            if (memoryStack != null) {
               try {
                  memoryStack.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (memoryStack != null) {
            memoryStack.close();
         }

         this.width = width;
         this.height = height;
      }

      return this.bufferSlice;
   }

   private Matrix4f createProjectionMatrix(float width, float height) {
      return new Matrix4f().setOrtho(0.0F, width, this.invertY ? height : 0.0F, this.invertY ? 0.0F : height, this.zNear, this.zFar);
   }

   @Override
   public void close() {
      this.buffer.close();
   }
}
