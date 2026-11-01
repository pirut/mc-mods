package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.UnbakedGlyph;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class EmptyGlyph implements UnbakedGlyph {
   final GlyphInfo info;

   public EmptyGlyph(float width) {
      this.info = GlyphInfo.simple(width);
   }

   @Override
   public GlyphInfo info() {
      return this.info;
   }

   @Override
   public BakedGlyph bake(UnbakedGlyph.Stitcher stitcher) {
      return new BakedGlyph() {
         @Override
         public GlyphInfo info() {
            return EmptyGlyph.this.info;
         }

         @Nullable
         @Override
         public TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
            return null;
         }
      };
   }
}
