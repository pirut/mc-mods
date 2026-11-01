package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class DynamicUniformStorage<T extends DynamicUniformStorage.DynamicUniform> implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final List<MappableRingBuffer> oldBuffers = new ArrayList<>();
   private final int blockSize;
   private MappableRingBuffer ringBuffer;
   private int nextBlock;
   private int capacity;
   @Nullable
   private T lastUniform;
   private final String label;

   public DynamicUniformStorage(String label, int blockSize, int capacity) {
      GpuDevice gpuDevice = RenderSystem.getDevice();
      this.blockSize = Mth.roundToward(blockSize, gpuDevice.getUniformOffsetAlignment());
      this.capacity = Mth.smallestEncompassingPowerOfTwo(capacity);
      this.nextBlock = 0;
      this.ringBuffer = new MappableRingBuffer(() -> label + " x" + this.blockSize, 130, this.blockSize * this.capacity);
      this.label = label;
   }

   public void endFrame() {
      this.nextBlock = 0;
      this.lastUniform = null;
      this.ringBuffer.rotate();
      if (!this.oldBuffers.isEmpty()) {
         for (MappableRingBuffer mappableRingBuffer : this.oldBuffers) {
            mappableRingBuffer.close();
         }

         this.oldBuffers.clear();
      }
   }

   private void resizeBuffers(int newSize) {
      this.capacity = newSize;
      this.nextBlock = 0;
      this.lastUniform = null;
      this.oldBuffers.add(this.ringBuffer);
      this.ringBuffer = new MappableRingBuffer(() -> this.label + " x" + this.blockSize, 130, this.blockSize * this.capacity);
   }

   public GpuBufferSlice writeUniform(T uniform) {
      if (this.lastUniform != null && this.lastUniform.equals(uniform)) {
         return this.ringBuffer.currentBuffer().slice((this.nextBlock - 1) * this.blockSize, this.blockSize);
      } else {
         if (this.nextBlock >= this.capacity) {
            int i = this.capacity * 2;
            LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", new Object[]{this.label, this.capacity, i});
            this.resizeBuffers(i);
         }

         int i = this.nextBlock * this.blockSize;

         try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice()
               .createCommandEncoder()
               .mapBuffer(this.ringBuffer.currentBuffer().slice(i, this.blockSize), false, true)) {
            uniform.write(mappedView.data());
         }

         this.nextBlock++;
         this.lastUniform = uniform;
         return this.ringBuffer.currentBuffer().slice(i, this.blockSize);
      }
   }

   public GpuBufferSlice[] writeUniforms(T[] uniforms) {
      if (uniforms.length == 0) {
         return new GpuBufferSlice[0];
      } else {
         if (this.nextBlock + uniforms.length > this.capacity) {
            int i = Mth.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, uniforms.length));
            LOGGER.info("Resizing {}, capacity limit of {} reached during a single frame. New capacity will be {}.", new Object[]{this.label, this.capacity, i});
            this.resizeBuffers(i);
         }

         int i = this.nextBlock * this.blockSize;
         GpuBufferSlice[] gpuBufferSlices = new GpuBufferSlice[uniforms.length];

         try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice()
               .createCommandEncoder()
               .mapBuffer(this.ringBuffer.currentBuffer().slice(i, uniforms.length * this.blockSize), false, true)) {
            ByteBuffer byteBuffer = mappedView.data();

            for (int j = 0; j < uniforms.length; j++) {
               T dynamicUniform = uniforms[j];
               gpuBufferSlices[j] = this.ringBuffer.currentBuffer().slice(i + j * this.blockSize, this.blockSize);
               byteBuffer.position(j * this.blockSize);
               dynamicUniform.write(byteBuffer);
            }
         }

         this.nextBlock += uniforms.length;
         this.lastUniform = uniforms[uniforms.length - 1];
         return gpuBufferSlices;
      }
   }

   @Override
   public void close() {
      for (MappableRingBuffer mappableRingBuffer : this.oldBuffers) {
         mappableRingBuffer.close();
      }

      this.ringBuffer.close();
   }

   @Environment(EnvType.CLIENT)
   public interface DynamicUniform {
      void write(ByteBuffer buffer);
   }
}
