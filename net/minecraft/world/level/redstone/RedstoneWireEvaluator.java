package net.minecraft.world.level.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public abstract class RedstoneWireEvaluator {
   protected final RedStoneWireBlock wireBlock;

   protected RedstoneWireEvaluator(RedStoneWireBlock p_363590_) {
      this.wireBlock = p_363590_;
   }

   public abstract void updatePowerStrength(Level var1, BlockPos var2, BlockState var3, @Nullable Orientation var4, boolean var5);

   protected int getBlockSignal(Level p_362603_, BlockPos p_364469_) {
      return this.wireBlock.getBlockSignal(p_362603_, p_364469_);
   }

   protected int getWireSignal(BlockPos p_362787_, BlockState p_363583_) {
      return p_363583_.is(this.wireBlock) ? p_363583_.getValue(RedStoneWireBlock.POWER) : 0;
   }

   protected int getIncomingWireSignal(Level p_361456_, BlockPos p_363935_) {
      int i = 0;

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos blockpos = p_363935_.relative(direction);
         BlockState blockstate = p_361456_.getBlockState(blockpos);
         i = Math.max(i, this.getWireSignal(blockpos, blockstate));
         BlockPos blockpos1 = p_363935_.above();
         if (blockstate.isRedstoneConductor(p_361456_, blockpos) && !p_361456_.getBlockState(blockpos1).isRedstoneConductor(p_361456_, blockpos1)) {
            BlockPos blockpos3 = blockpos.above();
            i = Math.max(i, this.getWireSignal(blockpos3, p_361456_.getBlockState(blockpos3)));
         } else if (!blockstate.isRedstoneConductor(p_361456_, blockpos)) {
            BlockPos blockpos2 = blockpos.below();
            i = Math.max(i, this.getWireSignal(blockpos2, p_361456_.getBlockState(blockpos2)));
         }
      }

      return Math.max(0, i - 1);
   }
}
