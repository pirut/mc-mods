package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class KelpFeature extends Feature<NoneFeatureConfiguration> {
   public KelpFeature(Codec<NoneFeatureConfiguration> p_66219_) {
      super(p_66219_);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_159956_) {
      int $$1 = 0;
      WorldGenLevel $$2 = p_159956_.level();
      BlockPos $$3 = p_159956_.origin();
      RandomSource $$4 = p_159956_.random();
      int $$5 = $$2.getHeight(Heightmap.Types.OCEAN_FLOOR, $$3.getX(), $$3.getZ());
      BlockPos $$6 = new BlockPos($$3.getX(), $$5, $$3.getZ());
      if ($$2.getBlockState($$6).is(Blocks.WATER)) {
         BlockState $$7 = Blocks.KELP.defaultBlockState();
         BlockState $$8 = Blocks.KELP_PLANT.defaultBlockState();
         int $$9 = 1 + $$4.nextInt(10);

         for (int $$10 = 0; $$10 <= $$9; $$10++) {
            if ($$2.getBlockState($$6).is(Blocks.WATER) && $$2.getBlockState($$6.above()).is(Blocks.WATER) && $$8.canSurvive($$2, $$6)) {
               if ($$10 == $$9) {
                  $$2.setBlock($$6, $$7.setValue(KelpBlock.AGE, $$4.nextInt(4) + 20), 2);
                  $$1++;
               } else {
                  $$2.setBlock($$6, $$8, 2);
               }
            } else if ($$10 > 0) {
               BlockPos $$11 = $$6.below();
               if ($$7.canSurvive($$2, $$11) && !$$2.getBlockState($$11.below()).is(Blocks.KELP)) {
                  $$2.setBlock($$11, $$7.setValue(KelpBlock.AGE, $$4.nextInt(4) + 20), 2);
                  $$1++;
               }
               break;
            }

            $$6 = $$6.above();
         }
      }

      return $$1 > 0;
   }
}
