package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record BlitRenderState(
   RenderPipeline pipeline,
   TextureSetup textureSetup,
   Matrix3x2f pose,
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
   public BlitRenderState(
      RenderPipeline p_415865_,
      TextureSetup p_416394_,
      Matrix3x2f p_415848_,
      int p_416365_,
      int p_416414_,
      int p_416112_,
      int p_416519_,
      float p_416693_,
      float p_415641_,
      float p_416035_,
      float p_415841_,
      int p_415610_,
      @Nullable ScreenRectangle p_415846_
   ) {
      this(
         p_415865_,
         p_416394_,
         p_415848_,
         p_416365_,
         p_416414_,
         p_416112_,
         p_416519_,
         p_416693_,
         p_415641_,
         p_416035_,
         p_415841_,
         p_415610_,
         p_415846_,
         getBounds(p_416365_, p_416414_, p_416112_, p_416519_, p_415848_, p_415846_)
      );
   }

   @Override
   public void buildVertices(VertexConsumer p_415779_) {
      p_415779_.addVertexWith2DPose(this.pose(), this.x0(), this.y0()).setUv(this.u0(), this.v0()).setColor(this.color());
      p_415779_.addVertexWith2DPose(this.pose(), this.x0(), this.y1()).setUv(this.u0(), this.v1()).setColor(this.color());
      p_415779_.addVertexWith2DPose(this.pose(), this.x1(), this.y1()).setUv(this.u1(), this.v1()).setColor(this.color());
      p_415779_.addVertexWith2DPose(this.pose(), this.x1(), this.y0()).setUv(this.u1(), this.v0()).setColor(this.color());
   }

   @Nullable
   private static ScreenRectangle getBounds(
      int p_421782_, int p_422661_, int p_421883_, int p_421626_, Matrix3x2f p_422718_, @Nullable ScreenRectangle p_422518_
   ) {
      ScreenRectangle screenrectangle = new ScreenRectangle(p_421782_, p_422661_, p_421883_ - p_421782_, p_421626_ - p_422661_).transformMaxBounds(p_422718_);
      return p_422518_ != null ? p_422518_.intersection(screenrectangle) : screenrectangle;
   }
}
