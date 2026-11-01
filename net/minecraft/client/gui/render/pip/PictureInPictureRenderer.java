package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
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

   protected PictureInPictureRenderer(MultiBufferSource.BufferSource bufferSource) {
      this.bufferSource = bufferSource;
   }

   public void prepare(T renderState, GuiRenderState guiRenderState, int guiScale) {
      int i = (renderState.x1() - renderState.x0()) * guiScale;
      int j = (renderState.y1() - renderState.y0()) * guiScale;
      boolean bl = this.texture == null || this.texture.getWidth(0) != i || this.texture.getHeight(0) != j;
      if (!bl && this.textureIsReadyToBlit(renderState)) {
         this.blitTexture(renderState, guiRenderState);
      } else {
         this.prepareTexturesAndProjection(bl, i, j);
         RenderSystem.outputColorTextureOverride = this.textureView;
         RenderSystem.outputDepthTextureOverride = this.depthTextureView;
         PoseStack poseStack = new PoseStack();
         poseStack.translate(i / 2.0F, this.getTranslateY(j, guiScale), 0.0F);
         float f = guiScale * renderState.scale();
         poseStack.scale(f, f, -f);
         this.renderToTexture(renderState, poseStack);
         this.bufferSource.endBatch();
         RenderSystem.outputColorTextureOverride = null;
         RenderSystem.outputDepthTextureOverride = null;
         this.blitTexture(renderState, guiRenderState);
      }
   }

   protected void blitTexture(T renderState, GuiRenderState guiRenderState) {
      guiRenderState.submitBlitToCurrentLayer(
         new BlitRenderState(
            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
            TextureSetup.singleTexture(this.textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
            renderState.pose(),
            renderState.x0(),
            renderState.y0(),
            renderState.x1(),
            renderState.y1(),
            0.0F,
            1.0F,
            1.0F,
            0.0F,
            -1,
            renderState.scissorArea(),
            null
         )
      );
   }

   private void prepareTexturesAndProjection(boolean resetTexture, int width, int height) {
      if (this.texture != null && resetTexture) {
         this.texture.close();
         this.texture = null;
         this.textureView.close();
         this.textureView = null;
         this.depthTexture.close();
         this.depthTexture = null;
         this.depthTextureView.close();
         this.depthTextureView = null;
      }

      GpuDevice gpuDevice = RenderSystem.getDevice();
      if (this.texture == null) {
         this.texture = gpuDevice.createTexture(() -> "UI " + this.getTextureLabel() + " texture", 12, TextureFormat.RGBA8, width, height, 1, 1);
         this.textureView = gpuDevice.createTextureView(this.texture);
         this.depthTexture = gpuDevice.createTexture(() -> "UI " + this.getTextureLabel() + " depth texture", 8, TextureFormat.DEPTH32, width, height, 1, 1);
         this.depthTextureView = gpuDevice.createTextureView(this.depthTexture);
      }

      gpuDevice.createCommandEncoder().clearColorAndDepthTextures(this.texture, 0, this.depthTexture, 1.0);
      RenderSystem.setProjectionMatrix(this.projectionMatrixBuffer.getBuffer(width, height), ProjectionType.ORTHOGRAPHIC);
   }

   protected boolean textureIsReadyToBlit(T renderState) {
      return false;
   }

   protected float getTranslateY(int height, int guiScale) {
      return height;
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

   protected abstract void renderToTexture(T renderState, PoseStack poseStack);

   protected abstract String getTextureLabel();
}
