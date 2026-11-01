package net.minecraft.client.gui.components.debugchart;

import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.debugchart.SampleStorage;
import net.minecraft.util.debugchart.TpsDebugDimensions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TpsDebugChart extends AbstractDebugChart {
   private static final int RED = -65536;
   private static final int YELLOW = -256;
   private static final int GREEN = -16711936;
   private static final int TICK_METHOD_COLOR = -6745839;
   private static final int TASK_COLOR = -4548257;
   private static final int OTHER_COLOR = -10547572;
   private final Supplier<Float> msptSupplier;

   public TpsDebugChart(Font p_299254_, SampleStorage p_324399_, Supplier<Float> p_309098_) {
      super(p_299254_, p_324399_);
      this.msptSupplier = p_309098_;
   }

   @Override
   protected void renderAdditionalLinesAndLabels(GuiGraphics p_298653_, int p_298791_, int p_298387_, int p_298869_) {
      float $$4 = (float)TimeUtil.MILLISECONDS_PER_SECOND / this.msptSupplier.get();
      this.drawStringWithShade(p_298653_, String.format("%.1f TPS", $$4), p_298791_ + 1, p_298869_ - 60 + 1);
   }

   @Override
   protected void drawAdditionalDimensions(GuiGraphics p_321511_, int p_321489_, int p_321791_, int p_321685_) {
      long $$4 = this.sampleStorage.get(p_321685_, TpsDebugDimensions.TICK_SERVER_METHOD.ordinal());
      int $$5 = this.getSampleHeight($$4);
      p_321511_.fill(RenderType.guiOverlay(), p_321791_, p_321489_ - $$5, p_321791_ + 1, p_321489_, -6745839);
      long $$6 = this.sampleStorage.get(p_321685_, TpsDebugDimensions.SCHEDULED_TASKS.ordinal());
      int $$7 = this.getSampleHeight($$6);
      p_321511_.fill(RenderType.guiOverlay(), p_321791_, p_321489_ - $$5 - $$7, p_321791_ + 1, p_321489_ - $$5, -4548257);
      long $$8 = this.sampleStorage.get(p_321685_) - this.sampleStorage.get(p_321685_, TpsDebugDimensions.IDLE.ordinal()) - $$4 - $$6;
      int $$9 = this.getSampleHeight($$8);
      p_321511_.fill(RenderType.guiOverlay(), p_321791_, p_321489_ - $$9 - $$7 - $$5, p_321791_ + 1, p_321489_ - $$7 - $$5, -10547572);
   }

   @Override
   protected long getValueForAggregation(int p_321565_) {
      return this.sampleStorage.get(p_321565_) - this.sampleStorage.get(p_321565_, TpsDebugDimensions.IDLE.ordinal());
   }

   @Override
   protected String toDisplayString(double p_298403_) {
      return String.format(Locale.ROOT, "%d ms", (int)Math.round(toMilliseconds(p_298403_)));
   }

   @Override
   protected int getSampleHeight(double p_299161_) {
      return (int)Math.round(toMilliseconds(p_299161_) * 60.0 / this.msptSupplier.get().floatValue());
   }

   @Override
   protected int getSampleColor(long p_299243_) {
      float $$1 = this.msptSupplier.get();
      return this.getSampleColor(toMilliseconds(p_299243_), $$1, -16711936, $$1 * 1.125, -256, $$1 * 1.25, -65536);
   }

   private static double toMilliseconds(double p_298223_) {
      return p_298223_ / 1000000.0;
   }
}
