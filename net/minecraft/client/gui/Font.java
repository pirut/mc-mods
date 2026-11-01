package net.minecraft.client.gui;

import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.IFontExtension;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class Font implements IFontExtension {
   private static final float EFFECT_DEPTH = 0.01F;
   private static final float OVER_EFFECT_DEPTH = 0.01F;
   private static final float UNDER_EFFECT_DEPTH = -0.01F;
   public static final float SHADOW_DEPTH = 0.03F;
   public final int lineHeight = 9;
   private final RandomSource random = RandomSource.create();
   final Font.Provider provider;
   private final StringSplitter splitter;
   public boolean enableTextTextureLinearFiltering = false;

   public Font(Font.Provider p_433313_) {
      this.provider = p_433313_;
      this.splitter = new StringSplitter(
         (p_438720_, p_438721_) -> this.getGlyphSource(p_438721_.getFont()).getGlyph(p_438720_).info().getAdvance(p_438721_.isBold())
      );
   }

   private GlyphSource getGlyphSource(FontDescription p_436593_) {
      return this.provider.glyphs(p_436593_);
   }

   public String bidirectionalShaping(String p_92802_) {
      try {
         Bidi bidi = new Bidi(new ArabicShaping(8).shape(p_92802_), 127);
         bidi.setReorderingMode(0);
         return bidi.writeReordered(2);
      } catch (ArabicShapingException var3) {
         return p_92802_;
      }
   }

   public void drawInBatch(
      String p_416062_,
      float p_273006_,
      float p_273254_,
      int p_273375_,
      boolean p_273674_,
      Matrix4f p_273525_,
      MultiBufferSource p_272624_,
      Font.DisplayMode p_273418_,
      int p_273330_,
      int p_272981_
   ) {
      Font.PreparedText font$preparedtext = this.prepareText(p_416062_, p_273006_, p_273254_, p_273375_, p_273674_, p_273330_);
      font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(p_272624_, p_273525_, p_273418_, p_272981_));
   }

   public void drawInBatch(
      Component p_273032_,
      float p_273249_,
      float p_273594_,
      int p_273714_,
      boolean p_273050_,
      Matrix4f p_272974_,
      MultiBufferSource p_273695_,
      Font.DisplayMode p_272782_,
      int p_272603_,
      int p_273632_
   ) {
      Font.PreparedText font$preparedtext = this.prepareText(p_273032_.getVisualOrderText(), p_273249_, p_273594_, p_273714_, p_273050_, false, p_272603_);
      font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(p_273695_, p_272974_, p_272782_, p_273632_));
   }

   public void drawInBatch(
      FormattedCharSequence p_416545_,
      float p_272811_,
      float p_272610_,
      int p_273422_,
      boolean p_273016_,
      Matrix4f p_273443_,
      MultiBufferSource p_273387_,
      Font.DisplayMode p_273551_,
      int p_272706_,
      int p_273114_
   ) {
      Font.PreparedText font$preparedtext = this.prepareText(p_416545_, p_272811_, p_272610_, p_273422_, p_273016_, false, p_272706_);
      font$preparedtext.visit(Font.GlyphVisitor.forMultiBufferSource(p_273387_, p_273443_, p_273551_, p_273114_));
   }

   public void drawInBatch8xOutline(
      FormattedCharSequence p_168646_,
      float p_168647_,
      float p_168648_,
      int p_168649_,
      int p_168650_,
      Matrix4f p_254170_,
      MultiBufferSource p_168652_,
      int p_168653_
   ) {
      Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(0.0F, 0.0F, p_168650_, false, false);

      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            if (i != 0 || j != 0) {
               float[] afloat = new float[]{p_168647_};
               int k = i;
               int l = j;
               p_168646_.accept((p_438717_, p_438718_, p_438719_) -> {
                  boolean flag = p_438718_.isBold();
                  BakedGlyph bakedglyph = this.getGlyph(p_438719_, p_438718_);
                  font$preparedtextbuilder.x = afloat[0] + k * bakedglyph.info().getShadowOffset();
                  font$preparedtextbuilder.y = p_168648_ + l * bakedglyph.info().getShadowOffset();
                  afloat[0] += bakedglyph.info().getAdvance(flag);
                  return font$preparedtextbuilder.accept(p_438717_, p_438718_.withColor(p_168650_), bakedglyph);
               });
            }
         }
      }

      Font.GlyphVisitor font$glyphvisitor = Font.GlyphVisitor.forMultiBufferSource(p_168652_, p_254170_, Font.DisplayMode.NORMAL, p_168653_);

      for (TextRenderable.Styled textrenderable$styled : font$preparedtextbuilder.glyphs) {
         font$glyphvisitor.acceptGlyph(textrenderable$styled);
      }

      Font.PreparedTextBuilder font$preparedtextbuilder1 = new Font.PreparedTextBuilder(p_168647_, p_168648_, p_168649_, false, true);
      p_168646_.accept(font$preparedtextbuilder1);
      font$preparedtextbuilder1.visit(Font.GlyphVisitor.forMultiBufferSource(p_168652_, p_254170_, Font.DisplayMode.POLYGON_OFFSET, p_168653_));
   }

   BakedGlyph getGlyph(int p_435358_, Style p_433792_) {
      GlyphSource glyphsource = this.getGlyphSource(p_433792_.getFont());
      BakedGlyph bakedglyph = glyphsource.getGlyph(p_435358_);
      if (p_433792_.isObfuscated() && p_435358_ != 32) {
         int i = Mth.ceil(bakedglyph.info().getAdvance(false));
         bakedglyph = glyphsource.getRandomGlyph(this.random, i);
      }

      return bakedglyph;
   }

   public Font.PreparedText prepareText(String p_425644_, float p_425748_, float p_425936_, int p_425702_, boolean p_426031_, int p_425535_) {
      if (this.isBidirectional()) {
         p_425644_ = this.bidirectionalShaping(p_425644_);
      }

      Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(p_425748_, p_425936_, p_425702_, p_425535_, p_426031_, false);
      StringDecomposer.iterateFormatted(p_425644_, Style.EMPTY, font$preparedtextbuilder);
      return font$preparedtextbuilder;
   }

   public Font.PreparedText prepareText(
      FormattedCharSequence p_426161_, float p_425846_, float p_425990_, int p_425723_, boolean p_425650_, boolean p_458142_, int p_425946_
   ) {
      Font.PreparedTextBuilder font$preparedtextbuilder = new Font.PreparedTextBuilder(p_425846_, p_425990_, p_425723_, p_425946_, p_425650_, p_458142_);
      p_426161_.accept(font$preparedtextbuilder);
      return font$preparedtextbuilder;
   }

   public int width(String p_92896_) {
      return Mth.ceil(this.splitter.stringWidth(p_92896_));
   }

   public int width(FormattedText p_92853_) {
      return Mth.ceil(this.splitter.stringWidth(p_92853_));
   }

   public int width(FormattedCharSequence p_92725_) {
      return Mth.ceil(this.splitter.stringWidth(p_92725_));
   }

   public String plainSubstrByWidth(String p_92838_, int p_92839_, boolean p_92840_) {
      return p_92840_ ? this.splitter.plainTailByWidth(p_92838_, p_92839_, Style.EMPTY) : this.splitter.plainHeadByWidth(p_92838_, p_92839_, Style.EMPTY);
   }

   public String plainSubstrByWidth(String p_92835_, int p_92836_) {
      return this.splitter.plainHeadByWidth(p_92835_, p_92836_, Style.EMPTY);
   }

   public FormattedText substrByWidth(FormattedText p_92855_, int p_92856_) {
      return this.splitter.headByWidth(p_92855_, p_92856_, Style.EMPTY);
   }

   public int wordWrapHeight(FormattedText p_239134_, int p_239135_) {
      return 9 * this.splitter.splitLines(p_239134_, p_239135_, Style.EMPTY).size();
   }

   public List<FormattedCharSequence> split(FormattedText p_92924_, int p_92925_) {
      return Language.getInstance().getVisualOrder(this.splitter.splitLines(p_92924_, p_92925_, Style.EMPTY));
   }

   public List<FormattedText> splitIgnoringLanguage(FormattedText p_428422_, int p_428207_) {
      return this.splitter.splitLines(p_428422_, p_428207_, Style.EMPTY);
   }

   public boolean isBidirectional() {
      return Language.getInstance().isDefaultRightToLeft();
   }

   public StringSplitter getSplitter() {
      return this.splitter;
   }

   @Override
   public Font self() {
      return this;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum DisplayMode {
      NORMAL,
      SEE_THROUGH,
      POLYGON_OFFSET;
   }

   @OnlyIn(Dist.CLIENT)
   public interface GlyphVisitor {
      static Font.GlyphVisitor forMultiBufferSource(
         final MultiBufferSource p_426098_, final Matrix4f p_425574_, final Font.DisplayMode p_425933_, final int p_426219_
      ) {
         return new Font.GlyphVisitor() {
            @Override
            public void acceptGlyph(TextRenderable.Styled p_457582_) {
               this.render(p_457582_);
            }

            @Override
            public void acceptEffect(TextRenderable p_439031_) {
               this.render(p_439031_);
            }

            private void render(TextRenderable p_440295_) {
               VertexConsumer vertexconsumer = p_426098_.getBuffer(p_440295_.renderType(p_425933_));
               p_440295_.render(p_425574_, vertexconsumer, p_426219_, false);
            }
         };
      }

      default void acceptGlyph(TextRenderable.Styled p_457603_) {
      }

      default void acceptEffect(TextRenderable p_439829_) {
      }

      default void acceptEmptyArea(EmptyArea p_458034_) {
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface PreparedText {
      void visit(Font.GlyphVisitor var1);

      @Nullable
      ScreenRectangle bounds();
   }

   @OnlyIn(Dist.CLIENT)
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

      public PreparedTextBuilder(float p_426112_, float p_425714_, int p_425822_, boolean p_425883_, boolean p_458220_) {
         this(p_426112_, p_425714_, p_425822_, 0, p_425883_, p_458220_);
      }

      public PreparedTextBuilder(float p_426009_, float p_426190_, int p_425840_, int p_457919_, boolean p_425870_, boolean p_457911_) {
         this.x = p_426009_;
         this.y = p_426190_;
         this.drawShadow = p_425870_;
         this.color = p_425840_;
         this.backgroundColor = p_457919_;
         this.includeEmpty = p_457911_;
         this.markBackground(p_426009_, p_426190_, 0.0F);
      }

      private void markSize(float p_426144_, float p_425569_, float p_425910_, float p_425784_) {
         this.left = Math.min(this.left, p_426144_);
         this.top = Math.min(this.top, p_425569_);
         this.right = Math.max(this.right, p_425910_);
         this.bottom = Math.max(this.bottom, p_425784_);
      }

      private void markBackground(float p_425950_, float p_425897_, float p_425529_) {
         if (ARGB.alpha(this.backgroundColor) != 0) {
            this.backgroundLeft = Math.min(this.backgroundLeft, p_425950_ - 1.0F);
            this.backgroundTop = Math.min(this.backgroundTop, p_425897_ - 1.0F);
            this.backgroundRight = Math.max(this.backgroundRight, p_425950_ + p_425529_);
            this.backgroundBottom = Math.max(this.backgroundBottom, p_425897_ + 9.0F);
            this.markSize(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom);
         }
      }

      private void addGlyph(TextRenderable.Styled p_457861_) {
         this.glyphs.add(p_457861_);
         this.markSize(p_457861_.left(), p_457861_.top(), p_457861_.right(), p_457861_.bottom());
      }

      private void addEffect(TextRenderable p_438987_) {
         if (this.effects == null) {
            this.effects = new ArrayList<>();
         }

         this.effects.add(p_438987_);
         this.markSize(p_438987_.left(), p_438987_.top(), p_438987_.right(), p_438987_.bottom());
      }

      private void addEmptyGlyph(EmptyArea p_457600_) {
         if (this.emptyAreas == null) {
            this.emptyAreas = new ArrayList<>();
         }

         this.emptyAreas.add(p_457600_);
      }

      @Override
      public boolean accept(int p_425893_, Style p_426323_, int p_426324_) {
         BakedGlyph bakedglyph = Font.this.getGlyph(p_426324_, p_426323_);
         return this.accept(p_425893_, p_426323_, bakedglyph);
      }

      public boolean accept(int p_435420_, Style p_433864_, BakedGlyph p_440301_) {
         GlyphInfo glyphinfo = p_440301_.info();
         boolean flag = p_433864_.isBold();
         TextColor textcolor = p_433864_.getColor();
         int i = this.getTextColor(textcolor);
         int j = this.getShadowColor(p_433864_, i);
         float f = glyphinfo.getAdvance(flag);
         float f1 = p_435420_ == 0 ? this.x - 1.0F : this.x;
         float f2 = glyphinfo.getShadowOffset();
         float f3 = flag ? glyphinfo.getBoldOffset() : 0.0F;
         TextRenderable.Styled textrenderable$styled = p_440301_.createGlyph(this.x, this.y, i, j, p_433864_, f3, f2);
         if (textrenderable$styled != null) {
            this.addGlyph(textrenderable$styled);
         } else if (this.includeEmpty) {
            this.addEmptyGlyph(new EmptyArea(this.x, this.y, f, 7.0F, 9.0F, p_433864_));
         }

         this.markBackground(this.x, this.y, f);
         if (p_433864_.isStrikethrough()) {
            this.addEffect(Font.this.provider.effect().createEffect(f1, this.y + 4.5F - 1.0F, this.x + f, this.y + 4.5F, 0.01F, i, j, f2));
         }

         if (p_433864_.isUnderlined()) {
            this.addEffect(Font.this.provider.effect().createEffect(f1, this.y + 9.0F - 1.0F, this.x + f, this.y + 9.0F, 0.01F, i, j, f2));
         }

         this.x += f;
         return true;
      }

      @Override
      public void visit(Font.GlyphVisitor p_425967_) {
         if (ARGB.alpha(this.backgroundColor) != 0) {
            p_425967_.acceptEffect(
               Font.this.provider
                  .effect()
                  .createEffect(this.backgroundLeft, this.backgroundTop, this.backgroundRight, this.backgroundBottom, -0.01F, this.backgroundColor, 0, 0.0F)
            );
         }

         for (TextRenderable.Styled textrenderable$styled : this.glyphs) {
            p_425967_.acceptGlyph(textrenderable$styled);
         }

         if (this.effects != null) {
            for (TextRenderable textrenderable : this.effects) {
               p_425967_.acceptEffect(textrenderable);
            }
         }

         if (this.emptyAreas != null) {
            for (EmptyArea emptyarea : this.emptyAreas) {
               p_425967_.acceptEmptyArea(emptyarea);
            }
         }
      }

      private int getTextColor(@Nullable TextColor p_425665_) {
         if (p_425665_ != null) {
            int i = ARGB.alpha(this.color);
            int j = p_425665_.getValue();
            return ARGB.color(i, j);
         } else {
            return this.color;
         }
      }

      private int getShadowColor(Style p_425833_, int p_425604_) {
         Integer integer = p_425833_.getShadowColor();
         if (integer != null) {
            float f = ARGB.alphaFloat(p_425604_);
            float f1 = ARGB.alphaFloat(integer);
            return f != 1.0F ? ARGB.color(ARGB.as8BitChannel(f * f1), integer) : integer;
         } else {
            return this.drawShadow ? ARGB.scaleRGB(p_425604_, 0.25F) : 0;
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

   @OnlyIn(Dist.CLIENT)
   public interface Provider {
      GlyphSource glyphs(FontDescription var1);

      EffectGlyph effect();
   }
}
