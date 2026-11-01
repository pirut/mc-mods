package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderPipelines;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class RenderTarget {
   private static int UNNAMED_RENDER_TARGETS = 0;
   public int width;
   public int height;
   protected final String label;
   public final boolean useDepth;
   @Nullable
   protected GpuTexture colorTexture;
   @Nullable
   protected GpuTextureView colorTextureView;
   @Nullable
   protected GpuTexture depthTexture;
   @Nullable
   protected GpuTextureView depthTextureView;

   public RenderTarget(@Nullable String name, boolean useDepth) {
      this.label = name == null ? "FBO " + UNNAMED_RENDER_TARGETS++ : name;
      this.useDepth = useDepth;
   }

   public void resize(int width, int height) {
      RenderSystem.assertOnRenderThread();
      this.destroyBuffers();
      this.createBuffers(width, height);
   }

   public void destroyBuffers() {
      RenderSystem.assertOnRenderThread();
      if (this.depthTexture != null) {
         this.depthTexture.close();
         this.depthTexture = null;
      }

      if (this.depthTextureView != null) {
         this.depthTextureView.close();
         this.depthTextureView = null;
      }

      if (this.colorTexture != null) {
         this.colorTexture.close();
         this.colorTexture = null;
      }

      if (this.colorTextureView != null) {
         this.colorTextureView.close();
         this.colorTextureView = null;
      }
   }

   public void copyDepthFrom(RenderTarget otherTarget) {
      RenderSystem.assertOnRenderThread();
      if (this.depthTexture == null) {
         throw new IllegalStateException("Trying to copy depth texture to a RenderTarget without a depth texture");
      } else if (otherTarget.depthTexture == null) {
         throw new IllegalStateException("Trying to copy depth texture from a RenderTarget without a depth texture");
      } else {
         RenderSystem.getDevice()
            .createCommandEncoder()
            .copyTextureToTexture(otherTarget.depthTexture, this.depthTexture, 0, 0, 0, 0, 0, this.width, this.height);
      }
   }

   public void createBuffers(int width, int height) {
      RenderSystem.assertOnRenderThread();
      GpuDevice gpuDevice = RenderSystem.getDevice();
      int i = gpuDevice.getMaxTextureSize();
      if (width > 0 && width <= i && height > 0 && height <= i) {
         this.width = width;
         this.height = height;
         if (this.useDepth) {
            this.depthTexture = gpuDevice.createTexture(() -> this.label + " / Depth", 15, TextureFormat.DEPTH32, width, height, 1, 1);
            this.depthTextureView = gpuDevice.createTextureView(this.depthTexture);
         }

         this.colorTexture = gpuDevice.createTexture(() -> this.label + " / Color", 15, TextureFormat.RGBA8, width, height, 1, 1);
         this.colorTextureView = gpuDevice.createTextureView(this.colorTexture);
      } else {
         throw new IllegalArgumentException("Window " + width + "x" + height + " size out of bounds (max. size: " + i + ")");
      }
   }

   public void blitToScreen() {
      if (this.colorTexture == null) {
         throw new IllegalStateException("Can't blit to screen, color texture doesn't exist yet");
      } else {
         RenderSystem.getDevice().createCommandEncoder().presentTexture(this.colorTextureView);
      }
   }

   public void blitAndBlendToTexture(GpuTextureView textureView) {
      RenderSystem.assertOnRenderThread();

      try (RenderPass renderPass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "Blit render target", textureView, OptionalInt.empty())) {
         renderPass.setPipeline(RenderPipelines.ENTITY_OUTLINE_BLIT);
         RenderSystem.bindDefaultUniforms(renderPass);
         renderPass.bindTexture("InSampler", this.colorTextureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
         renderPass.draw(0, 3);
      }
   }

   @Nullable
   public GpuTexture getColorTexture() {
      return this.colorTexture;
   }

   @Nullable
   public GpuTextureView getColorTextureView() {
      return this.colorTextureView;
   }

   @Nullable
   public GpuTexture getDepthTexture() {
      return this.depthTexture;
   }

   @Nullable
   public GpuTextureView getDepthTextureView() {
      return this.depthTextureView;
   }
}
