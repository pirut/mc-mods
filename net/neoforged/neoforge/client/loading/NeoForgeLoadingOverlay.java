package net.neoforged.neoforge.client.loading;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuDevice;

public class NeoForgeLoadingOverlay extends LoadingOverlay {
   public static final Identifier LOADING_OVERLAY_TEXTURE_ID = Identifier.parse("neoforge:loading_overlay");
   private final Minecraft minecraft;
   private final ReloadInstance reload;
   private final DisplayWindow displayWindow;
   private final ProgressMeter progressMeter;
   private float currentProgress;

   public NeoForgeLoadingOverlay(Minecraft mc, ReloadInstance reloader, Consumer<Optional<Throwable>> errorConsumer, DisplayWindow displayWindow) {
      super(mc, reloader, errorConsumer, false);
      this.minecraft = mc;
      this.reload = reloader;
      this.displayWindow = displayWindow;
      this.progressMeter = StartupNotificationManager.prependProgressBar("Minecraft Progress", 1000);
      GpuDevice gpuDevice = RenderSystem.getDevice();
      if (gpuDevice instanceof ValidationGpuDevice validationGpuDevice) {
         gpuDevice = validationGpuDevice.getRealDevice();
      }

      GpuTexture framebuffer = ((GlDevice)gpuDevice).createExternalTexture("loading overlay framebuffer", 4, displayWindow.getFramebufferTextureId());
      Minecraft.getInstance().getTextureManager().register(LOADING_OVERLAY_TEXTURE_ID, new NeoForgeLoadingOverlay.ExternalTexture(framebuffer));
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      long millis = Util.getMillis();
      float fadeouttimer = this.fadeOutStart > -1L ? (float)(millis - this.fadeOutStart) / 1000.0F : -1.0F;
      this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + this.reload.getActualProgress() * 0.05F, 0.0F, 1.0F);
      this.progressMeter.setAbsolute(Mth.ceil(this.currentProgress * 1000.0F));
      this.displayWindow.renderToFramebuffer();
      int fbWidth = this.minecraft.getWindow().getWidth();
      int fbHeight = this.minecraft.getWindow().getHeight();
      float fade = 1.0F - Mth.clamp(fadeouttimer - 1.0F, 0.0F, 1.0F);
      if (fadeouttimer >= 1.0F && this.minecraft.screen != null) {
         this.minecraft.screen.render(graphics, 0, 0, partialTick);
      }

      int width = this.minecraft.getWindow().getGuiScaledWidth();
      int height = this.minecraft.getWindow().getGuiScaledHeight();
      int color = ARGB.colorFromFloat(fade, 1.0F, 1.0F, 1.0F);
      graphics.blit(RenderPipelines.GUI_TEXTURED, LOADING_OVERLAY_TEXTURE_ID, 0, 0, 0.0F, 0.0F, fbWidth, fbHeight, fbWidth, fbHeight, width, height, color);
      if (fadeouttimer >= 2.0F) {
         this.progressMeter.complete();
         Minecraft.getInstance().schedule(() -> {
            Minecraft.getInstance().getTextureManager().release(LOADING_OVERLAY_TEXTURE_ID);
            this.displayWindow.close();
         });
         this.minecraft.setOverlay(null);
      }
   }

   static class ExternalTexture extends AbstractTexture {
      public ExternalTexture(GpuTexture texture) {
         this.texture = texture;
         this.sampler = RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.LINEAR, false);
         GpuDevice gpuDevice = RenderSystem.getDevice();
         if (gpuDevice instanceof ValidationGpuDevice validationGpuDevice) {
            gpuDevice = validationGpuDevice.getRealDevice();
         }

         this.textureView = gpuDevice.createTextureView(texture);
      }
   }
}
