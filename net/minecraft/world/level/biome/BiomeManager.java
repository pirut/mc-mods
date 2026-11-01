package net.minecraft.world.level.biome;

import com.google.common.hash.Hashing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.Mth;

public class BiomeManager {
   public static final int CHUNK_CENTER_QUART = QuartPos.fromBlock(8);
   private static final int ZOOM_BITS = 2;
   private static final int ZOOM = 4;
   private static final int ZOOM_MASK = 3;
   private final BiomeManager.NoiseBiomeSource noiseBiomeSource;
   private final long biomeZoomSeed;

   public BiomeManager(BiomeManager.NoiseBiomeSource p_186677_, long p_186678_) {
      this.noiseBiomeSource = p_186677_;
      this.biomeZoomSeed = p_186678_;
   }

   public static long obfuscateSeed(long p_47878_) {
      return Hashing.sha256().hashLong(p_47878_).asLong();
   }

   public BiomeManager withDifferentSource(BiomeManager.NoiseBiomeSource p_186688_) {
      return new BiomeManager(p_186688_, this.biomeZoomSeed);
   }

   public Holder<Biome> getBiome(BlockPos p_204215_) {
      int $$1 = p_204215_.getX() - 2;
      int $$2 = p_204215_.getY() - 2;
      int $$3 = p_204215_.getZ() - 2;
      int $$4 = $$1 >> 2;
      int $$5 = $$2 >> 2;
      int $$6 = $$3 >> 2;
      double $$7 = ($$1 & 3) / 4.0;
      double $$8 = ($$2 & 3) / 4.0;
      double $$9 = ($$3 & 3) / 4.0;
      int $$10 = 0;
      double $$11 = Double.POSITIVE_INFINITY;

      for (int $$12 = 0; $$12 < 8; $$12++) {
         boolean $$13 = ($$12 & 4) == 0;
         boolean $$14 = ($$12 & 2) == 0;
         boolean $$15 = ($$12 & 1) == 0;
         int $$16 = $$13 ? $$4 : $$4 + 1;
         int $$17 = $$14 ? $$5 : $$5 + 1;
         int $$18 = $$15 ? $$6 : $$6 + 1;
         double $$19 = $$13 ? $$7 : $$7 - 1.0;
         double $$20 = $$14 ? $$8 : $$8 - 1.0;
         double $$21 = $$15 ? $$9 : $$9 - 1.0;
         double $$22 = getFiddledDistance(this.biomeZoomSeed, $$16, $$17, $$18, $$19, $$20, $$21);
         if ($$11 > $$22) {
            $$10 = $$12;
            $$11 = $$22;
         }
      }

      int $$23 = ($$10 & 4) == 0 ? $$4 : $$4 + 1;
      int $$24 = ($$10 & 2) == 0 ? $$5 : $$5 + 1;
      int $$25 = ($$10 & 1) == 0 ? $$6 : $$6 + 1;
      return this.noiseBiomeSource.getNoiseBiome($$23, $$24, $$25);
   }

   public Holder<Biome> getNoiseBiomeAtPosition(double p_204207_, double p_204208_, double p_204209_) {
      int $$3 = QuartPos.fromBlock(Mth.floor(p_204207_));
      int $$4 = QuartPos.fromBlock(Mth.floor(p_204208_));
      int $$5 = QuartPos.fromBlock(Mth.floor(p_204209_));
      return this.getNoiseBiomeAtQuart($$3, $$4, $$5);
   }

   public Holder<Biome> getNoiseBiomeAtPosition(BlockPos p_204217_) {
      int $$1 = QuartPos.fromBlock(p_204217_.getX());
      int $$2 = QuartPos.fromBlock(p_204217_.getY());
      int $$3 = QuartPos.fromBlock(p_204217_.getZ());
      return this.getNoiseBiomeAtQuart($$1, $$2, $$3);
   }

   public Holder<Biome> getNoiseBiomeAtQuart(int p_204211_, int p_204212_, int p_204213_) {
      return this.noiseBiomeSource.getNoiseBiome(p_204211_, p_204212_, p_204213_);
   }

   private static double getFiddledDistance(long p_186680_, int p_186681_, int p_186682_, int p_186683_, double p_186684_, double p_186685_, double p_186686_) {
      long $$7 = LinearCongruentialGenerator.next(p_186680_, p_186681_);
      $$7 = LinearCongruentialGenerator.next($$7, p_186682_);
      $$7 = LinearCongruentialGenerator.next($$7, p_186683_);
      $$7 = LinearCongruentialGenerator.next($$7, p_186681_);
      $$7 = LinearCongruentialGenerator.next($$7, p_186682_);
      $$7 = LinearCongruentialGenerator.next($$7, p_186683_);
      double $$8 = getFiddle($$7);
      $$7 = LinearCongruentialGenerator.next($$7, p_186680_);
      double $$9 = getFiddle($$7);
      $$7 = LinearCongruentialGenerator.next($$7, p_186680_);
      double $$10 = getFiddle($$7);
      return Mth.square(p_186686_ + $$10) + Mth.square(p_186685_ + $$9) + Mth.square(p_186684_ + $$8);
   }

   private static double getFiddle(long p_186690_) {
      double $$1 = Math.floorMod(p_186690_ >> 24, 1024) / 1024.0;
      return ($$1 - 0.5) * 0.9;
   }

   public interface NoiseBiomeSource {
      Holder<Biome> getNoiseBiome(int var1, int var2, int var3);
   }
}
