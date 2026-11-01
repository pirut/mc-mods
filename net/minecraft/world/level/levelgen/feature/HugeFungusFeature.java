package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class HugeFungusFeature extends Feature<HugeFungusConfiguration> {
   private static final float HUGE_PROBABILITY = 0.06F;

   public HugeFungusFeature(Codec<HugeFungusConfiguration> p_65922_) {
      super(p_65922_);
   }

   @Override
   public boolean place(FeaturePlaceContext<HugeFungusConfiguration> p_159878_) {
      WorldGenLevel $$1 = p_159878_.level();
      BlockPos $$2 = p_159878_.origin();
      RandomSource $$3 = p_159878_.random();
      ChunkGenerator $$4 = p_159878_.chunkGenerator();
      HugeFungusConfiguration $$5 = p_159878_.config();
      Block $$6 = $$5.validBaseState.getBlock();
      BlockPos $$7 = null;
      BlockState $$8 = $$1.getBlockState($$2.below());
      if ($$8.is($$6)) {
         $$7 = $$2;
      }

      if ($$7 == null) {
         return false;
      } else {
         int $$9 = Mth.nextInt($$3, 4, 13);
         if ($$3.nextInt(12) == 0) {
            $$9 *= 2;
         }

         if (!$$5.planted) {
            int $$10 = $$4.getGenDepth();
            if ($$7.getY() + $$9 + 1 >= $$10) {
               return false;
            }
         }

         boolean $$11 = !$$5.planted && $$3.nextFloat() < 0.06F;
         $$1.setBlock($$2, Blocks.AIR.defaultBlockState(), 4);
         this.placeStem($$1, $$3, $$5, $$7, $$9, $$11);
         this.placeHat($$1, $$3, $$5, $$7, $$9, $$11);
         return true;
      }
   }

   private static boolean isReplaceable(WorldGenLevel p_285049_, BlockPos p_285309_, HugeFungusConfiguration p_284992_, boolean p_285162_) {
      if (p_285049_.isStateAtPosition(p_285309_, BlockBehaviour.BlockStateBase::canBeReplaced)) {
         return true;
      } else {
         return p_285162_ ? p_284992_.replaceableBlocks.test(p_285049_, p_285309_) : false;
      }
   }

   private void placeStem(
      WorldGenLevel p_285364_, RandomSource p_285032_, HugeFungusConfiguration p_285198_, BlockPos p_285090_, int p_285249_, boolean p_285355_
   ) {
      BlockPos.MutableBlockPos $$6 = new BlockPos.MutableBlockPos();
      BlockState $$7 = p_285198_.stemState;
      int $$8 = p_285355_ ? 1 : 0;

      for (int $$9 = -$$8; $$9 <= $$8; $$9++) {
         for (int $$10 = -$$8; $$10 <= $$8; $$10++) {
            boolean $$11 = p_285355_ && Mth.abs($$9) == $$8 && Mth.abs($$10) == $$8;

            for (int $$12 = 0; $$12 < p_285249_; $$12++) {
               $$6.setWithOffset(p_285090_, $$9, $$12, $$10);
               if (isReplaceable(p_285364_, $$6, p_285198_, true)) {
                  if (p_285198_.planted) {
                     if (!p_285364_.getBlockState($$6.below()).isAir()) {
                        p_285364_.destroyBlock($$6, true);
                     }

                     p_285364_.setBlock($$6, $$7, 3);
                  } else if ($$11) {
                     if (p_285032_.nextFloat() < 0.1F) {
                        this.setBlock(p_285364_, $$6, $$7);
                     }
                  } else {
                     this.setBlock(p_285364_, $$6, $$7);
                  }
               }
            }
         }
      }
   }

   private void placeHat(
      WorldGenLevel p_285200_, RandomSource p_285456_, HugeFungusConfiguration p_285146_, BlockPos p_285097_, int p_285156_, boolean p_285265_
   ) {
      BlockPos.MutableBlockPos $$6 = new BlockPos.MutableBlockPos();
      boolean $$7 = p_285146_.hatState.is(Blocks.NETHER_WART_BLOCK);
      int $$8 = Math.min(p_285456_.nextInt(1 + p_285156_ / 3) + 5, p_285156_);
      int $$9 = p_285156_ - $$8;

      for (int $$10 = $$9; $$10 <= p_285156_; $$10++) {
         int $$11 = $$10 < p_285156_ - p_285456_.nextInt(3) ? 2 : 1;
         if ($$8 > 8 && $$10 < $$9 + 4) {
            $$11 = 3;
         }

         if (p_285265_) {
            $$11++;
         }

         for (int $$12 = -$$11; $$12 <= $$11; $$12++) {
            for (int $$13 = -$$11; $$13 <= $$11; $$13++) {
               boolean $$14 = $$12 == -$$11 || $$12 == $$11;
               boolean $$15 = $$13 == -$$11 || $$13 == $$11;
               boolean $$16 = !$$14 && !$$15 && $$10 != p_285156_;
               boolean $$17 = $$14 && $$15;
               boolean $$18 = $$10 < $$9 + 3;
               $$6.setWithOffset(p_285097_, $$12, $$10, $$13);
               if (isReplaceable(p_285200_, $$6, p_285146_, false)) {
                  if (p_285146_.planted && !p_285200_.getBlockState($$6.below()).isAir()) {
                     p_285200_.destroyBlock($$6, true);
                  }

                  if ($$18) {
                     if (!$$16) {
                        this.placeHatDropBlock(p_285200_, p_285456_, $$6, p_285146_.hatState, $$7);
                     }
                  } else if ($$16) {
                     this.placeHatBlock(p_285200_, p_285456_, p_285146_, $$6, 0.1F, 0.2F, $$7 ? 0.1F : 0.0F);
                  } else if ($$17) {
                     this.placeHatBlock(p_285200_, p_285456_, p_285146_, $$6, 0.01F, 0.7F, $$7 ? 0.083F : 0.0F);
                  } else {
                     this.placeHatBlock(p_285200_, p_285456_, p_285146_, $$6, 5.0E-4F, 0.98F, $$7 ? 0.07F : 0.0F);
                  }
               }
            }
         }
      }
   }

   private void placeHatBlock(
      LevelAccessor p_225050_,
      RandomSource p_225051_,
      HugeFungusConfiguration p_225052_,
      BlockPos.MutableBlockPos p_225053_,
      float p_225054_,
      float p_225055_,
      float p_225056_
   ) {
      if (p_225051_.nextFloat() < p_225054_) {
         this.setBlock(p_225050_, p_225053_, p_225052_.decorState);
      } else if (p_225051_.nextFloat() < p_225055_) {
         this.setBlock(p_225050_, p_225053_, p_225052_.hatState);
         if (p_225051_.nextFloat() < p_225056_) {
            tryPlaceWeepingVines(p_225053_, p_225050_, p_225051_);
         }
      }
   }

   private void placeHatDropBlock(LevelAccessor p_225065_, RandomSource p_225066_, BlockPos p_225067_, BlockState p_225068_, boolean p_225069_) {
      if (p_225065_.getBlockState(p_225067_.below()).is(p_225068_.getBlock())) {
         this.setBlock(p_225065_, p_225067_, p_225068_);
      } else if (p_225066_.nextFloat() < 0.15) {
         this.setBlock(p_225065_, p_225067_, p_225068_);
         if (p_225069_ && p_225066_.nextInt(11) == 0) {
            tryPlaceWeepingVines(p_225067_, p_225065_, p_225066_);
         }
      }
   }

   private static void tryPlaceWeepingVines(BlockPos p_225071_, LevelAccessor p_225072_, RandomSource p_225073_) {
      BlockPos.MutableBlockPos $$3 = p_225071_.mutable().move(Direction.DOWN);
      if (p_225072_.isEmptyBlock($$3)) {
         int $$4 = Mth.nextInt(p_225073_, 1, 5);
         if (p_225073_.nextInt(7) == 0) {
            $$4 *= 2;
         }

         int $$5 = 23;
         int $$6 = 25;
         WeepingVinesFeature.placeWeepingVinesColumn(p_225072_, p_225073_, $$3, $$4, 23, 25);
      }
   }
}
