package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class VinesFeature extends Feature<NoneFeatureConfiguration> {
   public VinesFeature(Codec<NoneFeatureConfiguration> p_67337_) {
      super(p_67337_);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_160628_) {
      WorldGenLevel $$1 = p_160628_.level();
      BlockPos $$2 = p_160628_.origin();
      p_160628_.config();
      if (!$$1.isEmptyBlock($$2)) {
         return false;
      } else {
         for (Direction $$3 : Direction.values()) {
            if ($$3 != Direction.DOWN && VineBlock.isAcceptableNeighbour($$1, $$2.relative($$3), $$3)) {
               $$1.setBlock($$2, Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace($$3), true), 2);
               return true;
            }
         }

         return false;
      }
   }
}
