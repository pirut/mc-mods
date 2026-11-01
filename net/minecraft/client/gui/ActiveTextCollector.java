package net.minecraft.client.gui;

import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface ActiveTextCollector {
   double PERIOD_PER_SCROLLED_PIXEL = 0.5;
   double MIN_SCROLL_PERIOD = 3.0;

   ActiveTextCollector.Parameters defaultParameters();

   void defaultParameters(ActiveTextCollector.Parameters defaultParameters);

   default void accept(int x, int y, FormattedCharSequence text) {
      this.accept(TextAlignment.LEFT, x, y, this.defaultParameters(), text);
   }

   default void accept(int x, int y, Component text) {
      this.accept(TextAlignment.LEFT, x, y, this.defaultParameters(), text.getVisualOrderText());
   }

   default void accept(TextAlignment textAlignment, int x, int y, ActiveTextCollector.Parameters parameters, Component text) {
      this.accept(textAlignment, x, y, parameters, text.getVisualOrderText());
   }

   void accept(TextAlignment textAlignment, int x, int y, ActiveTextCollector.Parameters parameters, FormattedCharSequence text);

   default void accept(TextAlignment textAlignment, int x, int y, Component text) {
      this.accept(textAlignment, x, y, text.getVisualOrderText());
   }

   default void accept(TextAlignment textAlignment, int x, int y, FormattedCharSequence text) {
      this.accept(textAlignment, x, y, this.defaultParameters(), text);
   }

   void acceptScrolling(Component text, int center, int minX, int maxX, int minY, int maxY, ActiveTextCollector.Parameters parameters);

   default void acceptScrolling(Component text, int center, int minX, int maxX, int minY, int maxY) {
      this.acceptScrolling(text, center, minX, maxX, minY, maxY, this.defaultParameters());
   }

   default void acceptScrollingWithDefaultCenter(Component text, int minX, int maxX, int minY, int maxY) {
      this.acceptScrolling(text, (minX + maxX) / 2, minX, maxX, minY, maxY);
   }

   default void defaultScrollingHelper(
      Component text, int center, int minX, int maxX, int minY, int maxY, int width, int height, ActiveTextCollector.Parameters parameters
   ) {
      int i = (minY + maxY - height) / 2 + 1;
      int j = maxX - minX;
      if (width > j) {
         int k = width - j;
         double d = Util.getMillis() / 1000.0;
         double e = Math.max(k * 0.5, 3.0);
         double f = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * d / e)) / 2.0 + 0.5;
         double g = Mth.lerp(f, 0.0, (double)k);
         ActiveTextCollector.Parameters parameters2 = parameters.withScissor(minX, maxX, minY, maxY);
         this.accept(TextAlignment.LEFT, minX - (int)g, i, parameters2, text.getVisualOrderText());
      } else {
         int k = Mth.clamp(center, minX + width / 2, maxX - width / 2);
         this.accept(TextAlignment.CENTER, k, i, text);
      }
   }

   static void findElementUnderCursor(GuiTextRenderState renderState, float mouseX, float mouseY, Consumer<Style> styleScanner) {
      ScreenRectangle screenRectangle = renderState.bounds();
      if (screenRectangle != null && screenRectangle.containsPoint((int)mouseX, (int)mouseY)) {
         Vector2fc vector2fc = renderState.pose.invert(new Matrix3x2f()).transformPosition(new Vector2f(mouseX, mouseY));
         final float f = vector2fc.x();
         final float g = vector2fc.y();
         renderState.ensurePrepared()
            .visit(
               new Font.GlyphVisitor() {
                  @Override
                  public void acceptGlyph(TextRenderable.Styled glyph) {
                     this.acceptActiveArea(glyph);
                  }

                  @Override
                  public void acceptEmptyArea(EmptyArea emptyArea) {
                     this.acceptActiveArea(emptyArea);
                  }

                  private void acceptActiveArea(ActiveArea activeArea) {
                     if (ActiveTextCollector.isPointInRectangle(
                        f, g, activeArea.activeLeft(), activeArea.activeTop(), activeArea.activeRight(), activeArea.activeBottom()
                     )) {
                        styleScanner.accept(activeArea.style());
                     }
                  }
               }
            );
      }
   }

   static boolean isPointInRectangle(float mouseX, float mouseY, float minX, float minY, float maxX, float maxY) {
      return mouseX >= minX && mouseX < maxX && mouseY >= minY && mouseY < maxY;
   }

   @Environment(EnvType.CLIENT)
   public static class ClickableStyleFinder implements ActiveTextCollector {
      private static final ActiveTextCollector.Parameters INITIAL = new ActiveTextCollector.Parameters(new Matrix3x2f());
      private final Font font;
      private final int testX;
      private final int testY;
      private ActiveTextCollector.Parameters defaultParameters = INITIAL;
      private boolean includeInsertions;
      @Nullable
      private Style result;
      private final Consumer<Style> styleScanner = style -> {
         if (style.getClickEvent() != null || this.includeInsertions && style.getInsertion() != null) {
            this.result = style;
         }
      };

      public ClickableStyleFinder(Font font, int testX, int testY) {
         this.font = font;
         this.testX = testX;
         this.testY = testY;
      }

      @Override
      public ActiveTextCollector.Parameters defaultParameters() {
         return this.defaultParameters;
      }

      @Override
      public void defaultParameters(ActiveTextCollector.Parameters defaultParameters) {
         this.defaultParameters = defaultParameters;
      }

      @Override
      public void accept(TextAlignment textAlignment, int x, int y, ActiveTextCollector.Parameters parameters, FormattedCharSequence text) {
         int i = textAlignment.calculateLeft(x, this.font, text);
         GuiTextRenderState guiTextRenderState = new GuiTextRenderState(
            this.font, text, parameters.pose(), i, y, ARGB.white(parameters.opacity()), 0, true, true, parameters.scissor()
         );
         ActiveTextCollector.findElementUnderCursor(guiTextRenderState, this.testX, this.testY, this.styleScanner);
      }

      @Override
      public void acceptScrolling(Component text, int center, int minX, int maxX, int minY, int maxY, ActiveTextCollector.Parameters parameters) {
         int i = this.font.width(text);
         int j = 9;
         this.defaultScrollingHelper(text, center, minX, maxX, minY, maxY, i, j, parameters);
      }

      public ActiveTextCollector.ClickableStyleFinder includeInsertions(boolean includeInsertions) {
         this.includeInsertions = includeInsertions;
         return this;
      }

      @Nullable
      public Style result() {
         return this.result;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Parameters(Matrix3x2fc pose, float opacity, @Nullable ScreenRectangle scissor) {
      public Parameters(Matrix3x2fc pose) {
         this(pose, 1.0F, null);
      }

      public ActiveTextCollector.Parameters withPose(Matrix3x2fc pose) {
         return new ActiveTextCollector.Parameters(pose, this.opacity, this.scissor);
      }

      public ActiveTextCollector.Parameters withScale(float scale) {
         return this.withPose(this.pose.scale(scale, scale, new Matrix3x2f()));
      }

      public ActiveTextCollector.Parameters withOpacity(float opacity) {
         return this.opacity == opacity ? this : new ActiveTextCollector.Parameters(this.pose, opacity, this.scissor);
      }

      public ActiveTextCollector.Parameters withScissor(ScreenRectangle scissor) {
         return scissor.equals(this.scissor) ? this : new ActiveTextCollector.Parameters(this.pose, this.opacity, scissor);
      }

      public ActiveTextCollector.Parameters withScissor(int minX, int maxX, int minY, int maxY) {
         ScreenRectangle screenRectangle = new ScreenRectangle(minX, minY, maxX - minX, maxY - minY).transformAxisAligned(this.pose);
         if (this.scissor != null) {
            screenRectangle = Objects.requireNonNullElse(this.scissor.intersection(screenRectangle), ScreenRectangle.empty());
         }

         return this.withScissor(screenRectangle);
      }
   }
}
