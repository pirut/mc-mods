package net.neoforged.neoforge.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import java.text.DecimalFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ExtendedSlider extends AbstractSliderButton {
   protected Component prefix;
   protected Component suffix;
   protected double minValue;
   protected double maxValue;
   protected double stepSize;
   protected boolean drawString;
   private final DecimalFormat format;

   public ExtendedSlider(
      int x,
      int y,
      int width,
      int height,
      Component prefix,
      Component suffix,
      double minValue,
      double maxValue,
      double currentValue,
      double stepSize,
      int precision,
      boolean drawString
   ) {
      super(x, y, width, height, Component.empty(), 0.0);
      this.prefix = prefix;
      this.suffix = suffix;
      this.minValue = minValue;
      this.maxValue = maxValue;
      this.stepSize = Math.abs(stepSize);
      this.value = this.snapToNearest((currentValue - minValue) / (maxValue - minValue));
      this.drawString = drawString;
      if (stepSize == 0.0) {
         precision = Math.min(precision, 4);
         StringBuilder builder = new StringBuilder("0");
         if (precision > 0) {
            builder.append('.');
         }

         while (precision-- > 0) {
            builder.append('0');
         }

         this.format = new DecimalFormat(builder.toString());
      } else if (Mth.equal(this.stepSize, Math.floor(this.stepSize))) {
         this.format = new DecimalFormat("0");
      } else {
         this.format = new DecimalFormat(Double.toString(this.stepSize).replaceAll("\\d", "0"));
      }

      this.updateMessage();
   }

   public ExtendedSlider(
      int x, int y, int width, int height, Component prefix, Component suffix, double minValue, double maxValue, double currentValue, boolean drawString
   ) {
      this(x, y, width, height, prefix, suffix, minValue, maxValue, currentValue, 1.0, 0, drawString);
   }

   public double getValue() {
      return this.value * (this.maxValue - this.minValue) + this.minValue;
   }

   public long getValueLong() {
      return Math.round(this.getValue());
   }

   public int getValueInt() {
      return (int)this.getValueLong();
   }

   @Override
   public void setValue(double value) {
      this.setFractionalValue((value - this.minValue) / (this.maxValue - this.minValue));
   }

   public String getValueString() {
      return this.format.format(this.getValue());
   }

   @Override
   public void onClick(double mouseX, double mouseY) {
      this.setValueFromMouse(mouseX);
   }

   @Override
   protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
      super.onDrag(mouseX, mouseY, dragX, dragY);
      this.setValueFromMouse(mouseX);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      boolean flag = keyCode == 263;
      if (flag || keyCode == 262) {
         if (this.minValue > this.maxValue) {
            flag = !flag;
         }

         float f = flag ? -1.0F : 1.0F;
         if (this.stepSize <= 0.0) {
            this.setFractionalValue(this.value + f / (this.width - 8));
         } else {
            this.setValue(this.getValue() + f * this.stepSize);
         }
      }

      return false;
   }

   private void setValueFromMouse(double mouseX) {
      this.setFractionalValue((mouseX - (this.getX() + 4)) / (this.width - 8));
   }

   private void setFractionalValue(double fractionalValue) {
      double oldValue = this.value;
      this.value = this.snapToNearest(fractionalValue);
      if (!Mth.equal(oldValue, this.value)) {
         this.applyValue();
      }

      this.updateMessage();
   }

   private double snapToNearest(double value) {
      if (this.stepSize <= 0.0) {
         return Mth.clamp(value, 0.0, 1.0);
      } else {
         value = Mth.lerp(Mth.clamp(value, 0.0, 1.0), this.minValue, this.maxValue);
         value = this.stepSize * Math.round(value / this.stepSize);
         if (this.minValue > this.maxValue) {
            value = Mth.clamp(value, this.maxValue, this.minValue);
         } else {
            value = Mth.clamp(value, this.minValue, this.maxValue);
         }

         return Mth.map(value, this.minValue, this.maxValue, 0.0, 1.0);
      }
   }

   @Override
   protected void updateMessage() {
      if (this.drawString) {
         this.setMessage(Component.literal("").append(this.prefix).append(this.getValueString()).append(this.suffix));
      } else {
         this.setMessage(Component.empty());
      }
   }

   @Override
   protected void applyValue() {
   }

   @Override
   public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      Minecraft minecraft = Minecraft.getInstance();
      guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      guiGraphics.blitSprite(this.getSprite(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
      guiGraphics.blitSprite(this.getHandleSprite(), this.getX() + (int)(this.value * (this.width - 8)), this.getY(), 8, this.getHeight());
      guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      int i = this.active ? 16777215 : 10526880;
      this.renderScrollingString(guiGraphics, minecraft.font, 2, i | Mth.ceil(this.alpha * 255.0F) << 24);
   }
}
