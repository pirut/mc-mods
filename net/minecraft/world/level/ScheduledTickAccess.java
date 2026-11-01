package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

public interface ScheduledTickAccess {
   <T> ScheduledTick<T> createTick(BlockPos var1, T var2, int var3, TickPriority var4);

   <T> ScheduledTick<T> createTick(BlockPos var1, T var2, int var3);

   LevelTickAccess<Block> getBlockTicks();

   default void scheduleTick(BlockPos p_374271_, Block p_374034_, int p_374109_, TickPriority p_374496_) {
      this.getBlockTicks().schedule(this.createTick(p_374271_, p_374034_, p_374109_, p_374496_));
   }

   default void scheduleTick(BlockPos p_374141_, Block p_374171_, int p_374315_) {
      this.getBlockTicks().schedule(this.createTick(p_374141_, p_374171_, p_374315_));
   }

   LevelTickAccess<Fluid> getFluidTicks();

   default void scheduleTick(BlockPos p_374273_, Fluid p_374485_, int p_374400_, TickPriority p_374449_) {
      this.getFluidTicks().schedule(this.createTick(p_374273_, p_374485_, p_374400_, p_374449_));
   }

   default void scheduleTick(BlockPos p_374336_, Fluid p_374326_, int p_374385_) {
      this.getFluidTicks().schedule(this.createTick(p_374336_, p_374326_, p_374385_));
   }
}
