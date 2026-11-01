package net.minecraft.world.level.levelgen;

import java.util.function.LongFunction;
import net.minecraft.util.RandomSource;

public class WorldgenRandom extends LegacyRandomSource {
   private final RandomSource randomSource;
   private int count;

   public WorldgenRandom(RandomSource randomSource) {
      super(0L);
      this.randomSource = randomSource;
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
   public int next(int size) {
      this.count++;
      return this.randomSource instanceof LegacyRandomSource legacyRandomSource
         ? legacyRandomSource.next(size)
         : (int)(this.randomSource.nextLong() >>> 64 - size);
   }

   @Override
   public synchronized void setSeed(long seed) {
      if (this.randomSource != null) {
         this.randomSource.setSeed(seed);
      }
   }

   public long setDecorationSeed(long levelSeed, int minChunkBlockX, int minChunkBlockZ) {
      this.setSeed(levelSeed);
      long l = this.nextLong() | 1L;
      long m = this.nextLong() | 1L;
      long n = minChunkBlockX * l + minChunkBlockZ * m ^ levelSeed;
      this.setSeed(n);
      return n;
   }

   public void setFeatureSeed(long decorationSeed, int index, int decorationStep) {
      long l = decorationSeed + index + 10000 * decorationStep;
      this.setSeed(l);
   }

   public void setLargeFeatureSeed(long baseSeed, int chunkX, int chunkZ) {
      this.setSeed(baseSeed);
      long l = this.nextLong();
      long m = this.nextLong();
      long n = chunkX * l ^ chunkZ * m ^ baseSeed;
      this.setSeed(n);
   }

   public void setLargeFeatureWithSalt(long levelSeed, int regionX, int regionZ, int salt) {
      long l = regionX * 341873128712L + regionZ * 132897987541L + levelSeed + salt;
      this.setSeed(l);
   }

   public static RandomSource seedSlimeChunk(int chunkX, int chunkZ, long levelSeed, long salt) {
      return RandomSource.create(levelSeed + chunkX * chunkX * 4987142 + chunkX * 5947611 + chunkZ * chunkZ * 4392871L + chunkZ * 389711 ^ salt);
   }

   public static enum Algorithm {
      LEGACY(LegacyRandomSource::new),
      XOROSHIRO(XoroshiroRandomSource::new);

      private final LongFunction<RandomSource> constructor;

      private Algorithm(final LongFunction<RandomSource> constructor) {
         this.constructor = constructor;
      }

      public RandomSource newInstance(long seed) {
         return this.constructor.apply(seed);
      }
   }
}
