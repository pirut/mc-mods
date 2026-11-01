package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FallenTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;

public class FallenTreeFeature extends Feature<FallenTreeConfiguration> {
   private static final int STUMP_HEIGHT = 1;
   private static final int STUMP_HEIGHT_PLUS_EMPTY_SPACE = 2;
   private static final int FALLEN_LOG_MAX_FALL_HEIGHT_TO_GROUND = 5;
   private static final int FALLEN_LOG_MAX_GROUND_GAP = 2;
   private static final int FALLEN_LOG_MAX_SPACE_FROM_STUMP = 2;

   public FallenTreeFeature(Codec<FallenTreeConfiguration> p_410682_) {
      super(p_410682_);
   }

   @Override
   public boolean place(FeaturePlaceContext<FallenTreeConfiguration> p_410767_) {
      this.placeFallenTree(p_410767_.config(), p_410767_.origin(), p_410767_.level(), p_410767_.random());
      return true;
   }

   private void placeFallenTree(FallenTreeConfiguration p_409669_, BlockPos p_410693_, WorldGenLevel p_410144_, RandomSource p_410140_) {
      this.placeStump(p_409669_, p_410144_, p_410140_, p_410693_.mutable());
      Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(p_410140_);
      int i = p_409669_.logLength.sample(p_410140_) - 2;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_410693_.relative(direction, 2 + p_410140_.nextInt(2)).mutable();
      this.setGroundHeightForFallenLogStartPos(p_410144_, blockpos$mutableblockpos);
      if (this.canPlaceEntireFallenLog(p_410144_, i, blockpos$mutableblockpos, direction)) {
         this.placeFallenLog(p_409669_, p_410144_, p_410140_, i, blockpos$mutableblockpos, direction);
      }
   }

   private void setGroundHeightForFallenLogStartPos(WorldGenLevel p_410173_, BlockPos.MutableBlockPos p_410323_) {
      p_410323_.move(Direction.UP, 1);

      for (int i = 0; i < 6; i++) {
         if (this.mayPlaceOn(p_410173_, p_410323_)) {
            return;
         }

         p_410323_.move(Direction.DOWN);
      }
   }

   private void placeStump(FallenTreeConfiguration p_409699_, WorldGenLevel p_409643_, RandomSource p_409875_, BlockPos.MutableBlockPos p_409773_) {
      BlockPos blockpos = this.placeLogBlock(p_409699_, p_409643_, p_409875_, p_409773_, Function.identity());
      this.decorateLogs(p_409643_, p_409875_, Set.of(blockpos), p_409699_.stumpDecorators);
   }

   private boolean canPlaceEntireFallenLog(WorldGenLevel p_409872_, int p_409631_, BlockPos.MutableBlockPos p_409954_, Direction p_409753_) {
      int i = 0;

      for (int j = 0; j < p_409631_; j++) {
         if (!TreeFeature.validTreePos(p_409872_, p_409954_)) {
            return false;
         }

         if (!this.isOverSolidGround(p_409872_, p_409954_)) {
            if (++i > 2) {
               return false;
            }
         } else {
            i = 0;
         }

         p_409954_.move(p_409753_);
      }

      p_409954_.move(p_409753_.getOpposite(), p_409631_);
      return true;
   }

   private void placeFallenLog(
      FallenTreeConfiguration p_410657_,
      WorldGenLevel p_409594_,
      RandomSource p_410124_,
      int p_410497_,
      BlockPos.MutableBlockPos p_410509_,
      Direction p_409944_
   ) {
      Set<BlockPos> set = new HashSet<>();

      for (int i = 0; i < p_410497_; i++) {
         set.add(this.placeLogBlock(p_410657_, p_409594_, p_410124_, p_410509_, getSidewaysStateModifier(p_409944_)));
         p_410509_.move(p_409944_);
      }

      this.decorateLogs(p_409594_, p_410124_, set, p_410657_.logDecorators);
   }

   private boolean mayPlaceOn(LevelAccessor p_409861_, BlockPos p_410049_) {
      return TreeFeature.validTreePos(p_409861_, p_410049_) && this.isOverSolidGround(p_409861_, p_410049_);
   }

   private boolean isOverSolidGround(LevelAccessor p_410076_, BlockPos p_409697_) {
      return p_410076_.getBlockState(p_409697_.below()).isFaceSturdy(p_410076_, p_409697_, Direction.UP);
   }

   private BlockPos placeLogBlock(
      FallenTreeConfiguration p_410467_,
      WorldGenLevel p_410545_,
      RandomSource p_409840_,
      BlockPos.MutableBlockPos p_410701_,
      Function<BlockState, BlockState> p_410338_
   ) {
      p_410545_.setBlock(p_410701_, p_410338_.apply(p_410467_.trunkProvider.getState(p_409840_, p_410701_)), 3);
      this.markAboveForPostProcessing(p_410545_, p_410701_);
      return p_410701_.immutable();
   }

   private void decorateLogs(WorldGenLevel p_409797_, RandomSource p_410805_, Set<BlockPos> p_409916_, List<TreeDecorator> p_409800_) {
      if (!p_409800_.isEmpty()) {
         TreeDecorator.Context treedecorator$context = new TreeDecorator.Context(
            p_409797_, this.getDecorationSetter(p_409797_), p_410805_, p_409916_, Set.of(), Set.of()
         );
         p_409800_.forEach(p_409702_ -> p_409702_.place(treedecorator$context));
      }
   }

   private BiConsumer<BlockPos, BlockState> getDecorationSetter(WorldGenLevel p_409737_) {
      return (p_409651_, p_410621_) -> p_409737_.setBlock(p_409651_, p_410621_, 19);
   }

   private static Function<BlockState, BlockState> getSidewaysStateModifier(Direction p_410699_) {
      return p_410651_ -> p_410651_.trySetValue(RotatedPillarBlock.AXIS, p_410699_.getAxis());
   }
}
