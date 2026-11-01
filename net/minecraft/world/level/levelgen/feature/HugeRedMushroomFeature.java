package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public class HugeRedMushroomFeature extends AbstractHugeMushroomFeature {
   public HugeRedMushroomFeature(Codec<HugeMushroomFeatureConfiguration> p_65975_) {
      super(p_65975_);
   }

   @Override
   protected void makeCap(
      LevelAccessor p_225082_,
      RandomSource p_225083_,
      BlockPos p_225084_,
      int p_225085_,
      BlockPos.MutableBlockPos p_225086_,
      HugeMushroomFeatureConfiguration p_225087_
   ) {
      for (int $$6 = p_225085_ - 3; $$6 <= p_225085_; $$6++) {
         int $$7 = $$6 < p_225085_ ? p_225087_.foliageRadius : p_225087_.foliageRadius - 1;
         int $$8 = p_225087_.foliageRadius - 2;

         for (int $$9 = -$$7; $$9 <= $$7; $$9++) {
            for (int $$10 = -$$7; $$10 <= $$7; $$10++) {
               boolean $$11 = $$9 == -$$7;
               boolean $$12 = $$9 == $$7;
               boolean $$13 = $$10 == -$$7;
               boolean $$14 = $$10 == $$7;
               boolean $$15 = $$11 || $$12;
               boolean $$16 = $$13 || $$14;
               if ($$6 >= p_225085_ || $$15 != $$16) {
                  p_225086_.setWithOffset(p_225084_, $$9, $$6, $$10);
                  if (!p_225082_.getBlockState(p_225086_).isSolidRender(p_225082_, p_225086_)) {
                     BlockState $$17 = p_225087_.capProvider.getState(p_225083_, p_225084_);
                     if ($$17.hasProperty(HugeMushroomBlock.WEST)
                        && $$17.hasProperty(HugeMushroomBlock.EAST)
                        && $$17.hasProperty(HugeMushroomBlock.NORTH)
                        && $$17.hasProperty(HugeMushroomBlock.SOUTH)
                        && $$17.hasProperty(HugeMushroomBlock.UP)) {
                        $$17 = $$17.setValue(HugeMushroomBlock.UP, $$6 >= p_225085_ - 1)
                           .setValue(HugeMushroomBlock.WEST, $$9 < -$$8)
                           .setValue(HugeMushroomBlock.EAST, $$9 > $$8)
                           .setValue(HugeMushroomBlock.NORTH, $$10 < -$$8)
                           .setValue(HugeMushroomBlock.SOUTH, $$10 > $$8);
                     }

                     this.setBlock(p_225082_, p_225086_, $$17);
                  }
               }
            }
         }
      }
   }

   @Override
   protected int getTreeRadiusForHeight(int p_65977_, int p_65978_, int p_65979_, int p_65980_) {
      int $$4 = 0;
      if (p_65980_ < p_65978_ && p_65980_ >= p_65978_ - 3) {
         $$4 = p_65979_;
      } else if (p_65980_ == p_65978_) {
         $$4 = p_65979_;
      }

      return $$4;
   }
}
