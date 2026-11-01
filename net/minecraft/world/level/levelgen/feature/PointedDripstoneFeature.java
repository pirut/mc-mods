package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.configurations.PointedDripstoneConfiguration;

public class PointedDripstoneFeature extends Feature<PointedDripstoneConfiguration> {
   public PointedDripstoneFeature(Codec<PointedDripstoneConfiguration> p_191067_) {
      super(p_191067_);
   }

   @Override
   public boolean place(FeaturePlaceContext<PointedDripstoneConfiguration> p_191078_) {
      LevelAccessor $$1 = p_191078_.level();
      BlockPos $$2 = p_191078_.origin();
      RandomSource $$3 = p_191078_.random();
      PointedDripstoneConfiguration $$4 = p_191078_.config();
      Optional<Direction> $$5 = getTipDirection($$1, $$2, $$3);
      if ($$5.isEmpty()) {
         return false;
      } else {
         BlockPos $$6 = $$2.relative($$5.get().getOpposite());
         createPatchOfDripstoneBlocks($$1, $$3, $$6, $$4);
         int $$7 = $$3.nextFloat() < $$4.chanceOfTallerDripstone && DripstoneUtils.isEmptyOrWater($$1.getBlockState($$2.relative($$5.get()))) ? 2 : 1;
         DripstoneUtils.growPointedDripstone($$1, $$2, $$5.get(), $$7, false);
         return true;
      }
   }

   private static Optional<Direction> getTipDirection(LevelAccessor p_225199_, BlockPos p_225200_, RandomSource p_225201_) {
      boolean $$3 = DripstoneUtils.isDripstoneBase(p_225199_.getBlockState(p_225200_.above()));
      boolean $$4 = DripstoneUtils.isDripstoneBase(p_225199_.getBlockState(p_225200_.below()));
      if ($$3 && $$4) {
         return Optional.of(p_225201_.nextBoolean() ? Direction.DOWN : Direction.UP);
      } else if ($$3) {
         return Optional.of(Direction.DOWN);
      } else {
         return $$4 ? Optional.of(Direction.UP) : Optional.empty();
      }
   }

   private static void createPatchOfDripstoneBlocks(
      LevelAccessor p_225194_, RandomSource p_225195_, BlockPos p_225196_, PointedDripstoneConfiguration p_225197_
   ) {
      DripstoneUtils.placeDripstoneBlockIfPossible(p_225194_, p_225196_);

      for (Direction $$4 : Direction.Plane.HORIZONTAL) {
         if (!(p_225195_.nextFloat() > p_225197_.chanceOfDirectionalSpread)) {
            BlockPos $$5 = p_225196_.relative($$4);
            DripstoneUtils.placeDripstoneBlockIfPossible(p_225194_, $$5);
            if (!(p_225195_.nextFloat() > p_225197_.chanceOfSpreadRadius2)) {
               BlockPos $$6 = $$5.relative(Direction.getRandom(p_225195_));
               DripstoneUtils.placeDripstoneBlockIfPossible(p_225194_, $$6);
               if (!(p_225195_.nextFloat() > p_225197_.chanceOfSpreadRadius3)) {
                  BlockPos $$7 = $$6.relative(Direction.getRandom(p_225195_));
                  DripstoneUtils.placeDripstoneBlockIfPossible(p_225194_, $$7);
               }
            }
         }
      }
   }
}
