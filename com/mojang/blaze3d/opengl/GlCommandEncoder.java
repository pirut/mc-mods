package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.stencil.StencilPerFaceTest;
import net.neoforged.neoforge.client.stencil.StencilTest;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GlCommandEncoder implements CommandEncoder {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final GlDevice device;
   private final int readFbo;
   private final int drawFbo;
   @Nullable
   private RenderPipeline lastPipeline;
   private boolean inRenderPass;
   @Nullable
   private GlProgram lastProgram;
   @Nullable
   private GlTimerQuery activeTimerQuery;

   protected GlCommandEncoder(GlDevice p_410479_) {
      this.device = p_410479_;
      this.readFbo = p_410479_.directStateAccess().createFrameBufferObject();
      this.drawFbo = p_410479_.directStateAccess().createFrameBufferObject();
   }

   @Override
   public RenderPass createRenderPass(Supplier<String> p_419658_, GpuTextureView p_423471_, OptionalInt p_410192_) {
      return this.createRenderPass(p_419658_, p_423471_, p_410192_, null, OptionalDouble.empty());
   }

   @Override
   public RenderPass createRenderPass(
      Supplier<String> p_419957_, GpuTextureView p_423627_, OptionalInt p_410460_, @Nullable GpuTextureView p_423565_, OptionalDouble p_423486_
   ) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      } else {
         if (p_423486_.isPresent() && p_423565_ == null) {
            LOGGER.warn("Depth clear value was provided but no depth texture is being used");
         }

         if (p_423627_.isClosed()) {
            throw new IllegalStateException("Color texture is closed");
         } else if ((p_423627_.texture().usage() & 8) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
         } else if (p_423627_.texture().getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported as an attachment");
         } else {
            if (p_423565_ != null) {
               if (p_423565_.isClosed()) {
                  throw new IllegalStateException("Depth texture is closed");
               }

               if ((p_423565_.texture().usage() & 8) == 0) {
                  throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
               }

               if (p_423565_.texture().getDepthOrLayers() > 1) {
                  throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported as an attachment");
               }
            }

            this.inRenderPass = true;
            this.device.debugLabels().pushDebugGroup(p_419957_);
            int i = ((GlTextureView)p_423627_).getFbo(this.device.directStateAccess(), p_423565_ == null ? null : p_423565_.texture());
            GlStateManager._glBindFramebuffer(36160, i);
            int j = 0;
            if (p_410460_.isPresent()) {
               int k = p_410460_.getAsInt();
               GL11.glClearColor(ARGB.redFloat(k), ARGB.greenFloat(k), ARGB.blueFloat(k), ARGB.alphaFloat(k));
               j |= 16384;
            }

            if (p_423565_ != null && p_423486_.isPresent()) {
               GL11.glClearDepth(p_423486_.getAsDouble());
               j |= 256;
            }

            if (j != 0) {
               GlStateManager._disableScissorTest();
               GlStateManager._depthMask(true);
               GlStateManager._colorMask(true, true, true, true);
               GlStateManager._clear(j);
            }

            GlStateManager._viewport(0, 0, p_423627_.getWidth(0), p_423627_.getHeight(0));
            this.lastPipeline = null;
            return new GlRenderPass(this, p_423565_ != null);
         }
      }
   }

   @Override
   public void clearColorTexture(GpuTexture p_410228_, int p_410646_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      } else {
         this.verifyColorTexture(p_410228_);
         this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)p_410228_).id, 0, 0, 36160);
         GL11.glClearColor(ARGB.redFloat(p_410646_), ARGB.greenFloat(p_410646_), ARGB.blueFloat(p_410646_), ARGB.alphaFloat(p_410646_));
         GlStateManager._disableScissorTest();
         GlStateManager._colorMask(true, true, true, true);
         GlStateManager._clear(16384);
         GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
         GlStateManager._glBindFramebuffer(36160, 0);
      }
   }

   @Override
   public void clearColorAndDepthTextures(GpuTexture p_410863_, int p_410603_, GpuTexture p_409616_, double p_410193_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      } else {
         this.verifyColorTexture(p_410863_);
         this.verifyDepthTexture(p_409616_);
         int i = ((GlTexture)p_410863_).getFbo(this.device.directStateAccess(), p_409616_);
         GlStateManager._glBindFramebuffer(36160, i);
         GlStateManager._disableScissorTest();
         GL11.glClearDepth(p_410193_);
         GL11.glClearColor(ARGB.redFloat(p_410603_), ARGB.greenFloat(p_410603_), ARGB.blueFloat(p_410603_), ARGB.alphaFloat(p_410603_));
         GlStateManager._depthMask(true);
         GlStateManager._colorMask(true, true, true, true);
         GlStateManager._clear(16640);
         GlStateManager._glBindFramebuffer(36160, 0);
      }
   }

   @Override
   public void clearColorAndDepthTextures(
      GpuTexture p_416345_, int p_416520_, GpuTexture p_416382_, double p_415785_, int p_415996_, int p_416125_, int p_415580_, int p_415906_
   ) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      } else {
         this.verifyColorTexture(p_416345_);
         this.verifyDepthTexture(p_416382_);
         this.verifyRegion(p_416345_, p_415996_, p_416125_, p_415580_, p_415906_);
         int i = ((GlTexture)p_416345_).getFbo(this.device.directStateAccess(), p_416382_);
         GlStateManager._glBindFramebuffer(36160, i);
         GlStateManager._scissorBox(p_415996_, p_416125_, p_415580_, p_415906_);
         GlStateManager._enableScissorTest();
         GL11.glClearDepth(p_415785_);
         GL11.glClearColor(ARGB.redFloat(p_416520_), ARGB.greenFloat(p_416520_), ARGB.blueFloat(p_416520_), ARGB.alphaFloat(p_416520_));
         GlStateManager._depthMask(true);
         GlStateManager._colorMask(true, true, true, true);
         GlStateManager._clear(16640);
         GlStateManager._glBindFramebuffer(36160, 0);
      }
   }

   private void verifyRegion(GpuTexture p_416315_, int p_415567_, int p_416331_, int p_415993_, int p_415706_) {
      if (p_415567_ < 0 || p_415567_ >= p_416315_.getWidth(0)) {
         throw new IllegalArgumentException("regionX should not be outside of the texture");
      } else if (p_416331_ < 0 || p_416331_ >= p_416315_.getHeight(0)) {
         throw new IllegalArgumentException("regionY should not be outside of the texture");
      } else if (p_415993_ <= 0) {
         throw new IllegalArgumentException("regionWidth should be greater than 0");
      } else if (p_415567_ + p_415993_ > p_416315_.getWidth(0)) {
         throw new IllegalArgumentException("regionWidth + regionX should be less than the texture width");
      } else if (p_415706_ <= 0) {
         throw new IllegalArgumentException("regionHeight should be greater than 0");
      } else if (p_416331_ + p_415706_ > p_416315_.getHeight(0)) {
         throw new IllegalArgumentException("regionWidth + regionX should be less than the texture height");
      }
   }

   @Override
   public void clearDepthTexture(GpuTexture p_410548_, double p_410067_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      } else {
         this.verifyDepthTexture(p_410548_);
         boolean hasStencil = p_410548_.getFormat().hasStencilAspect();
         this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, 0, ((GlTexture)p_410548_).id, 0, 36160, hasStencil);
         GL11.glDrawBuffer(0);
         GL11.glClearDepth(p_410067_);
         GlStateManager._depthMask(true);
         GlStateManager._disableScissorTest();
         GlStateManager._clear(256);
         GL11.glDrawBuffer(36064);
         GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, 0, 0);
         GlStateManager._glBindFramebuffer(36160, 0);
      }
   }

   @Override
   public void clearStencilTexture(GpuTexture texture, int value) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      } else if (!texture.getFormat().hasStencilAspect()) {
         throw new IllegalStateException("Trying to clear stencil in a texture that has no stencil component!");
      } else {
         this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, 0, ((GlTexture)texture).id, 0, 36160, true);
         GL11.glDrawBuffer(0);
         GL11.glClearStencil(value);
         GlStateManager._depthMask(true);
         GlStateManager._clear(1024);
         GL11.glDrawBuffer(36064);
         GlStateManager._glBindFramebuffer(36160, 0);
      }
   }

   private void verifyColorTexture(GpuTexture p_416690_) {
      if (!p_416690_.getFormat().hasColorAspect()) {
         throw new IllegalStateException("Trying to clear a non-color texture as color");
      } else if (p_416690_.isClosed()) {
         throw new IllegalStateException("Color texture is closed");
      } else if ((p_416690_.usage() & 8) == 0) {
         throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
      } else if (p_416690_.getDepthOrLayers() > 1) {
         throw new UnsupportedOperationException("Clearing a texture with multiple layers or depths is not yet supported");
      }
   }

   private void verifyDepthTexture(GpuTexture p_416396_) {
      if (!p_416396_.getFormat().hasDepthAspect()) {
         throw new IllegalStateException("Trying to clear a non-depth texture as depth");
      } else if (p_416396_.isClosed()) {
         throw new IllegalStateException("Depth texture is closed");
      } else if ((p_416396_.usage() & 8) == 0) {
         throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
      } else if (p_416396_.getDepthOrLayers() > 1) {
         throw new UnsupportedOperationException("Clearing a texture with multiple layers or depths is not yet supported");
      }
   }

   @Override
   public void writeToBuffer(GpuBufferSlice p_418368_, ByteBuffer p_410689_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else {
         GlBuffer glbuffer = (GlBuffer)p_418368_.buffer();
         if (glbuffer.closed) {
            throw new IllegalStateException("Buffer already closed");
         } else if ((glbuffer.usage() & 8) == 0) {
            throw new IllegalStateException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
         } else {
            int i = p_410689_.remaining();
            if (i > p_418368_.length()) {
               throw new IllegalArgumentException(
                  "Cannot write more data than the slice allows (attempting to write " + i + " bytes into a slice of length " + p_418368_.length() + ")"
               );
            } else if (p_418368_.length() + p_418368_.offset() > glbuffer.size()) {
               throw new IllegalArgumentException(
                  "Cannot write more data than this buffer can hold (attempting to write "
                     + i
                     + " bytes at offset "
                     + p_418368_.offset()
                     + " to "
                     + glbuffer.size()
                     + " size buffer)"
               );
            } else {
               this.device.directStateAccess().bufferSubData(glbuffer.handle, p_418368_.offset(), p_410689_, glbuffer.usage());
            }
         }
      }
   }

   @Override
   public GpuBuffer.MappedView mapBuffer(GpuBuffer p_418290_, boolean p_418044_, boolean p_418126_) {
      return this.mapBuffer(p_418290_.slice(), p_418044_, p_418126_);
   }

   @Override
   public GpuBuffer.MappedView mapBuffer(GpuBufferSlice p_418128_, boolean p_418013_, boolean p_418412_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else {
         GlBuffer glbuffer = (GlBuffer)p_418128_.buffer();
         if (glbuffer.closed) {
            throw new IllegalStateException("Buffer already closed");
         } else if (!p_418013_ && !p_418412_) {
            throw new IllegalArgumentException("At least read or write must be true");
         } else if (p_418013_ && (glbuffer.usage() & 1) == 0) {
            throw new IllegalStateException("Buffer is not readable");
         } else if (p_418412_ && (glbuffer.usage() & 2) == 0) {
            throw new IllegalStateException("Buffer is not writable");
         } else if (p_418128_.offset() + p_418128_.length() > glbuffer.size()) {
            throw new IllegalArgumentException(
               "Cannot map more data than this buffer can hold (attempting to map "
                  + p_418128_.length()
                  + " bytes at offset "
                  + p_418128_.offset()
                  + " from "
                  + glbuffer.size()
                  + " size buffer)"
            );
         } else {
            int i = 0;
            if (p_418013_) {
               i |= 1;
            }

            if (p_418412_) {
               i |= 34;
            }

            return this.device.getBufferStorage().mapBuffer(this.device.directStateAccess(), glbuffer, p_418128_.offset(), p_418128_.length(), i);
         }
      }
   }

   @Override
   public void copyToBuffer(GpuBufferSlice p_428848_, GpuBufferSlice p_428840_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else {
         GlBuffer glbuffer = (GlBuffer)p_428848_.buffer();
         if (glbuffer.closed) {
            throw new IllegalStateException("Source buffer already closed");
         } else if ((glbuffer.usage() & 16) == 0) {
            throw new IllegalStateException("Source buffer needs USAGE_COPY_SRC to be a source for a copy");
         } else {
            GlBuffer glbuffer1 = (GlBuffer)p_428840_.buffer();
            if (glbuffer1.closed) {
               throw new IllegalStateException("Target buffer already closed");
            } else if ((glbuffer1.usage() & 8) == 0) {
               throw new IllegalStateException("Target buffer needs USAGE_COPY_DST to be a destination for a copy");
            } else if (p_428848_.length() != p_428840_.length()) {
               throw new IllegalArgumentException(
                  "Cannot copy from slice of size " + p_428848_.length() + " to slice of size " + p_428840_.length() + ", they must be equal"
               );
            } else if (p_428848_.offset() + p_428848_.length() > glbuffer.size()) {
               throw new IllegalArgumentException(
                  "Cannot copy more data than the source buffer holds (attempting to copy "
                     + p_428848_.length()
                     + " bytes at offset "
                     + p_428848_.offset()
                     + " from "
                     + glbuffer.size()
                     + " size buffer)"
               );
            } else if (p_428840_.offset() + p_428840_.length() > glbuffer1.size()) {
               throw new IllegalArgumentException(
                  "Cannot copy more data than the target buffer can hold (attempting to copy "
                     + p_428840_.length()
                     + " bytes at offset "
                     + p_428840_.offset()
                     + " to "
                     + glbuffer1.size()
                     + " size buffer)"
               );
            } else {
               this.device.directStateAccess().copyBufferSubData(glbuffer.handle, glbuffer1.handle, p_428848_.offset(), p_428840_.offset(), p_428848_.length());
            }
         }
      }
   }

   @Override
   public void writeToTexture(GpuTexture p_409824_, NativeImage p_410255_) {
      int i = p_409824_.getWidth(0);
      int j = p_409824_.getHeight(0);
      if (p_410255_.getWidth() != i || p_410255_.getHeight() != j) {
         throw new IllegalArgumentException(
            "Cannot replace texture of size " + i + "x" + j + " with image of size " + p_410255_.getWidth() + "x" + p_410255_.getHeight()
         );
      } else if (p_409824_.isClosed()) {
         throw new IllegalStateException("Destination texture is closed");
      } else if ((p_409824_.usage() & 1) == 0) {
         throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
      } else {
         this.writeToTexture(p_409824_, p_410255_, 0, 0, 0, 0, i, j, 0, 0);
      }
   }

   @Override
   public void writeToTexture(
      GpuTexture p_410473_,
      NativeImage p_423502_,
      int p_410132_,
      int p_409948_,
      int p_410810_,
      int p_409825_,
      int p_410770_,
      int p_423635_,
      int p_423531_,
      int p_423633_
   ) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else if (p_410132_ >= 0 && p_410132_ < p_410473_.getMipLevels()) {
         if (p_423531_ + p_410770_ > p_423502_.getWidth() || p_423633_ + p_423635_ > p_423502_.getHeight()) {
            throw new IllegalArgumentException(
               "Copy source ("
                  + p_423502_.getWidth()
                  + "x"
                  + p_423502_.getHeight()
                  + ") is not large enough to read a rectangle of "
                  + p_410770_
                  + "x"
                  + p_423635_
                  + " from "
                  + p_423531_
                  + "x"
                  + p_423633_
            );
         } else if (p_410810_ + p_410770_ > p_410473_.getWidth(p_410132_) || p_409825_ + p_423635_ > p_410473_.getHeight(p_410132_)) {
            throw new IllegalArgumentException(
               "Dest texture ("
                  + p_410770_
                  + "x"
                  + p_423635_
                  + ") is not large enough to write a rectangle of "
                  + p_410770_
                  + "x"
                  + p_423635_
                  + " at "
                  + p_410810_
                  + "x"
                  + p_409825_
                  + " (at mip level "
                  + p_410132_
                  + ")"
            );
         } else if (p_410473_.isClosed()) {
            throw new IllegalStateException("Destination texture is closed");
         } else if ((p_410473_.usage() & 1) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
         } else if (p_409948_ >= p_410473_.getDepthOrLayers()) {
            throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + p_410473_.getDepthOrLayers());
         } else {
            int i;
            if ((p_410473_.usage() & 16) != 0) {
               i = GlConst.CUBEMAP_TARGETS[p_409948_ % 6];
               GL11.glBindTexture(34067, ((GlTexture)p_410473_).id);
            } else {
               i = 3553;
               GlStateManager._bindTexture(((GlTexture)p_410473_).id);
            }

            GlStateManager._pixelStore(3314, p_423502_.getWidth());
            GlStateManager._pixelStore(3316, p_423531_);
            GlStateManager._pixelStore(3315, p_423633_);
            GlStateManager._pixelStore(3317, p_423502_.format().components());
            GlStateManager._texSubImage2D(
               i, p_410132_, p_410810_, p_409825_, p_410770_, p_423635_, GlConst.toGl(p_423502_.format()), 5121, p_423502_.getPointer()
            );
         }
      } else {
         throw new IllegalArgumentException("Invalid mipLevel " + p_410132_ + ", must be >= 0 and < " + p_410473_.getMipLevels());
      }
   }

   @Override
   public void writeToTexture(
      GpuTexture p_409608_,
      ByteBuffer p_436678_,
      NativeImage.Format p_423578_,
      int p_410252_,
      int p_410814_,
      int p_410606_,
      int p_410618_,
      int p_410484_,
      int p_410120_
   ) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else if (p_410252_ >= 0 && p_410252_ < p_409608_.getMipLevels()) {
         if (p_410484_ * p_410120_ * p_423578_.components() > p_436678_.remaining()) {
            throw new IllegalArgumentException(
               "Copy would overrun the source buffer (remaining length of "
                  + p_436678_.remaining()
                  + ", but copy is "
                  + p_410484_
                  + "x"
                  + p_410120_
                  + " of format "
                  + p_423578_
                  + ")"
            );
         } else if (p_410606_ + p_410484_ > p_409608_.getWidth(p_410252_) || p_410618_ + p_410120_ > p_409608_.getHeight(p_410252_)) {
            throw new IllegalArgumentException(
               "Dest texture ("
                  + p_409608_.getWidth(p_410252_)
                  + "x"
                  + p_409608_.getHeight(p_410252_)
                  + ") is not large enough to write a rectangle of "
                  + p_410484_
                  + "x"
                  + p_410120_
                  + " at "
                  + p_410606_
                  + "x"
                  + p_410618_
            );
         } else if (p_409608_.isClosed()) {
            throw new IllegalStateException("Destination texture is closed");
         } else if ((p_409608_.usage() & 1) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
         } else if (p_410814_ >= p_409608_.getDepthOrLayers()) {
            throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + p_409608_.getDepthOrLayers());
         } else {
            int i;
            if ((p_409608_.usage() & 16) != 0) {
               i = GlConst.CUBEMAP_TARGETS[p_410814_ % 6];
               GL11.glBindTexture(34067, ((GlTexture)p_409608_).id);
            } else {
               i = 3553;
               GlStateManager._bindTexture(((GlTexture)p_409608_).id);
            }

            GlStateManager._pixelStore(3314, p_410484_);
            GlStateManager._pixelStore(3316, 0);
            GlStateManager._pixelStore(3315, 0);
            GlStateManager._pixelStore(3317, p_423578_.components());
            GlStateManager._texSubImage2D(i, p_410252_, p_410606_, p_410618_, p_410484_, p_410120_, GlConst.toGl(p_423578_), 5121, p_436678_);
         }
      } else {
         throw new IllegalArgumentException("Invalid mipLevel, must be >= 0 and < " + p_409608_.getMipLevels());
      }
   }

   @Override
   public void copyTextureToBuffer(GpuTexture p_410088_, GpuBuffer p_409674_, long p_482318_, Runnable p_410567_, int p_410546_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else {
         this.copyTextureToBuffer(p_410088_, p_409674_, p_482318_, p_410567_, p_410546_, 0, 0, p_410088_.getWidth(p_410546_), p_410088_.getHeight(p_410546_));
      }
   }

   @Override
   public void copyTextureToBuffer(
      GpuTexture p_410781_, GpuBuffer p_410413_, long p_482330_, Runnable p_410081_, int p_410080_, int p_410819_, int p_409841_, int p_409880_, int p_409853_
   ) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else if (p_410080_ >= 0 && p_410080_ < p_410781_.getMipLevels()) {
         if (p_410781_.getWidth(p_410080_) * p_410781_.getHeight(p_410080_) * p_410781_.getFormat().pixelSize() + p_482330_ > p_410413_.size()) {
            throw new IllegalArgumentException(
               "Buffer of size "
                  + p_410413_.size()
                  + " is not large enough to hold "
                  + p_409880_
                  + "x"
                  + p_409853_
                  + " pixels ("
                  + p_410781_.getFormat().pixelSize()
                  + " bytes each) starting from offset "
                  + p_482330_
            );
         } else if ((p_410781_.usage() & 2) == 0) {
            throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
         } else if ((p_410413_.usage() & 8) == 0) {
            throw new IllegalArgumentException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
         } else if (p_410819_ + p_409880_ > p_410781_.getWidth(p_410080_) || p_409841_ + p_409853_ > p_410781_.getHeight(p_410080_)) {
            throw new IllegalArgumentException(
               "Copy source texture ("
                  + p_410781_.getWidth(p_410080_)
                  + "x"
                  + p_410781_.getHeight(p_410080_)
                  + ") is not large enough to read a rectangle of "
                  + p_409880_
                  + "x"
                  + p_409853_
                  + " from "
                  + p_410819_
                  + ","
                  + p_409841_
            );
         } else if (p_410781_.isClosed()) {
            throw new IllegalStateException("Source texture is closed");
         } else if (p_410413_.isClosed()) {
            throw new IllegalStateException("Destination buffer is closed");
         } else if (p_410781_.getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
         } else {
            GlStateManager.clearGlErrors();
            this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, ((GlTexture)p_410781_).glId(), 0, p_410080_, 36008);
            GlStateManager._glBindBuffer(35051, ((GlBuffer)p_410413_).handle);
            GlStateManager._pixelStore(3330, p_409880_);
            GlStateManager._readPixels(
               p_410819_, p_409841_, p_409880_, p_409853_, GlConst.toGlExternalId(p_410781_.getFormat()), GlConst.toGlType(p_410781_.getFormat()), p_482330_
            );
            RenderSystem.queueFencedTask(p_410081_);
            GlStateManager._glFramebufferTexture2D(36008, 36064, 3553, 0, p_410080_);
            GlStateManager._glBindFramebuffer(36008, 0);
            GlStateManager._glBindBuffer(35051, 0);
            int i = GlStateManager._getError();
            if (i != 0) {
               throw new IllegalStateException("Couldn't perform copyTobuffer for texture " + p_410781_.getLabel() + ": GL error " + i);
            }
         }
      } else {
         throw new IllegalArgumentException("Invalid mipLevel " + p_410080_ + ", must be >= 0 and < " + p_410781_.getMipLevels());
      }
   }

   @Override
   public void copyTextureToTexture(
      GpuTexture p_410700_, GpuTexture p_410735_, int p_410458_, int p_409803_, int p_410236_, int p_410552_, int p_410677_, int p_409870_, int p_409949_
   ) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else if (p_410458_ >= 0 && p_410458_ < p_410700_.getMipLevels() && p_410458_ < p_410735_.getMipLevels()) {
         if (p_409803_ + p_409870_ > p_410735_.getWidth(p_410458_) || p_410236_ + p_409949_ > p_410735_.getHeight(p_410458_)) {
            throw new IllegalArgumentException(
               "Dest texture ("
                  + p_410735_.getWidth(p_410458_)
                  + "x"
                  + p_410735_.getHeight(p_410458_)
                  + ") is not large enough to write a rectangle of "
                  + p_409870_
                  + "x"
                  + p_409949_
                  + " at "
                  + p_409803_
                  + "x"
                  + p_410236_
            );
         } else if (p_410552_ + p_409870_ > p_410700_.getWidth(p_410458_) || p_410677_ + p_409949_ > p_410700_.getHeight(p_410458_)) {
            throw new IllegalArgumentException(
               "Source texture ("
                  + p_410700_.getWidth(p_410458_)
                  + "x"
                  + p_410700_.getHeight(p_410458_)
                  + ") is not large enough to read a rectangle of "
                  + p_409870_
                  + "x"
                  + p_409949_
                  + " at "
                  + p_410552_
                  + "x"
                  + p_410677_
            );
         } else if (p_410700_.isClosed()) {
            throw new IllegalStateException("Source texture is closed");
         } else if (p_410735_.isClosed()) {
            throw new IllegalStateException("Destination texture is closed");
         } else if ((p_410700_.usage() & 2) == 0) {
            throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
         } else if ((p_410735_.usage() & 1) == 0) {
            throw new IllegalArgumentException("Texture needs USAGE_COPY_DST to be a destination for a copy");
         } else if (p_410700_.getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
         } else if (p_410735_.getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
         } else {
            TextureFormat sourceFormat = p_410700_.getFormat();
            TextureFormat destFormat = p_410735_.getFormat();
            if ((sourceFormat.hasDepthAspect() || sourceFormat.hasStencilAspect()) && sourceFormat != destFormat) {
               throw new IllegalArgumentException(
                  "When copying depth or stencil data, the source and destination texture formats must be identical. Source: "
                     + sourceFormat
                     + ", Destination: "
                     + destFormat
               );
            } else if (sourceFormat.hasColorAspect() != destFormat.hasColorAspect()) {
               throw new IllegalArgumentException(
                  "Source and destination texture formats must have consistent color aspects. Source: " + sourceFormat + ", Destination: " + destFormat
               );
            } else {
               GlStateManager.clearGlErrors();
               GlStateManager._disableScissorTest();
               boolean flag = p_410700_.getFormat().hasDepthAspect();
               int i = ((GlTexture)p_410700_).glId();
               int j = ((GlTexture)p_410735_).glId();
               boolean hasStencil = p_410700_.getFormat().hasStencilAspect();
               this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, flag ? 0 : i, flag ? i : 0, 0, 0, hasStencil);
               this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, flag ? 0 : j, flag ? j : 0, 0, 0, hasStencil);
               int bufferMask = 0;
               if (p_410700_.getFormat().hasColorAspect()) {
                  bufferMask |= 16384;
               }

               if (p_410700_.getFormat().hasDepthAspect()) {
                  bufferMask |= 256;
               }

               if (p_410700_.getFormat().hasStencilAspect()) {
                  bufferMask |= 1024;
               }

               this.device
                  .directStateAccess()
                  .blitFrameBuffers(
                     this.readFbo, this.drawFbo, p_410552_, p_410677_, p_409870_, p_409949_, p_409803_, p_410236_, p_409870_, p_409949_, bufferMask, 9728
                  );
               int k = GlStateManager._getError();
               if (k != 0) {
                  throw new IllegalStateException(
                     "Couldn't perform copyToTexture for texture " + p_410700_.getLabel() + " to " + p_410735_.getLabel() + ": GL error " + k
                  );
               }
            }
         }
      } else {
         throw new IllegalArgumentException(
            "Invalid mipLevel " + p_410458_ + ", must be >= 0 and < " + p_410700_.getMipLevels() + " and < " + p_410735_.getMipLevels()
         );
      }
   }

   @Override
   public void presentTexture(GpuTextureView p_423537_) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else if (!p_423537_.texture().getFormat().hasColorAspect()) {
         throw new IllegalStateException("Cannot present a non-color texture!");
      } else if ((p_423537_.texture().usage() & 8) == 0) {
         throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT to presented to the screen");
      } else if (p_423537_.texture().getDepthOrLayers() > 1) {
         throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for presentation");
      } else {
         GlStateManager._disableScissorTest();
         GlStateManager._viewport(0, 0, p_423537_.getWidth(0), p_423537_.getHeight(0));
         GlStateManager._depthMask(true);
         GlStateManager._colorMask(true, true, true, true);
         this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)p_423537_.texture()).glId(), 0, 0, 0);
         this.device
            .directStateAccess()
            .blitFrameBuffers(
               this.drawFbo, 0, 0, 0, p_423537_.getWidth(0), p_423537_.getHeight(0), 0, 0, p_423537_.getWidth(0), p_423537_.getHeight(0), 16384, 9728
            );
      }
   }

   @Override
   public GpuFence createFence() {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else {
         return new GlFence();
      }
   }

   protected <T> void executeDrawMultiple(
      GlRenderPass p_410685_,
      Collection<RenderPass.Draw<T>> p_410004_,
      @Nullable GpuBuffer p_412703_,
      @Nullable VertexFormat.IndexType p_412751_,
      Collection<String> p_418467_,
      T p_428381_
   ) {
      if (this.trySetup(p_410685_, p_418467_)) {
         if (p_412751_ == null) {
            p_412751_ = VertexFormat.IndexType.SHORT;
         }

         for (RenderPass.Draw<T> draw : p_410004_) {
            VertexFormat.IndexType vertexformat$indextype = draw.indexType() == null ? p_412751_ : draw.indexType();
            p_410685_.setIndexBuffer(draw.indexBuffer() == null ? p_412703_ : draw.indexBuffer(), vertexformat$indextype);
            p_410685_.setVertexBuffer(draw.slot(), draw.vertexBuffer());
            if (GlRenderPass.VALIDATION) {
               if (p_410685_.indexBuffer == null) {
                  throw new IllegalStateException("Missing index buffer");
               }

               if (p_410685_.indexBuffer.isClosed()) {
                  throw new IllegalStateException("Index buffer has been closed!");
               }

               if (p_410685_.vertexBuffers[0] == null) {
                  throw new IllegalStateException("Missing vertex buffer at slot 0");
               }

               if (p_410685_.vertexBuffers[0].isClosed()) {
                  throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
               }
            }

            BiConsumer<T, RenderPass.UniformUploader> biconsumer = draw.uniformUploaderConsumer();
            if (biconsumer != null) {
               biconsumer.accept(p_428381_, (p_482263_, p_482264_) -> {
                  if (p_410685_.pipeline.program().getUniform(p_482263_) instanceof Uniform.Ubo(int patt2$temp)) {
                     GL32.glBindBufferRange(35345, patt2$temp, ((GlBuffer)p_482264_.buffer()).handle, p_482264_.offset(), p_482264_.length());
                  }
               });
            }

            this.drawFromBuffers(p_410685_, 0, draw.firstIndex(), draw.indexCount(), vertexformat$indextype, p_410685_.pipeline, 1);
         }
      }
   }

   protected void executeDraw(GlRenderPass p_410011_, int p_410073_, int p_410287_, int p_418489_, @Nullable VertexFormat.IndexType p_410766_, int p_420033_) {
      if (this.trySetup(p_410011_, Collections.emptyList())) {
         if (GlRenderPass.VALIDATION) {
            if (p_410766_ != null) {
               if (p_410011_.indexBuffer == null) {
                  throw new IllegalStateException("Missing index buffer");
               }

               if (p_410011_.indexBuffer.isClosed()) {
                  throw new IllegalStateException("Index buffer has been closed!");
               }

               if ((p_410011_.indexBuffer.usage() & 64) == 0) {
                  throw new IllegalStateException("Index buffer must have GpuBuffer.USAGE_INDEX!");
               }
            }

            GlRenderPipeline glrenderpipeline = p_410011_.pipeline;
            if (p_410011_.vertexBuffers[0] == null && glrenderpipeline != null && !glrenderpipeline.info().getVertexFormat().getElements().isEmpty()) {
               throw new IllegalStateException("Vertex format contains elements but vertex buffer at slot 0 is null");
            }

            if (p_410011_.vertexBuffers[0] != null && p_410011_.vertexBuffers[0].isClosed()) {
               throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
            }

            if (p_410011_.vertexBuffers[0] != null && (p_410011_.vertexBuffers[0].usage() & 32) == 0) {
               throw new IllegalStateException("Vertex buffer must have GpuBuffer.USAGE_VERTEX!");
            }
         }

         this.drawFromBuffers(p_410011_, p_410073_, p_410287_, p_418489_, p_410766_, p_410011_.pipeline, p_420033_);
      }
   }

   private void drawFromBuffers(
      GlRenderPass p_410461_,
      int p_410115_,
      int p_410829_,
      int p_418095_,
      @Nullable VertexFormat.IndexType p_410615_,
      GlRenderPipeline p_409678_,
      int p_419748_
   ) {
      this.device.vertexArrayCache().bindVertexArray(p_409678_.info().getVertexFormat(), (GlBuffer)p_410461_.vertexBuffers[0]);
      if (p_410615_ != null) {
         GlStateManager._glBindBuffer(34963, ((GlBuffer)p_410461_.indexBuffer).handle);
         if (p_419748_ > 1) {
            if (p_410115_ > 0) {
               GL32.glDrawElementsInstancedBaseVertex(
                  GlConst.toGl(p_409678_.info().getVertexFormatMode()),
                  p_418095_,
                  GlConst.toGl(p_410615_),
                  (long)p_410829_ * p_410615_.bytes,
                  p_419748_,
                  p_410115_
               );
            } else {
               GL31.glDrawElementsInstanced(
                  GlConst.toGl(p_409678_.info().getVertexFormatMode()), p_418095_, GlConst.toGl(p_410615_), (long)p_410829_ * p_410615_.bytes, p_419748_
               );
            }
         } else if (p_410115_ > 0) {
            GL32.glDrawElementsBaseVertex(
               GlConst.toGl(p_409678_.info().getVertexFormatMode()), p_418095_, GlConst.toGl(p_410615_), (long)p_410829_ * p_410615_.bytes, p_410115_
            );
         } else {
            GlStateManager._drawElements(
               GlConst.toGl(p_409678_.info().getVertexFormatMode()), p_418095_, GlConst.toGl(p_410615_), (long)p_410829_ * p_410615_.bytes
            );
         }
      } else if (p_419748_ > 1) {
         GL31.glDrawArraysInstanced(GlConst.toGl(p_409678_.info().getVertexFormatMode()), p_410115_, p_418095_, p_419748_);
      } else {
         GlStateManager._drawArrays(GlConst.toGl(p_409678_.info().getVertexFormatMode()), p_410115_, p_418095_);
      }
   }

   private boolean trySetup(GlRenderPass p_410853_, Collection<String> p_418286_) {
      if (GlRenderPass.VALIDATION) {
         if (p_410853_.pipeline == null) {
            throw new IllegalStateException("Can't draw without a render pipeline");
         }

         if (p_410853_.pipeline.program() == GlProgram.INVALID_PROGRAM) {
            throw new IllegalStateException("Pipeline contains invalid shader program");
         }

         for (RenderPipeline.UniformDescription renderpipeline$uniformdescription : p_410853_.pipeline.info().getUniforms()) {
            GpuBufferSlice gpubufferslice = p_410853_.uniforms.get(renderpipeline$uniformdescription.name());
            if (!p_418286_.contains(renderpipeline$uniformdescription.name())) {
               if (gpubufferslice == null) {
                  throw new IllegalStateException(
                     "Missing uniform " + renderpipeline$uniformdescription.name() + " (should be " + renderpipeline$uniformdescription.type() + ")"
                  );
               }

               if (renderpipeline$uniformdescription.type() == UniformType.UNIFORM_BUFFER) {
                  if (gpubufferslice.buffer().isClosed()) {
                     throw new IllegalStateException("Uniform buffer " + renderpipeline$uniformdescription.name() + " is already closed");
                  }

                  if ((gpubufferslice.buffer().usage() & 128) == 0) {
                     throw new IllegalStateException("Uniform buffer " + renderpipeline$uniformdescription.name() + " must have GpuBuffer.USAGE_UNIFORM");
                  }
               }

               if (renderpipeline$uniformdescription.type() == UniformType.TEXEL_BUFFER) {
                  if (gpubufferslice.offset() != 0L || gpubufferslice.length() != gpubufferslice.buffer().size()) {
                     throw new IllegalStateException("Uniform texel buffers do not support a slice of a buffer, must be entire buffer");
                  }

                  if (renderpipeline$uniformdescription.textureFormat() == null) {
                     throw new IllegalStateException("Invalid uniform texel buffer " + renderpipeline$uniformdescription.name() + " (missing a texture format)");
                  }
               }
            }
         }

         for (Entry<String, Uniform> entry : p_410853_.pipeline.program().getUniforms().entrySet()) {
            if (entry.getValue() instanceof Uniform.Sampler) {
               String s1 = entry.getKey();
               GlRenderPass.TextureViewAndSampler glrenderpass$textureviewandsampler = p_410853_.samplers.get(s1);
               if (glrenderpass$textureviewandsampler == null) {
                  throw new IllegalStateException("Missing sampler " + s1);
               }

               GlTextureView gltextureview = glrenderpass$textureviewandsampler.view();
               if (gltextureview.isClosed()) {
                  throw new IllegalStateException("Texture view " + s1 + " (" + gltextureview.texture().getLabel() + ") has been closed!");
               }

               if ((gltextureview.texture().usage() & 4) == 0) {
                  throw new IllegalStateException("Texture view " + s1 + " (" + gltextureview.texture().getLabel() + ") must have USAGE_TEXTURE_BINDING!");
               }

               if (glrenderpass$textureviewandsampler.sampler().isClosed()) {
                  throw new IllegalStateException("Sampler for " + s1 + " (" + gltextureview.texture().getLabel() + ") has been closed!");
               }
            }
         }

         if (p_410853_.pipeline.info().wantsDepthTexture() && !p_410853_.hasDepthTexture()) {
            LOGGER.warn("Render pipeline {} wants a depth texture but none was provided - this is probably a bug", p_410853_.pipeline.info().getLocation());
         }
      } else if (p_410853_.pipeline == null || p_410853_.pipeline.program() == GlProgram.INVALID_PROGRAM) {
         return false;
      }

      RenderPipeline renderpipeline = p_410853_.pipeline.info();
      GlProgram glprogram = p_410853_.pipeline.program();
      this.applyPipelineState(renderpipeline);
      boolean flag1 = this.lastProgram != glprogram;
      if (flag1) {
         GlStateManager._glUseProgram(glprogram.getProgramId());
         this.lastProgram = glprogram;
      }

      for (Entry<String, Uniform> entry1 : glprogram.getUniforms().entrySet()) {
         String s = entry1.getKey();
         boolean flag = p_410853_.dirtyUniforms.contains(s);
         switch ((Uniform)entry1.getValue()) {
            case Uniform.Ubo(int var67):
               int k = var67;
               if (flag) {
                  GpuBufferSlice gpubufferslice1 = p_410853_.uniforms.get(s);
                  GL32.glBindBufferRange(35345, k, ((GlBuffer)gpubufferslice1.buffer()).handle, gpubufferslice1.offset(), gpubufferslice1.length());
               }
               break;
            case Uniform.Utb(int j1, int var49, TextureFormat var50, int var65):
               int var51 = var65;
               if (flag1 || flag) {
                  GlStateManager._glUniform1i(j1, var49);
               }

               GlStateManager._activeTexture(33984 + var49);
               GL11C.glBindTexture(35882, var51);
               if (flag) {
                  GpuBufferSlice gpubufferslice2 = p_410853_.uniforms.get(s);
                  GL31.glTexBuffer(35882, GlConst.toGlInternalId(var50), ((GlBuffer)gpubufferslice2.buffer()).handle);
               }
               break;
            case Uniform.Sampler(int k1, int var57):
               int var53 = var57;
               GlRenderPass.TextureViewAndSampler glrenderpass$textureviewandsampler1 = p_410853_.samplers.get(s);
               if (glrenderpass$textureviewandsampler1 == null) {
                  break;
               }

               GlTextureView gltextureview1 = glrenderpass$textureviewandsampler1.view();
               if (flag1 || flag) {
                  GlStateManager._glUniform1i(k1, var53);
               }

               GlStateManager._activeTexture(33984 + var53);
               GlTexture gltexture = gltextureview1.texture();
               int j;
               if ((gltexture.usage() & 16) != 0) {
                  j = 34067;
                  GL11.glBindTexture(34067, gltexture.id);
               } else {
                  j = 3553;
                  GlStateManager._bindTexture(gltexture.id);
               }

               GL33C.glBindSampler(var53, glrenderpass$textureviewandsampler1.sampler().getId());
               GlStateManager._texParameter(j, 33084, gltextureview1.baseMipLevel());
               GlStateManager._texParameter(j, 33085, gltextureview1.baseMipLevel() + gltextureview1.mipLevels() - 1);
               break;
            default:
               throw new MatchException(null, null);
         }
      }

      p_410853_.dirtyUniforms.clear();
      if (p_410853_.isScissorEnabled()) {
         GlStateManager._enableScissorTest();
         GlStateManager._scissorBox(p_410853_.getScissorX(), p_410853_.getScissorY(), p_410853_.getScissorWidth(), p_410853_.getScissorHeight());
      } else {
         GlStateManager._disableScissorTest();
      }

      Optional<StencilTest> stencilTestOpt = p_410853_.pipeline.info().getStencilTest();
      if (stencilTestOpt.isPresent()) {
         StencilTest stencilTest = stencilTestOpt.get();
         GlStateManager._enableStencilTest();
         StencilPerFaceTest front = stencilTest.front();
         StencilPerFaceTest back = stencilTest.back();
         if (front.equals(back)) {
            GlStateManager._stencilFunc(GlConst.toGl(front.compare()), stencilTest.referenceValue(), stencilTest.readMask());
            GlStateManager._stencilOp(GlConst.toGl(front.fail()), GlConst.toGl(front.depthFail()), GlConst.toGl(front.pass()));
         } else {
            GlStateManager._stencilFuncFront(GlConst.toGl(front.compare()), stencilTest.referenceValue(), stencilTest.readMask());
            GlStateManager._stencilFuncBack(GlConst.toGl(back.compare()), stencilTest.referenceValue(), stencilTest.readMask());
            GlStateManager._stencilOpFront(GlConst.toGl(front.fail()), GlConst.toGl(front.depthFail()), GlConst.toGl(front.pass()));
            GlStateManager._stencilOpBack(GlConst.toGl(back.fail()), GlConst.toGl(back.depthFail()), GlConst.toGl(back.pass()));
         }

         GlStateManager._stencilMask(stencilTest.writeMask());
      } else {
         GlStateManager._disableStencilTest();
      }

      return true;
   }

   private void applyPipelineState(RenderPipeline p_410655_) {
      if (this.lastPipeline != p_410655_) {
         this.lastPipeline = p_410655_;
         if (p_410655_.getDepthTestFunction() != DepthTestFunction.NO_DEPTH_TEST) {
            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(GlConst.toGl(p_410655_.getDepthTestFunction()));
         } else {
            GlStateManager._disableDepthTest();
         }

         if (p_410655_.isCull()) {
            GlStateManager._enableCull();
         } else {
            GlStateManager._disableCull();
         }

         if (p_410655_.getBlendFunction().isPresent()) {
            GlStateManager._enableBlend();
            BlendFunction blendfunction = p_410655_.getBlendFunction().get();
            GlStateManager._blendFuncSeparate(
               GlConst.toGl(blendfunction.sourceColor()),
               GlConst.toGl(blendfunction.destColor()),
               GlConst.toGl(blendfunction.sourceAlpha()),
               GlConst.toGl(blendfunction.destAlpha())
            );
         } else {
            GlStateManager._disableBlend();
         }

         GlStateManager._polygonMode(1032, GlConst.toGl(p_410655_.getPolygonMode()));
         GlStateManager._depthMask(p_410655_.isWriteDepth());
         GlStateManager._colorMask(p_410655_.isWriteColor(), p_410655_.isWriteColor(), p_410655_.isWriteColor(), p_410655_.isWriteAlpha());
         if (p_410655_.getDepthBiasConstant() == 0.0F && p_410655_.getDepthBiasScaleFactor() == 0.0F) {
            GlStateManager._disablePolygonOffset();
         } else {
            GlStateManager._polygonOffset(p_410655_.getDepthBiasScaleFactor(), p_410655_.getDepthBiasConstant());
            GlStateManager._enablePolygonOffset();
         }

         switch (p_410655_.getColorLogic()) {
            case NONE:
               GlStateManager._disableColorLogicOp();
               break;
            case OR_REVERSE:
               GlStateManager._enableColorLogicOp();
               GlStateManager._logicOp(5387);
         }
      }
   }

   public void finishRenderPass() {
      this.inRenderPass = false;
      GlStateManager._glBindFramebuffer(36160, 0);
      this.device.debugLabels().popDebugGroup();
   }

   protected GlDevice getDevice() {
      return this.device;
   }

   @Override
   public GpuQuery timerQueryBegin() {
      RenderSystem.assertOnRenderThread();
      if (this.activeTimerQuery != null) {
         throw new IllegalStateException("A GL_TIME_ELAPSED query is already active");
      } else {
         int i = GL32C.glGenQueries();
         GL32C.glBeginQuery(35007, i);
         this.activeTimerQuery = new GlTimerQuery(i);
         return this.activeTimerQuery;
      }
   }

   @Override
   public void timerQueryEnd(GpuQuery p_457672_) {
      RenderSystem.assertOnRenderThread();
      if (p_457672_ != this.activeTimerQuery) {
         throw new IllegalStateException("Mismatched or duplicate GpuQuery when ending timerQuery");
      } else {
         GL32C.glEndQuery(35007);
         this.activeTimerQuery = null;
      }
   }
}
