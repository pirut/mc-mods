package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

public class PoweredRailBlock extends BaseRailBlock {
   public static final MapCodec<PoweredRailBlock> CODEC = simpleCodec(PoweredRailBlock::new);
   public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   @Override
   public MapCodec<PoweredRailBlock> codec() {
      return CODEC;
   }

   protected PoweredRailBlock(BlockBehaviour.Properties properties) {
      super(true, properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH).setValue(POWERED, false).setValue(WATERLOGGED, false));
   }

   protected boolean findPoweredRailSignal(Level level, BlockPos pos, BlockState state, boolean searchForward, int recursionCount) {
      if (recursionCount >= 8) {
         return false;
      } else {
         int i = pos.getX();
         int j = pos.getY();
         int k = pos.getZ();
         boolean bl = true;
         RailShape railShape = state.getValue(SHAPE);
         switch (railShape) {
            case NORTH_SOUTH:
               if (searchForward) {
                  k++;
               } else {
                  k--;
               }
               break;
            case EAST_WEST:
               if (searchForward) {
                  i--;
               } else {
                  i++;
               }
               break;
            case ASCENDING_EAST:
               if (searchForward) {
                  i--;
               } else {
                  i++;
                  j++;
                  bl = false;
               }

               railShape = RailShape.EAST_WEST;
               break;
            case ASCENDING_WEST:
               if (searchForward) {
                  i--;
                  j++;
                  bl = false;
               } else {
                  i++;
               }

               railShape = RailShape.EAST_WEST;
               break;
            case ASCENDING_NORTH:
               if (searchForward) {
                  k++;
               } else {
                  k--;
                  j++;
                  bl = false;
               }

               railShape = RailShape.NORTH_SOUTH;
               break;
            case ASCENDING_SOUTH:
               if (searchForward) {
                  k++;
                  j++;
                  bl = false;
               } else {
                  k--;
               }

               railShape = RailShape.NORTH_SOUTH;
         }

         return this.isSameRailWithPower(level, new BlockPos(i, j, k), searchForward, recursionCount, railShape)
            ? true
            : bl && this.isSameRailWithPower(level, new BlockPos(i, j - 1, k), searchForward, recursionCount, railShape);
      }
   }

   protected boolean isSameRailWithPower(Level level, BlockPos state, boolean searchForward, int recursionCount, RailShape shape) {
      BlockState blockState = level.getBlockState(state);
      if (!blockState.is(this)) {
         return false;
      } else {
         RailShape railShape = blockState.getValue(SHAPE);
         if (shape != RailShape.EAST_WEST
            || railShape != RailShape.NORTH_SOUTH && railShape != RailShape.ASCENDING_NORTH && railShape != RailShape.ASCENDING_SOUTH) {
            if (shape != RailShape.NORTH_SOUTH
               || railShape != RailShape.EAST_WEST && railShape != RailShape.ASCENDING_EAST && railShape != RailShape.ASCENDING_WEST) {
               if (!blockState.getValue(POWERED)) {
                  return false;
               } else {
                  return level.hasNeighborSignal(state) ? true : this.findPoweredRailSignal(level, state, blockState, searchForward, recursionCount + 1);
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   @Override
   protected void updateState(BlockState state, Level level, BlockPos pos, Block neighborBlock) {
      boolean bl = state.getValue(POWERED);
      boolean bl2 = level.hasNeighborSignal(pos)
         || this.findPoweredRailSignal(level, pos, state, true, 0)
         || this.findPoweredRailSignal(level, pos, state, false, 0);
      if (bl2 != bl) {
         level.setBlock(pos, state.setValue(POWERED, bl2), 3);
         level.updateNeighborsAt(pos.below(), this);
         if (state.getValue(SHAPE).isSlope()) {
            level.updateNeighborsAt(pos.above(), this);
         }
      }
   }

   @Override
   public Property<RailShape> getShapeProperty() {
      return SHAPE;
   }

   @Override
   protected BlockState rotate(BlockState state, Rotation rotation) {
      RailShape railShape = state.getValue(SHAPE);
      RailShape railShape2 = this.rotate(railShape, rotation);
      return state.setValue(SHAPE, railShape2);
   }

   @Override
   protected BlockState mirror(BlockState state, Mirror mirror) {
      RailShape railShape = state.getValue(SHAPE);
      RailShape railShape2 = this.mirror(railShape, mirror);
      return state.setValue(SHAPE, railShape2);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(SHAPE, POWERED, WATERLOGGED);
   }
}
