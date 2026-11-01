package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleRandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class SimpleRandomSelectorFeature extends Feature<SimpleRandomFeatureConfiguration> {
   public SimpleRandomSelectorFeature(Codec<SimpleRandomFeatureConfiguration> p_66822_) {
      super(p_66822_);
   }

   @Override
   public boolean place(FeaturePlaceContext<SimpleRandomFeatureConfiguration> p_160343_) {
      RandomSource $$1 = p_160343_.random();
      SimpleRandomFeatureConfiguration $$2 = p_160343_.config();
      WorldGenLevel $$3 = p_160343_.level();
      BlockPos $$4 = p_160343_.origin();
      ChunkGenerator $$5 = p_160343_.chunkGenerator();
      int $$6 = $$1.nextInt($$2.features.size());
      PlacedFeature $$7 = $$2.features.get($$6).value();
      return $$7.place($$3, $$5, $$1, $$4);
   }
}
