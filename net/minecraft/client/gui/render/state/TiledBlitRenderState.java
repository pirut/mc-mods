package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record TiledBlitRenderState(
   RenderPipeline pipeline,
   TextureSetup textureSetup,
   Matrix3x2f pose,
   int tileWidth,
   int tileHeight,
   int x0,
   int y0,
   int x1,
   int y1,
   float u0,
   float u1,
   float v0,
   float v1,
   int color,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public TiledBlitRenderState(
      RenderPipeline pipeline,
      TextureSetup textureSetup,
      Matrix3x2f pose,
      int tileWidth,
      int tileHeight,
      int x0,
      int y0,
      int x1,
      int y1,
      float u0,
      float u1,
      float v0,
      float v1,
      int color,
      @Nullable ScreenRectangle scissorArea
   ) {
      this(
         pipeline, textureSetup, pose, tileWidth, tileHeight, x0, y0, x1, y1, u0, u1, v0, v1, color, scissorArea, getBounds(x0, y0, x1, y1, pose, scissorArea)
      );
   }

   @Override
   public void buildVertices(VertexConsumer consumer) {
      int i = this.x1() - this.x0();
      int j = this.y1() - this.y0();

      for (int k = 0; k < i; k += this.tileWidth()) {
         int l = i - k;
         int m;
         float f;
         if (this.tileWidth() <= l) {
            m = this.tileWidth();
            f = this.u1();
         } else {
            m = l;
            f = Mth.lerp((float)l / this.tileWidth(), this.u0(), this.u1());
         }

         for (int n = 0; n < j; n += this.tileHeight()) {
            int o = j - n;
            int p;
            float g;
            if (this.tileHeight() <= o) {
               p = this.tileHeight();
               g = this.v1();
            } else {
               p = o;
               g = Mth.lerp((float)o / this.tileHeight(), this.v0(), this.v1());
            }

            int q = this.x0() + k;
            int r = this.x0() + k + m;
            int s = this.y0() + n;
            int t = this.y0() + n + p;
            consumer.addVertexWith2DPose(this.pose(), q, s).setUv(this.u0(), this.v0()).setColor(this.color());
            consumer.addVertexWith2DPose(this.pose(), q, t).setUv(this.u0(), g).setColor(this.color());
            consumer.addVertexWith2DPose(this.pose(), r, t).setUv(f, g).setColor(this.color());
            consumer.addVertexWith2DPose(this.pose(), r, s).setUv(f, this.v0()).setColor(this.color());
         }
      }
   }

   @Nullable
   private static ScreenRectangle getBounds(int x0, int y0, int x1, int y1, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
      ScreenRectangle screenRectangle = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
      return scissorArea != null ? scissorArea.intersection(screenRectangle) : screenRectangle;
   }
}
