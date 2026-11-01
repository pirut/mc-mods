package net.minecraft.world.level;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BlockColumn;

public final class NoiseColumn implements BlockColumn {
   private final int minY;
   private final BlockState[] column;

   public NoiseColumn(int p_151623_, BlockState[] p_151624_) {
      this.minY = p_151623_;
      this.column = p_151624_;
   }

   @Override
   public BlockState getBlock(int p_186552_) {
      int $$1 = p_186552_ - this.minY;
      return $$1 >= 0 && $$1 < this.column.length ? this.column[$$1] : Blocks.AIR.defaultBlockState();
   }

   @Override
   public void setBlock(int p_186554_, BlockState p_186555_) {
      int $$2 = p_186554_ - this.minY;
      if ($$2 >= 0 && $$2 < this.column.length) {
         this.column[$$2] = p_186555_;
      } else {
         throw new IllegalArgumentException("Outside of column height: " + p_186554_);
      }
   }
}
