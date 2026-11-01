package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public class HugeBrownMushroomFeature extends AbstractHugeMushroomFeature {
   public HugeBrownMushroomFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
      super(codec);
   }

   @Override
   protected void makeCap(
      LevelAccessor level, RandomSource random, BlockPos pos, int treeHeight, BlockPos.MutableBlockPos mutablePos, HugeMushroomFeatureConfiguration config
   ) {
      int i = config.foliageRadius;

      for (int j = -i; j <= i; j++) {
         for (int k = -i; k <= i; k++) {
            boolean bl = j == -i;
            boolean bl2 = j == i;
            boolean bl3 = k == -i;
            boolean bl4 = k == i;
            boolean bl5 = bl || bl2;
            boolean bl6 = bl3 || bl4;
            if (!bl5 || !bl6) {
               mutablePos.setWithOffset(pos, j, treeHeight, k);
               boolean bl7 = bl || bl6 && j == 1 - i;
               boolean bl8 = bl2 || bl6 && j == i - 1;
               boolean bl9 = bl3 || bl5 && k == 1 - i;
               boolean bl10 = bl4 || bl5 && k == i - 1;
               BlockState blockState = config.capProvider.getState(random, pos);
               if (blockState.hasProperty(HugeMushroomBlock.WEST)
                  && blockState.hasProperty(HugeMushroomBlock.EAST)
                  && blockState.hasProperty(HugeMushroomBlock.NORTH)
                  && blockState.hasProperty(HugeMushroomBlock.SOUTH)) {
                  blockState = blockState.setValue(HugeMushroomBlock.WEST, bl7)
                     .setValue(HugeMushroomBlock.EAST, bl8)
                     .setValue(HugeMushroomBlock.NORTH, bl9)
                     .setValue(HugeMushroomBlock.SOUTH, bl10);
               }

               this.placeMushroomBlock(level, mutablePos, blockState);
            }
         }
      }
   }

   @Override
   protected int getTreeRadiusForHeight(int unused, int height, int foliageRadius, int y) {
      return y <= 3 ? 0 : foliageRadius;
   }
}
