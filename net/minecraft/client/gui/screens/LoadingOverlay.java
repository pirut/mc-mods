package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public class LoadingOverlay extends Overlay {
   public static final Identifier MOJANG_STUDIOS_LOGO_LOCATION = Identifier.withDefaultNamespace("textures/gui/title/mojangstudios.png");
   private static final int LOGO_BACKGROUND_COLOR = ARGB.color(255, 239, 50, 61);
   private static final int LOGO_BACKGROUND_COLOR_DARK = ARGB.color(255, 0, 0, 0);
   private static final IntSupplier BRAND_BACKGROUND = () -> Minecraft.getInstance().options.darkMojangStudiosBackground().get()
      ? LOGO_BACKGROUND_COLOR_DARK
      : LOGO_BACKGROUND_COLOR;
   private static final int LOGO_SCALE = 240;
   private static final float LOGO_QUARTER_FLOAT = 60.0F;
   private static final int LOGO_QUARTER = 60;
   private static final int LOGO_HALF = 120;
   private static final float LOGO_OVERLAP = 0.0625F;
   private static final float SMOOTHING = 0.95F;
   public static final long FADE_OUT_TIME = 1000L;
   public static final long FADE_IN_TIME = 500L;
   private final Minecraft minecraft;
   private final ReloadInstance reload;
   private final Consumer<Optional<Throwable>> onFinish;
   private final boolean fadeIn;
   private float currentProgress;
   private long fadeOutStart = -1L;
   private long fadeInStart = -1L;

   public LoadingOverlay(Minecraft minecraft, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish, boolean fadeIn) {
      this.minecraft = minecraft;
      this.reload = reload;
      this.onFinish = onFinish;
      this.fadeIn = fadeIn;
   }

   public static void registerTextures(TextureManager textureManager) {
      textureManager.registerAndLoad(MOJANG_STUDIOS_LOGO_LOCATION, new LoadingOverlay.LogoTexture());
   }

   private static int replaceAlpha(int color, int alpha) {
      return color & 16777215 | alpha << 24;
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int i = guiGraphics.guiWidth();
      int j = guiGraphics.guiHeight();
      long l = Util.getMillis();
      if (this.fadeIn && this.fadeInStart == -1L) {
         this.fadeInStart = l;
      }

      float f = this.fadeOutStart > -1L ? (float)(l - this.fadeOutStart) / 1000.0F : -1.0F;
      float g = this.fadeInStart > -1L ? (float)(l - this.fadeInStart) / 500.0F : -1.0F;
      float h;
      if (f >= 1.0F) {
         if (this.minecraft.screen != null) {
            this.minecraft.screen.renderWithTooltipAndSubtitles(guiGraphics, 0, 0, partialTick);
         } else {
            this.minecraft.gui.renderDeferredSubtitles();
         }

         int k = Mth.ceil((1.0F - Mth.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
         guiGraphics.nextStratum();
         guiGraphics.fill(0, 0, i, j, replaceAlpha(BRAND_BACKGROUND.getAsInt(), k));
         h = 1.0F - Mth.clamp(f - 1.0F, 0.0F, 1.0F);
      } else if (this.fadeIn) {
         if (this.minecraft.screen != null && g < 1.0F) {
            this.minecraft.screen.renderWithTooltipAndSubtitles(guiGraphics, mouseX, mouseY, partialTick);
         } else {
            this.minecraft.gui.renderDeferredSubtitles();
         }

         int k = Mth.ceil(Mth.clamp((double)g, 0.15, 1.0) * 255.0);
         guiGraphics.nextStratum();
         guiGraphics.fill(0, 0, i, j, replaceAlpha(BRAND_BACKGROUND.getAsInt(), k));
         h = Mth.clamp(g, 0.0F, 1.0F);
      } else {
         int k = BRAND_BACKGROUND.getAsInt();
         RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.minecraft.getMainRenderTarget().getColorTexture(), k);
         h = 1.0F;
      }

      int k = (int)(guiGraphics.guiWidth() * 0.5);
      int m = (int)(guiGraphics.guiHeight() * 0.5);
      double d = Math.min(guiGraphics.guiWidth() * 0.75, (double)guiGraphics.guiHeight()) * 0.25;
      int n = (int)(d * 0.5);
      double e = d * 4.0;
      int o = (int)(e * 0.5);
      int p = ARGB.white(h);
      guiGraphics.blit(RenderPipelines.MOJANG_LOGO, MOJANG_STUDIOS_LOGO_LOCATION, k - o, m - n, -0.0625F, 0.0F, o, (int)d, 120, 60, 120, 120, p);
      guiGraphics.blit(RenderPipelines.MOJANG_LOGO, MOJANG_STUDIOS_LOGO_LOCATION, k, m - n, 0.0625F, 60.0F, o, (int)d, 120, 60, 120, 120, p);
      int q = (int)(guiGraphics.guiHeight() * 0.8325);
      float r = this.reload.getActualProgress();
      this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + r * 0.050000012F, 0.0F, 1.0F);
      if (f < 1.0F) {
         this.drawProgressBar(guiGraphics, i / 2 - o, q - 5, i / 2 + o, q + 5, 1.0F - Mth.clamp(f, 0.0F, 1.0F));
      }

      if (f >= 2.0F) {
         this.minecraft.setOverlay(null);
      }
   }

   @Override
   public void tick() {
      if (this.fadeOutStart == -1L && this.reload.isDone() && this.isReadyToFadeOut()) {
         try {
            this.reload.checkExceptions();
            this.onFinish.accept(Optional.empty());
         } catch (Throwable var2) {
            this.onFinish.accept(Optional.of(var2));
         }

         this.fadeOutStart = Util.getMillis();
         if (this.minecraft.screen != null) {
            Window window = this.minecraft.getWindow();
            this.minecraft.screen.init(window.getGuiScaledWidth(), window.getGuiScaledHeight());
         }
      }
   }

   private boolean isReadyToFadeOut() {
      return !this.fadeIn || this.fadeInStart > -1L && Util.getMillis() - this.fadeInStart >= 1000L;
   }

   private void drawProgressBar(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY, float partialTick) {
      int i = Mth.ceil((maxX - minX - 2) * this.currentProgress);
      int j = Math.round(partialTick * 255.0F);
      int k = ARGB.color(j, 255, 255, 255);
      guiGraphics.fill(minX + 2, minY + 2, minX + i, maxY - 2, k);
      guiGraphics.fill(minX + 1, minY, maxX - 1, minY + 1, k);
      guiGraphics.fill(minX + 1, maxY, maxX - 1, maxY - 1, k);
      guiGraphics.fill(minX, minY, minX + 1, maxY, k);
      guiGraphics.fill(maxX, minY, maxX - 1, maxY, k);
   }

   @Override
   public boolean isPauseScreen() {
      return true;
   }

   @Environment(EnvType.CLIENT)
   static class LogoTexture extends ReloadableTexture {
      public LogoTexture() {
         super(LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION);
      }

      @Override
      public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
         ResourceProvider resourceProvider = Minecraft.getInstance().getVanillaPackResources().asProvider();

         TextureContents var4;
         try (InputStream inputStream = resourceProvider.open(LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION)) {
            var4 = new TextureContents(NativeImage.read(inputStream), new TextureMetadataSection(true, true, MipmapStrategy.MEAN, 0.0F));
         }

         return var4;
      }
   }
}
