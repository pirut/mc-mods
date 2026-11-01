package net.minecraft.world.level.levelgen;

import java.util.function.LongFunction;
import net.minecraft.util.RandomSource;

public class WorldgenRandom extends LegacyRandomSource {
   private final RandomSource randomSource;
   private int count;

   public WorldgenRandom(RandomSource p_224680_) {
      super(0L);
      this.randomSource = p_224680_;
   }

   public int getCount() {
      return this.count;
   }

   @Override
   public RandomSource fork() {
      return this.randomSource.fork();
   }

   @Override
   public PositionalRandomFactory forkPositional() {
      return this.randomSource.forkPositional();
   }

   @Override
   public int next(int p_64708_) {
      this.count++;
      return this.randomSource instanceof LegacyRandomSource $$1 ? $$1.next(p_64708_) : (int)(this.randomSource.nextLong() >>> 64 - p_64708_);
   }

   @Override
   public synchronized void setSeed(long p_190073_) {
      if (this.randomSource != null) {
         this.randomSource.setSeed(p_190073_);
      }
   }

   public long setDecorationSeed(long p_64691_, int p_64692_, int p_64693_) {
      this.setSeed(p_64691_);
      long $$3 = this.nextLong() | 1L;
      long $$4 = this.nextLong() | 1L;
      long $$5 = p_64692_ * $$3 + p_64693_ * $$4 ^ p_64691_;
      this.setSeed($$5);
      return $$5;
   }

   public void setFeatureSeed(long p_190065_, int p_190066_, int p_190067_) {
      long $$3 = p_190065_ + p_190066_ + 10000 * p_190067_;
      this.setSeed($$3);
   }

   public void setLargeFeatureSeed(long p_190069_, int p_190070_, int p_190071_) {
      this.setSeed(p_190069_);
      long $$3 = this.nextLong();
      long $$4 = this.nextLong();
      long $$5 = p_190070_ * $$3 ^ p_190071_ * $$4 ^ p_190069_;
      this.setSeed($$5);
   }

   public void setLargeFeatureWithSalt(long p_190059_, int p_190060_, int p_190061_, int p_190062_) {
      long $$4 = p_190060_ * 341873128712L + p_190061_ * 132897987541L + p_190059_ + p_190062_;
      this.setSeed($$4);
   }

   public static RandomSource seedSlimeChunk(int p_224682_, int p_224683_, long p_224684_, long p_224685_) {
      return RandomSource.create(
         p_224684_ + p_224682_ * p_224682_ * 4987142 + p_224682_ * 5947611 + p_224683_ * p_224683_ * 4392871L + p_224683_ * 389711 ^ p_224685_
      );
   }

   public static enum Algorithm {
      LEGACY(LegacyRandomSource::new),
      XOROSHIRO(XoroshiroRandomSource::new);

      private final LongFunction<RandomSource> constructor;

      private Algorithm(LongFunction<RandomSource> p_190082_) {
         this.constructor = p_190082_;
      }

      public RandomSource newInstance(long p_224688_) {
         return this.constructor.apply(p_224688_);
      }
   }
}
