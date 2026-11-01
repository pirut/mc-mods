package net.minecraft.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PanoramaRenderer {
   public static final Identifier PANORAMA_OVERLAY = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_overlay.png");
   private Minecraft minecraft;
   private final CubeMap cubeMap;
   private float spin;

   public PanoramaRenderer(CubeMap p_110002_) {
      this.cubeMap = p_110002_;
      this.minecraft = Minecraft.getInstance();
   }

   public void render(GuiGraphics p_334063_, int p_333839_, int p_333923_, boolean p_418077_) {
      if (p_418077_) {
         float f = this.minecraft.getDeltaTracker().getRealtimeDeltaTicks();
         float f1 = (float)(f * this.minecraft.options.panoramaSpeed().get());
         this.spin = wrap(this.spin + f1 * 0.1F, 360.0F);
      }

      this.cubeMap.render(this.minecraft, 10.0F, -this.spin);
      p_334063_.blit(RenderPipelines.GUI_TEXTURED, PANORAMA_OVERLAY, 0, 0, 0.0F, 0.0F, p_333839_, p_333923_, 16, 128, 16, 128);
   }

   private static float wrap(float p_249058_, float p_249548_) {
      return p_249058_ > p_249548_ ? p_249058_ - p_249548_ : p_249058_;
   }

   public void registerTextures(TextureManager p_423546_) {
      this.cubeMap.registerTextures(p_423546_);
   }
}
