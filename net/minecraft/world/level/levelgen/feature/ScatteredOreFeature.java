package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public class ScatteredOreFeature extends Feature<OreConfiguration> {
   private static final int MAX_DIST_FROM_ORIGIN = 7;

   ScatteredOreFeature(Codec<OreConfiguration> p_160304_) {
      super(p_160304_);
   }

   @Override
   public boolean place(FeaturePlaceContext<OreConfiguration> p_160306_) {
      WorldGenLevel $$1 = p_160306_.level();
      RandomSource $$2 = p_160306_.random();
      OreConfiguration $$3 = p_160306_.config();
      BlockPos $$4 = p_160306_.origin();
      int $$5 = $$2.nextInt($$3.size + 1);
      BlockPos.MutableBlockPos $$6 = new BlockPos.MutableBlockPos();

      for (int $$7 = 0; $$7 < $$5; $$7++) {
         this.offsetTargetPos($$6, $$2, $$4, Math.min($$7, 7));
         BlockState $$8 = $$1.getBlockState($$6);

         for (OreConfiguration.TargetBlockState $$9 : $$3.targetStates) {
            if (OreFeature.canPlaceOre($$8, $$1::getBlockState, $$2, $$3, $$9, $$6)) {
               $$1.setBlock($$6, $$9.state, 2);
               break;
            }
         }
      }

      return true;
   }

   private void offsetTargetPos(BlockPos.MutableBlockPos p_225232_, RandomSource p_225233_, BlockPos p_225234_, int p_225235_) {
      int $$4 = this.getRandomPlacementInOneAxisRelativeToOrigin(p_225233_, p_225235_);
      int $$5 = this.getRandomPlacementInOneAxisRelativeToOrigin(p_225233_, p_225235_);
      int $$6 = this.getRandomPlacementInOneAxisRelativeToOrigin(p_225233_, p_225235_);
      p_225232_.setWithOffset(p_225234_, $$4, $$5, $$6);
   }

   private int getRandomPlacementInOneAxisRelativeToOrigin(RandomSource p_225229_, int p_225230_) {
      return Math.round((p_225229_.nextFloat() - p_225229_.nextFloat()) * p_225230_);
   }
}
