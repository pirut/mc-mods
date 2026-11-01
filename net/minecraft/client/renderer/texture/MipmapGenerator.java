package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

@Environment(EnvType.CLIENT)
public class MipmapGenerator {
   private static final String ITEM_PREFIX = "item/";
   private static final float ALPHA_CUTOFF = 0.5F;
   private static final float STRICT_ALPHA_CUTOFF = 0.3F;

   private MipmapGenerator() {
   }

   private static float alphaTestCoverage(NativeImage image, float alphaRef, float alphaScale) {
      int i = image.getWidth();
      int j = image.getHeight();
      float f = 0.0F;
      int k = 4;

      for (int l = 0; l < j - 1; l++) {
         for (int m = 0; m < i - 1; m++) {
            float g = Math.clamp(ARGB.alphaFloat(image.getPixel(m, l)) * alphaScale, 0.0F, 1.0F);
            float h = Math.clamp(ARGB.alphaFloat(image.getPixel(m + 1, l)) * alphaScale, 0.0F, 1.0F);
            float n = Math.clamp(ARGB.alphaFloat(image.getPixel(m, l + 1)) * alphaScale, 0.0F, 1.0F);
            float o = Math.clamp(ARGB.alphaFloat(image.getPixel(m + 1, l + 1)) * alphaScale, 0.0F, 1.0F);
            float p = 0.0F;

            for (int q = 0; q < 4; q++) {
               float r = (q + 0.5F) / 4.0F;

               for (int s = 0; s < 4; s++) {
                  float t = (s + 0.5F) / 4.0F;
                  float u = g * (1.0F - t) * (1.0F - r) + h * t * (1.0F - r) + n * (1.0F - t) * r + o * t * r;
                  if (u > alphaRef) {
                     p++;
                  }
               }
            }

            f += p / 16.0F;
         }
      }

      return f / ((i - 1) * (j - 1));
   }

   private static void scaleAlphaToCoverage(NativeImage image, float desiredCoverage, float alphaRef, float alphaCutoffBias) {
      float f = 0.0F;
      float g = 4.0F;
      float h = 1.0F;
      float i = 1.0F;
      float j = Float.MAX_VALUE;
      int k = image.getWidth();
      int l = image.getHeight();

      for (int m = 0; m < 5; m++) {
         float n = alphaTestCoverage(image, alphaRef, h);
         float o = Math.abs(n - desiredCoverage);
         if (o < j) {
            j = o;
            i = h;
         }

         if (n < desiredCoverage) {
            f = h;
         } else {
            if (!(n > desiredCoverage)) {
               break;
            }

            g = h;
         }

         h = (f + g) * 0.5F;
      }

      for (int m = 0; m < l; m++) {
         for (int p = 0; p < k; p++) {
            int q = image.getPixel(p, m);
            float r = ARGB.alphaFloat(q);
            r = r * i + alphaCutoffBias + 0.025F;
            r = Math.clamp(r, 0.0F, 1.0F);
            image.setPixel(p, m, ARGB.color(r, q));
         }
      }
   }

   public static NativeImage[] generateMipLevels(Identifier texture, NativeImage[] mipLevels, int mipLevel, MipmapStrategy strategy, float alphaCutoffBias) {
      if (strategy == MipmapStrategy.AUTO) {
         strategy = hasTransparentPixel(mipLevels[0]) ? MipmapStrategy.CUTOUT : MipmapStrategy.MEAN;
      }

      if (mipLevels.length == 1 && !texture.getPath().startsWith("item/")) {
         if (strategy == MipmapStrategy.CUTOUT || strategy == MipmapStrategy.STRICT_CUTOUT) {
            TextureUtil.solidify(mipLevels[0]);
         } else if (strategy == MipmapStrategy.DARK_CUTOUT) {
            TextureUtil.fillEmptyAreasWithDarkColor(mipLevels[0]);
         }
      }

      if (mipLevel + 1 <= mipLevels.length) {
         return mipLevels;
      } else {
         NativeImage[] nativeImages = new NativeImage[mipLevel + 1];
         nativeImages[0] = mipLevels[0];
         boolean bl = strategy == MipmapStrategy.CUTOUT || strategy == MipmapStrategy.STRICT_CUTOUT || strategy == MipmapStrategy.DARK_CUTOUT;
         float f = strategy == MipmapStrategy.STRICT_CUTOUT ? 0.3F : 0.5F;
         float g = bl ? alphaTestCoverage(mipLevels[0], f, 1.0F) : 0.0F;

         for (int i = 1; i <= mipLevel; i++) {
            if (i < mipLevels.length) {
               nativeImages[i] = mipLevels[i];
            } else {
               NativeImage nativeImage = nativeImages[i - 1];
               NativeImage nativeImage2 = new NativeImage(nativeImage.getWidth() >> 1, nativeImage.getHeight() >> 1, false);
               int j = nativeImage2.getWidth();
               int k = nativeImage2.getHeight();

               for (int l = 0; l < j; l++) {
                  for (int m = 0; m < k; m++) {
                     int n = nativeImage.getPixel(l * 2 + 0, m * 2 + 0);
                     int o = nativeImage.getPixel(l * 2 + 1, m * 2 + 0);
                     int p = nativeImage.getPixel(l * 2 + 0, m * 2 + 1);
                     int q = nativeImage.getPixel(l * 2 + 1, m * 2 + 1);
                     int r;
                     if (strategy == MipmapStrategy.DARK_CUTOUT) {
                        r = darkenedAlphaBlend(n, o, p, q);
                     } else {
                        r = ARGB.meanLinear(n, o, p, q);
                     }

                     nativeImage2.setPixel(l, m, r);
                  }
               }

               nativeImages[i] = nativeImage2;
            }

            if (bl) {
               scaleAlphaToCoverage(nativeImages[i], g, f, alphaCutoffBias);
            }
         }

         return nativeImages;
      }
   }

   private static boolean hasTransparentPixel(NativeImage image) {
      for (int i = 0; i < image.getWidth(); i++) {
         for (int j = 0; j < image.getHeight(); j++) {
            if (ARGB.alpha(image.getPixel(i, j)) == 0) {
               return true;
            }
         }
      }

      return false;
   }

   private static int darkenedAlphaBlend(int col0, int col1, int col2, int col3) {
      float f = 0.0F;
      float g = 0.0F;
      float h = 0.0F;
      float i = 0.0F;
      if (ARGB.alpha(col0) != 0) {
         f += ARGB.srgbToLinearChannel(ARGB.alpha(col0));
         g += ARGB.srgbToLinearChannel(ARGB.red(col0));
         h += ARGB.srgbToLinearChannel(ARGB.green(col0));
         i += ARGB.srgbToLinearChannel(ARGB.blue(col0));
      }

      if (ARGB.alpha(col1) != 0) {
         f += ARGB.srgbToLinearChannel(ARGB.alpha(col1));
         g += ARGB.srgbToLinearChannel(ARGB.red(col1));
         h += ARGB.srgbToLinearChannel(ARGB.green(col1));
         i += ARGB.srgbToLinearChannel(ARGB.blue(col1));
      }

      if (ARGB.alpha(col2) != 0) {
         f += ARGB.srgbToLinearChannel(ARGB.alpha(col2));
         g += ARGB.srgbToLinearChannel(ARGB.red(col2));
         h += ARGB.srgbToLinearChannel(ARGB.green(col2));
         i += ARGB.srgbToLinearChannel(ARGB.blue(col2));
      }

      if (ARGB.alpha(col3) != 0) {
         f += ARGB.srgbToLinearChannel(ARGB.alpha(col3));
         g += ARGB.srgbToLinearChannel(ARGB.red(col3));
         h += ARGB.srgbToLinearChannel(ARGB.green(col3));
         i += ARGB.srgbToLinearChannel(ARGB.blue(col3));
      }

      f /= 4.0F;
      g /= 4.0F;
      h /= 4.0F;
      i /= 4.0F;
      return ARGB.color(ARGB.linearToSrgbChannel(f), ARGB.linearToSrgbChannel(g), ARGB.linearToSrgbChannel(h), ARGB.linearToSrgbChannel(i));
   }
}
