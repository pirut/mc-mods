package net.minecraft.world.level.levelgen.synth;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public class PerlinSimplexNoise {
   private final SimplexNoise[] noiseLevels;
   private final double highestFreqValueFactor;
   private final double highestFreqInputFactor;

   public PerlinSimplexNoise(RandomSource p_230546_, List<Integer> p_230547_) {
      this(p_230546_, new IntRBTreeSet(p_230547_));
   }

   private PerlinSimplexNoise(RandomSource p_230543_, IntSortedSet p_230544_) {
      if (p_230544_.isEmpty()) {
         throw new IllegalArgumentException("Need some octaves!");
      } else {
         int $$2 = -p_230544_.firstInt();
         int $$3 = p_230544_.lastInt();
         int $$4 = $$2 + $$3 + 1;
         if ($$4 < 1) {
            throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
         } else {
            SimplexNoise $$5 = new SimplexNoise(p_230543_);
            int $$6 = $$3;
            this.noiseLevels = new SimplexNoise[$$4];
            if ($$3 >= 0 && $$3 < $$4 && p_230544_.contains(0)) {
               this.noiseLevels[$$3] = $$5;
            }

            for (int $$7 = $$3 + 1; $$7 < $$4; $$7++) {
               if ($$7 >= 0 && p_230544_.contains($$6 - $$7)) {
                  this.noiseLevels[$$7] = new SimplexNoise(p_230543_);
               } else {
                  p_230543_.consumeCount(262);
               }
            }

            if ($$3 > 0) {
               long $$8 = (long)($$5.getValue($$5.xo, $$5.yo, $$5.zo) * 9.223372E18F);
               RandomSource $$9 = new WorldgenRandom(new LegacyRandomSource($$8));

               for (int $$10 = $$6 - 1; $$10 >= 0; $$10--) {
                  if ($$10 < $$4 && p_230544_.contains($$6 - $$10)) {
                     this.noiseLevels[$$10] = new SimplexNoise($$9);
                  } else {
                     $$9.consumeCount(262);
                  }
               }
            }

            this.highestFreqInputFactor = Math.pow(2.0, $$3);
            this.highestFreqValueFactor = 1.0 / (Math.pow(2.0, $$4) - 1.0);
         }
      }
   }

   public double getValue(double p_75450_, double p_75451_, boolean p_75452_) {
      double $$3 = 0.0;
      double $$4 = this.highestFreqInputFactor;
      double $$5 = this.highestFreqValueFactor;

      for (SimplexNoise $$6 : this.noiseLevels) {
         if ($$6 != null) {
            $$3 += $$6.getValue(p_75450_ * $$4 + (p_75452_ ? $$6.xo : 0.0), p_75451_ * $$4 + (p_75452_ ? $$6.yo : 0.0)) * $$5;
         }

         $$4 /= 2.0;
         $$5 *= 2.0;
      }

      return $$3;
   }
}
