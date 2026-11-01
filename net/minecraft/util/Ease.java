package net.minecraft.util;

public class Ease {
   public static float inBack(float p_469597_) {
      float f = 1.70158F;
      float f1 = 2.70158F;
      return Mth.square(p_469597_) * (2.70158F * p_469597_ - 1.70158F);
   }

   public static float inBounce(float p_466860_) {
      return 1.0F - outBounce(1.0F - p_466860_);
   }

   public static float inCubic(float p_469403_) {
      return Mth.cube(p_469403_);
   }

   public static float inElastic(float p_469147_) {
      if (p_469147_ == 0.0F) {
         return 0.0F;
      } else if (p_469147_ == 1.0F) {
         return 1.0F;
      } else {
         float f = (float) (Math.PI * 2.0 / 3.0);
         return (float)(-Math.pow(2.0, 10.0 * p_469147_ - 10.0) * Math.sin((p_469147_ * 10.0 - 10.75) * (float) (Math.PI * 2.0 / 3.0)));
      }
   }

   public static float inExpo(float p_468094_) {
      return p_468094_ == 0.0F ? 0.0F : (float)Math.pow(2.0, 10.0 * p_468094_ - 10.0);
   }

   public static float inQuart(float p_468744_) {
      return Mth.square(Mth.square(p_468744_));
   }

   public static float inQuint(float p_467880_) {
      return Mth.square(Mth.square(p_467880_)) * p_467880_;
   }

   public static float inSine(float p_468287_) {
      return 1.0F - Mth.cos(p_468287_ * (float) (Math.PI / 2));
   }

   public static float inOutBounce(float p_467185_) {
      return p_467185_ < 0.5F ? (1.0F - outBounce(1.0F - 2.0F * p_467185_)) / 2.0F : (1.0F + outBounce(2.0F * p_467185_ - 1.0F)) / 2.0F;
   }

   public static float inOutCirc(float p_466900_) {
      return p_466900_ < 0.5F
         ? (float)((1.0 - Math.sqrt(1.0 - Math.pow(2.0 * p_466900_, 2.0))) / 2.0)
         : (float)((Math.sqrt(1.0 - Math.pow(-2.0 * p_466900_ + 2.0, 2.0)) + 1.0) / 2.0);
   }

   public static float inOutCubic(float p_468253_) {
      return p_468253_ < 0.5F ? 4.0F * Mth.cube(p_468253_) : (float)(1.0 - Math.pow(-2.0 * p_468253_ + 2.0, 3.0) / 2.0);
   }

   public static float inOutQuad(float p_468083_) {
      return p_468083_ < 0.5F ? 2.0F * Mth.square(p_468083_) : (float)(1.0 - Math.pow(-2.0 * p_468083_ + 2.0, 2.0) / 2.0);
   }

   public static float inOutQuart(float p_467721_) {
      return p_467721_ < 0.5F ? 8.0F * Mth.square(Mth.square(p_467721_)) : (float)(1.0 - Math.pow(-2.0 * p_467721_ + 2.0, 4.0) / 2.0);
   }

   public static float inOutQuint(float p_469501_) {
      return p_469501_ < 0.5 ? 16.0F * p_469501_ * p_469501_ * p_469501_ * p_469501_ * p_469501_ : (float)(1.0 - Math.pow(-2.0 * p_469501_ + 2.0, 5.0) / 2.0);
   }

   public static float outBounce(float p_468344_) {
      float f = 7.5625F;
      float f1 = 2.75F;
      if (p_468344_ < 0.36363637F) {
         return 7.5625F * Mth.square(p_468344_);
      } else if (p_468344_ < 0.72727275F) {
         return 7.5625F * Mth.square(p_468344_ - 0.54545456F) + 0.75F;
      } else {
         return p_468344_ < 0.9090909090909091
            ? 7.5625F * Mth.square(p_468344_ - 0.8181818F) + 0.9375F
            : 7.5625F * Mth.square(p_468344_ - 0.95454544F) + 0.984375F;
      }
   }

   public static float outElastic(float p_468347_) {
      float f = (float) (Math.PI * 2.0 / 3.0);
      if (p_468347_ == 0.0F) {
         return 0.0F;
      } else {
         return p_468347_ == 1.0F
            ? 1.0F
            : (float)(Math.pow(2.0, -10.0 * p_468347_) * Math.sin((p_468347_ * 10.0 - 0.75) * (float) (Math.PI * 2.0 / 3.0)) + 1.0);
      }
   }

   public static float outExpo(float p_467706_) {
      return p_467706_ == 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0 * p_467706_);
   }

   public static float outQuad(float p_469763_) {
      return 1.0F - Mth.square(1.0F - p_469763_);
   }

   public static float outQuint(float p_469797_) {
      return 1.0F - (float)Math.pow(1.0 - p_469797_, 5.0);
   }

   public static float outSine(float p_467764_) {
      return Mth.sin(p_467764_ * (float) (Math.PI / 2));
   }

   public static float inOutSine(float p_455467_) {
      return -(Mth.cos((float) Math.PI * p_455467_) - 1.0F) / 2.0F;
   }

   public static float outBack(float p_456100_) {
      float f = 1.70158F;
      float f1 = 2.70158F;
      return 1.0F + 2.70158F * Mth.cube(p_456100_ - 1.0F) + 1.70158F * Mth.square(p_456100_ - 1.0F);
   }

   public static float outQuart(float p_454850_) {
      return 1.0F - Mth.square(Mth.square(1.0F - p_454850_));
   }

   public static float outCubic(float p_455575_) {
      return 1.0F - Mth.cube(1.0F - p_455575_);
   }

   public static float inOutExpo(float p_455426_) {
      if (p_455426_ < 0.5F) {
         return p_455426_ == 0.0F ? 0.0F : (float)(Math.pow(2.0, 20.0 * p_455426_ - 10.0) / 2.0);
      } else {
         return p_455426_ == 1.0F ? 1.0F : (float)((2.0 - Math.pow(2.0, -20.0 * p_455426_ + 10.0)) / 2.0);
      }
   }

   public static float inQuad(float p_455543_) {
      return p_455543_ * p_455543_;
   }

   public static float outCirc(float p_455350_) {
      return (float)Math.sqrt(1.0F - Mth.square(p_455350_ - 1.0F));
   }

   public static float inOutElastic(float p_455853_) {
      float f = (float) Math.PI * 4.0F / 9.0F;
      if (p_455853_ == 0.0F) {
         return 0.0F;
      } else if (p_455853_ == 1.0F) {
         return 1.0F;
      } else {
         double d0 = Math.sin((20.0 * p_455853_ - 11.125) * (float) Math.PI * 4.0 / 9.0);
         return p_455853_ < 0.5F
            ? (float)(-(Math.pow(2.0, 20.0 * p_455853_ - 10.0) * d0) / 2.0)
            : (float)(Math.pow(2.0, -20.0 * p_455853_ + 10.0) * d0 / 2.0 + 1.0);
      }
   }

   public static float inCirc(float p_454924_) {
      return (float)(-Math.sqrt(1.0F - p_454924_ * p_454924_)) + 1.0F;
   }

   public static float inOutBack(float p_456133_) {
      float f = 1.70158F;
      float f1 = 2.5949094F;
      if (p_456133_ < 0.5F) {
         return 4.0F * p_456133_ * p_456133_ * (7.189819F * p_456133_ - 2.5949094F) / 2.0F;
      } else {
         float f2 = 2.0F * p_456133_ - 2.0F;
         return (f2 * f2 * (3.5949094F * f2 + 2.5949094F) + 2.0F) / 2.0F;
      }
   }
}
