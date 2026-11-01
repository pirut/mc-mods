package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

public class PerlinNoise {
   private static final int ROUND_OFF = 33554432;
   private final ImprovedNoise[] noiseLevels;
   private final int firstOctave;
   private final DoubleList amplitudes;
   private final double lowestFreqValueFactor;
   private final double lowestFreqInputFactor;
   private final double maxValue;

   @Deprecated
   public static PerlinNoise createLegacyForBlendedNoise(RandomSource p_230533_, IntStream p_230534_) {
      return new PerlinNoise(p_230533_, makeAmplitudes(new IntRBTreeSet(p_230534_.boxed().collect(ImmutableList.toImmutableList()))), false);
   }

   @Deprecated
   public static PerlinNoise createLegacyForLegacyNetherBiome(RandomSource p_230526_, int p_230527_, DoubleList p_230528_) {
      return new PerlinNoise(p_230526_, Pair.of(p_230527_, p_230528_), false);
   }

   public static PerlinNoise create(RandomSource p_230540_, IntStream p_230541_) {
      return create(p_230540_, p_230541_.boxed().collect(ImmutableList.toImmutableList()));
   }

   public static PerlinNoise create(RandomSource p_230530_, List<Integer> p_230531_) {
      return new PerlinNoise(p_230530_, makeAmplitudes(new IntRBTreeSet(p_230531_)), true);
   }

   public static PerlinNoise create(RandomSource p_230521_, int p_230522_, double p_230523_, double... p_230524_) {
      DoubleArrayList $$4 = new DoubleArrayList(p_230524_);
      $$4.add(0, p_230523_);
      return new PerlinNoise(p_230521_, Pair.of(p_230522_, $$4), true);
   }

   public static PerlinNoise create(RandomSource p_230536_, int p_230537_, DoubleList p_230538_) {
      return new PerlinNoise(p_230536_, Pair.of(p_230537_, p_230538_), true);
   }

   private static Pair<Integer, DoubleList> makeAmplitudes(IntSortedSet p_75431_) {
      if (p_75431_.isEmpty()) {
         throw new IllegalArgumentException("Need some octaves!");
      } else {
         int $$1 = -p_75431_.firstInt();
         int $$2 = p_75431_.lastInt();
         int $$3 = $$1 + $$2 + 1;
         if ($$3 < 1) {
            throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
         } else {
            DoubleList $$4 = new DoubleArrayList(new double[$$3]);
            IntBidirectionalIterator $$5 = p_75431_.iterator();

            while ($$5.hasNext()) {
               int $$6 = $$5.nextInt();
               $$4.set($$6 + $$1, 1.0);
            }

            return Pair.of(-$$1, $$4);
         }
      }
   }

   protected PerlinNoise(RandomSource p_230515_, Pair<Integer, DoubleList> p_230516_, boolean p_230517_) {
      this.firstOctave = (Integer)p_230516_.getFirst();
      this.amplitudes = (DoubleList)p_230516_.getSecond();
      int $$3 = this.amplitudes.size();
      int $$4 = -this.firstOctave;
      this.noiseLevels = new ImprovedNoise[$$3];
      if (p_230517_) {
         PositionalRandomFactory $$5 = p_230515_.forkPositional();

         for (int $$6 = 0; $$6 < $$3; $$6++) {
            if (this.amplitudes.getDouble($$6) != 0.0) {
               int $$7 = this.firstOctave + $$6;
               this.noiseLevels[$$6] = new ImprovedNoise($$5.fromHashOf("octave_" + $$7));
            }
         }
      } else {
         ImprovedNoise $$8 = new ImprovedNoise(p_230515_);
         if ($$4 >= 0 && $$4 < $$3) {
            double $$9 = this.amplitudes.getDouble($$4);
            if ($$9 != 0.0) {
               this.noiseLevels[$$4] = $$8;
            }
         }

         for (int $$10 = $$4 - 1; $$10 >= 0; $$10--) {
            if ($$10 < $$3) {
               double $$11 = this.amplitudes.getDouble($$10);
               if ($$11 != 0.0) {
                  this.noiseLevels[$$10] = new ImprovedNoise(p_230515_);
               } else {
                  skipOctave(p_230515_);
               }
            } else {
               skipOctave(p_230515_);
            }
         }

         if (Arrays.stream(this.noiseLevels).filter(Objects::nonNull).count() != this.amplitudes.stream().filter(p_192897_ -> p_192897_ != 0.0).count()) {
            throw new IllegalStateException("Failed to create correct number of noise levels for given non-zero amplitudes");
         }

         if ($$4 < $$3 - 1) {
            throw new IllegalArgumentException("Positive octaves are temporarily disabled");
         }
      }

      this.lowestFreqInputFactor = Math.pow(2.0, -$$4);
      this.lowestFreqValueFactor = Math.pow(2.0, $$3 - 1) / (Math.pow(2.0, $$3) - 1.0);
      this.maxValue = this.edgeValue(2.0);
   }

   protected double maxValue() {
      return this.maxValue;
   }

   private static void skipOctave(RandomSource p_230519_) {
      p_230519_.consumeCount(262);
   }

   public double getValue(double p_75409_, double p_75410_, double p_75411_) {
      return this.getValue(p_75409_, p_75410_, p_75411_, 0.0, 0.0, false);
   }

   @Deprecated
   public double getValue(double p_75418_, double p_75419_, double p_75420_, double p_75421_, double p_75422_, boolean p_75423_) {
      double $$6 = 0.0;
      double $$7 = this.lowestFreqInputFactor;
      double $$8 = this.lowestFreqValueFactor;

      for (int $$9 = 0; $$9 < this.noiseLevels.length; $$9++) {
         ImprovedNoise $$10 = this.noiseLevels[$$9];
         if ($$10 != null) {
            double $$11 = $$10.noise(wrap(p_75418_ * $$7), p_75423_ ? -$$10.yo : wrap(p_75419_ * $$7), wrap(p_75420_ * $$7), p_75421_ * $$7, p_75422_ * $$7);
            $$6 += this.amplitudes.getDouble($$9) * $$11 * $$8;
         }

         $$7 *= 2.0;
         $$8 /= 2.0;
      }

      return $$6;
   }

   public double maxBrokenValue(double p_210644_) {
      return this.edgeValue(p_210644_ + 2.0);
   }

   private double edgeValue(double p_210650_) {
      double $$1 = 0.0;
      double $$2 = this.lowestFreqValueFactor;

      for (int $$3 = 0; $$3 < this.noiseLevels.length; $$3++) {
         ImprovedNoise $$4 = this.noiseLevels[$$3];
         if ($$4 != null) {
            $$1 += this.amplitudes.getDouble($$3) * p_210650_ * $$2;
         }

         $$2 /= 2.0;
      }

      return $$1;
   }

   @Nullable
   public ImprovedNoise getOctaveNoise(int p_75425_) {
      return this.noiseLevels[this.noiseLevels.length - 1 - p_75425_];
   }

   public static double wrap(double p_75407_) {
      return p_75407_ - Mth.lfloor(p_75407_ / 3.3554432E7 + 0.5) * 3.3554432E7;
   }

   protected int firstOctave() {
      return this.firstOctave;
   }

   protected DoubleList amplitudes() {
      return this.amplitudes;
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder p_192891_) {
      p_192891_.append("PerlinNoise{");
      List<String> $$1 = this.amplitudes.stream().map(p_192889_ -> String.format(Locale.ROOT, "%.2f", p_192889_)).toList();
      p_192891_.append("first octave: ").append(this.firstOctave).append(", amplitudes: ").append($$1).append(", noise levels: [");

      for (int $$2 = 0; $$2 < this.noiseLevels.length; $$2++) {
         p_192891_.append($$2).append(": ");
         ImprovedNoise $$3 = this.noiseLevels[$$2];
         if ($$3 == null) {
            p_192891_.append("null");
         } else {
            $$3.parityConfigString(p_192891_);
         }

         p_192891_.append(", ");
      }

      p_192891_.append("]");
      p_192891_.append("}");
   }
}
