package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class CachedPerspectiveProjectionMatrixBuffer implements AutoCloseable {
   private final GpuBuffer buffer;
   private final GpuBufferSlice bufferSlice;
   private final float zNear;
   private final float zFar;
   private int width;
   private int height;
   private float fov;

   public CachedPerspectiveProjectionMatrixBuffer(String p_418411_, float p_418058_, float p_418083_) {
      this.zNear = p_418058_;
      this.zFar = p_418083_;
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.buffer = gpudevice.createBuffer(() -> "Projection matrix UBO " + p_418411_, 136, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
      this.bufferSlice = this.buffer.slice(0L, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
   }

   public GpuBufferSlice getBuffer(int p_418258_, int p_418008_, float p_418380_) {
      if (this.width != p_418258_ || this.height != p_418008_ || this.fov != p_418380_) {
         Matrix4f matrix4f = this.createProjectionMatrix(p_418258_, p_418008_, p_418380_);
         MemoryStack memorystack = MemoryStack.stackPush();

         try {
            ByteBuffer bytebuffer = Std140Builder.onStack(memorystack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE).putMat4f(matrix4f).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), bytebuffer);
         } catch (Throwable var9) {
            if (memorystack != null) {
               try {
                  memorystack.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (memorystack != null) {
            memorystack.close();
         }

         this.width = p_418258_;
         this.height = p_418008_;
         this.fov = p_418380_;
      }

      return this.bufferSlice;
   }

   private Matrix4f createProjectionMatrix(int p_418522_, int p_418062_, float p_418303_) {
      return new Matrix4f().perspective(p_418303_ * (float) (Math.PI / 180.0), (float)p_418522_ / p_418062_, this.zNear, this.zFar);
   }

   @Override
   public void close() {
      this.buffer.close();
   }
}
