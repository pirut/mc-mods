package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class AtlasGlyphProvider {
   static final GlyphInfo GLYPH_INFO = GlyphInfo.simple(8.0F);
   final TextureAtlas atlas;
   final GlyphRenderTypes renderTypes;
   private final GlyphSource missingWrapper;
   private final Map<Identifier, GlyphSource> wrapperCache = new HashMap<>();
   private final Function<Identifier, GlyphSource> spriteResolver;

   public AtlasGlyphProvider(TextureAtlas atlas) {
      this.atlas = atlas;
      this.renderTypes = GlyphRenderTypes.createForColorTexture(atlas.location());
      TextureAtlasSprite textureAtlasSprite = atlas.missingSprite();
      this.missingWrapper = this.createSprite(textureAtlasSprite);
      this.spriteResolver = identifier -> {
         TextureAtlasSprite textureAtlasSprite2 = atlas.getSprite(identifier);
         return textureAtlasSprite2 == textureAtlasSprite ? this.missingWrapper : this.createSprite(textureAtlasSprite2);
      };
   }

   public GlyphSource sourceForSprite(Identifier path) {
      return this.wrapperCache.computeIfAbsent(path, this.spriteResolver);
   }

   private GlyphSource createSprite(TextureAtlasSprite sprite) {
      return new SingleSpriteSource(
         new BakedGlyph() {
            @Override
            public GlyphInfo info() {
               return AtlasGlyphProvider.GLYPH_INFO;
            }

            @Override
            public TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
               return new AtlasGlyphProvider.Instance(
                  AtlasGlyphProvider.this.renderTypes, AtlasGlyphProvider.this.atlas.getTextureView(), sprite, x, y, color, shadowColor, shadowOffset, style
               );
            }
         }
      );
   }

   @Environment(EnvType.CLIENT)
   record Instance(
      GlyphRenderTypes renderTypes,
      GpuTextureView textureView,
      TextureAtlasSprite sprite,
      float x,
      float y,
      int color,
      int shadowColor,
      float shadowOffset,
      Style style
   ) implements PlainTextRenderable {
      @Override
      public void renderSprite(Matrix4f pose, VertexConsumer consumer, int packedLight, float x, float y, float z, int color) {
         float f = x + this.left();
         float g = x + this.right();
         float h = y + this.top();
         float i = y + this.bottom();
         consumer.addVertex(pose, f, h, z).setUv(this.sprite.getU0(), this.sprite.getV0()).setColor(color).setLight(packedLight);
         consumer.addVertex(pose, f, i, z).setUv(this.sprite.getU0(), this.sprite.getV1()).setColor(color).setLight(packedLight);
         consumer.addVertex(pose, g, i, z).setUv(this.sprite.getU1(), this.sprite.getV1()).setColor(color).setLight(packedLight);
         consumer.addVertex(pose, g, h, z).setUv(this.sprite.getU1(), this.sprite.getV0()).setColor(color).setLight(packedLight);
      }

      @Override
      public RenderType renderType(Font.DisplayMode displayMode) {
         return this.renderTypes.select(displayMode);
      }

      @Override
      public RenderPipeline guiPipeline() {
         return this.renderTypes.guiPipeline();
      }
   }
}
