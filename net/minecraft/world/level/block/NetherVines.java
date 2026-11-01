package net.minecraft.world.level.block;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class NetherVines {
   private static final double BONEMEAL_GROW_PROBABILITY_DECREASE_RATE = 0.826;
   public static final double GROW_PER_TICK_PROBABILITY = 0.1;

   public static boolean isValidGrowthState(BlockState p_54964_) {
      return p_54964_.isAir();
   }

   public static int getBlocksToGrowWhenBonemealed(RandomSource p_221804_) {
      double $$1 = 1.0;

      int $$2;
      for ($$2 = 0; p_221804_.nextDouble() < $$1; $$2++) {
         $$1 *= 0.826;
      }

      return $$2;
   }
}
