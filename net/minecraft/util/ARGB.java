package net.minecraft.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ARGB {
   private static final int LINEAR_CHANNEL_DEPTH = 1024;
   private static final short[] SRGB_TO_LINEAR = Util.make(new short[256], p_457842_ -> {
      for (int i = 0; i < p_457842_.length; i++) {
         float f = i / 255.0F;
         p_457842_[i] = (short)Math.round(computeSrgbToLinear(f) * 1023.0F);
      }
   });
   private static final byte[] LINEAR_TO_SRGB = Util.make(new byte[1024], p_457787_ -> {
      for (int i = 0; i < p_457787_.length; i++) {
         float f = i / 1023.0F;
         p_457787_[i] = (byte)Math.round(computeLinearToSrgb(f) * 255.0F);
      }
   });

   private static float computeSrgbToLinear(float p_458100_) {
      return p_458100_ >= 0.04045F ? (float)Math.pow((p_458100_ + 0.055) / 1.055, 2.4) : p_458100_ / 12.92F;
   }

   private static float computeLinearToSrgb(float p_458098_) {
      return p_458098_ >= 0.0031308F ? (float)(1.055 * Math.pow(p_458098_, 0.4166666666666667) - 0.055) : 12.92F * p_458098_;
   }

   public static float srgbToLinearChannel(int p_457809_) {
      return SRGB_TO_LINEAR[p_457809_] / 1023.0F;
   }

   public static int linearToSrgbChannel(float p_458264_) {
      return LINEAR_TO_SRGB[Mth.floor(p_458264_ * 1023.0F)] & 0xFF;
   }

   public static int meanLinear(int p_457569_, int p_457922_, int p_457806_, int p_457575_) {
      return color(
         (alpha(p_457569_) + alpha(p_457922_) + alpha(p_457806_) + alpha(p_457575_)) / 4,
         linearChannelMean(red(p_457569_), red(p_457922_), red(p_457806_), red(p_457575_)),
         linearChannelMean(green(p_457569_), green(p_457922_), green(p_457806_), green(p_457575_)),
         linearChannelMean(blue(p_457569_), blue(p_457922_), blue(p_457806_), blue(p_457575_))
      );
   }

   private static int linearChannelMean(int p_457629_, int p_457788_, int p_458200_, int p_457818_) {
      int i = (SRGB_TO_LINEAR[p_457629_] + SRGB_TO_LINEAR[p_457788_] + SRGB_TO_LINEAR[p_458200_] + SRGB_TO_LINEAR[p_457818_]) / 4;
      return LINEAR_TO_SRGB[i] & 0xFF;
   }

   public static int alpha(int p_360646_) {
      return p_360646_ >>> 24;
   }

   public static int red(int p_365322_) {
      return p_365322_ >> 16 & 0xFF;
   }

   public static int green(int p_364426_) {
      return p_364426_ >> 8 & 0xFF;
   }

   public static int blue(int p_365014_) {
      return p_365014_ & 0xFF;
   }

   public static int color(int p_364075_, int p_361505_, int p_364686_, int p_361326_) {
      return (p_364075_ & 0xFF) << 24 | (p_361505_ & 0xFF) << 16 | (p_364686_ & 0xFF) << 8 | p_361326_ & 0xFF;
   }

   public static int color(int p_364209_, int p_361249_, int p_362935_) {
      return color(255, p_364209_, p_361249_, p_362935_);
   }

   public static int color(Vec3 p_362659_) {
      return color(as8BitChannel((float)p_362659_.x()), as8BitChannel((float)p_362659_.y()), as8BitChannel((float)p_362659_.z()));
   }

   public static int multiply(int p_365204_, int p_362101_) {
      if (p_365204_ == -1) {
         return p_362101_;
      } else {
         return p_362101_ == -1
            ? p_365204_
            : color(
               alpha(p_365204_) * alpha(p_362101_) / 255,
               red(p_365204_) * red(p_362101_) / 255,
               green(p_365204_) * green(p_362101_) / 255,
               blue(p_365204_) * blue(p_362101_) / 255
            );
      }
   }

   public static int addRgb(int p_457560_, int p_457634_) {
      return color(
         alpha(p_457560_),
         Math.min(red(p_457560_) + red(p_457634_), 255),
         Math.min(green(p_457560_) + green(p_457634_), 255),
         Math.min(blue(p_457560_) + blue(p_457634_), 255)
      );
   }

   public static int subtractRgb(int p_457957_, int p_458122_) {
      return color(
         alpha(p_457957_),
         Math.max(red(p_457957_) - red(p_458122_), 0),
         Math.max(green(p_457957_) - green(p_458122_), 0),
         Math.max(blue(p_457957_) - blue(p_458122_), 0)
      );
   }

   public static int multiplyAlpha(int p_457553_, float p_457968_) {
      if (p_457553_ != 0 && !(p_457968_ <= 0.0F)) {
         return p_457968_ >= 1.0F ? p_457553_ : color(alphaFloat(p_457553_) * p_457968_, p_457553_);
      } else {
         return 0;
      }
   }

   public static int scaleRGB(int p_365021_, float p_363883_) {
      return scaleRGB(p_365021_, p_363883_, p_363883_, p_363883_);
   }

   public static int scaleRGB(int p_379627_, float p_379288_, float p_380277_, float p_380015_) {
      return color(
         alpha(p_379627_),
         Math.clamp((long)((int)(red(p_379627_) * p_379288_)), 0, 255),
         Math.clamp((long)((int)(green(p_379627_) * p_380277_)), 0, 255),
         Math.clamp((long)((int)(blue(p_379627_) * p_380015_)), 0, 255)
      );
   }

   public static int scaleRGB(int p_360775_, int p_363230_) {
      return color(
         alpha(p_360775_),
         Math.clamp((long)red(p_360775_) * p_363230_ / 255L, 0, 255),
         Math.clamp((long)green(p_360775_) * p_363230_ / 255L, 0, 255),
         Math.clamp((long)blue(p_360775_) * p_363230_ / 255L, 0, 255)
      );
   }

   public static int greyscale(int p_363419_) {
      int i = (int)(red(p_363419_) * 0.3F + green(p_363419_) * 0.59F + blue(p_363419_) * 0.11F);
      return color(alpha(p_363419_), i, i, i);
   }

   public static int alphaBlend(int p_461075_, int p_460904_) {
      int i = alpha(p_461075_);
      int j = alpha(p_460904_);
      if (j == 255) {
         return p_460904_;
      } else if (j == 0) {
         return p_461075_;
      } else {
         int k = j + i * (255 - j) / 255;
         return color(
            k,
            alphaBlendChannel(k, j, red(p_461075_), red(p_460904_)),
            alphaBlendChannel(k, j, green(p_461075_), green(p_460904_)),
            alphaBlendChannel(k, j, blue(p_461075_), blue(p_460904_))
         );
      }
   }

   private static int alphaBlendChannel(int p_461089_, int p_461137_, int p_460961_, int p_461202_) {
      return (p_461202_ * p_461137_ + p_460961_ * (p_461089_ - p_461137_)) / p_461089_;
   }

   public static int srgbLerp(float p_361174_, int p_362196_, int p_364079_) {
      int i = Mth.lerpInt(p_361174_, alpha(p_362196_), alpha(p_364079_));
      int j = Mth.lerpInt(p_361174_, red(p_362196_), red(p_364079_));
      int k = Mth.lerpInt(p_361174_, green(p_362196_), green(p_364079_));
      int l = Mth.lerpInt(p_361174_, blue(p_362196_), blue(p_364079_));
      return color(i, j, k, l);
   }

   public static int linearLerp(float p_458079_, int p_458201_, int p_457667_) {
      return color(
         Mth.lerpInt(p_458079_, alpha(p_458201_), alpha(p_457667_)),
         LINEAR_TO_SRGB[Mth.lerpInt(p_458079_, SRGB_TO_LINEAR[red(p_458201_)], SRGB_TO_LINEAR[red(p_457667_)])] & 0xFF,
         LINEAR_TO_SRGB[Mth.lerpInt(p_458079_, SRGB_TO_LINEAR[green(p_458201_)], SRGB_TO_LINEAR[green(p_457667_)])] & 0xFF,
         LINEAR_TO_SRGB[Mth.lerpInt(p_458079_, SRGB_TO_LINEAR[blue(p_458201_)], SRGB_TO_LINEAR[blue(p_457667_)])] & 0xFF
      );
   }

   public static int opaque(int p_363279_) {
      return p_363279_ | 0xFF000000;
   }

   public static int transparent(int p_361588_) {
      return p_361588_ & 16777215;
   }

   public static int color(int p_360598_, int p_363344_) {
      return p_360598_ << 24 | p_363344_ & 16777215;
   }

   public static int color(float p_422159_, int p_421556_) {
      return as8BitChannel(p_422159_) << 24 | p_421556_ & 16777215;
   }

   public static int white(float p_361517_) {
      return as8BitChannel(p_361517_) << 24 | 16777215;
   }

   public static int white(int p_457752_) {
      return p_457752_ << 24 | 16777215;
   }

   public static int black(float p_457888_) {
      return as8BitChannel(p_457888_) << 24;
   }

   public static int black(int p_457971_) {
      return p_457971_ << 24;
   }

   public static int colorFromFloat(float p_361263_, float p_361756_, float p_363770_, float p_362317_) {
      return color(as8BitChannel(p_361263_), as8BitChannel(p_361756_), as8BitChannel(p_363770_), as8BitChannel(p_362317_));
   }

   public static Vector3f vector3fFromRGB24(int p_381022_) {
      return new Vector3f(redFloat(p_381022_), greenFloat(p_381022_), blueFloat(p_381022_));
   }

   public static Vector4f vector4fFromARGB32(int p_467543_) {
      return new Vector4f(redFloat(p_467543_), greenFloat(p_467543_), blueFloat(p_467543_), alphaFloat(p_467543_));
   }

   public static int average(int p_363294_, int p_365166_) {
      return color(
         (alpha(p_363294_) + alpha(p_365166_)) / 2,
         (red(p_363294_) + red(p_365166_)) / 2,
         (green(p_363294_) + green(p_365166_)) / 2,
         (blue(p_363294_) + blue(p_365166_)) / 2
      );
   }

   public static int as8BitChannel(float p_362531_) {
      return Mth.floor(p_362531_ * 255.0F);
   }

   public static float alphaFloat(int p_383239_) {
      return from8BitChannel(alpha(p_383239_));
   }

   public static float redFloat(int p_382787_) {
      return from8BitChannel(red(p_382787_));
   }

   public static float greenFloat(int p_382823_) {
      return from8BitChannel(green(p_382823_));
   }

   public static float blueFloat(int p_382858_) {
      return from8BitChannel(blue(p_382858_));
   }

   private static float from8BitChannel(int p_362888_) {
      return p_362888_ / 255.0F;
   }

   public static int toABGR(int p_364127_) {
      return p_364127_ & -16711936 | (p_364127_ & 0xFF0000) >> 16 | (p_364127_ & 0xFF) << 16;
   }

   public static int fromABGR(int p_362222_) {
      return toABGR(p_362222_);
   }

   public static int setBrightness(int p_416733_, float p_415549_) {
      int i = red(p_416733_);
      int j = green(p_416733_);
      int k = blue(p_416733_);
      int l = alpha(p_416733_);
      int i1 = Math.max(Math.max(i, j), k);
      int j1 = Math.min(Math.min(i, j), k);
      float f = i1 - j1;
      float f1;
      if (i1 != 0) {
         f1 = f / i1;
      } else {
         f1 = 0.0F;
      }

      float f2;
      if (f1 == 0.0F) {
         f2 = 0.0F;
      } else {
         float f3 = (i1 - i) / f;
         float f4 = (i1 - j) / f;
         float f5 = (i1 - k) / f;
         if (i == i1) {
            f2 = f5 - f4;
         } else if (j == i1) {
            f2 = 2.0F + f3 - f5;
         } else {
            f2 = 4.0F + f4 - f3;
         }

         f2 /= 6.0F;
         if (f2 < 0.0F) {
            f2++;
         }
      }

      if (f1 == 0.0F) {
         i = j = k = Math.round(p_415549_ * 255.0F);
         return color(l, i, j, k);
      } else {
         float f8 = (f2 - (float)Math.floor(f2)) * 6.0F;
         float f9 = f8 - (float)Math.floor(f8);
         float f10 = p_415549_ * (1.0F - f1);
         float f6 = p_415549_ * (1.0F - f1 * f9);
         float f7 = p_415549_ * (1.0F - f1 * (1.0F - f9));
         switch ((int)f8) {
            case 0:
               i = Math.round(p_415549_ * 255.0F);
               j = Math.round(f7 * 255.0F);
               k = Math.round(f10 * 255.0F);
               break;
            case 1:
               i = Math.round(f6 * 255.0F);
               j = Math.round(p_415549_ * 255.0F);
               k = Math.round(f10 * 255.0F);
               break;
            case 2:
               i = Math.round(f10 * 255.0F);
               j = Math.round(p_415549_ * 255.0F);
               k = Math.round(f7 * 255.0F);
               break;
            case 3:
               i = Math.round(f10 * 255.0F);
               j = Math.round(f6 * 255.0F);
               k = Math.round(p_415549_ * 255.0F);
               break;
            case 4:
               i = Math.round(f7 * 255.0F);
               j = Math.round(f10 * 255.0F);
               k = Math.round(p_415549_ * 255.0F);
               break;
            case 5:
               i = Math.round(p_415549_ * 255.0F);
               j = Math.round(f10 * 255.0F);
               k = Math.round(f6 * 255.0F);
         }

         return color(l, i, j, k);
      }
   }
}
