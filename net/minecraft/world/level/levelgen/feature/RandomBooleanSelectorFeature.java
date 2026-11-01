package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RandomBooleanFeatureConfiguration;

public class RandomBooleanSelectorFeature extends Feature<RandomBooleanFeatureConfiguration> {
   public RandomBooleanSelectorFeature(Codec<RandomBooleanFeatureConfiguration> p_66591_) {
      super(p_66591_);
   }

   @Override
   public boolean place(FeaturePlaceContext<RandomBooleanFeatureConfiguration> p_160208_) {
      RandomSource $$1 = p_160208_.random();
      RandomBooleanFeatureConfiguration $$2 = p_160208_.config();
      WorldGenLevel $$3 = p_160208_.level();
      ChunkGenerator $$4 = p_160208_.chunkGenerator();
      BlockPos $$5 = p_160208_.origin();
      boolean $$6 = $$1.nextBoolean();
      return ($$6 ? $$2.featureTrue : $$2.featureFalse).value().place($$3, $$4, $$1, $$5);
   }
}
