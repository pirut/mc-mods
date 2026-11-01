package net.minecraft.client.gui.components.debugchart;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.debugchart.SampleStorage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractDebugChart {
   protected static final int COLOR_GREY = 14737632;
   protected static final int CHART_HEIGHT = 60;
   protected static final int LINE_WIDTH = 1;
   protected final Font font;
   protected final SampleStorage sampleStorage;

   protected AbstractDebugChart(Font p_299029_, SampleStorage p_324391_) {
      this.font = p_299029_;
      this.sampleStorage = p_324391_;
   }

   public int getWidth(int p_298843_) {
      return Math.min(this.sampleStorage.capacity() + 2, p_298843_);
   }

   public void drawChart(GuiGraphics p_298960_, int p_298986_, int p_298301_) {
      int $$3 = p_298960_.guiHeight();
      p_298960_.fill(RenderType.guiOverlay(), p_298986_, $$3 - 60, p_298986_ + p_298301_, $$3, -1873784752);
      long $$4 = 0L;
      long $$5 = 2147483647L;
      long $$6 = -2147483648L;
      int $$7 = Math.max(0, this.sampleStorage.capacity() - (p_298301_ - 2));
      int $$8 = this.sampleStorage.size() - $$7;

      for (int $$9 = 0; $$9 < $$8; $$9++) {
         int $$10 = p_298986_ + $$9 + 1;
         int $$11 = $$7 + $$9;
         long $$12 = this.getValueForAggregation($$11);
         $$5 = Math.min($$5, $$12);
         $$6 = Math.max($$6, $$12);
         $$4 += $$12;
         this.drawDimensions(p_298960_, $$3, $$10, $$11);
      }

      p_298960_.hLine(RenderType.guiOverlay(), p_298986_, p_298986_ + p_298301_ - 1, $$3 - 60, -1);
      p_298960_.hLine(RenderType.guiOverlay(), p_298986_, p_298986_ + p_298301_ - 1, $$3 - 1, -1);
      p_298960_.vLine(RenderType.guiOverlay(), p_298986_, $$3 - 60, $$3, -1);
      p_298960_.vLine(RenderType.guiOverlay(), p_298986_ + p_298301_ - 1, $$3 - 60, $$3, -1);
      if ($$8 > 0) {
         String $$13 = this.toDisplayString($$5) + " min";
         String $$14 = this.toDisplayString((double)$$4 / $$8) + " avg";
         String $$15 = this.toDisplayString($$6) + " max";
         p_298960_.drawString(this.font, $$13, p_298986_ + 2, $$3 - 60 - 9, 14737632);
         p_298960_.drawCenteredString(this.font, $$14, p_298986_ + p_298301_ / 2, $$3 - 60 - 9, 14737632);
         p_298960_.drawString(this.font, $$15, p_298986_ + p_298301_ - this.font.width($$15) - 2, $$3 - 60 - 9, 14737632);
      }

      this.renderAdditionalLinesAndLabels(p_298960_, p_298986_, p_298301_, $$3);
   }

   protected void drawDimensions(GuiGraphics p_321561_, int p_321861_, int p_321591_, int p_321654_) {
      this.drawMainDimension(p_321561_, p_321861_, p_321591_, p_321654_);
      this.drawAdditionalDimensions(p_321561_, p_321861_, p_321591_, p_321654_);
   }

   protected void drawMainDimension(GuiGraphics p_321499_, int p_321849_, int p_321568_, int p_321766_) {
      long $$4 = this.sampleStorage.get(p_321766_);
      int $$5 = this.getSampleHeight($$4);
      int $$6 = this.getSampleColor($$4);
      p_321499_.fill(RenderType.guiOverlay(), p_321568_, p_321849_ - $$5, p_321568_ + 1, p_321849_, $$6);
   }

   protected void drawAdditionalDimensions(GuiGraphics p_321486_, int p_321516_, int p_321827_, int p_321819_) {
   }

   protected long getValueForAggregation(int p_321706_) {
      return this.sampleStorage.get(p_321706_);
   }

   protected void renderAdditionalLinesAndLabels(GuiGraphics p_298895_, int p_298979_, int p_298732_, int p_299176_) {
   }

   protected void drawStringWithShade(GuiGraphics p_298386_, String p_298809_, int p_298657_, int p_298698_) {
      p_298386_.fill(RenderType.guiOverlay(), p_298657_, p_298698_, p_298657_ + this.font.width(p_298809_) + 1, p_298698_ + 9, -1873784752);
      p_298386_.drawString(this.font, p_298809_, p_298657_ + 1, p_298698_ + 1, 14737632, false);
   }

   protected abstract String toDisplayString(double var1);

   protected abstract int getSampleHeight(double var1);

   protected abstract int getSampleColor(long var1);

   protected int getSampleColor(double p_298217_, double p_298257_, int p_298676_, double p_299233_, int p_298930_, double p_299140_, int p_298542_) {
      p_298217_ = Mth.clamp(p_298217_, p_298257_, p_299140_);
      return p_298217_ < p_299233_
         ? FastColor.ARGB32.lerp((float)((p_298217_ - p_298257_) / (p_299233_ - p_298257_)), p_298676_, p_298930_)
         : FastColor.ARGB32.lerp((float)((p_298217_ - p_299233_) / (p_299140_ - p_299233_)), p_298930_, p_298542_);
   }
}
