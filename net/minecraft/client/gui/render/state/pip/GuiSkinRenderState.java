package net.minecraft.client.gui.render.state.pip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record GuiSkinRenderState(
   PlayerModel playerModel,
   Identifier texture,
   float rotationX,
   float rotationY,
   float pivotY,
   int x0,
   int y0,
   int x1,
   int y1,
   float scale,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
   public GuiSkinRenderState(
      PlayerModel playerModel,
      Identifier texture,
      float rotationX,
      float rotationY,
      float pivotY,
      int x0,
      int y0,
      int x1,
      int y1,
      float scale,
      @Nullable ScreenRectangle scissorArea
   ) {
      this(
         playerModel,
         texture,
         rotationX,
         rotationY,
         pivotY,
         x0,
         y0,
         x1,
         y1,
         scale,
         scissorArea,
         PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea)
      );
   }
}
