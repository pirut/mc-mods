package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
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
      GlyphInfo info,
      GlyphRenderTypes renderTypes,
      GpuTextureView textureView,
      float u0,
      float u1,
      float v0,
      float v1,
      float left,
      float right,
      float up,
      float down
   ) {
      this.info = info;
      this.renderTypes = renderTypes;
      this.textureView = textureView;
      this.u0 = u0;
      this.u1 = u1;
      this.v0 = v0;
      this.v1 = v1;
      this.left = left;
      this.right = right;
      this.up = up;
      this.down = down;
   }

   float left(BakedSheetGlyph.GlyphInstance glyph) {
      return glyph.x + this.left + (glyph.style.isItalic() ? Math.min(this.shearTop(), this.shearBottom()) : 0.0F) - extraThickness(glyph.style.isBold());
   }

   float top(BakedSheetGlyph.GlyphInstance glyph) {
      return glyph.y + this.up - extraThickness(glyph.style.isBold());
   }

   float right(BakedSheetGlyph.GlyphInstance glyph) {
      return glyph.x
         + this.right
         + (glyph.hasShadow() ? glyph.shadowOffset : 0.0F)
         + (glyph.style.isItalic() ? Math.max(this.shearTop(), this.shearBottom()) : 0.0F)
         + extraThickness(glyph.style.isBold());
   }

   float bottom(BakedSheetGlyph.GlyphInstance glyph) {
      return glyph.y + this.down + (glyph.hasShadow() ? glyph.shadowOffset : 0.0F) + extraThickness(glyph.style.isBold());
   }

   void renderChar(BakedSheetGlyph.GlyphInstance glyph, Matrix4f pose, VertexConsumer consumer, int packedLight, boolean noDepth) {
      Style style = glyph.style();
      boolean bl = style.isItalic();
      float f = glyph.x();
      float g = glyph.y();
      int i = glyph.color();
      boolean bl2 = style.isBold();
      float h = noDepth ? 0.0F : 0.001F;
      float k;
      if (glyph.hasShadow()) {
         int j = glyph.shadowColor();
         this.render(bl, f + glyph.shadowOffset(), g + glyph.shadowOffset(), 0.0F, pose, consumer, j, bl2, packedLight);
         if (bl2) {
            this.render(bl, f + glyph.boldOffset() + glyph.shadowOffset(), g + glyph.shadowOffset(), h, pose, consumer, j, true, packedLight);
         }

         k = noDepth ? 0.0F : 0.03F;
      } else {
         k = 0.0F;
      }

      this.render(bl, f, g, k, pose, consumer, i, bl2, packedLight);
      if (bl2) {
         this.render(bl, f + glyph.boldOffset(), g, k + h, pose, consumer, i, true, packedLight);
      }
   }

   private void render(boolean italic, float x, float y, float z, Matrix4f pose, VertexConsumer consumer, int color, boolean bold, int packedLight) {
      float f = x + this.left;
      float g = x + this.right;
      float h = y + this.up;
      float i = y + this.down;
      float j = italic ? this.shearTop() : 0.0F;
      float k = italic ? this.shearBottom() : 0.0F;
      float l = extraThickness(bold);
      consumer.addVertex(pose, f + j - l, h - l, z).setColor(color).setUv(this.u0, this.v0).setLight(packedLight);
      consumer.addVertex(pose, f + k - l, i + l, z).setColor(color).setUv(this.u0, this.v1).setLight(packedLight);
      consumer.addVertex(pose, g + k + l, i + l, z).setColor(color).setUv(this.u1, this.v1).setLight(packedLight);
      consumer.addVertex(pose, g + j + l, h - l, z).setColor(color).setUv(this.u1, this.v0).setLight(packedLight);
   }

   private static float extraThickness(boolean bold) {
      return bold ? 0.1F : 0.0F;
   }

   private float shearBottom() {
      return 1.0F - 0.25F * this.down;
   }

   private float shearTop() {
      return 1.0F - 0.25F * this.up;
   }

   void renderEffect(BakedSheetGlyph.EffectInstance effect, Matrix4f pose, VertexConsumer consumer, int packedLight, boolean noDepth) {
      float f = noDepth ? 0.0F : effect.depth;
      if (effect.hasShadow()) {
         this.buildEffect(effect, effect.shadowOffset(), f, effect.shadowColor(), consumer, packedLight, pose);
         f += noDepth ? 0.0F : 0.03F;
      }

      this.buildEffect(effect, 0.0F, f, effect.color, consumer, packedLight, pose);
   }

   private void buildEffect(
      BakedSheetGlyph.EffectInstance effect, float shadowOffset, float depthOffset, int shadowColor, VertexConsumer consumer, int packedLight, Matrix4f pose
   ) {
      consumer.addVertex(pose, effect.x0 + shadowOffset, effect.y1 + shadowOffset, depthOffset)
         .setColor(shadowColor)
         .setUv(this.u0, this.v0)
         .setLight(packedLight);
      consumer.addVertex(pose, effect.x1 + shadowOffset, effect.y1 + shadowOffset, depthOffset)
         .setColor(shadowColor)
         .setUv(this.u0, this.v1)
         .setLight(packedLight);
      consumer.addVertex(pose, effect.x1 + shadowOffset, effect.y0 + shadowOffset, depthOffset)
         .setColor(shadowColor)
         .setUv(this.u1, this.v1)
         .setLight(packedLight);
      consumer.addVertex(pose, effect.x0 + shadowOffset, effect.y0 + shadowOffset, depthOffset)
         .setColor(shadowColor)
         .setUv(this.u1, this.v0)
         .setLight(packedLight);
   }

   @Override
   public GlyphInfo info() {
      return this.info;
   }

   @Override
   public TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
      return new BakedSheetGlyph.GlyphInstance(x, y, color, shadowColor, this, style, boldOffset, shadowOffset);
   }

   @Override
   public TextRenderable createEffect(float x0, float y0, float x1, float y1, float depth, int color, int shadowColor, float shadowOffset) {
      return new BakedSheetGlyph.EffectInstance(this, x0, y0, x1, y1, depth, color, shadowColor, shadowOffset);
   }

   @Environment(EnvType.CLIENT)
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
      public void render(Matrix4f pose, VertexConsumer consumer, int packedLight, boolean noDepth) {
         this.glyph.renderEffect(this, pose, consumer, packedLight, false);
      }

      @Override
      public RenderType renderType(Font.DisplayMode displayMode) {
         return this.glyph.renderTypes.select(displayMode);
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

   @Environment(EnvType.CLIENT)
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
      public void render(Matrix4f pose, VertexConsumer consumer, int packedLight, boolean noDepth) {
         this.glyph.renderChar(this, pose, consumer, packedLight, noDepth);
      }

      @Override
      public RenderType renderType(Font.DisplayMode displayMode) {
         return this.glyph.renderTypes.select(displayMode);
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
