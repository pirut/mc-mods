package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class EndIslandFeature extends Feature<NoneFeatureConfiguration> {
   public EndIslandFeature(Codec<NoneFeatureConfiguration> p_65701_) {
      super(p_65701_);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_159717_) {
      WorldGenLevel $$1 = p_159717_.level();
      RandomSource $$2 = p_159717_.random();
      BlockPos $$3 = p_159717_.origin();
      float $$4 = $$2.nextInt(3) + 4.0F;

      for (int $$5 = 0; $$4 > 0.5F; $$5--) {
         for (int $$6 = Mth.floor(-$$4); $$6 <= Mth.ceil($$4); $$6++) {
            for (int $$7 = Mth.floor(-$$4); $$7 <= Mth.ceil($$4); $$7++) {
               if ($$6 * $$6 + $$7 * $$7 <= ($$4 + 1.0F) * ($$4 + 1.0F)) {
                  this.setBlock($$1, $$3.offset($$6, $$5, $$7), Blocks.END_STONE.defaultBlockState());
               }
            }
         }

         $$4 -= $$2.nextInt(2) + 0.5F;
      }

      return true;
   }
}
