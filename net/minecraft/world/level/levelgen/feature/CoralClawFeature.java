package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CoralClawFeature extends CoralFeature {
   public CoralClawFeature(Codec<NoneFeatureConfiguration> p_65422_) {
      super(p_65422_);
   }

   @Override
   protected boolean placeFeature(LevelAccessor p_224959_, RandomSource p_224960_, BlockPos p_224961_, BlockState p_224962_) {
      if (!this.placeCoralBlock(p_224959_, p_224960_, p_224961_, p_224962_)) {
         return false;
      } else {
         Direction $$4 = Direction.Plane.HORIZONTAL.getRandomDirection(p_224960_);
         int $$5 = p_224960_.nextInt(2) + 2;
         List<Direction> $$6 = Util.toShuffledList(Stream.of($$4, $$4.getClockWise(), $$4.getCounterClockWise()), p_224960_);

         for (Direction $$8 : $$6.subList(0, $$5)) {
            BlockPos.MutableBlockPos $$9 = p_224961_.mutable();
            int $$10 = p_224960_.nextInt(2) + 1;
            $$9.move($$8);
            int $$12;
            Direction $$11;
            if ($$8 == $$4) {
               $$11 = $$4;
               $$12 = p_224960_.nextInt(3) + 2;
            } else {
               $$9.move(Direction.UP);
               Direction[] $$13 = new Direction[]{$$8, Direction.UP};
               $$11 = Util.getRandom($$13, p_224960_);
               $$12 = p_224960_.nextInt(3) + 3;
            }

            for (int $$16 = 0; $$16 < $$10 && this.placeCoralBlock(p_224959_, p_224960_, $$9, p_224962_); $$16++) {
               $$9.move($$11);
            }

            $$9.move($$11.getOpposite());
            $$9.move(Direction.UP);

            for (int $$17 = 0; $$17 < $$12; $$17++) {
               $$9.move($$4);
               if (!this.placeCoralBlock(p_224959_, p_224960_, $$9, p_224962_)) {
                  break;
               }

               if (p_224960_.nextFloat() < 0.25F) {
                  $$9.move(Direction.UP);
               }
            }
         }

         return true;
      }
   }
}
