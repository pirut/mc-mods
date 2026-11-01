package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;

public class RandomSelectorFeature extends Feature<RandomFeatureConfiguration> {
   public RandomSelectorFeature(Codec<RandomFeatureConfiguration> p_66619_) {
      super(p_66619_);
   }

   @Override
   public boolean place(FeaturePlaceContext<RandomFeatureConfiguration> p_160212_) {
      RandomFeatureConfiguration $$1 = p_160212_.config();
      RandomSource $$2 = p_160212_.random();
      WorldGenLevel $$3 = p_160212_.level();
      ChunkGenerator $$4 = p_160212_.chunkGenerator();
      BlockPos $$5 = p_160212_.origin();

      for (WeightedPlacedFeature $$6 : $$1.features) {
         if ($$2.nextFloat() < $$6.chance) {
            return $$6.place($$3, $$4, $$2, $$5);
         }
      }

      return $$1.defaultFeature.value().place($$3, $$4, $$2, $$5);
   }
}
