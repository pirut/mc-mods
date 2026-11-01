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
import net.minecraft.world.level.levelgen.feature.configurations.TwistingVinesConfig;

public class TwistingVinesFeature extends Feature<TwistingVinesConfig> {
   public TwistingVinesFeature(Codec<TwistingVinesConfig> p_67292_) {
      super(p_67292_);
   }

   @Override
   public boolean place(FeaturePlaceContext<TwistingVinesConfig> p_160558_) {
      WorldGenLevel $$1 = p_160558_.level();
      BlockPos $$2 = p_160558_.origin();
      if (isInvalidPlacementLocation($$1, $$2)) {
         return false;
      } else {
         RandomSource $$3 = p_160558_.random();
         TwistingVinesConfig $$4 = p_160558_.config();
         int $$5 = $$4.spreadWidth();
         int $$6 = $$4.spreadHeight();
         int $$7 = $$4.maxHeight();
         BlockPos.MutableBlockPos $$8 = new BlockPos.MutableBlockPos();

         for (int $$9 = 0; $$9 < $$5 * $$5; $$9++) {
            $$8.set($$2).move(Mth.nextInt($$3, -$$5, $$5), Mth.nextInt($$3, -$$6, $$6), Mth.nextInt($$3, -$$5, $$5));
            if (findFirstAirBlockAboveGround($$1, $$8) && !isInvalidPlacementLocation($$1, $$8)) {
               int $$10 = Mth.nextInt($$3, 1, $$7);
               if ($$3.nextInt(6) == 0) {
                  $$10 *= 2;
               }

               if ($$3.nextInt(5) == 0) {
                  $$10 = 1;
               }

               int $$11 = 17;
               int $$12 = 25;
               placeWeepingVinesColumn($$1, $$3, $$8, $$10, 17, 25);
            }
         }

         return true;
      }
   }

   private static boolean findFirstAirBlockAboveGround(LevelAccessor p_67294_, BlockPos.MutableBlockPos p_67295_) {
      do {
         p_67295_.move(0, -1, 0);
         if (p_67294_.isOutsideBuildHeight(p_67295_)) {
            return false;
         }
      } while (p_67294_.getBlockState(p_67295_).isAir());

      p_67295_.move(0, 1, 0);
      return true;
   }

   public static void placeWeepingVinesColumn(
      LevelAccessor p_225301_, RandomSource p_225302_, BlockPos.MutableBlockPos p_225303_, int p_225304_, int p_225305_, int p_225306_
   ) {
      for (int $$6 = 1; $$6 <= p_225304_; $$6++) {
         if (p_225301_.isEmptyBlock(p_225303_)) {
            if ($$6 == p_225304_ || !p_225301_.isEmptyBlock(p_225303_.above())) {
               p_225301_.setBlock(
                  p_225303_, Blocks.TWISTING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, Mth.nextInt(p_225302_, p_225305_, p_225306_)), 2
               );
               break;
            }

            p_225301_.setBlock(p_225303_, Blocks.TWISTING_VINES_PLANT.defaultBlockState(), 2);
         }

         p_225303_.move(Direction.UP);
      }
   }

   private static boolean isInvalidPlacementLocation(LevelAccessor p_67297_, BlockPos p_67298_) {
      if (!p_67297_.isEmptyBlock(p_67298_)) {
         return true;
      } else {
         BlockState $$2 = p_67297_.getBlockState(p_67298_.below());
         return !$$2.is(Blocks.NETHERRACK) && !$$2.is(Blocks.WARPED_NYLIUM) && !$$2.is(Blocks.WARPED_WART_BLOCK);
      }
   }
}
