package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class CoralTreeFeature extends CoralFeature {
   public CoralTreeFeature(Codec<NoneFeatureConfiguration> p_65488_) {
      super(p_65488_);
   }

   @Override
   protected boolean placeFeature(LevelAccessor p_224987_, RandomSource p_224988_, BlockPos p_224989_, BlockState p_224990_) {
      BlockPos.MutableBlockPos $$4 = p_224989_.mutable();
      int $$5 = p_224988_.nextInt(3) + 1;

      for (int $$6 = 0; $$6 < $$5; $$6++) {
         if (!this.placeCoralBlock(p_224987_, p_224988_, $$4, p_224990_)) {
            return true;
         }

         $$4.move(Direction.UP);
      }

      BlockPos $$7 = $$4.immutable();
      int $$8 = p_224988_.nextInt(3) + 2;
      List<Direction> $$9 = Direction.Plane.HORIZONTAL.shuffledCopy(p_224988_);

      for (Direction $$11 : $$9.subList(0, $$8)) {
         $$4.set($$7);
         $$4.move($$11);
         int $$12 = p_224988_.nextInt(5) + 2;
         int $$13 = 0;

         for (int $$14 = 0; $$14 < $$12 && this.placeCoralBlock(p_224987_, p_224988_, $$4, p_224990_); $$14++) {
            $$13++;
            $$4.move(Direction.UP);
            if ($$14 == 0 || $$13 >= 2 && p_224988_.nextFloat() < 0.25F) {
               $$4.move($$11);
               $$13 = 0;
            }
         }
      }

      return true;
   }
}
