package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class CaveWorldCarver extends WorldCarver<CaveCarverConfiguration> {
   public CaveWorldCarver(Codec<CaveCarverConfiguration> p_159194_) {
      super(p_159194_);
   }

   public boolean isStartChunk(CaveCarverConfiguration p_224894_, RandomSource p_224895_) {
      return p_224895_.nextFloat() <= p_224894_.probability;
   }

   public boolean carve(
      CarvingContext p_224885_,
      CaveCarverConfiguration p_224886_,
      ChunkAccess p_224887_,
      Function<BlockPos, Holder<Biome>> p_224888_,
      RandomSource p_224889_,
      Aquifer p_224890_,
      ChunkPos p_224891_,
      CarvingMask p_224892_
   ) {
      int $$8 = SectionPos.sectionToBlockCoord(this.getRange() * 2 - 1);
      int $$9 = p_224889_.nextInt(p_224889_.nextInt(p_224889_.nextInt(this.getCaveBound()) + 1) + 1);

      for (int $$10 = 0; $$10 < $$9; $$10++) {
         double $$11 = p_224891_.getBlockX(p_224889_.nextInt(16));
         double $$12 = p_224886_.y.sample(p_224889_, p_224885_);
         double $$13 = p_224891_.getBlockZ(p_224889_.nextInt(16));
         double $$14 = p_224886_.horizontalRadiusMultiplier.sample(p_224889_);
         double $$15 = p_224886_.verticalRadiusMultiplier.sample(p_224889_);
         double $$16 = p_224886_.floorLevel.sample(p_224889_);
         WorldCarver.CarveSkipChecker $$17 = (p_159202_, p_159203_, p_159204_, p_159205_, p_159206_) -> shouldSkip(p_159203_, p_159204_, p_159205_, $$16);
         int $$18 = 1;
         if (p_224889_.nextInt(4) == 0) {
            double $$19 = p_224886_.yScale.sample(p_224889_);
            float $$20 = 1.0F + p_224889_.nextFloat() * 6.0F;
            this.createRoom(p_224885_, p_224886_, p_224887_, p_224888_, p_224890_, $$11, $$12, $$13, $$20, $$19, p_224892_, $$17);
            $$18 += p_224889_.nextInt(4);
         }

         for (int $$21 = 0; $$21 < $$18; $$21++) {
            float $$22 = p_224889_.nextFloat() * (float) (Math.PI * 2);
            float $$23 = (p_224889_.nextFloat() - 0.5F) / 4.0F;
            float $$24 = this.getThickness(p_224889_);
            int $$25 = $$8 - p_224889_.nextInt($$8 / 4);
            int $$26 = 0;
            this.createTunnel(
               p_224885_,
               p_224886_,
               p_224887_,
               p_224888_,
               p_224889_.nextLong(),
               p_224890_,
               $$11,
               $$12,
               $$13,
               $$14,
               $$15,
               $$24,
               $$22,
               $$23,
               0,
               $$25,
               this.getYScale(),
               p_224892_,
               $$17
            );
         }
      }

      return true;
   }

   protected int getCaveBound() {
      return 15;
   }

   protected float getThickness(RandomSource p_224871_) {
      float $$1 = p_224871_.nextFloat() * 2.0F + p_224871_.nextFloat();
      if (p_224871_.nextInt(10) == 0) {
         $$1 *= p_224871_.nextFloat() * p_224871_.nextFloat() * 3.0F + 1.0F;
      }

      return $$1;
   }

   protected double getYScale() {
      return 1.0;
   }

   protected void createRoom(
      CarvingContext p_190691_,
      CaveCarverConfiguration p_190692_,
      ChunkAccess p_190693_,
      Function<BlockPos, Holder<Biome>> p_190694_,
      Aquifer p_190695_,
      double p_190696_,
      double p_190697_,
      double p_190698_,
      float p_190699_,
      double p_190700_,
      CarvingMask p_190701_,
      WorldCarver.CarveSkipChecker p_190702_
   ) {
      double $$12 = 1.5 + Mth.sin((float) (Math.PI / 2)) * p_190699_;
      double $$13 = $$12 * p_190700_;
      this.carveEllipsoid(p_190691_, p_190692_, p_190693_, p_190694_, p_190695_, p_190696_ + 1.0, p_190697_, p_190698_, $$12, $$13, p_190701_, p_190702_);
   }

   protected void createTunnel(
      CarvingContext p_190671_,
      CaveCarverConfiguration p_190672_,
      ChunkAccess p_190673_,
      Function<BlockPos, Holder<Biome>> p_190674_,
      long p_190675_,
      Aquifer p_190676_,
      double p_190677_,
      double p_190678_,
      double p_190679_,
      double p_190680_,
      double p_190681_,
      float p_190682_,
      float p_190683_,
      float p_190684_,
      int p_190685_,
      int p_190686_,
      double p_190687_,
      CarvingMask p_190688_,
      WorldCarver.CarveSkipChecker p_190689_
   ) {
      RandomSource $$19 = RandomSource.create(p_190675_);
      int $$20 = $$19.nextInt(p_190686_ / 2) + p_190686_ / 4;
      boolean $$21 = $$19.nextInt(6) == 0;
      float $$22 = 0.0F;
      float $$23 = 0.0F;

      for (int $$24 = p_190685_; $$24 < p_190686_; $$24++) {
         double $$25 = 1.5 + Mth.sin((float) Math.PI * $$24 / p_190686_) * p_190682_;
         double $$26 = $$25 * p_190687_;
         float $$27 = Mth.cos(p_190684_);
         p_190677_ += Mth.cos(p_190683_) * $$27;
         p_190678_ += Mth.sin(p_190684_);
         p_190679_ += Mth.sin(p_190683_) * $$27;
         p_190684_ *= $$21 ? 0.92F : 0.7F;
         p_190684_ += $$23 * 0.1F;
         p_190683_ += $$22 * 0.1F;
         $$23 *= 0.9F;
         $$22 *= 0.75F;
         $$23 += ($$19.nextFloat() - $$19.nextFloat()) * $$19.nextFloat() * 2.0F;
         $$22 += ($$19.nextFloat() - $$19.nextFloat()) * $$19.nextFloat() * 4.0F;
         if ($$24 == $$20 && p_190682_ > 1.0F) {
            this.createTunnel(
               p_190671_,
               p_190672_,
               p_190673_,
               p_190674_,
               $$19.nextLong(),
               p_190676_,
               p_190677_,
               p_190678_,
               p_190679_,
               p_190680_,
               p_190681_,
               $$19.nextFloat() * 0.5F + 0.5F,
               p_190683_ - (float) (Math.PI / 2),
               p_190684_ / 3.0F,
               $$24,
               p_190686_,
               1.0,
               p_190688_,
               p_190689_
            );
            this.createTunnel(
               p_190671_,
               p_190672_,
               p_190673_,
               p_190674_,
               $$19.nextLong(),
               p_190676_,
               p_190677_,
               p_190678_,
               p_190679_,
               p_190680_,
               p_190681_,
               $$19.nextFloat() * 0.5F + 0.5F,
               p_190683_ + (float) (Math.PI / 2),
               p_190684_ / 3.0F,
               $$24,
               p_190686_,
               1.0,
               p_190688_,
               p_190689_
            );
            return;
         }

         if ($$19.nextInt(4) != 0) {
            if (!canReach(p_190673_.getPos(), p_190677_, p_190679_, $$24, p_190686_, p_190682_)) {
               return;
            }

            this.carveEllipsoid(
               p_190671_, p_190672_, p_190673_, p_190674_, p_190676_, p_190677_, p_190678_, p_190679_, $$25 * p_190680_, $$26 * p_190681_, p_190688_, p_190689_
            );
         }
      }
   }

   private static boolean shouldSkip(double p_159196_, double p_159197_, double p_159198_, double p_159199_) {
      return p_159197_ <= p_159199_ ? true : p_159196_ * p_159196_ + p_159197_ * p_159197_ + p_159198_ * p_159198_ >= 1.0;
   }
}
