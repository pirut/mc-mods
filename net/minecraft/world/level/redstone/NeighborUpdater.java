package net.minecraft.world.level.redstone;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface NeighborUpdater {
   Direction[] UPDATE_ORDER = new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};

   void shapeUpdate(Direction var1, BlockState var2, BlockPos var3, BlockPos var4, int var5, int var6);

   void neighborChanged(BlockPos var1, Block var2, BlockPos var3);

   void neighborChanged(BlockState var1, BlockPos var2, Block var3, BlockPos var4, boolean var5);

   default void updateNeighborsAtExceptFromFacing(BlockPos p_230788_, Block p_230789_, @Nullable Direction p_230790_) {
      for (Direction $$3 : UPDATE_ORDER) {
         if ($$3 != p_230790_) {
            this.neighborChanged(p_230788_.relative($$3), p_230789_, p_230788_);
         }
      }
   }

   static void executeShapeUpdate(
      LevelAccessor p_230771_, Direction p_230772_, BlockState p_230773_, BlockPos p_230774_, BlockPos p_230775_, int p_230776_, int p_230777_
   ) {
      BlockState $$7 = p_230771_.getBlockState(p_230774_);
      BlockState $$8 = $$7.updateShape(p_230772_, p_230773_, p_230771_, p_230774_, p_230775_);
      Block.updateOrDestroy($$7, $$8, p_230771_, p_230774_, p_230776_, p_230777_);
   }

   static void executeUpdate(Level p_230764_, BlockState p_230765_, BlockPos p_230766_, Block p_230767_, BlockPos p_230768_, boolean p_230769_) {
      try {
         p_230765_.handleNeighborChanged(p_230764_, p_230766_, p_230767_, p_230768_, p_230769_);
      } catch (Throwable var9) {
         CrashReport $$7 = CrashReport.forThrowable(var9, "Exception while updating neighbours");
         CrashReportCategory $$8 = $$7.addCategory("Block being updated");
         $$8.setDetail(
            "Source block type",
            () -> {
               try {
                  return String.format(
                     Locale.ROOT,
                     "ID #%s (%s // %s)",
                     BuiltInRegistries.BLOCK.getKey(p_230767_),
                     p_230767_.getDescriptionId(),
                     p_230767_.getClass().getCanonicalName()
                  );
               } catch (Throwable var2x) {
                  return "ID #" + BuiltInRegistries.BLOCK.getKey(p_230767_);
               }
            }
         );
         CrashReportCategory.populateBlockDetails($$8, p_230764_, p_230766_, p_230765_);
         throw new ReportedException($$7);
      }
   }
}
