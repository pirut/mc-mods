package net.minecraft.client.gui;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringDecomposer;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class Font {
   private static final float EFFECT_DEPTH = 0.01F;
   private static final float OVER_EFFECT_DEPTH = 0.01F;
   private static final float UNDER_EFFECT_DEPTH = -0.01F;
   public static final float SHADOW_DEPTH = 0.03F;
   public final int lineHeight = 9;
   private final RandomSource random = RandomSource.create();
   final Font.Provider provider;
   private final StringSplitter splitter;

   public Font(Font.Provider provider) {
      this.provider = provider;
      this.splitter = new StringSplitter((i, style) -> this.getGlyphSource(style.getFont()).getGlyph(i).info().getAdvance(style.isBold()));
   }

   private GlyphSource getGlyphSource(FontDescription fontDescription) {
      return this.provider.glyphs(fontDescription);
   }

   public String bidirectionalShaping(String text) {
      try {
         Bidi bidi = new Bidi(new ArabicShaping(8).shape(text), 127);
         bidi.setReorderingMode(0);
         return bidi.writeReordered(2);
      } catch (ArabicShapingException var3) {
         return text;
      }
   }

   public void drawInBatch(
      String text,
      float x,
      float y,
      int color,
      boolean drawShadow,
      Matrix4f pose,
      MultiBufferSource bufferSource,
      Font.DisplayMode mode,
      int backgroundColor,
      int packedLightCoords
   ) {
      Font.PreparedText preparedText = this.prepareText(text, x, y, color, drawShadow, backgroundColor);
      preparedText.visit(Font.GlyphVisitor.forMultiBufferSource(bufferSource, pose, mode, packedLightCoords));
   }

   public void drawInBatch(
      Component text,
      float x,
      float y,
      int color,
      boolean drawShadow,
      Matrix4f pose,
      MultiBufferSource bufferSource,
      Font.DisplayMode mode,
      int backgroundColor,
      int packedLightCoords
   ) {
      Font.PreparedText preparedText = this.prepareText(text.getVisualOrderText(), x, y, color, drawShadow, false, backgroundColor);
      preparedText.visit(Font.GlyphVisitor.forMultiBufferSource(bufferSource, pose, mode, packedLightCoords));
   }

   public void drawInBatch(
      FormattedCharSequence text,
      float x,
      float y,
      int color,
      boolean drawShadow,
      Matrix4f pose,
      MultiBufferSource bufferSource,
      Font.DisplayMode mode,
      int backgroundColor,
      int packedLightCoords
   ) {
      Font.PreparedText preparedText = this.prepareText(text, x, y, color, drawShadow, false, backgroundColor);
      preparedText.visit(Font.GlyphVisitor.forMultiBufferSource(bufferSource, pose, mode, packedLightCoords));
   }

   public void drawInBatch8xOutline(
      FormattedCharSequence text, float x, float y, int color, int backgroundColor, Matrix4f pose, MultiBufferSource bufferSource, int packedLightCoords
   ) {
      Font.PreparedTextBuilder preparedTextBuilder = new Font.PreparedTextBuilder(0.0F, 0.0F, backgroundColor, false, false);

      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            if (i != 0 || j != 0) {
               float[] fs = new float[]{x};
               int k = i;
               int l = j;
               text.accept((lx, style, m) -> {
                  boolean bl = style.isBold();
                  BakedGlyph bakedGlyph = this.getGlyph(m, style);
                  preparedTextBuilder.x = fs[0] + k * bakedGlyph.info().getShadowOffset();
                  preparedTextBuilder.y = y + l * bakedGlyph.info().getShadowOffset();
                  fs[0] += bakedGlyph.info().getAdvance(bl);
                  return preparedTextBuilder.accept(lx, style.withColor(backgroundColor), bakedGlyph);
               });
            }
         }
      }

      Font.GlyphVisitor glyphVisitor = Font.GlyphVisitor.forMultiBufferSource(bufferSource, pose, Font.DisplayMode.NORMAL, packedLightCoords);

      for (TextRenderable.Styled styled : preparedTextBuilder.glyphs) {
         glyphVisitor.acceptGlyph(styled);
      }

      Font.PreparedTextBuilder preparedTextBuilder2 = new Font.PreparedTextBuilder(x, y, color, false, true);
      text.accept(preparedTextBuilder2);
      preparedTextBuilder2.visit(Font.GlyphVisitor.forMultiBufferSource(bufferSource, pose, Font.DisplayMode.POLYGON_OFFSET, packedLightCoords));
   }

   BakedGlyph getGlyph(int character, Style style) {
      GlyphSource glyphSource = this.getGlyphSource(style.getFont());
      BakedGlyph bakedGlyph = glyphSource.getGlyph(character);
      if (style.isObfuscated() && character != 32) {
         int i = Mth.ceil(bakedGlyph.info().getAdvance(false));
         bakedGlyph = glyphSource.getRandomGlyph(this.random, i);
      }

      return bakedGlyph;
   }

   public Font.PreparedText prepareText(String text, float x, float y, int color, boolean drawShadow, int backgroundColor) {
      if (this.isBidirectional()) {
         text = this.bidirectionalShaping(text);
      }

      Font.PreparedTextBuilder preparedTextBuilder = new Font.PreparedTextBuilder(x, y, color, backgroundColor, drawShadow, false);
      StringDecomposer.iterateFormatted(text, Style.EMPTY, preparedTextBuilder);
      return preparedTextBuilder;
   }

   public Font.PreparedText prepareText(FormattedCharSequence text, float x, float y, int color, boolean drawShadow, boolean includeEmpty, int backgroundColor) {
      Font.PreparedTextBuilder preparedTextBuilder = new Font.PreparedTextBuilder(x, y, color, backgroundColor, drawShadow, includeEmpty);
      text.accept(preparedTextBuilder);
      return preparedTextBuilder;
   }

   public int width(String text) {
      return Mth.ceil(this.splitter.stringWidth(text));
   }

   public int width(FormattedText text) {
      return Mth.ceil(this.splitter.stringWidth(text));
   }

   public int width(FormattedCharSequence text) {
      return Mth.ceil(this.splitter.stringWidth(text));
   }

   public String plainSubstrByWidth(String text, int maxWidth, boolean tail) {
      return tail ? this.splitter.plainTailByWidth(text, maxWidth, Style.EMPTY) : this.splitter.plainHeadByWidth(text, maxWidth, Style.EMPTY);
   }

   public String plainSubstrByWidth(String text, int maxWidth) {
      return this.splitter.plainHeadByWidth(text, maxWidth, Style.EMPTY);
   }

   public FormattedText substrByWidth(FormattedText text, int maxWidth) {
      return this.splitter.headByWidth(text, maxWidth, Style.EMPTY);
   }

   public int wordWrapHeight(FormattedText text, int maxWidth) {
      return 9 * this.splitter.splitLines(text, maxWidth, Style.EMPTY).size();
   }

   public List<FormattedCharSequence> split(FormattedText text, int maxWidth) {
      return Language.getInstance().getVisualOrder(this.splitter.splitLines(text, maxWidth, Style.EMPTY));
   }

   public List<FormattedText> splitIgnoringLanguage(FormattedText text, int maxWidth) {
      return this.splitter.splitLines(text, maxWidth, Style.EMPTY);
   }

   public boolean isBidirectional() {
      return Language.getInstance().isDefaultRightToLeft();
   }

   public StringSplitter getSplitter() {
      return this.splitter;
   }

   @Environment(EnvType.CLIENT)
   public static enum DisplayMode {
      NORMAL,
      SEE_THROUGH,
      POLYGON_OFFSET;
   }

   @Environment(EnvType.CLIENT)
   public interface GlyphVisitor {
      static Font.GlyphVisitor forMultiBufferSource(MultiBufferSource bufferSource, Matrix4f pose, Font.DisplayMode displayMode, int packedLight) {
         return new Font.GlyphVisitor() {
            @Override
            public void acceptGlyph(TextRenderable.Styled glyph) {
               this.render(glyph);
            }

            @Override
            public void acceptEffect(TextRenderable effect) {
               this.render(effect);
            }

            private void render(TextRenderable renderable) {
               VertexConsumer vertexConsumer = bufferSource.getBuffer(renderable.renderType(displayMode));
               renderable.render(pose, vertexConsumer, packedLight, false);
            }
         };
      }

      default void acceptGlyph(TextRenderable.Styled glyph) {
      }

      default void acceptEffect(TextRenderable effect) {
      }

      default void acceptEmptyArea(EmptyArea emptyArea) {
      }
   }

   @Environment(EnvType.CLIENT)
   public interface PreparedText {
      void visit(Font.GlyphVisitor visitor);

      @Nullable
      ScreenRectangle bounds();
   }

   @Environment(EnvType.CLIENT)
   class PreparedTextBuilder implements FormattedCharSink, Font.PreparedText {
      private final boolean drawShadow;
      private final int color;
      private final int backgroundColor;
      private final boolean includeEmpty;
      float x;
      float y;
      private float left = Float.MAX_VALUE;
      private float top = Float.MAX_VALUE;
      private float right = -Float.MAX_VALUE;
      private float bottom = -Float.MAX_VALUE;
      private float backgroundLeft = Float.MAX_VALUE;
      private float backgroundTop = Float.MAX_VALUE;
      private float backgroundRight = -Float.MAX_VALUE;
      private float backgroundBottom = -Float.MAX_VALUE;
      final List<TextRenderable.Styled> glyphs = new ArrayList<>();
      @Nullable
      private List<TextRenderable> effects;
      @Nullable
      private List<EmptyArea> emptyAreas;

      public PreparedTextBuilder(final float x, final float y, final int color, final boolean drawShadow, final boolean includeEmpty) {
         this(x, y, color, 0, drawShadow, includeEmpty);
      }

      public PreparedTextBuilder(final float x, final float y, final int color, final int backgroundColor, final boolean drawShadow, final boolean includeEmpty) {
         this.x = x;
         this.y = y;
         this.drawShadow = drawShadow;
         this.color = color;
         this.backgroundColor = backgroundColor;
         this.includeEmpty = includeEmpty;
         this.markBackground(x, y, 0.0F);
      }

      private void markSize(float left, float top, float right, float bottom) {
         this.left = Math.min(this.left, left);
         this.top = Math.min(this.top, top);
         this.right = Math.max(this.right, right);
         this.bottom = Math.max(this.bottom, bottom);
      }

      private void markBackground(float x, float y, float advance) {
         if (ARGB.alpha(this.backgroundColor) != 0) {
            this.backgroundLeft = Math.min(this.backgroundLeft, x - 1.0F);
            this.backgroundTop = Math.min(this.backgroundTop, y - 1.0F);
            this.backgroundRight = Math.max(this.backgroundRight, x + advance);
            this.backgroundBottom = Math.max(this.backgroundBottom, y + 9.0F);
            this.markSize(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom);
         }
      }

      private void addGlyph(TextRenderable.Styled glyph) {
         this.glyphs.add(glyph);
         this.markSize(glyph.left(), glyph.top(), glyph.right(), glyph.bottom());
      }

      private void addEffect(TextRenderable effect) {
         if (this.effects == null) {
            this.effects = new ArrayList<>();
         }

         this.effects.add(effect);
         this.markSize(effect.left(), effect.top(), effect.right(), effect.bottom());
      }

      private void addEmptyGlyph(EmptyArea emptyArea) {
         if (this.emptyAreas == null) {
            this.emptyAreas = new ArrayList<>();
         }

         this.emptyAreas.add(emptyArea);
      }

      @Override
      public boolean accept(int i, Style style, int j) {
         BakedGlyph bakedGlyph = Font.this.getGlyph(j, style);
         return this.accept(i, style, bakedGlyph);
      }

      public boolean accept(int positionInCurrentSequence, Style style, BakedGlyph glyph) {
         GlyphInfo glyphInfo = glyph.info();
         boolean bl = style.isBold();
         TextColor textColor = style.getColor();
         int i = this.getTextColor(textColor);
         int j = this.getShadowColor(style, i);
         float f = glyphInfo.getAdvance(bl);
         float g = positionInCurrentSequence == 0 ? this.x - 1.0F : this.x;
         float h = glyphInfo.getShadowOffset();
         float k = bl ? glyphInfo.getBoldOffset() : 0.0F;
         TextRenderable.Styled styled = glyph.createGlyph(this.x, this.y, i, j, style, k, h);
         if (styled != null) {
            this.addGlyph(styled);
         } else if (this.includeEmpty) {
            this.addEmptyGlyph(new EmptyArea(this.x, this.y, f, 7.0F, 9.0F, style));
         }

         this.markBackground(this.x, this.y, f);
         if (style.isStrikethrough()) {
            this.addEffect(Font.this.provider.effect().createEffect(g, this.y + 4.5F - 1.0F, this.x + f, this.y + 4.5F, 0.01F, i, j, h));
         }

         if (style.isUnderlined()) {
            this.addEffect(Font.this.provider.effect().createEffect(g, this.y + 9.0F - 1.0F, this.x + f, this.y + 9.0F, 0.01F, i, j, h));
         }

         this.x += f;
         return true;
      }

      @Override
      public void visit(Font.GlyphVisitor visitor) {
         if (ARGB.alpha(this.backgroundColor) != 0) {
            visitor.acceptEffect(
               Font.this.provider
                  .effect()
                  .createEffect(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom, -0.01F, this.backgroundColor, 0, 0.0F)
            );
         }

         for (TextRenderable.Styled styled : this.glyphs) {
            visitor.acceptGlyph(styled);
         }

         if (this.effects != null) {
            for (TextRenderable textRenderable : this.effects) {
               visitor.acceptEffect(textRenderable);
            }
         }

         if (this.emptyAreas != null) {
            for (EmptyArea emptyArea : this.emptyAreas) {
               visitor.acceptEmptyArea(emptyArea);
            }
         }
      }

      private int getTextColor(@Nullable TextColor textColor) {
         if (textColor != null) {
            int i = ARGB.alpha(this.color);
            int j = textColor.getValue();
            return ARGB.color(i, j);
         } else {
            return this.color;
         }
      }

      private int getShadowColor(Style style, int textColor) {
         Integer integer = style.getShadowColor();
         if (integer != null) {
            float f = ARGB.alphaFloat(textColor);
            float g = ARGB.alphaFloat(integer);
            return f != 1.0F ? ARGB.color(ARGB.as8BitChannel(f * g), integer) : integer;
         } else {
            return this.drawShadow ? ARGB.scaleRGB(textColor, 0.25F) : 0;
         }
      }

      @Nullable
      @Override
      public ScreenRectangle bounds() {
         if (!(this.left >= this.right) && !(this.top >= this.bottom)) {
            int i = Mth.floor(this.left);
            int j = Mth.floor(this.top);
            int k = Mth.ceil(this.right);
            int l = Mth.ceil(this.bottom);
            return new ScreenRectangle(i, j, k - i, l - j);
         } else {
            return null;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Provider {
      GlyphSource glyphs(FontDescription fontDescription);

      EffectGlyph effect();
   }
}
