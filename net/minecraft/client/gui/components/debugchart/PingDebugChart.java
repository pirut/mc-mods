package net.minecraft.client.gui.components.debugchart;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.debugchart.SampleStorage;

@Environment(EnvType.CLIENT)
public class PingDebugChart extends AbstractDebugChart {
   private static final int CHART_TOP_VALUE = 500;

   public PingDebugChart(Font font, SampleStorage sampleStorage) {
      super(font, sampleStorage);
   }

   @Override
   protected void renderAdditionalLinesAndLabels(GuiGraphics guiGraphics, int x, int width, int height) {
      this.drawStringWithShade(guiGraphics, "500 ms", x + 1, height - 60 + 1);
   }

   @Override
   protected String toDisplayString(double value) {
      return String.format(Locale.ROOT, "%d ms", (int)Math.round(value));
   }

   @Override
   protected int getSampleHeight(double value) {
      return (int)Math.round(value * 60.0 / 500.0);
   }

   @Override
   protected int getSampleColor(long value) {
      return this.getSampleColor(value, 0.0, -16711936, 250.0, -256, 500.0, -65536);
   }
}
