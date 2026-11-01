package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class PictureInPictureRenderer<T extends PictureInPictureRenderState> implements AutoCloseable {
   protected final MultiBufferSource.BufferSource bufferSource;
   @Nullable
   private GpuTexture texture;
   @Nullable
   private GpuTextureView textureView;
   @Nullable
   private GpuTexture depthTexture;
   @Nullable
   private GpuTextureView depthTextureView;
   private final CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer = new CachedOrthoProjectionMatrixBuffer(
      "PIP - " + this.getClass().getSimpleName(), -1000.0F, 1000.0F, true
   );

   protected PictureInPictureRenderer(MultiBufferSource.BufferSource p_416185_) {
      this.bufferSource = p_416185_;
   }

   public void prepare(T p_416683_, GuiRenderState p_415872_, int p_416691_) {
      int i = (p_416683_.x1() - p_416683_.x0()) * p_416691_;
      int j = (p_416683_.y1() - p_416683_.y0()) * p_416691_;
      boolean flag = this.texture == null || this.texture.getWidth(0) != i || this.texture.getHeight(0) != j;
      if (!flag && this.textureIsReadyToBlit(p_416683_)) {
         this.blitTexture(p_416683_, p_415872_);
      } else {
         this.prepareTexturesAndProjection(flag, i, j);
         RenderSystem.outputColorTextureOverride = this.textureView;
         RenderSystem.outputDepthTextureOverride = this.depthTextureView;
         PoseStack posestack = new PoseStack();
         posestack.translate(i / 2.0F, this.getTranslateY(j, p_416691_), 0.0F);
         float f = p_416691_ * p_416683_.scale();
         posestack.scale(f, f, -f);
         this.renderToTexture(p_416683_, posestack);
         this.bufferSource.endBatch();
         RenderSystem.outputColorTextureOverride = null;
         RenderSystem.outputDepthTextureOverride = null;
         this.blitTexture(p_416683_, p_415872_);
      }
   }

   protected void blitTexture(T p_428425_, GuiRenderState p_428420_) {
      p_428420_.submitBlitToCurrentLayer(
         new BlitRenderState(
            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
            TextureSetup.singleTexture(this.textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
            p_428425_.pose(),
            p_428425_.x0(),
            p_428425_.y0(),
            p_428425_.x1(),
            p_428425_.y1(),
            0.0F,
            1.0F,
            1.0F,
            0.0F,
            -1,
            p_428425_.scissorArea(),
            null
         )
      );
   }

   private void prepareTexturesAndProjection(boolean p_428358_, int p_415840_, int p_415644_) {
      if (this.texture != null && p_428358_) {
         this.texture.close();
         this.texture = null;
         this.textureView.close();
         this.textureView = null;
         this.depthTexture.close();
         this.depthTexture = null;
         this.depthTextureView.close();
         this.depthTextureView = null;
      }

      GpuDevice gpudevice = RenderSystem.getDevice();
      if (this.texture == null) {
         this.texture = gpudevice.createTexture(() -> "UI " + this.getTextureLabel() + " texture", 12, TextureFormat.RGBA8, p_415840_, p_415644_, 1, 1);
         this.textureView = gpudevice.createTextureView(this.texture);
         TextureFormat depthFormat = Minecraft.getInstance().getMainRenderTarget().getDepthTexture().getFormat();
         this.depthTexture = gpudevice.createTexture(() -> "UI " + this.getTextureLabel() + " depth texture", 8, depthFormat, p_415840_, p_415644_, 1, 1);
         this.depthTextureView = gpudevice.createTextureView(this.depthTexture);
      }

      gpudevice.createCommandEncoder().clearColorAndDepthTextures(this.texture, 0, this.depthTexture, 1.0);
      RenderSystem.setProjectionMatrix(this.projectionMatrixBuffer.getBuffer(p_415840_, p_415644_), ProjectionType.ORTHOGRAPHIC);
   }

   protected boolean textureIsReadyToBlit(T p_428278_) {
      return false;
   }

   protected float getTranslateY(int p_416617_, int p_416124_) {
      return p_416617_;
   }

   @Override
   public void close() {
      if (this.texture != null) {
         this.texture.close();
      }

      if (this.textureView != null) {
         this.textureView.close();
      }

      if (this.depthTexture != null) {
         this.depthTexture.close();
      }

      if (this.depthTextureView != null) {
         this.depthTextureView.close();
      }

      this.projectionMatrixBuffer.close();
   }

   public abstract Class<T> getRenderStateClass();

   protected abstract void renderToTexture(T var1, PoseStack var2);

   protected abstract String getTextureLabel();

   public boolean canBeReusedFor(T state, int textureWidth, int textureHeight) {
      return this.texture == null || this.texture.getWidth(0) == textureWidth && this.texture.getHeight(0) == textureHeight;
   }
}
