package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<WallBlock> CODEC = simpleCodec(WallBlock::new);
   public static final BooleanProperty UP = BlockStateProperties.UP;
   public static final EnumProperty<WallSide> EAST = BlockStateProperties.EAST_WALL;
   public static final EnumProperty<WallSide> NORTH = BlockStateProperties.NORTH_WALL;
   public static final EnumProperty<WallSide> SOUTH = BlockStateProperties.SOUTH_WALL;
   public static final EnumProperty<WallSide> WEST = BlockStateProperties.WEST_WALL;
   public static final Map<Direction, EnumProperty<WallSide>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(
      Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST))
   );
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private final Function<BlockState, VoxelShape> shapes;
   private final Function<BlockState, VoxelShape> collisionShapes;
   private static final VoxelShape TEST_SHAPE_POST = Block.column(2.0, 0.0, 16.0);
   private static final Map<Direction, VoxelShape> TEST_SHAPES_WALL = Shapes.rotateHorizontal(Block.boxZ(2.0, 16.0, 0.0, 9.0));

   @Override
   public MapCodec<WallBlock> codec() {
      return CODEC;
   }

   public WallBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(UP, true)
            .setValue(NORTH, WallSide.NONE)
            .setValue(EAST, WallSide.NONE)
            .setValue(SOUTH, WallSide.NONE)
            .setValue(WEST, WallSide.NONE)
            .setValue(WATERLOGGED, false)
      );
      this.shapes = this.makeShapes(16.0F, 14.0F);
      this.collisionShapes = this.makeShapes(24.0F, 24.0F);
   }

   private Function<BlockState, VoxelShape> makeShapes(float height, float width) {
      VoxelShape voxelShape = Block.column(8.0, 0.0, height);
      int i = 6;
      Map<Direction, VoxelShape> map = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, width, 0.0, 11.0));
      Map<Direction, VoxelShape> map2 = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, height, 0.0, 11.0));
      return this.getShapeForEachState(blockState -> {
         VoxelShape voxelShape2 = blockState.getValue(UP) ? voxelShape : Shapes.empty();

         for (Entry<Direction, EnumProperty<WallSide>> entry : PROPERTY_BY_DIRECTION.entrySet()) {
            voxelShape2 = Shapes.or(voxelShape2, switch ((WallSide)blockState.getValue(entry.getValue())) {
               case NONE -> Shapes.empty();
               case LOW -> (VoxelShape)map.get(entry.getKey());
               case TALL -> (VoxelShape)map2.get(entry.getKey());
            });
         }

         return voxelShape2;
      }, new Property[]{WATERLOGGED});
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.shapes.apply(state);
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.collisionShapes.apply(state);
   }

   @Override
   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   private boolean connectsTo(BlockState state, boolean sideSolid, Direction direction) {
      Block block = state.getBlock();
      boolean bl = block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(state, direction);
      return state.is(BlockTags.WALLS) || !isExceptionForConnection(state) && sideSolid || block instanceof IronBarsBlock || bl;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      LevelReader levelReader = context.getLevel();
      BlockPos blockPos = context.getClickedPos();
      FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
      BlockPos blockPos2 = blockPos.north();
      BlockPos blockPos3 = blockPos.east();
      BlockPos blockPos4 = blockPos.south();
      BlockPos blockPos5 = blockPos.west();
      BlockPos blockPos6 = blockPos.above();
      BlockState blockState = levelReader.getBlockState(blockPos2);
      BlockState blockState2 = levelReader.getBlockState(blockPos3);
      BlockState blockState3 = levelReader.getBlockState(blockPos4);
      BlockState blockState4 = levelReader.getBlockState(blockPos5);
      BlockState blockState5 = levelReader.getBlockState(blockPos6);
      boolean bl = this.connectsTo(blockState, blockState.isFaceSturdy(levelReader, blockPos2, Direction.SOUTH), Direction.SOUTH);
      boolean bl2 = this.connectsTo(blockState2, blockState2.isFaceSturdy(levelReader, blockPos3, Direction.WEST), Direction.WEST);
      boolean bl3 = this.connectsTo(blockState3, blockState3.isFaceSturdy(levelReader, blockPos4, Direction.NORTH), Direction.NORTH);
      boolean bl4 = this.connectsTo(blockState4, blockState4.isFaceSturdy(levelReader, blockPos5, Direction.EAST), Direction.EAST);
      BlockState blockState6 = this.defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
      return this.updateShape(levelReader, blockState6, blockPos6, blockState5, bl, bl2, bl3, bl4);
   }

   @Override
   protected BlockState updateShape(
      BlockState state,
      LevelReader level,
      ScheduledTickAccess scheduledTickAccess,
      BlockPos pos,
      Direction direction,
      BlockPos neighborPos,
      BlockState neighborState,
      RandomSource random
   ) {
      if (state.getValue(WATERLOGGED)) {
         scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      if (direction == Direction.DOWN) {
         return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
      } else {
         return direction == Direction.UP
            ? this.topUpdate(level, state, neighborPos, neighborState)
            : this.sideUpdate(level, pos, state, neighborPos, neighborState, direction);
      }
   }

   private static boolean isConnected(BlockState state, Property<WallSide> heightProperty) {
      return state.getValue(heightProperty) != WallSide.NONE;
   }

   private static boolean isCovered(VoxelShape firstShape, VoxelShape secondShape) {
      return !Shapes.joinIsNotEmpty(secondShape, firstShape, BooleanOp.ONLY_FIRST);
   }

   private BlockState topUpdate(LevelReader level, BlockState state, BlockPos pos, BlockState secondState) {
      boolean bl = isConnected(state, NORTH);
      boolean bl2 = isConnected(state, EAST);
      boolean bl3 = isConnected(state, SOUTH);
      boolean bl4 = isConnected(state, WEST);
      return this.updateShape(level, state, pos, secondState, bl, bl2, bl3, bl4);
   }

   private BlockState sideUpdate(LevelReader level, BlockPos firstPos, BlockState firstState, BlockPos secondPos, BlockState secondState, Direction dir) {
      Direction direction = dir.getOpposite();
      boolean bl = dir == Direction.NORTH
         ? this.connectsTo(secondState, secondState.isFaceSturdy(level, secondPos, direction), direction)
         : isConnected(firstState, NORTH);
      boolean bl2 = dir == Direction.EAST
         ? this.connectsTo(secondState, secondState.isFaceSturdy(level, secondPos, direction), direction)
         : isConnected(firstState, EAST);
      boolean bl3 = dir == Direction.SOUTH
         ? this.connectsTo(secondState, secondState.isFaceSturdy(level, secondPos, direction), direction)
         : isConnected(firstState, SOUTH);
      boolean bl4 = dir == Direction.WEST
         ? this.connectsTo(secondState, secondState.isFaceSturdy(level, secondPos, direction), direction)
         : isConnected(firstState, WEST);
      BlockPos blockPos = firstPos.above();
      BlockState blockState = level.getBlockState(blockPos);
      return this.updateShape(level, firstState, blockPos, blockState, bl, bl2, bl3, bl4);
   }

   private BlockState updateShape(
      LevelReader level,
      BlockState state,
      BlockPos pos,
      BlockState neighbour,
      boolean northConnection,
      boolean eastConnection,
      boolean southConnection,
      boolean westConnection
   ) {
      VoxelShape voxelShape = neighbour.getCollisionShape(level, pos).getFaceShape(Direction.DOWN);
      BlockState blockState = this.updateSides(state, northConnection, eastConnection, southConnection, westConnection, voxelShape);
      return blockState.setValue(UP, this.shouldRaisePost(blockState, neighbour, voxelShape));
   }

   private boolean shouldRaisePost(BlockState state, BlockState neighbour, VoxelShape shape) {
      boolean bl = neighbour.getBlock() instanceof WallBlock && neighbour.getValue(UP);
      if (bl) {
         return true;
      } else {
         WallSide wallSide = state.getValue(NORTH);
         WallSide wallSide2 = state.getValue(SOUTH);
         WallSide wallSide3 = state.getValue(EAST);
         WallSide wallSide4 = state.getValue(WEST);
         boolean bl2 = wallSide2 == WallSide.NONE;
         boolean bl3 = wallSide4 == WallSide.NONE;
         boolean bl4 = wallSide3 == WallSide.NONE;
         boolean bl5 = wallSide == WallSide.NONE;
         boolean bl6 = bl5 && bl2 && bl3 && bl4 || bl5 != bl2 || bl3 != bl4;
         if (bl6) {
            return true;
         } else {
            boolean bl7 = wallSide == WallSide.TALL && wallSide2 == WallSide.TALL || wallSide3 == WallSide.TALL && wallSide4 == WallSide.TALL;
            return bl7 ? false : neighbour.is(BlockTags.WALL_POST_OVERRIDE) || isCovered(shape, TEST_SHAPE_POST);
         }
      }
   }

   private BlockState updateSides(
      BlockState state, boolean northConnection, boolean eastConnection, boolean southConnection, boolean westConnection, VoxelShape wallShape
   ) {
      return state.setValue(NORTH, this.makeWallState(northConnection, wallShape, TEST_SHAPES_WALL.get(Direction.NORTH)))
         .setValue(EAST, this.makeWallState(eastConnection, wallShape, TEST_SHAPES_WALL.get(Direction.EAST)))
         .setValue(SOUTH, this.makeWallState(southConnection, wallShape, TEST_SHAPES_WALL.get(Direction.SOUTH)))
         .setValue(WEST, this.makeWallState(westConnection, wallShape, TEST_SHAPES_WALL.get(Direction.WEST)));
   }

   private WallSide makeWallState(boolean allowConnection, VoxelShape shape, VoxelShape neighbourShape) {
      if (allowConnection) {
         return isCovered(shape, neighbourShape) ? WallSide.TALL : WallSide.LOW;
      } else {
         return WallSide.NONE;
      }
   }

   @Override
   protected FluidState getFluidState(BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState state) {
      return !state.getValue(WATERLOGGED);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(UP, NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }

   @Override
   protected BlockState rotate(BlockState state, Rotation rotation) {
      switch (rotation) {
         case CLOCKWISE_180:
            return state.setValue(NORTH, state.getValue(SOUTH))
               .setValue(EAST, state.getValue(WEST))
               .setValue(SOUTH, state.getValue(NORTH))
               .setValue(WEST, state.getValue(EAST));
         case COUNTERCLOCKWISE_90:
            return state.setValue(NORTH, state.getValue(EAST))
               .setValue(EAST, state.getValue(SOUTH))
               .setValue(SOUTH, state.getValue(WEST))
               .setValue(WEST, state.getValue(NORTH));
         case CLOCKWISE_90:
            return state.setValue(NORTH, state.getValue(WEST))
               .setValue(EAST, state.getValue(NORTH))
               .setValue(SOUTH, state.getValue(EAST))
               .setValue(WEST, state.getValue(SOUTH));
         default:
            return state;
      }
   }

   @Override
   protected BlockState mirror(BlockState state, Mirror mirror) {
      switch (mirror) {
         case LEFT_RIGHT:
            return state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
         case FRONT_BACK:
            return state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
         default:
            return super.mirror(state, mirror);
      }
   }
}
