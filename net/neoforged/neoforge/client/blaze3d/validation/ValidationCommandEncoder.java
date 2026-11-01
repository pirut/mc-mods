package net.neoforged.neoforge.client.blaze3d.validation;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class ValidationCommandEncoder implements CommandEncoder {
   private final CommandEncoder realCommandEncoder;
   private final GpuDeviceUsageValidator validator;

   protected ValidationCommandEncoder(CommandEncoder realCommandEncoder, GpuDeviceUsageValidator validator) {
      this.realCommandEncoder = realCommandEncoder;
      this.validator = validator;
   }

   protected ValidationRenderPass wrapRenderPass(RenderPass renderPass, GpuDeviceUsageValidator validator) {
      return new ValidationRenderPass(renderPass, validator);
   }

   @Override
   public RenderPass createRenderPass(Supplier<String> label, GpuTextureView colorTextureView, OptionalInt clearColor) {
      if (colorTextureView instanceof ValidationGpuTextureView validationColorTextureView) {
         colorTextureView = validationColorTextureView.getRealTextureView();
      }

      return this.wrapRenderPass(this.realCommandEncoder.createRenderPass(label, colorTextureView, clearColor), this.validator);
   }

   @Override
   public RenderPass createRenderPass(
      Supplier<String> label, GpuTextureView colorTextureView, OptionalInt clearColor, @Nullable GpuTextureView depthTextureView, OptionalDouble clearDepth
   ) {
      if (colorTextureView instanceof ValidationGpuTextureView validationColorTextureView) {
         colorTextureView = validationColorTextureView.getRealTextureView();
      }

      if (depthTextureView instanceof ValidationGpuTextureView validationDepthTextureView) {
         depthTextureView = validationDepthTextureView.getRealTextureView();
      }

      return this.wrapRenderPass(this.realCommandEncoder.createRenderPass(label, colorTextureView, clearColor, depthTextureView, clearDepth), this.validator);
   }

   @Override
   public void clearColorTexture(GpuTexture texture, int clearColor) {
      if (texture instanceof ValidationGpuTexture validationTexture) {
         this.realCommandEncoder.clearColorTexture(validationTexture.getRealTexture(), clearColor);
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public void clearColorAndDepthTextures(GpuTexture colorTexture, int clearColor, GpuTexture depthTexture, double clearDepth) {
      if (colorTexture instanceof ValidationGpuTexture validationColorTexture) {
         if (depthTexture instanceof ValidationGpuTexture validationDepthTexture) {
            this.realCommandEncoder
               .clearColorAndDepthTextures(validationColorTexture.getRealTexture(), clearColor, validationDepthTexture.getRealTexture(), clearDepth);
         } else {
            throw new IllegalArgumentException();
         }
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public void clearColorAndDepthTextures(
      GpuTexture colorTexture, int clearColor, GpuTexture depthTexture, double clearDepth, int scissorX, int scissorY, int scissorWidth, int scissorHeight
   ) {
      if (colorTexture instanceof ValidationGpuTexture validationColorTexture) {
         if (depthTexture instanceof ValidationGpuTexture validationDepthTexture) {
            this.realCommandEncoder
               .clearColorAndDepthTextures(
                  validationColorTexture.getRealTexture(),
                  clearColor,
                  validationDepthTexture.getRealTexture(),
                  clearDepth,
                  scissorX,
                  scissorY,
                  scissorWidth,
                  scissorHeight
               );
         } else {
            throw new IllegalArgumentException();
         }
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public void clearDepthTexture(GpuTexture depthTexture, double clearDepth) {
      if (depthTexture instanceof ValidationGpuTexture validationDepthTexture) {
         this.realCommandEncoder.clearDepthTexture(validationDepthTexture.getRealTexture(), clearDepth);
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public void clearStencilTexture(GpuTexture stencilTexture, int clearValue) {
      if (stencilTexture instanceof ValidationGpuTexture validationStencilTexture) {
         this.realCommandEncoder.clearStencilTexture(validationStencilTexture.getRealTexture(), clearValue);
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public void writeToBuffer(GpuBufferSlice bufferSlice, ByteBuffer data) {
      this.realCommandEncoder.writeToBuffer(bufferSlice, data);
   }

   @Override
   public GpuBuffer.MappedView mapBuffer(GpuBuffer buffer, boolean read, boolean write) {
      return this.realCommandEncoder.mapBuffer(buffer, read, write);
   }

   @Override
   public GpuBuffer.MappedView mapBuffer(GpuBufferSlice bufferSlice, boolean read, boolean write) {
      return this.realCommandEncoder.mapBuffer(bufferSlice, read, write);
   }

   @Override
   public void copyToBuffer(GpuBufferSlice src, GpuBufferSlice dst) {
      this.realCommandEncoder.copyToBuffer(src, dst);
   }

   @Override
   public void writeToTexture(GpuTexture texture, NativeImage image) {
      if (texture instanceof ValidationGpuTexture validationTexture) {
         this.realCommandEncoder.writeToTexture(validationTexture.getRealTexture(), image);
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public void writeToTexture(GpuTexture texture, NativeImage image, int mipLevel, int layer, int dstX, int dstY, int width, int height, int srcX, int srcY) {
      if (texture instanceof ValidationGpuTexture validationTexture) {
         this.realCommandEncoder.writeToTexture(validationTexture.getRealTexture(), image, mipLevel, layer, dstX, dstY, width, height, srcX, srcY);
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public void writeToTexture(
      GpuTexture texture, ByteBuffer imageData, NativeImage.Format imageFormat, int mipLevel, int layer, int x, int y, int width, int height
   ) {
      if (texture instanceof ValidationGpuTexture validationTexture) {
         this.realCommandEncoder.writeToTexture(validationTexture.getRealTexture(), imageData, imageFormat, mipLevel, layer, x, y, width, height);
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public void copyTextureToBuffer(GpuTexture texture, GpuBuffer buffer, long bufferOffset, Runnable callback, int mipLevel) {
      if (texture instanceof ValidationGpuTexture validationTexture) {
         this.realCommandEncoder.copyTextureToBuffer(validationTexture.getRealTexture(), buffer, bufferOffset, callback, mipLevel);
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public void copyTextureToBuffer(
      GpuTexture texture, GpuBuffer buffer, long bufferOffset, Runnable callback, int mipLevel, int x, int y, int width, int height
   ) {
      if (texture instanceof ValidationGpuTexture validationTexture) {
         this.realCommandEncoder.copyTextureToBuffer(validationTexture.getRealTexture(), buffer, bufferOffset, callback, mipLevel, x, y, width, height);
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public void copyTextureToTexture(GpuTexture srcTexture, GpuTexture dstTexture, int mipLevel, int srcX, int srcY, int dstX, int dstY, int width, int height) {
      if (srcTexture instanceof ValidationGpuTexture validationSrcTexture) {
         if (dstTexture instanceof ValidationGpuTexture validationDstTexture) {
            this.realCommandEncoder
               .copyTextureToTexture(
                  validationSrcTexture.getRealTexture(), validationDstTexture.getRealTexture(), mipLevel, srcX, srcY, dstX, dstY, width, height
               );
         } else {
            throw new IllegalArgumentException();
         }
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public void presentTexture(GpuTextureView textureView) {
      if (textureView instanceof ValidationGpuTextureView validationTextureView) {
         textureView = validationTextureView.getRealTextureView();
      }

      this.realCommandEncoder.presentTexture(textureView);
   }

   @Override
   public GpuFence createFence() {
      return this.realCommandEncoder.createFence();
   }

   @Override
   public GpuQuery timerQueryBegin() {
      return this.realCommandEncoder.timerQueryBegin();
   }

   @Override
   public void timerQueryEnd(GpuQuery query) {
      this.realCommandEncoder.timerQueryEnd(query);
   }
}
