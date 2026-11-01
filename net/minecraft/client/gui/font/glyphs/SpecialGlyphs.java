package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.GlyphStitcher;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public enum SpecialGlyphs implements GlyphInfo {
   WHITE(() -> generate(5, 8, (i, j) -> -1)),
   MISSING(() -> {
      int i = 5;
      int j = 8;
      return generate(5, 8, (ix, jx) -> {
         boolean bl = ix == 0 || ix + 1 == 5 || jx == 0 || jx + 1 == 8;
         return bl ? -1 : 0;
      });
   });

   final NativeImage image;

   private static NativeImage generate(int width, int height, SpecialGlyphs.PixelProvider pixelProvider) {
      NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);

      for (int i = 0; i < height; i++) {
         for (int j = 0; j < width; j++) {
            nativeImage.setPixel(j, i, pixelProvider.getColor(j, i));
         }
      }

      nativeImage.untrack();
      return nativeImage;
   }

   private SpecialGlyphs(final Supplier<NativeImage> image) {
      this.image = image.get();
   }

   @Override
   public float getAdvance() {
      return this.image.getWidth() + 1;
   }

   @Nullable
   public BakedSheetGlyph bake(GlyphStitcher stitcher) {
      return stitcher.stitch(
         this,
         new GlyphBitmap() {
            @Override
            public int getPixelWidth() {
               return SpecialGlyphs.this.image.getWidth();
            }

            @Override
            public int getPixelHeight() {
               return SpecialGlyphs.this.image.getHeight();
            }

            @Override
            public float getOversample() {
               return 1.0F;
            }

            @Override
            public void upload(int x, int y, GpuTexture texture) {
               RenderSystem.getDevice()
                  .createCommandEncoder()
                  .writeToTexture(
                     texture, SpecialGlyphs.this.image, 0, 0, x, y, SpecialGlyphs.this.image.getWidth(), SpecialGlyphs.this.image.getHeight(), 0, 0
                  );
            }

            @Override
            public boolean isColored() {
               return true;
            }
         }
      );
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   interface PixelProvider {
      int getColor(int i, int j);
   }
}
