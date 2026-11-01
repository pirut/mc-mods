package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;

public class SimpleBlockFeature extends Feature<SimpleBlockConfiguration> {
   public SimpleBlockFeature(Codec<SimpleBlockConfiguration> p_66808_) {
      super(p_66808_);
   }

   @Override
   public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> p_160341_) {
      SimpleBlockConfiguration $$1 = p_160341_.config();
      WorldGenLevel $$2 = p_160341_.level();
      BlockPos $$3 = p_160341_.origin();
      BlockState $$4 = $$1.toPlace().getState(p_160341_.random(), $$3);
      if ($$4.canSurvive($$2, $$3)) {
         if ($$4.getBlock() instanceof DoublePlantBlock) {
            if (!$$2.isEmptyBlock($$3.above())) {
               return false;
            }

            DoublePlantBlock.placeAt($$2, $$4, $$3, 2);
         } else {
            $$2.setBlock($$3, $$4, 2);
         }

         return true;
      } else {
         return false;
      }
   }
}
