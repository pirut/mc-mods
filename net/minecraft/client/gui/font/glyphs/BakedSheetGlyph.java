package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class BakedSheetGlyph implements BakedGlyph, EffectGlyph {
   public static final float Z_FIGHTER = 0.001F;
   final GlyphInfo info;
   final GlyphRenderTypes renderTypes;
   final GpuTextureView textureView;
   private final float u0;
   private final float u1;
   private final float v0;
   private final float v1;
   private final float left;
   private final float right;
   private final float up;
   private final float down;

   public BakedSheetGlyph(
      GlyphInfo p_440471_,
      GlyphRenderTypes p_440242_,
      GpuTextureView p_439993_,
      float p_440646_,
      float p_439512_,
      float p_438872_,
      float p_439787_,
      float p_440221_,
      float p_440678_,
      float p_439676_,
      float p_440561_
   ) {
      this.info = p_440471_;
      this.renderTypes = p_440242_;
      this.textureView = p_439993_;
      this.u0 = p_440646_;
      this.u1 = p_439512_;
      this.v0 = p_438872_;
      this.v1 = p_439787_;
      this.left = p_440221_;
      this.right = p_440678_;
      this.up = p_439676_;
      this.down = p_440561_;
   }

   float left(BakedSheetGlyph.GlyphInstance p_440544_) {
      return p_440544_.x
         + this.left
         + (p_440544_.style.isItalic() ? Math.min(this.shearTop(), this.shearBottom()) : 0.0F)
         - extraThickness(p_440544_.style.isBold());
   }

   float top(BakedSheetGlyph.GlyphInstance p_440290_) {
      return p_440290_.y + this.up - extraThickness(p_440290_.style.isBold());
   }

   float right(BakedSheetGlyph.GlyphInstance p_439421_) {
      return p_439421_.x
         + this.right
         + (p_439421_.hasShadow() ? p_439421_.shadowOffset : 0.0F)
         + (p_439421_.style.isItalic() ? Math.max(this.shearTop(), this.shearBottom()) : 0.0F)
         + extraThickness(p_439421_.style.isBold());
   }

   float bottom(BakedSheetGlyph.GlyphInstance p_439104_) {
      return p_439104_.y + this.down + (p_439104_.hasShadow() ? p_439104_.shadowOffset : 0.0F) + extraThickness(p_439104_.style.isBold());
   }

   void renderChar(BakedSheetGlyph.GlyphInstance p_440448_, Matrix4f p_440346_, VertexConsumer p_440417_, int p_439312_, boolean p_439149_) {
      Style style = p_440448_.style();
      boolean flag = style.isItalic();
      float f = p_440448_.x();
      float f1 = p_440448_.y();
      int i = p_440448_.color();
      boolean flag1 = style.isBold();
      float f3 = p_439149_ ? 0.0F : 0.001F;
      float f2;
      if (p_440448_.hasShadow()) {
         int j = p_440448_.shadowColor();
         this.render(flag, f + p_440448_.shadowOffset(), f1 + p_440448_.shadowOffset(), 0.0F, p_440346_, p_440417_, j, flag1, p_439312_);
         if (flag1) {
            this.render(
               flag, f + p_440448_.boldOffset() + p_440448_.shadowOffset(), f1 + p_440448_.shadowOffset(), f3, p_440346_, p_440417_, j, true, p_439312_
            );
         }

         f2 = p_439149_ ? 0.0F : 0.03F;
      } else {
         f2 = 0.0F;
      }

      this.render(flag, f, f1, f2, p_440346_, p_440417_, i, flag1, p_439312_);
      if (flag1) {
         this.render(flag, f + p_440448_.boldOffset(), f1, f2 + f3, p_440346_, p_440417_, i, true, p_439312_);
      }
   }

   private void render(
      boolean p_439490_,
      float p_439401_,
      float p_439118_,
      float p_439581_,
      Matrix4f p_440553_,
      VertexConsumer p_439224_,
      int p_439758_,
      boolean p_438982_,
      int p_439714_
   ) {
      float f = p_439401_ + this.left;
      float f1 = p_439401_ + this.right;
      float f2 = p_439118_ + this.up;
      float f3 = p_439118_ + this.down;
      float f4 = p_439490_ ? this.shearTop() : 0.0F;
      float f5 = p_439490_ ? this.shearBottom() : 0.0F;
      float f6 = extraThickness(p_438982_);
      p_439224_.addVertex(p_440553_, f + f4 - f6, f2 - f6, p_439581_).setColor(p_439758_).setUv(this.u0, this.v0).setLight(p_439714_);
      p_439224_.addVertex(p_440553_, f + f5 - f6, f3 + f6, p_439581_).setColor(p_439758_).setUv(this.u0, this.v1).setLight(p_439714_);
      p_439224_.addVertex(p_440553_, f1 + f5 + f6, f3 + f6, p_439581_).setColor(p_439758_).setUv(this.u1, this.v1).setLight(p_439714_);
      p_439224_.addVertex(p_440553_, f1 + f4 + f6, f2 - f6, p_439581_).setColor(p_439758_).setUv(this.u1, this.v0).setLight(p_439714_);
   }

   private static float extraThickness(boolean p_440703_) {
      return p_440703_ ? 0.1F : 0.0F;
   }

   private float shearBottom() {
      return 1.0F - 0.25F * this.down;
   }

   private float shearTop() {
      return 1.0F - 0.25F * this.up;
   }

   void renderEffect(BakedSheetGlyph.EffectInstance p_439295_, Matrix4f p_439049_, VertexConsumer p_439865_, int p_439777_, boolean p_440568_) {
      float f = p_440568_ ? 0.0F : p_439295_.depth;
      if (p_439295_.hasShadow()) {
         this.buildEffect(p_439295_, p_439295_.shadowOffset(), f, p_439295_.shadowColor(), p_439865_, p_439777_, p_439049_);
         f += p_440568_ ? 0.0F : 0.03F;
      }

      this.buildEffect(p_439295_, 0.0F, f, p_439295_.color, p_439865_, p_439777_, p_439049_);
   }

   private void buildEffect(
      BakedSheetGlyph.EffectInstance p_439101_, float p_439607_, float p_440600_, int p_440214_, VertexConsumer p_439927_, int p_438929_, Matrix4f p_439382_
   ) {
      p_439927_.addVertex(p_439382_, p_439101_.x0 + p_439607_, p_439101_.y1 + p_439607_, p_440600_)
         .setColor(p_440214_)
         .setUv(this.u0, this.v0)
         .setLight(p_438929_);
      p_439927_.addVertex(p_439382_, p_439101_.x1 + p_439607_, p_439101_.y1 + p_439607_, p_440600_)
         .setColor(p_440214_)
         .setUv(this.u0, this.v1)
         .setLight(p_438929_);
      p_439927_.addVertex(p_439382_, p_439101_.x1 + p_439607_, p_439101_.y0 + p_439607_, p_440600_)
         .setColor(p_440214_)
         .setUv(this.u1, this.v1)
         .setLight(p_438929_);
      p_439927_.addVertex(p_439382_, p_439101_.x0 + p_439607_, p_439101_.y0 + p_439607_, p_440600_)
         .setColor(p_440214_)
         .setUv(this.u1, this.v0)
         .setLight(p_438929_);
   }

   @Override
   public GlyphInfo info() {
      return this.info;
   }

   @Override
   public TextRenderable.Styled createGlyph(float p_439638_, float p_439877_, int p_440324_, int p_439600_, Style p_440608_, float p_440490_, float p_440025_) {
      return new BakedSheetGlyph.GlyphInstance(p_439638_, p_439877_, p_440324_, p_439600_, this, p_440608_, p_440490_, p_440025_);
   }

   @Override
   public TextRenderable createEffect(
      float p_439937_, float p_439747_, float p_439686_, float p_439982_, float p_440147_, int p_439079_, int p_440306_, float p_440077_
   ) {
      return new BakedSheetGlyph.EffectInstance(this, p_439937_, p_439747_, p_439686_, p_439982_, p_440147_, p_439079_, p_440306_, p_440077_);
   }

   @OnlyIn(Dist.CLIENT)
   record EffectInstance(BakedSheetGlyph glyph, float x0, float y0, float x1, float y1, float depth, int color, int shadowColor, float shadowOffset)
      implements TextRenderable {
      @Override
      public float left() {
         return this.x0;
      }

      @Override
      public float top() {
         return this.y0;
      }

      @Override
      public float right() {
         return this.x1 + (this.hasShadow() ? this.shadowOffset : 0.0F);
      }

      @Override
      public float bottom() {
         return this.y1 + (this.hasShadow() ? this.shadowOffset : 0.0F);
      }

      boolean hasShadow() {
         return this.shadowColor() != 0;
      }

      @Override
      public void render(Matrix4f p_440671_, VertexConsumer p_439075_, int p_439912_, boolean p_439548_) {
         this.glyph.renderEffect(this, p_440671_, p_439075_, p_439912_, false);
      }

      @Override
      public RenderType renderType(Font.DisplayMode p_181388_, boolean blur) {
         return this.glyph.renderTypes.select(p_181388_, blur);
      }

      @Deprecated
      @Override
      public RenderType renderType(Font.DisplayMode p_440152_) {
         return this.glyph.renderTypes.select(p_440152_);
      }

      @Override
      public GpuTextureView textureView() {
         return this.glyph.textureView;
      }

      @Override
      public RenderPipeline guiPipeline() {
         return this.glyph.renderTypes.guiPipeline();
      }
   }

   @OnlyIn(Dist.CLIENT)
   record GlyphInstance(float x, float y, int color, int shadowColor, BakedSheetGlyph glyph, Style style, float boldOffset, float shadowOffset)
      implements TextRenderable.Styled {
      @Override
      public float left() {
         return this.glyph.left(this);
      }

      @Override
      public float top() {
         return this.glyph.top(this);
      }

      @Override
      public float right() {
         return this.glyph.right(this);
      }

      @Override
      public float activeRight() {
         return this.x + this.glyph.info.getAdvance(this.style.isBold());
      }

      @Override
      public float bottom() {
         return this.glyph.bottom(this);
      }

      boolean hasShadow() {
         return this.shadowColor() != 0;
      }

      @Override
      public void render(Matrix4f p_439203_, VertexConsumer p_440403_, int p_439729_, boolean p_439133_) {
         this.glyph.renderChar(this, p_439203_, p_440403_, p_439729_, p_439133_);
      }

      @Override
      public RenderType renderType(Font.DisplayMode p_181388_, boolean blur) {
         return this.glyph.renderTypes.select(p_181388_, blur);
      }

      @Deprecated
      @Override
      public RenderType renderType(Font.DisplayMode p_439720_) {
         return this.glyph.renderTypes.select(p_439720_);
      }

      @Override
      public GpuTextureView textureView() {
         return this.glyph.textureView;
      }

      @Override
      public RenderPipeline guiPipeline() {
         return this.glyph.renderTypes.guiPipeline();
      }
   }
}
