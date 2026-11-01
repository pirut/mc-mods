package net.neoforged.neoforge.event.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;

public abstract class CropGrowEvent extends BlockEvent {
   public CropGrowEvent(Level level, BlockPos pos, BlockState state) {
      super(level, pos, state);
   }

   public static class Post extends CropGrowEvent {
      private final BlockState originalState;

      public Post(Level level, BlockPos pos, BlockState original, BlockState state) {
         super(level, pos, state);
         this.originalState = original;
      }

      public BlockState getOriginalState() {
         return this.originalState;
      }

      @Override
      public BlockState getState() {
         return super.getState();
      }
   }

   public static class Pre extends CropGrowEvent {
      private CropGrowEvent.Pre.Result result = CropGrowEvent.Pre.Result.DEFAULT;

      public Pre(Level level, BlockPos pos, BlockState state) {
         super(level, pos, state);
      }

      public void setResult(CropGrowEvent.Pre.Result result) {
         this.result = result;
      }

      public CropGrowEvent.Pre.Result getResult() {
         return this.result;
      }

      public static enum Result {
         GROW,
         DEFAULT,
         DO_NOT_GROW;
      }
   }
}
