package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class GlowstoneFeature extends Feature<NoneFeatureConfiguration> {
   public GlowstoneFeature(Codec<NoneFeatureConfiguration> p_65865_) {
      super(p_65865_);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_159861_) {
      WorldGenLevel $$1 = p_159861_.level();
      BlockPos $$2 = p_159861_.origin();
      RandomSource $$3 = p_159861_.random();
      if (!$$1.isEmptyBlock($$2)) {
         return false;
      } else {
         BlockState $$4 = $$1.getBlockState($$2.above());
         if (!$$4.is(Blocks.NETHERRACK) && !$$4.is(Blocks.BASALT) && !$$4.is(Blocks.BLACKSTONE)) {
            return false;
         } else {
            $$1.setBlock($$2, Blocks.GLOWSTONE.defaultBlockState(), 2);

            for (int $$5 = 0; $$5 < 1500; $$5++) {
               BlockPos $$6 = $$2.offset($$3.nextInt(8) - $$3.nextInt(8), -$$3.nextInt(12), $$3.nextInt(8) - $$3.nextInt(8));
               if ($$1.getBlockState($$6).isAir()) {
                  int $$7 = 0;

                  for (Direction $$8 : Direction.values()) {
                     if ($$1.getBlockState($$6.relative($$8)).is(Blocks.GLOWSTONE)) {
                        $$7++;
                     }

                     if ($$7 > 1) {
                        break;
                     }
                  }

                  if ($$7 == 1) {
                     $$1.setBlock($$6, Blocks.GLOWSTONE.defaultBlockState(), 2);
                  }
               }
            }

            return true;
         }
      }
   }
}
