package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceBlockConfiguration;

public class ReplaceBlockFeature extends Feature<ReplaceBlockConfiguration> {
   public ReplaceBlockFeature(Codec<ReplaceBlockConfiguration> p_66651_) {
      super(p_66651_);
   }

   @Override
   public boolean place(FeaturePlaceContext<ReplaceBlockConfiguration> p_160216_) {
      WorldGenLevel $$1 = p_160216_.level();
      BlockPos $$2 = p_160216_.origin();
      ReplaceBlockConfiguration $$3 = p_160216_.config();

      for (OreConfiguration.TargetBlockState $$4 : $$3.targetStates) {
         if ($$4.target.test($$1.getBlockState($$2), p_160216_.random())) {
            $$1.setBlock($$2, $$4.state, 2);
            break;
         }
      }

      return true;
   }
}
