package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RootSystemConfiguration;

public class RootSystemFeature extends Feature<RootSystemConfiguration> {
   public RootSystemFeature(Codec<RootSystemConfiguration> p_160218_) {
      super(p_160218_);
   }

   @Override
   public boolean place(FeaturePlaceContext<RootSystemConfiguration> p_160257_) {
      WorldGenLevel $$1 = p_160257_.level();
      BlockPos $$2 = p_160257_.origin();
      if (!$$1.getBlockState($$2).isAir()) {
         return false;
      } else {
         RandomSource $$3 = p_160257_.random();
         BlockPos $$4 = p_160257_.origin();
         RootSystemConfiguration $$5 = p_160257_.config();
         BlockPos.MutableBlockPos $$6 = $$4.mutable();
         if (placeDirtAndTree($$1, p_160257_.chunkGenerator(), $$5, $$3, $$6, $$4)) {
            placeRoots($$1, $$5, $$3, $$4, $$6);
         }

         return true;
      }
   }

   private static boolean spaceForTree(WorldGenLevel p_160236_, RootSystemConfiguration p_160237_, BlockPos p_160238_) {
      BlockPos.MutableBlockPos $$3 = p_160238_.mutable();

      for (int $$4 = 1; $$4 <= p_160237_.requiredVerticalSpaceForTree; $$4++) {
         $$3.move(Direction.UP);
         BlockState $$5 = p_160236_.getBlockState($$3);
         if (!isAllowedTreeSpace($$5, $$4, p_160237_.allowedVerticalWaterForTree)) {
            return false;
         }
      }

      return true;
   }

   private static boolean isAllowedTreeSpace(BlockState p_160253_, int p_160254_, int p_160255_) {
      if (p_160253_.isAir()) {
         return true;
      } else {
         int $$3 = p_160254_ + 1;
         return $$3 <= p_160255_ && p_160253_.getFluidState().is(FluidTags.WATER);
      }
   }

   private static boolean placeDirtAndTree(
      WorldGenLevel p_225203_,
      ChunkGenerator p_225204_,
      RootSystemConfiguration p_225205_,
      RandomSource p_225206_,
      BlockPos.MutableBlockPos p_225207_,
      BlockPos p_225208_
   ) {
      for (int $$6 = 0; $$6 < p_225205_.rootColumnMaxHeight; $$6++) {
         p_225207_.move(Direction.UP);
         if (p_225205_.allowedTreePosition.test(p_225203_, p_225207_) && spaceForTree(p_225203_, p_225205_, p_225207_)) {
            BlockPos $$7 = p_225207_.below();
            if (p_225203_.getFluidState($$7).is(FluidTags.LAVA) || !p_225203_.getBlockState($$7).isSolid()) {
               return false;
            }

            if (p_225205_.treeFeature.value().place(p_225203_, p_225204_, p_225206_, p_225207_)) {
               placeDirt(p_225208_, p_225208_.getY() + $$6, p_225203_, p_225205_, p_225206_);
               return true;
            }
         }
      }

      return false;
   }

   private static void placeDirt(BlockPos p_225223_, int p_225224_, WorldGenLevel p_225225_, RootSystemConfiguration p_225226_, RandomSource p_225227_) {
      int $$5 = p_225223_.getX();
      int $$6 = p_225223_.getZ();
      BlockPos.MutableBlockPos $$7 = p_225223_.mutable();

      for (int $$8 = p_225223_.getY(); $$8 < p_225224_; $$8++) {
         placeRootedDirt(p_225225_, p_225226_, p_225227_, $$5, $$6, $$7.set($$5, $$8, $$6));
      }
   }

   private static void placeRootedDirt(
      WorldGenLevel p_225210_, RootSystemConfiguration p_225211_, RandomSource p_225212_, int p_225213_, int p_225214_, BlockPos.MutableBlockPos p_225215_
   ) {
      int $$6 = p_225211_.rootRadius;
      Predicate<BlockState> $$7 = p_204762_ -> p_204762_.is(p_225211_.rootReplaceable);

      for (int $$8 = 0; $$8 < p_225211_.rootPlacementAttempts; $$8++) {
         p_225215_.setWithOffset(p_225215_, p_225212_.nextInt($$6) - p_225212_.nextInt($$6), 0, p_225212_.nextInt($$6) - p_225212_.nextInt($$6));
         if ($$7.test(p_225210_.getBlockState(p_225215_))) {
            p_225210_.setBlock(p_225215_, p_225211_.rootStateProvider.getState(p_225212_, p_225215_), 2);
         }

         p_225215_.setX(p_225213_);
         p_225215_.setZ(p_225214_);
      }
   }

   private static void placeRoots(
      WorldGenLevel p_225217_, RootSystemConfiguration p_225218_, RandomSource p_225219_, BlockPos p_225220_, BlockPos.MutableBlockPos p_225221_
   ) {
      int $$5 = p_225218_.hangingRootRadius;
      int $$6 = p_225218_.hangingRootsVerticalSpan;

      for (int $$7 = 0; $$7 < p_225218_.hangingRootPlacementAttempts; $$7++) {
         p_225221_.setWithOffset(
            p_225220_,
            p_225219_.nextInt($$5) - p_225219_.nextInt($$5),
            p_225219_.nextInt($$6) - p_225219_.nextInt($$6),
            p_225219_.nextInt($$5) - p_225219_.nextInt($$5)
         );
         if (p_225217_.isEmptyBlock(p_225221_)) {
            BlockState $$8 = p_225218_.hangingRootStateProvider.getState(p_225219_, p_225221_);
            if ($$8.canSurvive(p_225217_, p_225221_) && p_225217_.getBlockState(p_225221_.above()).isFaceSturdy(p_225217_, p_225221_, Direction.DOWN)) {
               p_225217_.setBlock(p_225221_, $$8, 2);
            }
         }
      }
   }
}
