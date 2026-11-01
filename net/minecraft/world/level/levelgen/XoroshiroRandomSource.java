package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class XoroshiroRandomSource implements RandomSource {
   private static final float FLOAT_UNIT = 5.9604645E-8F;
   private static final double DOUBLE_UNIT = 1.110223E-16F;
   public static final Codec<XoroshiroRandomSource> CODEC = Xoroshiro128PlusPlus.CODEC
      .xmap(p_287645_ -> new XoroshiroRandomSource(p_287645_), p_287690_ -> p_287690_.randomNumberGenerator);
   private Xoroshiro128PlusPlus randomNumberGenerator;
   private final MarsagliaPolarGaussian gaussianSource = new MarsagliaPolarGaussian(this);

   public XoroshiroRandomSource(long p_190102_) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(RandomSupport.upgradeSeedTo128bit(p_190102_));
   }

   public XoroshiroRandomSource(RandomSupport.Seed128bit p_289014_) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(p_289014_);
   }

   public XoroshiroRandomSource(long p_190104_, long p_190105_) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(p_190104_, p_190105_);
   }

   private XoroshiroRandomSource(Xoroshiro128PlusPlus p_287656_) {
      this.randomNumberGenerator = p_287656_;
   }

   @Override
   public RandomSource fork() {
      return new XoroshiroRandomSource(this.randomNumberGenerator.nextLong(), this.randomNumberGenerator.nextLong());
   }

   @Override
   public PositionalRandomFactory forkPositional() {
      return new XoroshiroRandomSource.XoroshiroPositionalRandomFactory(this.randomNumberGenerator.nextLong(), this.randomNumberGenerator.nextLong());
   }

   @Override
   public void setSeed(long p_190121_) {
      this.randomNumberGenerator = new Xoroshiro128PlusPlus(RandomSupport.upgradeSeedTo128bit(p_190121_));
      this.gaussianSource.reset();
   }

   @Override
   public int nextInt() {
      return (int)this.randomNumberGenerator.nextLong();
   }

   @Override
   public int nextInt(int p_190118_) {
      if (p_190118_ <= 0) {
         throw new IllegalArgumentException("Bound must be positive");
      } else {
         long $$1 = Integer.toUnsignedLong(this.nextInt());
         long $$2 = $$1 * p_190118_;
         long $$3 = $$2 & 4294967295L;
         if ($$3 < p_190118_) {
            for (int $$4 = Integer.remainderUnsigned(~p_190118_ + 1, p_190118_); $$3 < $$4; $$3 = $$2 & 4294967295L) {
               $$1 = Integer.toUnsignedLong(this.nextInt());
               $$2 = $$1 * p_190118_;
            }
         }

         long $$5 = $$2 >> 32;
         return (int)$$5;
      }
   }

   @Override
   public long nextLong() {
      return this.randomNumberGenerator.nextLong();
   }

   @Override
   public boolean nextBoolean() {
      return (this.randomNumberGenerator.nextLong() & 1L) != 0L;
   }

   @Override
   public float nextFloat() {
      return (float)this.nextBits(24) * 5.9604645E-8F;
   }

   @Override
   public double nextDouble() {
      return this.nextBits(53) * 1.110223E-16F;
   }

   @Override
   public double nextGaussian() {
      return this.gaussianSource.nextGaussian();
   }

   @Override
   public void consumeCount(int p_190111_) {
      for (int $$1 = 0; $$1 < p_190111_; $$1++) {
         this.randomNumberGenerator.nextLong();
      }
   }

   private long nextBits(int p_190108_) {
      return this.randomNumberGenerator.nextLong() >>> 64 - p_190108_;
   }

   public static class XoroshiroPositionalRandomFactory implements PositionalRandomFactory {
      private final long seedLo;
      private final long seedHi;

      public XoroshiroPositionalRandomFactory(long p_190127_, long p_190128_) {
         this.seedLo = p_190127_;
         this.seedHi = p_190128_;
      }

      @Override
      public RandomSource at(int p_224691_, int p_224692_, int p_224693_) {
         long $$3 = Mth.getSeed(p_224691_, p_224692_, p_224693_);
         long $$4 = $$3 ^ this.seedLo;
         return new XoroshiroRandomSource($$4, this.seedHi);
      }

      @Override
      public RandomSource fromHashOf(String p_224695_) {
         RandomSupport.Seed128bit $$1 = RandomSupport.seedFromHashOf(p_224695_);
         return new XoroshiroRandomSource($$1.xor(this.seedLo, this.seedHi));
      }

      @Override
      public RandomSource fromSeed(long p_348665_) {
         return new XoroshiroRandomSource(p_348665_ ^ this.seedLo, p_348665_ ^ this.seedHi);
      }

      @VisibleForTesting
      @Override
      public void parityConfigString(StringBuilder p_190136_) {
         p_190136_.append("seedLo: ").append(this.seedLo).append(", seedHi: ").append(this.seedHi);
      }
   }
}
