package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;

public class RandomPatchFeature extends Feature<RandomPatchConfiguration> {
   public RandomPatchFeature(Codec<RandomPatchConfiguration> p_66605_) {
      super(p_66605_);
   }

   @Override
   public boolean place(FeaturePlaceContext<RandomPatchConfiguration> p_160210_) {
      RandomPatchConfiguration $$1 = p_160210_.config();
      RandomSource $$2 = p_160210_.random();
      BlockPos $$3 = p_160210_.origin();
      WorldGenLevel $$4 = p_160210_.level();
      int $$5 = 0;
      BlockPos.MutableBlockPos $$6 = new BlockPos.MutableBlockPos();
      int $$7 = $$1.xzSpread() + 1;
      int $$8 = $$1.ySpread() + 1;

      for (int $$9 = 0; $$9 < $$1.tries(); $$9++) {
         $$6.setWithOffset($$3, $$2.nextInt($$7) - $$2.nextInt($$7), $$2.nextInt($$8) - $$2.nextInt($$8), $$2.nextInt($$7) - $$2.nextInt($$7));
         if ($$1.feature().value().place($$4, p_160210_.chunkGenerator(), $$2, $$6)) {
            $$5++;
         }
      }

      return $$5 > 0;
   }
}
