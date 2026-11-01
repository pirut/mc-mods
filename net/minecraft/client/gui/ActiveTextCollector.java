package net.minecraft.client.gui;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.font.ActiveArea;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface ActiveTextCollector {
   double PERIOD_PER_SCROLLED_PIXEL = 0.5;
   double MIN_SCROLL_PERIOD = 3.0;

   ActiveTextCollector.Parameters defaultParameters();

   void defaultParameters(ActiveTextCollector.Parameters var1);

   default void accept(int p_457613_, int p_458257_, FormattedCharSequence p_458196_) {
      this.accept(TextAlignment.LEFT, p_457613_, p_458257_, this.defaultParameters(), p_458196_);
   }

   default void accept(int p_458199_, int p_458227_, Component p_457757_) {
      this.accept(TextAlignment.LEFT, p_458199_, p_458227_, this.defaultParameters(), p_457757_.getVisualOrderText());
   }

   default void accept(TextAlignment p_458285_, int p_458082_, int p_457612_, ActiveTextCollector.Parameters p_458223_, Component p_458261_) {
      this.accept(p_458285_, p_458082_, p_457612_, p_458223_, p_458261_.getVisualOrderText());
   }

   void accept(TextAlignment var1, int var2, int var3, ActiveTextCollector.Parameters var4, FormattedCharSequence var5);

   default void accept(TextAlignment p_457700_, int p_458083_, int p_457753_, Component p_457658_) {
      this.accept(p_457700_, p_458083_, p_457753_, p_457658_.getVisualOrderText());
   }

   default void accept(TextAlignment p_458146_, int p_457836_, int p_457639_, FormattedCharSequence p_458266_) {
      this.accept(p_458146_, p_457836_, p_457639_, this.defaultParameters(), p_458266_);
   }

   void acceptScrolling(Component var1, int var2, int var3, int var4, int var5, int var6, ActiveTextCollector.Parameters var7);

   default void acceptScrolling(Component p_457543_, int p_458067_, int p_458229_, int p_457515_, int p_457514_, int p_457898_) {
      this.acceptScrolling(p_457543_, p_458067_, p_458229_, p_457515_, p_457514_, p_457898_, this.defaultParameters());
   }

   default void acceptScrollingWithDefaultCenter(Component p_458174_, int p_457707_, int p_457659_, int p_457640_, int p_458150_) {
      this.acceptScrolling(p_458174_, (p_457707_ + p_457659_) / 2, p_457707_, p_457659_, p_457640_, p_458150_);
   }

   default void defaultScrollingHelper(
      Component p_457559_,
      int p_457567_,
      int p_458224_,
      int p_457546_,
      int p_457893_,
      int p_458192_,
      int p_457951_,
      int p_458230_,
      ActiveTextCollector.Parameters p_457949_
   ) {
      int i = (p_457893_ + p_458192_ - p_458230_) / 2 + 1;
      int j = p_457546_ - p_458224_;
      if (p_457951_ > j) {
         int k = p_457951_ - j;
         double d0 = Util.getMillis() / 1000.0;
         double d1 = Math.max(k * 0.5, 3.0);
         double d2 = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * d0 / d1)) / 2.0 + 0.5;
         double d3 = Mth.lerp(d2, 0.0, (double)k);
         ActiveTextCollector.Parameters activetextcollector$parameters = p_457949_.withScissor(p_458224_, p_457546_, p_457893_, p_458192_);
         this.accept(TextAlignment.LEFT, p_458224_ - (int)d3, i, activetextcollector$parameters, p_457559_.getVisualOrderText());
      } else {
         int l = Mth.clamp(p_457567_, p_458224_ + p_457951_ / 2, p_457546_ - p_457951_ / 2);
         this.accept(TextAlignment.CENTER, l, i, p_457559_);
      }
   }

   static void findElementUnderCursor(GuiTextRenderState p_458064_, float p_457995_, float p_458305_, final Consumer<Style> p_457797_) {
      ScreenRectangle screenrectangle = p_458064_.bounds();
      if (screenrectangle != null && screenrectangle.containsPoint((int)p_457995_, (int)p_458305_)) {
         Vector2fc vector2fc = p_458064_.pose.invert(new Matrix3x2f()).transformPosition(new Vector2f(p_457995_, p_458305_));
         final float f = vector2fc.x();
         final float f1 = vector2fc.y();
         p_458064_.ensurePrepared()
            .visit(
               new Font.GlyphVisitor() {
                  @Override
                  public void acceptGlyph(TextRenderable.Styled p_458185_) {
                     this.acceptActiveArea(p_458185_);
                  }

                  @Override
                  public void acceptEmptyArea(EmptyArea p_457803_) {
                     this.acceptActiveArea(p_457803_);
                  }

                  private void acceptActiveArea(ActiveArea p_457513_) {
                     if (ActiveTextCollector.isPointInRectangle(
                        f, f1, p_457513_.activeLeft(), p_457513_.activeTop(), p_457513_.activeRight(), p_457513_.activeBottom()
                     )) {
                        p_457797_.accept(p_457513_.style());
                     }
                  }
               }
            );
      }
   }

   static boolean isPointInRectangle(float p_457595_, float p_457765_, float p_457571_, float p_458191_, float p_457699_, float p_457844_) {
      return p_457595_ >= p_457571_ && p_457595_ < p_457699_ && p_457765_ >= p_458191_ && p_457765_ < p_457844_;
   }

   @OnlyIn(Dist.CLIENT)
   public static class ClickableStyleFinder implements ActiveTextCollector {
      private static final ActiveTextCollector.Parameters INITIAL = new ActiveTextCollector.Parameters(new Matrix3x2f());
      private final Font font;
      private final int testX;
      private final int testY;
      private ActiveTextCollector.Parameters defaultParameters = INITIAL;
      private boolean includeInsertions;
      @Nullable
      private Style result;
      private final Consumer<Style> styleScanner = p_477720_ -> {
         if (p_477720_.getClickEvent() != null || this.includeInsertions && p_477720_.getInsertion() != null) {
            this.result = p_477720_;
         }
      };

      public ClickableStyleFinder(Font p_458203_, int p_457824_, int p_457934_) {
         this.font = p_458203_;
         this.testX = p_457824_;
         this.testY = p_457934_;
      }

      @Override
      public ActiveTextCollector.Parameters defaultParameters() {
         return this.defaultParameters;
      }

      @Override
      public void defaultParameters(ActiveTextCollector.Parameters p_457712_) {
         this.defaultParameters = p_457712_;
      }

      @Override
      public void accept(TextAlignment p_457904_, int p_458286_, int p_457947_, ActiveTextCollector.Parameters p_457608_, FormattedCharSequence p_457770_) {
         int i = p_457904_.calculateLeft(p_458286_, this.font, p_457770_);
         GuiTextRenderState guitextrenderstate = new GuiTextRenderState(
            this.font, p_457770_, p_457608_.pose(), i, p_457947_, ARGB.white(p_457608_.opacity()), 0, true, true, p_457608_.scissor()
         );
         ActiveTextCollector.findElementUnderCursor(guitextrenderstate, this.testX, this.testY, this.styleScanner);
      }

      @Override
      public void acceptScrolling(
         Component p_458278_, int p_457953_, int p_457705_, int p_458095_, int p_457790_, int p_457858_, ActiveTextCollector.Parameters p_458162_
      ) {
         int i = this.font.width(p_458278_);
         int j = 9;
         this.defaultScrollingHelper(p_458278_, p_457953_, p_457705_, p_458095_, p_457790_, p_457858_, i, j, p_458162_);
      }

      public ActiveTextCollector.ClickableStyleFinder includeInsertions(boolean p_479621_) {
         this.includeInsertions = p_479621_;
         return this;
      }

      @Nullable
      public Style result() {
         return this.result;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Parameters(Matrix3x2fc pose, float opacity, @Nullable ScreenRectangle scissor) {
      public Parameters(Matrix3x2fc p_457626_) {
         this(p_457626_, 1.0F, null);
      }

      public ActiveTextCollector.Parameters withPose(Matrix3x2fc p_458063_) {
         return new ActiveTextCollector.Parameters(p_458063_, this.opacity, this.scissor);
      }

      public ActiveTextCollector.Parameters withScale(float p_458158_) {
         return this.withPose(this.pose.scale(p_458158_, p_458158_, new Matrix3x2f()));
      }

      public ActiveTextCollector.Parameters withOpacity(float p_458291_) {
         return this.opacity == p_458291_ ? this : new ActiveTextCollector.Parameters(this.pose, p_458291_, this.scissor);
      }

      public ActiveTextCollector.Parameters withScissor(ScreenRectangle p_457975_) {
         return p_457975_.equals(this.scissor) ? this : new ActiveTextCollector.Parameters(this.pose, this.opacity, p_457975_);
      }

      public ActiveTextCollector.Parameters withScissor(int p_458153_, int p_458164_, int p_457572_, int p_458265_) {
         ScreenRectangle screenrectangle = new ScreenRectangle(p_458153_, p_457572_, p_458164_ - p_458153_, p_458265_ - p_457572_)
            .transformAxisAligned(this.pose);
         if (this.scissor != null) {
            screenrectangle = Objects.requireNonNullElse(this.scissor.intersection(screenrectangle), ScreenRectangle.empty());
         }

         return this.withScissor(screenrectangle);
      }
   }
}
