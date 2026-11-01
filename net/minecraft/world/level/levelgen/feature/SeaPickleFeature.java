package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.CountConfiguration;

public class SeaPickleFeature extends Feature<CountConfiguration> {
   public SeaPickleFeature(Codec<CountConfiguration> p_66754_) {
      super(p_66754_);
   }

   @Override
   public boolean place(FeaturePlaceContext<CountConfiguration> p_160316_) {
      int $$1 = 0;
      RandomSource $$2 = p_160316_.random();
      WorldGenLevel $$3 = p_160316_.level();
      BlockPos $$4 = p_160316_.origin();
      int $$5 = p_160316_.config().count().sample($$2);

      for (int $$6 = 0; $$6 < $$5; $$6++) {
         int $$7 = $$2.nextInt(8) - $$2.nextInt(8);
         int $$8 = $$2.nextInt(8) - $$2.nextInt(8);
         int $$9 = $$3.getHeight(Heightmap.Types.OCEAN_FLOOR, $$4.getX() + $$7, $$4.getZ() + $$8);
         BlockPos $$10 = new BlockPos($$4.getX() + $$7, $$9, $$4.getZ() + $$8);
         BlockState $$11 = Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, $$2.nextInt(4) + 1);
         if ($$3.getBlockState($$10).is(Blocks.WATER) && $$11.canSurvive($$3, $$10)) {
            $$3.setBlock($$10, $$11, 2);
            $$1++;
         }
      }

      return $$1 > 0;
   }
}
