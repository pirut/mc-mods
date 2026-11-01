package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class WeepingVinesFeature extends Feature<NoneFeatureConfiguration> {
   private static final Direction[] DIRECTIONS = Direction.values();

   public WeepingVinesFeature(Codec<NoneFeatureConfiguration> p_67375_) {
      super(p_67375_);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_160661_) {
      WorldGenLevel $$1 = p_160661_.level();
      BlockPos $$2 = p_160661_.origin();
      RandomSource $$3 = p_160661_.random();
      if (!$$1.isEmptyBlock($$2)) {
         return false;
      } else {
         BlockState $$4 = $$1.getBlockState($$2.above());
         if (!$$4.is(Blocks.NETHERRACK) && !$$4.is(Blocks.NETHER_WART_BLOCK)) {
            return false;
         } else {
            this.placeRoofNetherWart($$1, $$3, $$2);
            this.placeRoofWeepingVines($$1, $$3, $$2);
            return true;
         }
      }
   }

   private void placeRoofNetherWart(LevelAccessor p_225360_, RandomSource p_225361_, BlockPos p_225362_) {
      p_225360_.setBlock(p_225362_, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
      BlockPos.MutableBlockPos $$3 = new BlockPos.MutableBlockPos();
      BlockPos.MutableBlockPos $$4 = new BlockPos.MutableBlockPos();

      for (int $$5 = 0; $$5 < 200; $$5++) {
         $$3.setWithOffset(
            p_225362_, p_225361_.nextInt(6) - p_225361_.nextInt(6), p_225361_.nextInt(2) - p_225361_.nextInt(5), p_225361_.nextInt(6) - p_225361_.nextInt(6)
         );
         if (p_225360_.isEmptyBlock($$3)) {
            int $$6 = 0;

            for (Direction $$7 : DIRECTIONS) {
               BlockState $$8 = p_225360_.getBlockState($$4.setWithOffset($$3, $$7));
               if ($$8.is(Blocks.NETHERRACK) || $$8.is(Blocks.NETHER_WART_BLOCK)) {
                  $$6++;
               }

               if ($$6 > 1) {
                  break;
               }
            }

            if ($$6 == 1) {
               p_225360_.setBlock($$3, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
            }
         }
      }
   }

   private void placeRoofWeepingVines(LevelAccessor p_225364_, RandomSource p_225365_, BlockPos p_225366_) {
      BlockPos.MutableBlockPos $$3 = new BlockPos.MutableBlockPos();

      for (int $$4 = 0; $$4 < 100; $$4++) {
         $$3.setWithOffset(
            p_225366_, p_225365_.nextInt(8) - p_225365_.nextInt(8), p_225365_.nextInt(2) - p_225365_.nextInt(7), p_225365_.nextInt(8) - p_225365_.nextInt(8)
         );
         if (p_225364_.isEmptyBlock($$3)) {
            BlockState $$5 = p_225364_.getBlockState($$3.above());
            if ($$5.is(Blocks.NETHERRACK) || $$5.is(Blocks.NETHER_WART_BLOCK)) {
               int $$6 = Mth.nextInt(p_225365_, 1, 8);
               if (p_225365_.nextInt(6) == 0) {
                  $$6 *= 2;
               }

               if (p_225365_.nextInt(5) == 0) {
                  $$6 = 1;
               }

               int $$7 = 17;
               int $$8 = 25;
               placeWeepingVinesColumn(p_225364_, p_225365_, $$3, $$6, 17, 25);
            }
         }
      }
   }

   public static void placeWeepingVinesColumn(
      LevelAccessor p_225353_, RandomSource p_225354_, BlockPos.MutableBlockPos p_225355_, int p_225356_, int p_225357_, int p_225358_
   ) {
      for (int $$6 = 0; $$6 <= p_225356_; $$6++) {
         if (p_225353_.isEmptyBlock(p_225355_)) {
            if ($$6 == p_225356_ || !p_225353_.isEmptyBlock(p_225355_.below())) {
               p_225353_.setBlock(
                  p_225355_, Blocks.WEEPING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, Mth.nextInt(p_225354_, p_225357_, p_225358_)), 2
               );
               break;
            }

            p_225353_.setBlock(p_225355_, Blocks.WEEPING_VINES_PLANT.defaultBlockState(), 2);
         }

         p_225355_.move(Direction.DOWN);
      }
   }
}
