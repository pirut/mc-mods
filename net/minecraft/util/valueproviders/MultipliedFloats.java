package net.minecraft.util.valueproviders;

import java.util.Arrays;
import net.minecraft.util.RandomSource;

public class MultipliedFloats implements SampledFloat {
   private final SampledFloat[] values;

   public MultipliedFloats(SampledFloat... p_216858_) {
      this.values = p_216858_;
   }

   @Override
   public float sample(RandomSource p_216860_) {
      float $$1 = 1.0F;

      for (SampledFloat $$2 : this.values) {
         $$1 *= $$2.sample(p_216860_);
      }

      return $$1;
   }

   @Override
   public String toString() {
      return "MultipliedFloats" + Arrays.toString((Object[])this.values);
   }
}
