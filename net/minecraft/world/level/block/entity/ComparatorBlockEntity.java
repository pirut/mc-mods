package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ComparatorBlockEntity extends BlockEntity {
   private static final int DEFAULT_OUTPUT = 0;
   private int output = 0;

   public ComparatorBlockEntity(BlockPos p_155386_, BlockState p_155387_) {
      super(BlockEntityType.COMPARATOR, p_155386_, p_155387_);
   }

   @Override
   protected void saveAdditional(ValueOutput p_421778_) {
      super.saveAdditional(p_421778_);
      p_421778_.putInt("OutputSignal", this.output);
   }

   @Override
   protected void loadAdditional(ValueInput p_422269_) {
      super.loadAdditional(p_422269_);
      this.output = p_422269_.getIntOr("OutputSignal", 0);
   }

   public int getOutputSignal() {
      return this.output;
   }

   public void setOutputSignal(int p_59176_) {
      this.output = p_59176_;
   }
}
