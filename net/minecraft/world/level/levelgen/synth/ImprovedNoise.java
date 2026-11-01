package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class ImprovedNoise {
   private static final float SHIFT_UP_EPSILON = 1.0E-7F;
   private final byte[] p;
   public final double xo;
   public final double yo;
   public final double zo;

   public ImprovedNoise(RandomSource p_230499_) {
      this.xo = p_230499_.nextDouble() * 256.0;
      this.yo = p_230499_.nextDouble() * 256.0;
      this.zo = p_230499_.nextDouble() * 256.0;
      this.p = new byte[256];

      for (int $$1 = 0; $$1 < 256; $$1++) {
         this.p[$$1] = (byte)$$1;
      }

      for (int $$2 = 0; $$2 < 256; $$2++) {
         int $$3 = p_230499_.nextInt(256 - $$2);
         byte $$4 = this.p[$$2];
         this.p[$$2] = this.p[$$2 + $$3];
         this.p[$$2 + $$3] = $$4;
      }
   }

   public double noise(double p_164309_, double p_164310_, double p_164311_) {
      return this.noise(p_164309_, p_164310_, p_164311_, 0.0, 0.0);
   }

   @Deprecated
   public double noise(double p_75328_, double p_75329_, double p_75330_, double p_75331_, double p_75332_) {
      double $$5 = p_75328_ + this.xo;
      double $$6 = p_75329_ + this.yo;
      double $$7 = p_75330_ + this.zo;
      int $$8 = Mth.floor($$5);
      int $$9 = Mth.floor($$6);
      int $$10 = Mth.floor($$7);
      double $$11 = $$5 - $$8;
      double $$12 = $$6 - $$9;
      double $$13 = $$7 - $$10;
      double $$16;
      if (p_75331_ != 0.0) {
         double $$14;
         if (p_75332_ >= 0.0 && p_75332_ < $$12) {
            $$14 = p_75332_;
         } else {
            $$14 = $$12;
         }

         $$16 = Mth.floor($$14 / p_75331_ + 1.0E-7F) * p_75331_;
      } else {
         $$16 = 0.0;
      }

      return this.sampleAndLerp($$8, $$9, $$10, $$11, $$12 - $$16, $$13, $$12);
   }

   public double noiseWithDerivative(double p_164313_, double p_164314_, double p_164315_, double[] p_164316_) {
      double $$4 = p_164313_ + this.xo;
      double $$5 = p_164314_ + this.yo;
      double $$6 = p_164315_ + this.zo;
      int $$7 = Mth.floor($$4);
      int $$8 = Mth.floor($$5);
      int $$9 = Mth.floor($$6);
      double $$10 = $$4 - $$7;
      double $$11 = $$5 - $$8;
      double $$12 = $$6 - $$9;
      return this.sampleWithDerivative($$7, $$8, $$9, $$10, $$11, $$12, p_164316_);
   }

   private static double gradDot(int p_75336_, double p_75337_, double p_75338_, double p_75339_) {
      return SimplexNoise.dot(SimplexNoise.GRADIENT[p_75336_ & 15], p_75337_, p_75338_, p_75339_);
   }

   private int p(int p_75334_) {
      return this.p[p_75334_ & 0xFF] & 0xFF;
   }

   private double sampleAndLerp(int p_164318_, int p_164319_, int p_164320_, double p_164321_, double p_164322_, double p_164323_, double p_164324_) {
      int $$7 = this.p(p_164318_);
      int $$8 = this.p(p_164318_ + 1);
      int $$9 = this.p($$7 + p_164319_);
      int $$10 = this.p($$7 + p_164319_ + 1);
      int $$11 = this.p($$8 + p_164319_);
      int $$12 = this.p($$8 + p_164319_ + 1);
      double $$13 = gradDot(this.p($$9 + p_164320_), p_164321_, p_164322_, p_164323_);
      double $$14 = gradDot(this.p($$11 + p_164320_), p_164321_ - 1.0, p_164322_, p_164323_);
      double $$15 = gradDot(this.p($$10 + p_164320_), p_164321_, p_164322_ - 1.0, p_164323_);
      double $$16 = gradDot(this.p($$12 + p_164320_), p_164321_ - 1.0, p_164322_ - 1.0, p_164323_);
      double $$17 = gradDot(this.p($$9 + p_164320_ + 1), p_164321_, p_164322_, p_164323_ - 1.0);
      double $$18 = gradDot(this.p($$11 + p_164320_ + 1), p_164321_ - 1.0, p_164322_, p_164323_ - 1.0);
      double $$19 = gradDot(this.p($$10 + p_164320_ + 1), p_164321_, p_164322_ - 1.0, p_164323_ - 1.0);
      double $$20 = gradDot(this.p($$12 + p_164320_ + 1), p_164321_ - 1.0, p_164322_ - 1.0, p_164323_ - 1.0);
      double $$21 = Mth.smoothstep(p_164321_);
      double $$22 = Mth.smoothstep(p_164324_);
      double $$23 = Mth.smoothstep(p_164323_);
      return Mth.lerp3($$21, $$22, $$23, $$13, $$14, $$15, $$16, $$17, $$18, $$19, $$20);
   }

   private double sampleWithDerivative(int p_164326_, int p_164327_, int p_164328_, double p_164329_, double p_164330_, double p_164331_, double[] p_164332_) {
      int $$7 = this.p(p_164326_);
      int $$8 = this.p(p_164326_ + 1);
      int $$9 = this.p($$7 + p_164327_);
      int $$10 = this.p($$7 + p_164327_ + 1);
      int $$11 = this.p($$8 + p_164327_);
      int $$12 = this.p($$8 + p_164327_ + 1);
      int $$13 = this.p($$9 + p_164328_);
      int $$14 = this.p($$11 + p_164328_);
      int $$15 = this.p($$10 + p_164328_);
      int $$16 = this.p($$12 + p_164328_);
      int $$17 = this.p($$9 + p_164328_ + 1);
      int $$18 = this.p($$11 + p_164328_ + 1);
      int $$19 = this.p($$10 + p_164328_ + 1);
      int $$20 = this.p($$12 + p_164328_ + 1);
      int[] $$21 = SimplexNoise.GRADIENT[$$13 & 15];
      int[] $$22 = SimplexNoise.GRADIENT[$$14 & 15];
      int[] $$23 = SimplexNoise.GRADIENT[$$15 & 15];
      int[] $$24 = SimplexNoise.GRADIENT[$$16 & 15];
      int[] $$25 = SimplexNoise.GRADIENT[$$17 & 15];
      int[] $$26 = SimplexNoise.GRADIENT[$$18 & 15];
      int[] $$27 = SimplexNoise.GRADIENT[$$19 & 15];
      int[] $$28 = SimplexNoise.GRADIENT[$$20 & 15];
      double $$29 = SimplexNoise.dot($$21, p_164329_, p_164330_, p_164331_);
      double $$30 = SimplexNoise.dot($$22, p_164329_ - 1.0, p_164330_, p_164331_);
      double $$31 = SimplexNoise.dot($$23, p_164329_, p_164330_ - 1.0, p_164331_);
      double $$32 = SimplexNoise.dot($$24, p_164329_ - 1.0, p_164330_ - 1.0, p_164331_);
      double $$33 = SimplexNoise.dot($$25, p_164329_, p_164330_, p_164331_ - 1.0);
      double $$34 = SimplexNoise.dot($$26, p_164329_ - 1.0, p_164330_, p_164331_ - 1.0);
      double $$35 = SimplexNoise.dot($$27, p_164329_, p_164330_ - 1.0, p_164331_ - 1.0);
      double $$36 = SimplexNoise.dot($$28, p_164329_ - 1.0, p_164330_ - 1.0, p_164331_ - 1.0);
      double $$37 = Mth.smoothstep(p_164329_);
      double $$38 = Mth.smoothstep(p_164330_);
      double $$39 = Mth.smoothstep(p_164331_);
      double $$40 = Mth.lerp3($$37, $$38, $$39, $$21[0], $$22[0], $$23[0], $$24[0], $$25[0], $$26[0], $$27[0], $$28[0]);
      double $$41 = Mth.lerp3($$37, $$38, $$39, $$21[1], $$22[1], $$23[1], $$24[1], $$25[1], $$26[1], $$27[1], $$28[1]);
      double $$42 = Mth.lerp3($$37, $$38, $$39, $$21[2], $$22[2], $$23[2], $$24[2], $$25[2], $$26[2], $$27[2], $$28[2]);
      double $$43 = Mth.lerp2($$38, $$39, $$30 - $$29, $$32 - $$31, $$34 - $$33, $$36 - $$35);
      double $$44 = Mth.lerp2($$39, $$37, $$31 - $$29, $$35 - $$33, $$32 - $$30, $$36 - $$34);
      double $$45 = Mth.lerp2($$37, $$38, $$33 - $$29, $$34 - $$30, $$35 - $$31, $$36 - $$32);
      double $$46 = Mth.smoothstepDerivative(p_164329_);
      double $$47 = Mth.smoothstepDerivative(p_164330_);
      double $$48 = Mth.smoothstepDerivative(p_164331_);
      double $$49 = $$40 + $$46 * $$43;
      double $$50 = $$41 + $$47 * $$44;
      double $$51 = $$42 + $$48 * $$45;
      p_164332_[0] += $$49;
      p_164332_[1] += $$50;
      p_164332_[2] += $$51;
      return Mth.lerp3($$37, $$38, $$39, $$29, $$30, $$31, $$32, $$33, $$34, $$35, $$36);
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder p_192824_) {
      NoiseUtils.parityNoiseOctaveConfigString(p_192824_, this.xo, this.yo, this.zo, this.p);
   }
}
