package net.minecraft.client.gui.render.state.pip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record GuiBookModelRenderState(
   BookModel bookModel,
   Identifier texture,
   float open,
   float flip,
   int x0,
   int y0,
   int x1,
   int y1,
   float scale,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
   public GuiBookModelRenderState(
      BookModel bookModel, Identifier texture, float open, float flip, int x0, int y0, int x1, int y1, float scale, @Nullable ScreenRectangle scissorArea
   ) {
      this(bookModel, texture, open, flip, x0, y0, x1, y1, scale, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
   }
}
