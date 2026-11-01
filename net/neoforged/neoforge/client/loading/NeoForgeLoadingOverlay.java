package net.neoforged.neoforge.client.loading;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import net.neoforged.fml.earlydisplay.ColourScheme.Colour;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;

public class NeoForgeLoadingOverlay extends LoadingOverlay {
   private final Minecraft minecraft;
   private final ReloadInstance reload;
   private final Consumer<Optional<Throwable>> onFinish;
   private final DisplayWindow displayWindow;
   private final ProgressMeter progressMeter;
   private float currentProgress;
   private long fadeOutStart = -1L;

   public NeoForgeLoadingOverlay(Minecraft mc, ReloadInstance reloader, Consumer<Optional<Throwable>> errorConsumer, DisplayWindow displayWindow) {
      super(mc, reloader, errorConsumer, false);
      this.minecraft = mc;
      this.reload = reloader;
      this.onFinish = errorConsumer;
      this.displayWindow = displayWindow;
      displayWindow.addMojangTexture(mc.getTextureManager().getTexture(ResourceLocation.withDefaultNamespace("textures/gui/title/mojangstudios.png")).getId());
      this.progressMeter = StartupNotificationManager.prependProgressBar("Minecraft Progress", 1000);
   }

   public static Supplier<LoadingOverlay> newInstance(
      Supplier<Minecraft> mc, Supplier<ReloadInstance> ri, Consumer<Optional<Throwable>> handler, DisplayWindow window
   ) {
      return () -> new NeoForgeLoadingOverlay(mc.get(), ri.get(), handler, window);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      long millis = Util.getMillis();
      float fadeouttimer = this.fadeOutStart > -1L ? (float)(millis - this.fadeOutStart) / 1000.0F : -1.0F;
      this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + this.reload.getActualProgress() * 0.05F, 0.0F, 1.0F);
      this.progressMeter.setAbsolute(Mth.ceil(this.currentProgress * 1000.0F));
      float fade = 1.0F - Mth.clamp(fadeouttimer - 1.0F, 0.0F, 1.0F);
      Colour colour = this.displayWindow.context().colourScheme().background();
      if (fadeouttimer >= 1.0F) {
         if (this.minecraft.screen != null) {
            this.minecraft.screen.render(graphics, 0, 0, partialTick);
         }

         this.displayWindow.render(255);
      } else {
         GlStateManager._clearColor(colour.redf(), colour.greenf(), colour.bluef(), 1.0F);
         GlStateManager._clear(16384, Minecraft.ON_OSX);
         this.displayWindow.render(255);
      }

      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 771);
      int fbWidth = this.minecraft.getWindow().getWidth();
      int fbHeight = this.minecraft.getWindow().getHeight();
      GL30C.glViewport(0, 0, fbWidth, fbHeight);
      int twidth = this.displayWindow.context().width();
      int theight = this.displayWindow.context().height();
      float wscale = (float)fbWidth / twidth;
      float hscale = (float)fbHeight / theight;
      float scale = this.displayWindow.context().scale() * Math.min(wscale, hscale) / 2.0F;
      float wleft = Mth.clamp(fbWidth * 0.5F - scale * twidth, 0.0F, (float)fbWidth);
      float wtop = Mth.clamp(fbHeight * 0.5F - scale * theight, 0.0F, (float)fbHeight);
      float wright = Mth.clamp(fbWidth * 0.5F + scale * twidth, 0.0F, (float)fbWidth);
      float wbottom = Mth.clamp(fbHeight * 0.5F + scale * theight, 0.0F, (float)fbHeight);
      GlStateManager.glActiveTexture(33984);
      RenderSystem.disableCull();
      BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fade);
      RenderSystem.getModelViewMatrix().identity();
      RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, fbWidth, 0.0F, fbHeight, 0.1F, -0.1F), VertexSorting.ORTHOGRAPHIC_Z);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      addQuad(bufferbuilder, 0.0F, fbWidth, wtop, fbHeight, colour, fade);
      addQuad(bufferbuilder, 0.0F, fbWidth, 0.0F, wtop, colour, fade);
      addQuad(bufferbuilder, 0.0F, wleft, wtop, wbottom, colour, fade);
      addQuad(bufferbuilder, wright, fbWidth, wtop, wbottom, colour, fade);
      BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(770, 771);
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderTexture(0, this.displayWindow.getFramebufferTextureId());
      bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      bufferbuilder.addVertex(wleft, wbottom, 0.0F).setUv(0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, fade);
      bufferbuilder.addVertex(wright, wbottom, 0.0F).setUv(1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, fade);
      bufferbuilder.addVertex(wright, wtop, 0.0F).setUv(1.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, fade);
      bufferbuilder.addVertex(wleft, wtop, 0.0F).setUv(0.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, fade);
      GL30C.glTexParameterIi(3553, 10241, 9728);
      GL30C.glTexParameterIi(3553, 10240, 9728);
      BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (fadeouttimer >= 2.0F) {
         this.progressMeter.complete();
         this.minecraft.setOverlay(null);
         this.displayWindow.close();
      }

      if (this.fadeOutStart == -1L && this.reload.isDone()) {
         this.fadeOutStart = Util.getMillis();

         try {
            this.reload.checkExceptions();
            this.onFinish.accept(Optional.empty());
         } catch (Throwable var23) {
            this.onFinish.accept(Optional.of(var23));
         }

         if (this.minecraft.screen != null) {
            this.minecraft.screen.init(this.minecraft, this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight());
         }
      }
   }

   private static void addQuad(VertexConsumer bufferbuilder, float x0, float x1, float y0, float y1, Colour colour, float fade) {
      bufferbuilder.addVertex(x0, y0, 0.0F).setColor(colour.redf(), colour.greenf(), colour.bluef(), fade);
      bufferbuilder.addVertex(x0, y1, 0.0F).setColor(colour.redf(), colour.greenf(), colour.bluef(), fade);
      bufferbuilder.addVertex(x1, y1, 0.0F).setColor(colour.redf(), colour.greenf(), colour.bluef(), fade);
      bufferbuilder.addVertex(x1, y0, 0.0F).setColor(colour.redf(), colour.greenf(), colour.bluef(), fade);
   }
}
