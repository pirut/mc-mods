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
   public AbstractHugeMushroomFeature(Codec<HugeMushroomFeatureConfiguration> p_65093_) {
      super(p_65093_);
   }

   protected void placeTrunk(
      LevelAccessor p_224930_,
      RandomSource p_224931_,
      BlockPos p_224932_,
      HugeMushroomFeatureConfiguration p_224933_,
      int p_224934_,
      BlockPos.MutableBlockPos p_224935_
   ) {
      for (int $$6 = 0; $$6 < p_224934_; $$6++) {
         p_224935_.set(p_224932_).move(Direction.UP, $$6);
         if (!p_224930_.getBlockState(p_224935_).isSolidRender(p_224930_, p_224935_)) {
            this.setBlock(p_224930_, p_224935_, p_224933_.stemProvider.getState(p_224931_, p_224932_));
         }
      }
   }

   protected int getTreeHeight(RandomSource p_224922_) {
      int $$1 = p_224922_.nextInt(3) + 4;
      if (p_224922_.nextInt(12) == 0) {
         $$1 *= 2;
      }

      return $$1;
   }

   protected boolean isValidPosition(
      LevelAccessor p_65099_, BlockPos p_65100_, int p_65101_, BlockPos.MutableBlockPos p_65102_, HugeMushroomFeatureConfiguration p_65103_
   ) {
      int $$5 = p_65100_.getY();
      if ($$5 >= p_65099_.getMinBuildHeight() + 1 && $$5 + p_65101_ + 1 < p_65099_.getMaxBuildHeight()) {
         BlockState $$6 = p_65099_.getBlockState(p_65100_.below());
         if (!isDirt($$6) && !$$6.is(BlockTags.MUSHROOM_GROW_BLOCK)) {
            return false;
         } else {
            for (int $$7 = 0; $$7 <= p_65101_; $$7++) {
               int $$8 = this.getTreeRadiusForHeight(-1, -1, p_65103_.foliageRadius, $$7);

               for (int $$9 = -$$8; $$9 <= $$8; $$9++) {
                  for (int $$10 = -$$8; $$10 <= $$8; $$10++) {
                     BlockState $$11 = p_65099_.getBlockState(p_65102_.setWithOffset(p_65100_, $$9, $$7, $$10));
                     if (!$$11.isAir() && !$$11.is(BlockTags.LEAVES)) {
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
   public boolean place(FeaturePlaceContext<HugeMushroomFeatureConfiguration> p_159436_) {
      WorldGenLevel $$1 = p_159436_.level();
      BlockPos $$2 = p_159436_.origin();
      RandomSource $$3 = p_159436_.random();
      HugeMushroomFeatureConfiguration $$4 = p_159436_.config();
      int $$5 = this.getTreeHeight($$3);
      BlockPos.MutableBlockPos $$6 = new BlockPos.MutableBlockPos();
      if (!this.isValidPosition($$1, $$2, $$5, $$6, $$4)) {
         return false;
      } else {
         this.makeCap($$1, $$3, $$2, $$5, $$6, $$4);
         this.placeTrunk($$1, $$3, $$2, $$4, $$5, $$6);
         return true;
      }
   }

   protected abstract int getTreeRadiusForHeight(int var1, int var2, int var3, int var4);

   protected abstract void makeCap(
      LevelAccessor var1, RandomSource var2, BlockPos var3, int var4, BlockPos.MutableBlockPos var5, HugeMushroomFeatureConfiguration var6
   );
}
