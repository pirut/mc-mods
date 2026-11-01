package net.minecraft.client.gui.render.state;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class GuiTextRenderState implements ScreenArea {
   public final Font font;
   public final FormattedCharSequence text;
   public final Matrix3x2fc pose;
   public final int x;
   public final int y;
   public final int color;
   public final int backgroundColor;
   public final boolean dropShadow;
   final boolean includeEmpty;
   @Nullable
   public final ScreenRectangle scissor;
   @Nullable
   private Font.PreparedText preparedText;
   @Nullable
   private ScreenRectangle bounds;

   public GuiTextRenderState(
      Font p_426159_,
      FormattedCharSequence p_425925_,
      Matrix3x2fc p_457791_,
      int p_416612_,
      int p_416232_,
      int p_425576_,
      int p_426044_,
      boolean p_426039_,
      boolean p_457587_,
      @Nullable ScreenRectangle p_416431_
   ) {
      this.font = p_426159_;
      this.text = p_425925_;
      this.pose = p_457791_;
      this.x = p_416612_;
      this.y = p_416232_;
      this.color = p_425576_;
      this.backgroundColor = p_426044_;
      this.dropShadow = p_426039_;
      this.includeEmpty = p_457587_;
      this.scissor = p_416431_;
   }

   public Font.PreparedText ensurePrepared() {
      if (this.preparedText == null) {
         this.preparedText = this.font.prepareText(this.text, this.x, this.y, this.color, this.dropShadow, this.includeEmpty, this.backgroundColor);
         ScreenRectangle screenrectangle = this.preparedText.bounds();
         if (screenrectangle != null) {
            screenrectangle = screenrectangle.transformMaxBounds(this.pose);
            this.bounds = this.scissor != null ? this.scissor.intersection(screenrectangle) : screenrectangle;
         }
      }

      return this.preparedText;
   }

   @Nullable
   @Override
   public ScreenRectangle bounds() {
      this.ensurePrepared();
      return this.bounds;
   }
}
