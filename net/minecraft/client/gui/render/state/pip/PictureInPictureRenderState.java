package net.minecraft.client.gui.render.state.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.ScreenArea;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface PictureInPictureRenderState extends ScreenArea {
   Matrix3x2f IDENTITY_POSE = new Matrix3x2f();

   int x0();

   int x1();

   int y0();

   int y1();

   float scale();

   default Matrix3x2f pose() {
      return IDENTITY_POSE;
   }

   @Nullable
   ScreenRectangle scissorArea();

   @Nullable
   static ScreenRectangle getBounds(int p_421590_, int p_421871_, int p_422465_, int p_422594_, @Nullable ScreenRectangle p_421800_) {
      ScreenRectangle screenrectangle = new ScreenRectangle(p_421590_, p_421871_, p_422465_ - p_421590_, p_422594_ - p_421871_);
      return p_421800_ != null ? p_421800_.intersection(screenrectangle) : screenrectangle;
   }
}
