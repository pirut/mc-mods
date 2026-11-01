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
public class CachedOrthoProjectionMatrixBuffer implements AutoCloseable {
   private final GpuBuffer buffer;
   private final GpuBufferSlice bufferSlice;
   private final float zNear;
   private final float zFar;
   private final boolean invertY;
   private float width;
   private float height;

   public CachedOrthoProjectionMatrixBuffer(String p_418505_, float p_418501_, float p_418321_, boolean p_418108_) {
      this.zNear = p_418501_;
      this.zFar = p_418321_;
      this.invertY = p_418108_;
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.buffer = gpudevice.createBuffer(() -> "Projection matrix UBO " + p_418505_, 136, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
      this.bufferSlice = this.buffer.slice(0L, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
   }

   public GpuBufferSlice getBuffer(float p_418457_, float p_418403_) {
      if (this.width != p_418457_ || this.height != p_418403_) {
         Matrix4f matrix4f = this.createProjectionMatrix(p_418457_, p_418403_);
         MemoryStack memorystack = MemoryStack.stackPush();

         try {
            ByteBuffer bytebuffer = Std140Builder.onStack(memorystack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE).putMat4f(matrix4f).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), bytebuffer);
         } catch (Throwable var8) {
            if (memorystack != null) {
               try {
                  memorystack.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (memorystack != null) {
            memorystack.close();
         }

         this.width = p_418457_;
         this.height = p_418403_;
      }

      return this.bufferSlice;
   }

   private Matrix4f createProjectionMatrix(float p_418447_, float p_418426_) {
      return new Matrix4f().setOrtho(0.0F, p_418447_, this.invertY ? p_418426_ : 0.0F, this.invertY ? 0.0F : p_418426_, this.zNear, this.zFar);
   }

   @Override
   public void close() {
      this.buffer.close();
   }
}
