package net.minecraft.world.level.block;

import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface SegmentableBlock {
   int MIN_SEGMENT = 1;
   int MAX_SEGMENT = 4;
   IntegerProperty AMOUNT = BlockStateProperties.SEGMENT_AMOUNT;

   default Function<BlockState, VoxelShape> getShapeCalculator(EnumProperty<Direction> p_394273_, IntegerProperty p_394415_) {
      Map<Direction, VoxelShape> map = Shapes.rotateHorizontal(Block.box(0.0, 0.0, 0.0, 8.0, this.getShapeHeight(), 8.0));
      return p_393772_ -> {
         VoxelShape voxelshape = Shapes.empty();
         Direction direction = p_393772_.getValue(p_394273_);
         int i = p_393772_.getValue(p_394415_);

         for (int j = 0; j < i; j++) {
            voxelshape = Shapes.or(voxelshape, map.get(direction));
            direction = direction.getCounterClockWise();
         }

         return voxelshape.singleEncompassing();
      };
   }

   default IntegerProperty getSegmentAmountProperty() {
      return AMOUNT;
   }

   default double getShapeHeight() {
      return 1.0;
   }

   default boolean canBeReplaced(BlockState p_394221_, BlockPlaceContext p_394124_, IntegerProperty p_394071_) {
      return !p_394124_.isSecondaryUseActive() && p_394124_.getItemInHand().is(p_394221_.getBlock().asItem()) && p_394221_.getValue(p_394071_) < 4;
   }

   default BlockState getStateForPlacement(BlockPlaceContext p_394035_, Block p_393593_, IntegerProperty p_394488_, EnumProperty<Direction> p_393563_) {
      BlockState blockstate = p_394035_.getLevel().getBlockState(p_394035_.getClickedPos());
      return blockstate.is(p_393593_)
         ? blockstate.setValue(p_394488_, Math.min(4, blockstate.getValue(p_394488_) + 1))
         : p_393593_.defaultBlockState().setValue(p_393563_, p_394035_.getHorizontalDirection().getOpposite());
   }
}
