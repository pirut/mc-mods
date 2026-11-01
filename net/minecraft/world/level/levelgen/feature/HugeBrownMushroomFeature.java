package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public class HugeBrownMushroomFeature extends AbstractHugeMushroomFeature {
   public HugeBrownMushroomFeature(Codec<HugeMushroomFeatureConfiguration> p_65879_) {
      super(p_65879_);
   }

   @Override
   protected void makeCap(
      LevelAccessor p_225043_,
      RandomSource p_225044_,
      BlockPos p_225045_,
      int p_225046_,
      BlockPos.MutableBlockPos p_225047_,
      HugeMushroomFeatureConfiguration p_225048_
   ) {
      int $$6 = p_225048_.foliageRadius;

      for (int $$7 = -$$6; $$7 <= $$6; $$7++) {
         for (int $$8 = -$$6; $$8 <= $$6; $$8++) {
            boolean $$9 = $$7 == -$$6;
            boolean $$10 = $$7 == $$6;
            boolean $$11 = $$8 == -$$6;
            boolean $$12 = $$8 == $$6;
            boolean $$13 = $$9 || $$10;
            boolean $$14 = $$11 || $$12;
            if (!$$13 || !$$14) {
               p_225047_.setWithOffset(p_225045_, $$7, p_225046_, $$8);
               if (!p_225043_.getBlockState(p_225047_).isSolidRender(p_225043_, p_225047_)) {
                  boolean $$15 = $$9 || $$14 && $$7 == 1 - $$6;
                  boolean $$16 = $$10 || $$14 && $$7 == $$6 - 1;
                  boolean $$17 = $$11 || $$13 && $$8 == 1 - $$6;
                  boolean $$18 = $$12 || $$13 && $$8 == $$6 - 1;
                  BlockState $$19 = p_225048_.capProvider.getState(p_225044_, p_225045_);
                  if ($$19.hasProperty(HugeMushroomBlock.WEST)
                     && $$19.hasProperty(HugeMushroomBlock.EAST)
                     && $$19.hasProperty(HugeMushroomBlock.NORTH)
                     && $$19.hasProperty(HugeMushroomBlock.SOUTH)) {
                     $$19 = $$19.setValue(HugeMushroomBlock.WEST, $$15)
                        .setValue(HugeMushroomBlock.EAST, $$16)
                        .setValue(HugeMushroomBlock.NORTH, $$17)
                        .setValue(HugeMushroomBlock.SOUTH, $$18);
                  }

                  this.setBlock(p_225043_, p_225047_, $$19);
               }
            }
         }
      }
   }

   @Override
   protected int getTreeRadiusForHeight(int p_65881_, int p_65882_, int p_65883_, int p_65884_) {
      return p_65884_ <= 3 ? 0 : p_65883_;
   }
}
