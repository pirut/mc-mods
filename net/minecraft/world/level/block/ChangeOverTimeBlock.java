package net.minecraft.world.level.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public interface ChangeOverTimeBlock<T extends Enum<T>> {
   int SCAN_DISTANCE = 4;

   Optional<BlockState> getNext(BlockState var1);

   float getChanceModifier();

   default void changeOverTime(BlockState p_309154_, ServerLevel p_309115_, BlockPos p_308999_, RandomSource p_308868_) {
      float $$4 = 0.05688889F;
      if (p_308868_.nextFloat() < 0.05688889F) {
         this.getNextState(p_309154_, p_309115_, p_308999_, p_308868_).ifPresent(p_153039_ -> p_309115_.setBlockAndUpdate(p_308999_, p_153039_));
      }
   }

   T getAge();

   default Optional<BlockState> getNextState(BlockState p_309150_, ServerLevel p_309039_, BlockPos p_309042_, RandomSource p_308890_) {
      int $$4 = this.getAge().ordinal();
      int $$5 = 0;
      int $$6 = 0;

      for (BlockPos $$7 : BlockPos.withinManhattan(p_309042_, 4, 4, 4)) {
         int $$8 = $$7.distManhattan(p_309042_);
         if ($$8 > 4) {
            break;
         }

         if (!$$7.equals(p_309042_) && p_309039_.getBlockState($$7).getBlock() instanceof ChangeOverTimeBlock<?> $$9) {
            Enum<?> $$10 = $$9.getAge();
            if (this.getAge().getClass() == $$10.getClass()) {
               int $$11 = $$10.ordinal();
               if ($$11 < $$4) {
                  return Optional.empty();
               }

               if ($$11 > $$4) {
                  $$6++;
               } else {
                  $$5++;
               }
            }
         }
      }

      float $$12 = (float)($$6 + 1) / ($$6 + $$5 + 1);
      float $$13 = $$12 * $$12 * this.getChanceModifier();
      return p_308890_.nextFloat() < $$13 ? this.getNext(p_309150_) : Optional.empty();
   }
}
