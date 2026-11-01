package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CoralClawFeature extends CoralFeature {
   public CoralClawFeature(Codec<NoneFeatureConfiguration> codec) {
      super(codec);
   }

   @Override
   protected boolean placeFeature(LevelAccessor level, RandomSource random, BlockPos pos, BlockState state) {
      if (!this.placeCoralBlock(level, random, pos, state)) {
         return false;
      } else {
         Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
         int i = random.nextInt(2) + 2;
         List<Direction> list = Util.toShuffledList(Stream.of(direction, direction.getClockWise(), direction.getCounterClockWise()), random);

         for (Direction direction2 : list.subList(0, i)) {
            BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();
            int j = random.nextInt(2) + 1;
            mutableBlockPos.move(direction2);
            int k;
            Direction direction3;
            if (direction2 == direction) {
               direction3 = direction;
               k = random.nextInt(3) + 2;
            } else {
               mutableBlockPos.move(Direction.UP);
               Direction[] directions = new Direction[]{direction2, Direction.UP};
               direction3 = Util.getRandom(directions, random);
               k = random.nextInt(3) + 3;
            }

            for (int l = 0; l < j && this.placeCoralBlock(level, random, mutableBlockPos, state); l++) {
               mutableBlockPos.move(direction3);
            }

            mutableBlockPos.move(direction3.getOpposite());
            mutableBlockPos.move(Direction.UP);

            for (int l = 0; l < k; l++) {
               mutableBlockPos.move(direction);
               if (!this.placeCoralBlock(level, random, mutableBlockPos, state)) {
                  break;
               }

               if (random.nextFloat() < 0.25F) {
                  mutableBlockPos.move(Direction.UP);
               }
            }
         }

         return true;
      }
   }
}
