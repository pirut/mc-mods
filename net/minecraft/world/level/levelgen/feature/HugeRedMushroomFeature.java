package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public class HugeRedMushroomFeature extends AbstractHugeMushroomFeature {
   public HugeRedMushroomFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
      super(codec);
   }

   @Override
   protected void makeCap(
      LevelAccessor level, RandomSource random, BlockPos pos, int treeHeight, BlockPos.MutableBlockPos mutablePos, HugeMushroomFeatureConfiguration config
   ) {
      for (int i = treeHeight - 3; i <= treeHeight; i++) {
         int j = i < treeHeight ? config.foliageRadius : config.foliageRadius - 1;
         int k = config.foliageRadius - 2;

         for (int l = -j; l <= j; l++) {
            for (int m = -j; m <= j; m++) {
               boolean bl = l == -j;
               boolean bl2 = l == j;
               boolean bl3 = m == -j;
               boolean bl4 = m == j;
               boolean bl5 = bl || bl2;
               boolean bl6 = bl3 || bl4;
               if (i >= treeHeight || bl5 != bl6) {
                  mutablePos.setWithOffset(pos, l, i, m);
                  BlockState blockState = config.capProvider.getState(random, pos);
                  if (blockState.hasProperty(HugeMushroomBlock.WEST)
                     && blockState.hasProperty(HugeMushroomBlock.EAST)
                     && blockState.hasProperty(HugeMushroomBlock.NORTH)
                     && blockState.hasProperty(HugeMushroomBlock.SOUTH)
                     && blockState.hasProperty(HugeMushroomBlock.UP)) {
                     blockState = blockState.setValue(HugeMushroomBlock.UP, i >= treeHeight - 1)
                        .setValue(HugeMushroomBlock.WEST, l < -k)
                        .setValue(HugeMushroomBlock.EAST, l > k)
                        .setValue(HugeMushroomBlock.NORTH, m < -k)
                        .setValue(HugeMushroomBlock.SOUTH, m > k);
                  }

                  this.placeMushroomBlock(level, mutablePos, blockState);
               }
            }
         }
      }
   }

   @Override
   protected int getTreeRadiusForHeight(int unused, int height, int foliageRadius, int y) {
      int i = 0;
      if (y < height && y >= height - 3) {
         i = foliageRadius;
      } else if (y == height) {
         i = foliageRadius;
      }

      return i;
   }
}
