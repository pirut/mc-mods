package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FocusableTextWidget extends MultiLineTextWidget {
   public static final int DEFAULT_PADDING = 4;
   private final int padding;
   private final int maxWidth;
   private final boolean alwaysShowBorder;
   private final FocusableTextWidget.BackgroundFill backgroundFill;

   FocusableTextWidget(Component p_295867_, Font p_294548_, int p_295671_, int p_330770_, FocusableTextWidget.BackgroundFill p_456236_, boolean p_455824_) {
      super(p_295867_, p_294548_);
      this.active = true;
      this.padding = p_295671_;
      this.maxWidth = p_330770_;
      this.alwaysShowBorder = p_455824_;
      this.backgroundFill = p_456236_;
      this.updateWidth();
      this.updateHeight();
      this.setCentered(true);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput p_295798_) {
      p_295798_.add(NarratedElementType.TITLE, this.getMessage());
   }

   @Override
   public void renderWidget(GuiGraphics p_296375_, int p_295686_, int p_295354_, float p_295563_) {
      int i = this.alwaysShowBorder && !this.isFocused() ? ARGB.color(this.alpha, -6250336) : ARGB.white(this.alpha);
      switch (this.backgroundFill) {
         case ALWAYS:
            p_296375_.fill(this.getX() + 1, this.getY(), this.getRight(), this.getBottom(), ARGB.black(this.alpha));
            break;
         case ON_FOCUS:
            if (this.isFocused()) {
               p_296375_.fill(this.getX() + 1, this.getY(), this.getRight(), this.getBottom(), ARGB.black(this.alpha));
            }
         case NEVER:
      }

      if (this.isFocused() || this.alwaysShowBorder) {
         p_296375_.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), i);
      }

      super.renderWidget(p_296375_, p_295686_, p_295354_, p_295563_);
   }

   @Override
   protected int getTextX() {
      return this.getX() + this.padding;
   }

   @Override
   protected int getTextY() {
      return super.getTextY() + this.padding;
   }

   @Override
   public MultiLineTextWidget setMaxWidth(int p_455173_) {
      return super.setMaxWidth(p_455173_ - this.padding * 2);
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   public int getPadding() {
      return this.padding;
   }

   public void updateWidth() {
      if (this.maxWidth != -1) {
         this.setWidth(this.maxWidth);
         this.setMaxWidth(this.maxWidth);
      } else {
         this.setWidth(this.getFont().width(this.getMessage()) + this.padding * 2);
      }
   }

   public void updateHeight() {
      int i = 9 * this.getFont().split(this.getMessage(), super.getWidth()).size();
      this.setHeight(i + this.padding * 2);
   }

   @Override
   public void setMessage(Component p_455477_) {
      this.message = p_455477_;
      int i;
      if (this.maxWidth != -1) {
         i = this.maxWidth;
      } else {
         i = this.getFont().width(p_455477_) + this.padding * 2;
      }

      this.setWidth(i);
      this.updateHeight();
   }

   @Override
   public void playDownSound(SoundManager p_295576_) {
   }

   public static FocusableTextWidget.Builder builder(Component p_455077_, Font p_454887_) {
      return new FocusableTextWidget.Builder(p_455077_, p_454887_);
   }

   public static FocusableTextWidget.Builder builder(Component p_455873_, Font p_456208_, int p_455678_) {
      return new FocusableTextWidget.Builder(p_455873_, p_456208_, p_455678_);
   }

   @OnlyIn(Dist.CLIENT)
   public static enum BackgroundFill {
      ALWAYS,
      ON_FOCUS,
      NEVER;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final Component message;
      private final Font font;
      private final int padding;
      private int maxWidth = -1;
      private boolean alwaysShowBorder = true;
      private FocusableTextWidget.BackgroundFill backgroundFill = FocusableTextWidget.BackgroundFill.ALWAYS;

      Builder(Component p_455066_, Font p_455727_) {
         this(p_455066_, p_455727_, 4);
      }

      Builder(Component p_455986_, Font p_455930_, int p_455825_) {
         this.message = p_455986_;
         this.font = p_455930_;
         this.padding = p_455825_;
      }

      public FocusableTextWidget.Builder maxWidth(int p_455673_) {
         this.maxWidth = p_455673_;
         return this;
      }

      public FocusableTextWidget.Builder textWidth(int p_455316_) {
         this.maxWidth = p_455316_ + this.padding * 2;
         return this;
      }

      public FocusableTextWidget.Builder alwaysShowBorder(boolean p_455833_) {
         this.alwaysShowBorder = p_455833_;
         return this;
      }

      public FocusableTextWidget.Builder backgroundFill(FocusableTextWidget.BackgroundFill p_454759_) {
         this.backgroundFill = p_454759_;
         return this;
      }

      public FocusableTextWidget build() {
         return new FocusableTextWidget(this.message, this.font, this.padding, this.maxWidth, this.backgroundFill, this.alwaysShowBorder);
      }
   }
}
