package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public abstract class AbstractHugeMushroomFeature extends Feature<HugeMushroomFeatureConfiguration> {
   public AbstractHugeMushroomFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
      super(codec);
   }

   protected void placeTrunk(
      LevelAccessor level, RandomSource random, BlockPos pos, HugeMushroomFeatureConfiguration config, int maxHeight, BlockPos.MutableBlockPos mutablePos
   ) {
      for (int i = 0; i < maxHeight; i++) {
         mutablePos.set(pos).move(Direction.UP, i);
         this.placeMushroomBlock(level, mutablePos, config.stemProvider.getState(random, pos));
      }
   }

   protected void placeMushroomBlock(LevelAccessor level, BlockPos.MutableBlockPos mutablePos, BlockState state) {
      BlockState blockState = level.getBlockState(mutablePos);
      if (blockState.isAir() || blockState.is(BlockTags.REPLACEABLE_BY_MUSHROOMS)) {
         this.setBlock(level, mutablePos, state);
      }
   }

   protected int getTreeHeight(RandomSource random) {
      int i = random.nextInt(3) + 4;
      if (random.nextInt(12) == 0) {
         i *= 2;
      }

      return i;
   }

   protected boolean isValidPosition(
      LevelAccessor level, BlockPos pos, int maxHeight, BlockPos.MutableBlockPos mutablePos, HugeMushroomFeatureConfiguration config
   ) {
      int i = pos.getY();
      if (i >= level.getMinY() + 1 && i + maxHeight + 1 <= level.getMaxY()) {
         BlockState blockState = level.getBlockState(pos.below());
         if (!isDirt(blockState) && !blockState.is(BlockTags.MUSHROOM_GROW_BLOCK)) {
            return false;
         } else {
            for (int j = 0; j <= maxHeight; j++) {
               int k = this.getTreeRadiusForHeight(-1, -1, config.foliageRadius, j);

               for (int l = -k; l <= k; l++) {
                  for (int m = -k; m <= k; m++) {
                     BlockState blockState2 = level.getBlockState(mutablePos.setWithOffset(pos, l, j, m));
                     if (!blockState2.isAir() && !blockState2.is(BlockTags.LEAVES)) {
                        return false;
                     }
                  }
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean place(FeaturePlaceContext<HugeMushroomFeatureConfiguration> context) {
      WorldGenLevel worldGenLevel = context.level();
      BlockPos blockPos = context.origin();
      RandomSource randomSource = context.random();
      HugeMushroomFeatureConfiguration hugeMushroomFeatureConfiguration = context.config();
      int i = this.getTreeHeight(randomSource);
      BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
      if (!this.isValidPosition(worldGenLevel, blockPos, i, mutableBlockPos, hugeMushroomFeatureConfiguration)) {
         return false;
      } else {
         this.makeCap(worldGenLevel, randomSource, blockPos, i, mutableBlockPos, hugeMushroomFeatureConfiguration);
         this.placeTrunk(worldGenLevel, randomSource, blockPos, hugeMushroomFeatureConfiguration, i, mutableBlockPos);
         return true;
      }
   }

   protected abstract int getTreeRadiusForHeight(int unused, int height, int foliageRadius, int y);

   protected abstract void makeCap(
      LevelAccessor level, RandomSource random, BlockPos pos, int treeHeight, BlockPos.MutableBlockPos mutablePos, HugeMushroomFeatureConfiguration config
   );
}
