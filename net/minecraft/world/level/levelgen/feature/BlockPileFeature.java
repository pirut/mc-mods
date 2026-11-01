package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;

public class BlockPileFeature extends Feature<BlockPileConfiguration> {
   public BlockPileFeature(Codec<BlockPileConfiguration> p_65262_) {
      super(p_65262_);
   }

   @Override
   public boolean place(FeaturePlaceContext<BlockPileConfiguration> p_159473_) {
      BlockPos $$1 = p_159473_.origin();
      WorldGenLevel $$2 = p_159473_.level();
      RandomSource $$3 = p_159473_.random();
      BlockPileConfiguration $$4 = p_159473_.config();
      if ($$1.getY() < $$2.getMinBuildHeight() + 5) {
         return false;
      } else {
         int $$5 = 2 + $$3.nextInt(2);
         int $$6 = 2 + $$3.nextInt(2);

         for (BlockPos $$7 : BlockPos.betweenClosed($$1.offset(-$$5, 0, -$$6), $$1.offset($$5, 1, $$6))) {
            int $$8 = $$1.getX() - $$7.getX();
            int $$9 = $$1.getZ() - $$7.getZ();
            if ($$8 * $$8 + $$9 * $$9 <= $$3.nextFloat() * 10.0F - $$3.nextFloat() * 6.0F) {
               this.tryPlaceBlock($$2, $$7, $$3, $$4);
            } else if ($$3.nextFloat() < 0.031) {
               this.tryPlaceBlock($$2, $$7, $$3, $$4);
            }
         }

         return true;
      }
   }

   private boolean mayPlaceOn(LevelAccessor p_224945_, BlockPos p_224946_, RandomSource p_224947_) {
      BlockPos $$3 = p_224946_.below();
      BlockState $$4 = p_224945_.getBlockState($$3);
      return $$4.is(Blocks.DIRT_PATH) ? p_224947_.nextBoolean() : $$4.isFaceSturdy(p_224945_, $$3, Direction.UP);
   }

   private void tryPlaceBlock(LevelAccessor p_224949_, BlockPos p_224950_, RandomSource p_224951_, BlockPileConfiguration p_224952_) {
      if (p_224949_.isEmptyBlock(p_224950_) && this.mayPlaceOn(p_224949_, p_224950_, p_224951_)) {
         p_224949_.setBlock(p_224950_, p_224952_.stateProvider.getState(p_224951_, p_224950_), 4);
      }
   }
}
