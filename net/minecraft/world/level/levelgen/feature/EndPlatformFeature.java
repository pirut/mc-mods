package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class EndPlatformFeature extends Feature<NoneFeatureConfiguration> {
   public EndPlatformFeature(Codec<NoneFeatureConfiguration> p_352966_) {
      super(p_352966_);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> p_352935_) {
      createEndPlatform(p_352935_.level(), p_352935_.origin(), false);
      return true;
   }

   public static void createEndPlatform(ServerLevelAccessor p_352905_, BlockPos p_352961_, boolean p_352931_) {
      BlockPos.MutableBlockPos $$3 = p_352961_.mutable();

      for (int $$4 = -2; $$4 <= 2; $$4++) {
         for (int $$5 = -2; $$5 <= 2; $$5++) {
            for (int $$6 = -1; $$6 < 3; $$6++) {
               BlockPos $$7 = $$3.set(p_352961_).move($$5, $$6, $$4);
               Block $$8 = $$6 == -1 ? Blocks.OBSIDIAN : Blocks.AIR;
               if (!p_352905_.getBlockState($$7).is($$8)) {
                  if (p_352931_) {
                     p_352905_.destroyBlock($$7, true, null);
                  }

                  p_352905_.setBlock($$7, $$8.defaultBlockState(), 3);
               }
            }
         }
      }
   }
}
