package net.minecraft.client.gui.font;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class PlayerGlyphProvider {
   static final GlyphInfo GLYPH_INFO = GlyphInfo.simple(8.0F);
   final PlayerSkinRenderCache playerSkinRenderCache;
   private final LoadingCache<FontDescription.PlayerSprite, GlyphSource> wrapperCache = CacheBuilder.newBuilder()
      .expireAfterAccess(PlayerSkinRenderCache.CACHE_DURATION)
      .build(new CacheLoader<FontDescription.PlayerSprite, GlyphSource>() {
         public GlyphSource load(FontDescription.PlayerSprite playerSprite) {
            final Supplier<PlayerSkinRenderCache.RenderInfo> supplier = PlayerGlyphProvider.this.playerSkinRenderCache.createLookup(playerSprite.profile());
            final boolean bl = playerSprite.hat();
            return new SingleSpriteSource(new BakedGlyph() {
               @Override
               public GlyphInfo info() {
                  return PlayerGlyphProvider.GLYPH_INFO;
               }

               @Override
               public TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
                  return new PlayerGlyphProvider.Instance(supplier, bl, x, y, color, shadowColor, shadowOffset, style);
               }
            });
         }
      });

   public PlayerGlyphProvider(PlayerSkinRenderCache playerSkinRenderCache) {
      this.playerSkinRenderCache = playerSkinRenderCache;
   }

   public GlyphSource sourceForPlayer(FontDescription.PlayerSprite player) {
      return (GlyphSource)this.wrapperCache.getUnchecked(player);
   }

   @Environment(EnvType.CLIENT)
   record Instance(Supplier<PlayerSkinRenderCache.RenderInfo> skin, boolean hat, float x, float y, int color, int shadowColor, float shadowOffset, Style style)
      implements PlainTextRenderable {
      @Override
      public void renderSprite(Matrix4f pose, VertexConsumer consumer, int packedLight, float x, float y, float z, int color) {
         float f = x + this.left();
         float g = x + this.right();
         float h = y + this.top();
         float i = y + this.bottom();
         renderQuad(pose, consumer, packedLight, f, g, h, i, z, color, 8.0F, 8.0F, 8, 8, 64, 64);
         if (this.hat) {
            renderQuad(pose, consumer, packedLight, f, g, h, i, z, color, 40.0F, 8.0F, 8, 8, 64, 64);
         }
      }

      private static void renderQuad(
         Matrix4f pose,
         VertexConsumer consumer,
         int packedLight,
         float x1,
         float x2,
         float y1,
         float y2,
         float z,
         int color,
         float u,
         float v,
         int width,
         int height,
         int textureWidth,
         int textureHeight
      ) {
         float f = (u + 0.0F) / textureWidth;
         float g = (u + width) / textureWidth;
         float h = (v + 0.0F) / textureHeight;
         float i = (v + height) / textureHeight;
         consumer.addVertex(pose, x1, y1, z).setUv(f, h).setColor(color).setLight(packedLight);
         consumer.addVertex(pose, x1, y2, z).setUv(f, i).setColor(color).setLight(packedLight);
         consumer.addVertex(pose, x2, y2, z).setUv(g, i).setColor(color).setLight(packedLight);
         consumer.addVertex(pose, x2, y1, z).setUv(g, h).setColor(color).setLight(packedLight);
      }

      @Override
      public RenderType renderType(Font.DisplayMode displayMode) {
         return this.skin.get().glyphRenderTypes().select(displayMode);
      }

      @Override
      public RenderPipeline guiPipeline() {
         return this.skin.get().glyphRenderTypes().guiPipeline();
      }

      @Override
      public GpuTextureView textureView() {
         return this.skin.get().textureView();
      }
   }
}
