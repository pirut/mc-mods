package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MipmapGenerator {
   private static final String ITEM_PREFIX = "item/";
   private static final float ALPHA_CUTOFF = 0.5F;
   private static final float STRICT_ALPHA_CUTOFF = 0.3F;

   private MipmapGenerator() {
   }

   private static float alphaTestCoverage(NativeImage p_478072_, float p_478061_, float p_478486_) {
      int i = p_478072_.getWidth();
      int j = p_478072_.getHeight();
      float f = 0.0F;
      int k = 4;

      for (int l = 0; l < j - 1; l++) {
         for (int i1 = 0; i1 < i - 1; i1++) {
            float f1 = Math.clamp(ARGB.alphaFloat(p_478072_.getPixel(i1, l)) * p_478486_, 0.0F, 1.0F);
            float f2 = Math.clamp(ARGB.alphaFloat(p_478072_.getPixel(i1 + 1, l)) * p_478486_, 0.0F, 1.0F);
            float f3 = Math.clamp(ARGB.alphaFloat(p_478072_.getPixel(i1, l + 1)) * p_478486_, 0.0F, 1.0F);
            float f4 = Math.clamp(ARGB.alphaFloat(p_478072_.getPixel(i1 + 1, l + 1)) * p_478486_, 0.0F, 1.0F);
            float f5 = 0.0F;

            for (int j1 = 0; j1 < 4; j1++) {
               float f6 = (j1 + 0.5F) / 4.0F;

               for (int k1 = 0; k1 < 4; k1++) {
                  float f7 = (k1 + 0.5F) / 4.0F;
                  float f8 = f1 * (1.0F - f7) * (1.0F - f6) + f2 * f7 * (1.0F - f6) + f3 * (1.0F - f7) * f6 + f4 * f7 * f6;
                  if (f8 > p_478061_) {
                     f5++;
                  }
               }
            }

            f += f5 / 16.0F;
         }
      }

      return f / ((i - 1) * (j - 1));
   }

   private static void scaleAlphaToCoverage(NativeImage p_480597_, float p_481328_, float p_479641_, float p_480704_) {
      float f = 0.0F;
      float f1 = 4.0F;
      float f2 = 1.0F;
      float f3 = 1.0F;
      float f4 = Float.MAX_VALUE;
      int i = p_480597_.getWidth();
      int j = p_480597_.getHeight();

      for (int k = 0; k < 5; k++) {
         float f5 = alphaTestCoverage(p_480597_, p_479641_, f2);
         float f6 = Math.abs(f5 - p_481328_);
         if (f6 < f4) {
            f4 = f6;
            f3 = f2;
         }

         if (f5 < p_481328_) {
            f = f2;
         } else {
            if (!(f5 > p_481328_)) {
               break;
            }

            f1 = f2;
         }

         f2 = (f + f1) * 0.5F;
      }

      for (int l = 0; l < j; l++) {
         for (int i1 = 0; i1 < i; i1++) {
            int j1 = p_480597_.getPixel(i1, l);
            float f7 = ARGB.alphaFloat(j1);
            f7 = f7 * f3 + p_480704_ + 0.025F;
            f7 = Math.clamp(f7, 0.0F, 1.0F);
            p_480597_.setPixel(i1, l, ARGB.color(f7, j1));
         }
      }
   }

   public static NativeImage[] generateMipLevels(Identifier p_467069_, NativeImage[] p_251300_, int p_252326_, MipmapStrategy p_459088_, float p_481022_) {
      if (p_459088_ == MipmapStrategy.AUTO) {
         p_459088_ = hasTransparentPixel(p_251300_[0]) ? MipmapStrategy.CUTOUT : MipmapStrategy.MEAN;
      }

      if (p_251300_.length == 1 && !p_467069_.getPath().startsWith("item/")) {
         if (p_459088_ == MipmapStrategy.CUTOUT || p_459088_ == MipmapStrategy.STRICT_CUTOUT) {
            TextureUtil.solidify(p_251300_[0]);
         } else if (p_459088_ == MipmapStrategy.DARK_CUTOUT) {
            TextureUtil.fillEmptyAreasWithDarkColor(p_251300_[0]);
         }
      }

      if (p_252326_ + 1 <= p_251300_.length) {
         return p_251300_;
      } else {
         NativeImage[] anativeimage = new NativeImage[p_252326_ + 1];
         anativeimage[0] = p_251300_[0];
         boolean flag = p_459088_ == MipmapStrategy.CUTOUT || p_459088_ == MipmapStrategy.STRICT_CUTOUT || p_459088_ == MipmapStrategy.DARK_CUTOUT;
         float f = p_459088_ == MipmapStrategy.STRICT_CUTOUT ? 0.3F : 0.5F;
         float f1 = flag ? alphaTestCoverage(p_251300_[0], f, 1.0F) : 0.0F;

         for (int i = 1; i <= p_252326_; i++) {
            if (i < p_251300_.length) {
               anativeimage[i] = p_251300_[i];
            } else {
               NativeImage nativeimage = anativeimage[i - 1];
               NativeImage nativeimage1 = new NativeImage(nativeimage.getWidth() >> 1, nativeimage.getHeight() >> 1, false);
               int j = nativeimage1.getWidth();
               int k = nativeimage1.getHeight();

               for (int l = 0; l < j; l++) {
                  for (int i1 = 0; i1 < k; i1++) {
                     int j1 = nativeimage.getPixel(l * 2 + 0, i1 * 2 + 0);
                     int k1 = nativeimage.getPixel(l * 2 + 1, i1 * 2 + 0);
                     int l1 = nativeimage.getPixel(l * 2 + 0, i1 * 2 + 1);
                     int i2 = nativeimage.getPixel(l * 2 + 1, i1 * 2 + 1);
                     int j2;
                     if (p_459088_ == MipmapStrategy.DARK_CUTOUT) {
                        j2 = darkenedAlphaBlend(j1, k1, l1, i2);
                     } else {
                        j2 = ARGB.meanLinear(j1, k1, l1, i2);
                     }

                     nativeimage1.setPixel(l, i1, j2);
                  }
               }

               anativeimage[i] = nativeimage1;
            }

            if (flag) {
               scaleAlphaToCoverage(anativeimage[i], f1, f, p_481022_);
            }
         }

         return anativeimage;
      }
   }

   private static boolean hasTransparentPixel(NativeImage p_252279_) {
      for (int i = 0; i < p_252279_.getWidth(); i++) {
         for (int j = 0; j < p_252279_.getHeight(); j++) {
            if (ARGB.alpha(p_252279_.getPixel(i, j)) == 0) {
               return true;
            }
         }
      }

      return false;
   }

   private static int darkenedAlphaBlend(int p_455002_, int p_454995_, int p_455537_, int p_455647_) {
      float f = 0.0F;
      float f1 = 0.0F;
      float f2 = 0.0F;
      float f3 = 0.0F;
      if (ARGB.alpha(p_455002_) != 0) {
         f += ARGB.srgbToLinearChannel(ARGB.alpha(p_455002_));
         f1 += ARGB.srgbToLinearChannel(ARGB.red(p_455002_));
         f2 += ARGB.srgbToLinearChannel(ARGB.green(p_455002_));
         f3 += ARGB.srgbToLinearChannel(ARGB.blue(p_455002_));
      }

      if (ARGB.alpha(p_454995_) != 0) {
         f += ARGB.srgbToLinearChannel(ARGB.alpha(p_454995_));
         f1 += ARGB.srgbToLinearChannel(ARGB.red(p_454995_));
         f2 += ARGB.srgbToLinearChannel(ARGB.green(p_454995_));
         f3 += ARGB.srgbToLinearChannel(ARGB.blue(p_454995_));
      }

      if (ARGB.alpha(p_455537_) != 0) {
         f += ARGB.srgbToLinearChannel(ARGB.alpha(p_455537_));
         f1 += ARGB.srgbToLinearChannel(ARGB.red(p_455537_));
         f2 += ARGB.srgbToLinearChannel(ARGB.green(p_455537_));
         f3 += ARGB.srgbToLinearChannel(ARGB.blue(p_455537_));
      }

      if (ARGB.alpha(p_455647_) != 0) {
         f += ARGB.srgbToLinearChannel(ARGB.alpha(p_455647_));
         f1 += ARGB.srgbToLinearChannel(ARGB.red(p_455647_));
         f2 += ARGB.srgbToLinearChannel(ARGB.green(p_455647_));
         f3 += ARGB.srgbToLinearChannel(ARGB.blue(p_455647_));
      }

      f /= 4.0F;
      f1 /= 4.0F;
      f2 /= 4.0F;
      f3 /= 4.0F;
      return ARGB.color(ARGB.linearToSrgbChannel(f), ARGB.linearToSrgbChannel(f1), ARGB.linearToSrgbChannel(f2), ARGB.linearToSrgbChannel(f3));
   }
}
