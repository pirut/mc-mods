package net.minecraft.client.gui.render.state.pip;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.profiling.ResultField;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record GuiProfilerChartRenderState(
   List<ResultField> chartData, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
   public GuiProfilerChartRenderState(List<ResultField> chartData, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea) {
      this(chartData, x0, y0, x1, y1, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
   }

   @Override
   public float scale() {
      return 1.0F;
   }
}
