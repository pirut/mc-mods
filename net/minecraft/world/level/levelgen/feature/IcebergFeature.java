package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;

public class IcebergFeature extends Feature<BlockStateConfiguration> {
   public IcebergFeature(Codec<BlockStateConfiguration> p_66017_) {
      super(p_66017_);
   }

   @Override
   public boolean place(FeaturePlaceContext<BlockStateConfiguration> p_159884_) {
      BlockPos $$1 = p_159884_.origin();
      WorldGenLevel $$2 = p_159884_.level();
      $$1 = new BlockPos($$1.getX(), p_159884_.chunkGenerator().getSeaLevel(), $$1.getZ());
      RandomSource $$3 = p_159884_.random();
      boolean $$4 = $$3.nextDouble() > 0.7;
      BlockState $$5 = p_159884_.config().state;
      double $$6 = $$3.nextDouble() * 2.0 * Math.PI;
      int $$7 = 11 - $$3.nextInt(5);
      int $$8 = 3 + $$3.nextInt(3);
      boolean $$9 = $$3.nextDouble() > 0.7;
      int $$10 = 11;
      int $$11 = $$9 ? $$3.nextInt(6) + 6 : $$3.nextInt(15) + 3;
      if (!$$9 && $$3.nextDouble() > 0.9) {
         $$11 += $$3.nextInt(19) + 7;
      }

      int $$12 = Math.min($$11 + $$3.nextInt(11), 18);
      int $$13 = Math.min($$11 + $$3.nextInt(7) - $$3.nextInt(5), 11);
      int $$14 = $$9 ? $$7 : 11;

      for (int $$15 = -$$14; $$15 < $$14; $$15++) {
         for (int $$16 = -$$14; $$16 < $$14; $$16++) {
            for (int $$17 = 0; $$17 < $$11; $$17++) {
               int $$18 = $$9 ? this.heightDependentRadiusEllipse($$17, $$11, $$13) : this.heightDependentRadiusRound($$3, $$17, $$11, $$13);
               if ($$9 || $$15 < $$18) {
                  this.generateIcebergBlock($$2, $$3, $$1, $$11, $$15, $$17, $$16, $$18, $$14, $$9, $$8, $$6, $$4, $$5);
               }
            }
         }
      }

      this.smooth($$2, $$1, $$13, $$11, $$9, $$7);

      for (int $$19 = -$$14; $$19 < $$14; $$19++) {
         for (int $$20 = -$$14; $$20 < $$14; $$20++) {
            for (int $$21 = -1; $$21 > -$$12; $$21--) {
               int $$22 = $$9 ? Mth.ceil($$14 * (1.0F - (float)Math.pow($$21, 2.0) / ($$12 * 8.0F))) : $$14;
               int $$23 = this.heightDependentRadiusSteep($$3, -$$21, $$12, $$13);
               if ($$19 < $$23) {
                  this.generateIcebergBlock($$2, $$3, $$1, $$12, $$19, $$21, $$20, $$23, $$22, $$9, $$8, $$6, $$4, $$5);
               }
            }
         }
      }

      boolean $$24 = $$9 ? $$3.nextDouble() > 0.1 : $$3.nextDouble() > 0.7;
      if ($$24) {
         this.generateCutOut($$3, $$2, $$13, $$11, $$1, $$9, $$7, $$6, $$8);
      }

      return true;
   }

   private void generateCutOut(
      RandomSource p_225100_,
      LevelAccessor p_225101_,
      int p_225102_,
      int p_225103_,
      BlockPos p_225104_,
      boolean p_225105_,
      int p_225106_,
      double p_225107_,
      int p_225108_
   ) {
      int $$9 = p_225100_.nextBoolean() ? -1 : 1;
      int $$10 = p_225100_.nextBoolean() ? -1 : 1;
      int $$11 = p_225100_.nextInt(Math.max(p_225102_ / 2 - 2, 1));
      if (p_225100_.nextBoolean()) {
         $$11 = p_225102_ / 2 + 1 - p_225100_.nextInt(Math.max(p_225102_ - p_225102_ / 2 - 1, 1));
      }

      int $$12 = p_225100_.nextInt(Math.max(p_225102_ / 2 - 2, 1));
      if (p_225100_.nextBoolean()) {
         $$12 = p_225102_ / 2 + 1 - p_225100_.nextInt(Math.max(p_225102_ - p_225102_ / 2 - 1, 1));
      }

      if (p_225105_) {
         $$11 = $$12 = p_225100_.nextInt(Math.max(p_225106_ - 5, 1));
      }

      BlockPos $$13 = new BlockPos($$9 * $$11, 0, $$10 * $$12);
      double $$14 = p_225105_ ? p_225107_ + (Math.PI / 2) : p_225100_.nextDouble() * 2.0 * Math.PI;

      for (int $$15 = 0; $$15 < p_225103_ - 3; $$15++) {
         int $$16 = this.heightDependentRadiusRound(p_225100_, $$15, p_225103_, p_225102_);
         this.carve($$16, $$15, p_225104_, p_225101_, false, $$14, $$13, p_225106_, p_225108_);
      }

      for (int $$17 = -1; $$17 > -p_225103_ + p_225100_.nextInt(5); $$17--) {
         int $$18 = this.heightDependentRadiusSteep(p_225100_, -$$17, p_225103_, p_225102_);
         this.carve($$18, $$17, p_225104_, p_225101_, true, $$14, $$13, p_225106_, p_225108_);
      }
   }

   private void carve(
      int p_66036_, int p_66037_, BlockPos p_66038_, LevelAccessor p_66039_, boolean p_66040_, double p_66041_, BlockPos p_66042_, int p_66043_, int p_66044_
   ) {
      int $$9 = p_66036_ + 1 + p_66043_ / 3;
      int $$10 = Math.min(p_66036_ - 3, 3) + p_66044_ / 2 - 1;

      for (int $$11 = -$$9; $$11 < $$9; $$11++) {
         for (int $$12 = -$$9; $$12 < $$9; $$12++) {
            double $$13 = this.signedDistanceEllipse($$11, $$12, p_66042_, $$9, $$10, p_66041_);
            if ($$13 < 0.0) {
               BlockPos $$14 = p_66038_.offset($$11, p_66037_, $$12);
               BlockState $$15 = p_66039_.getBlockState($$14);
               if (isIcebergState($$15) || $$15.is(Blocks.SNOW_BLOCK)) {
                  if (p_66040_) {
                     this.setBlock(p_66039_, $$14, Blocks.WATER.defaultBlockState());
                  } else {
                     this.setBlock(p_66039_, $$14, Blocks.AIR.defaultBlockState());
                     this.removeFloatingSnowLayer(p_66039_, $$14);
                  }
               }
            }
         }
      }
   }

   private void removeFloatingSnowLayer(LevelAccessor p_66049_, BlockPos p_66050_) {
      if (p_66049_.getBlockState(p_66050_.above()).is(Blocks.SNOW)) {
         this.setBlock(p_66049_, p_66050_.above(), Blocks.AIR.defaultBlockState());
      }
   }

   private void generateIcebergBlock(
      LevelAccessor p_225110_,
      RandomSource p_225111_,
      BlockPos p_225112_,
      int p_225113_,
      int p_225114_,
      int p_225115_,
      int p_225116_,
      int p_225117_,
      int p_225118_,
      boolean p_225119_,
      int p_225120_,
      double p_225121_,
      boolean p_225122_,
      BlockState p_225123_
   ) {
      double $$14 = p_225119_
         ? this.signedDistanceEllipse(p_225114_, p_225116_, BlockPos.ZERO, p_225118_, this.getEllipseC(p_225115_, p_225113_, p_225120_), p_225121_)
         : this.signedDistanceCircle(p_225114_, p_225116_, BlockPos.ZERO, p_225117_, p_225111_);
      if ($$14 < 0.0) {
         BlockPos $$15 = p_225112_.offset(p_225114_, p_225115_, p_225116_);
         double $$16 = p_225119_ ? -0.5 : -6 - p_225111_.nextInt(3);
         if ($$14 > $$16 && p_225111_.nextDouble() > 0.9) {
            return;
         }

         this.setIcebergBlock($$15, p_225110_, p_225111_, p_225113_ - p_225115_, p_225113_, p_225119_, p_225122_, p_225123_);
      }
   }

   private void setIcebergBlock(
      BlockPos p_225125_,
      LevelAccessor p_225126_,
      RandomSource p_225127_,
      int p_225128_,
      int p_225129_,
      boolean p_225130_,
      boolean p_225131_,
      BlockState p_225132_
   ) {
      BlockState $$8 = p_225126_.getBlockState(p_225125_);
      if ($$8.isAir() || $$8.is(Blocks.SNOW_BLOCK) || $$8.is(Blocks.ICE) || $$8.is(Blocks.WATER)) {
         boolean $$9 = !p_225130_ || p_225127_.nextDouble() > 0.05;
         int $$10 = p_225130_ ? 3 : 2;
         if (p_225131_ && !$$8.is(Blocks.WATER) && p_225128_ <= p_225127_.nextInt(Math.max(1, p_225129_ / $$10)) + p_225129_ * 0.6 && $$9) {
            this.setBlock(p_225126_, p_225125_, Blocks.SNOW_BLOCK.defaultBlockState());
         } else {
            this.setBlock(p_225126_, p_225125_, p_225132_);
         }
      }
   }

   private int getEllipseC(int p_66019_, int p_66020_, int p_66021_) {
      int $$3 = p_66021_;
      if (p_66019_ > 0 && p_66020_ - p_66019_ <= 3) {
         $$3 = p_66021_ - (4 - (p_66020_ - p_66019_));
      }

      return $$3;
   }

   private double signedDistanceCircle(int p_225089_, int p_225090_, BlockPos p_225091_, int p_225092_, RandomSource p_225093_) {
      float $$5 = 10.0F * Mth.clamp(p_225093_.nextFloat(), 0.2F, 0.8F) / p_225092_;
      return $$5 + Math.pow(p_225089_ - p_225091_.getX(), 2.0) + Math.pow(p_225090_ - p_225091_.getZ(), 2.0) - Math.pow(p_225092_, 2.0);
   }

   private double signedDistanceEllipse(int p_66023_, int p_66024_, BlockPos p_66025_, int p_66026_, int p_66027_, double p_66028_) {
      return Math.pow(((p_66023_ - p_66025_.getX()) * Math.cos(p_66028_) - (p_66024_ - p_66025_.getZ()) * Math.sin(p_66028_)) / p_66026_, 2.0)
         + Math.pow(((p_66023_ - p_66025_.getX()) * Math.sin(p_66028_) + (p_66024_ - p_66025_.getZ()) * Math.cos(p_66028_)) / p_66027_, 2.0)
         - 1.0;
   }

   private int heightDependentRadiusRound(RandomSource p_225095_, int p_225096_, int p_225097_, int p_225098_) {
      float $$4 = 3.5F - p_225095_.nextFloat();
      float $$5 = (1.0F - (float)Math.pow(p_225096_, 2.0) / (p_225097_ * $$4)) * p_225098_;
      if (p_225097_ > 15 + p_225095_.nextInt(5)) {
         int $$6 = p_225096_ < 3 + p_225095_.nextInt(6) ? p_225096_ / 2 : p_225096_;
         $$5 = (1.0F - $$6 / (p_225097_ * $$4 * 0.4F)) * p_225098_;
      }

      return Mth.ceil($$5 / 2.0F);
   }

   private int heightDependentRadiusEllipse(int p_66110_, int p_66111_, int p_66112_) {
      float $$3 = 1.0F;
      float $$4 = (1.0F - (float)Math.pow(p_66110_, 2.0) / (p_66111_ * 1.0F)) * p_66112_;
      return Mth.ceil($$4 / 2.0F);
   }

   private int heightDependentRadiusSteep(RandomSource p_225134_, int p_225135_, int p_225136_, int p_225137_) {
      float $$4 = 1.0F + p_225134_.nextFloat() / 2.0F;
      float $$5 = (1.0F - p_225135_ / (p_225136_ * $$4)) * p_225137_;
      return Mth.ceil($$5 / 2.0F);
   }

   private static boolean isIcebergState(BlockState p_159886_) {
      return p_159886_.is(Blocks.PACKED_ICE) || p_159886_.is(Blocks.SNOW_BLOCK) || p_159886_.is(Blocks.BLUE_ICE);
   }

   private boolean belowIsAir(BlockGetter p_66046_, BlockPos p_66047_) {
      return p_66046_.getBlockState(p_66047_.below()).isAir();
   }

   private void smooth(LevelAccessor p_66052_, BlockPos p_66053_, int p_66054_, int p_66055_, boolean p_66056_, int p_66057_) {
      int $$6 = p_66056_ ? p_66057_ : p_66054_ / 2;

      for (int $$7 = -$$6; $$7 <= $$6; $$7++) {
         for (int $$8 = -$$6; $$8 <= $$6; $$8++) {
            for (int $$9 = 0; $$9 <= p_66055_; $$9++) {
               BlockPos $$10 = p_66053_.offset($$7, $$9, $$8);
               BlockState $$11 = p_66052_.getBlockState($$10);
               if (isIcebergState($$11) || $$11.is(Blocks.SNOW)) {
                  if (this.belowIsAir(p_66052_, $$10)) {
                     this.setBlock(p_66052_, $$10, Blocks.AIR.defaultBlockState());
                     this.setBlock(p_66052_, $$10.above(), Blocks.AIR.defaultBlockState());
                  } else if (isIcebergState($$11)) {
                     BlockState[] $$12 = new BlockState[]{
                        p_66052_.getBlockState($$10.west()),
                        p_66052_.getBlockState($$10.east()),
                        p_66052_.getBlockState($$10.north()),
                        p_66052_.getBlockState($$10.south())
                     };
                     int $$13 = 0;

                     for (BlockState $$14 : $$12) {
                        if (!isIcebergState($$14)) {
                           $$13++;
                        }
                     }

                     if ($$13 >= 3) {
                        this.setBlock(p_66052_, $$10, Blocks.AIR.defaultBlockState());
                     }
                  }
               }
            }
         }
      }
   }
}
