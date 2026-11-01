package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ChorusPlantFeature extends Feature<NoneFeatureConfiguration> {
   public ChorusPlantFeature(Codec<NoneFeatureConfiguration> p_65360_) {
      super(p_65360_);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_159521_) {
      WorldGenLevel $$1 = p_159521_.level();
      BlockPos $$2 = p_159521_.origin();
      RandomSource $$3 = p_159521_.random();
      if ($$1.isEmptyBlock($$2) && $$1.getBlockState($$2.below()).is(Blocks.END_STONE)) {
         ChorusFlowerBlock.generatePlant($$1, $$2, $$3, 8);
         return true;
      } else {
         return false;
      }
   }
}
