package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class MossyCarpetBlock extends Block implements BonemealableBlock {
   public static final MapCodec<MossyCarpetBlock> CODEC = simpleCodec(MossyCarpetBlock::new);
   public static final BooleanProperty BASE = BlockStateProperties.BOTTOM;
   public static final EnumProperty<WallSide> NORTH = BlockStateProperties.NORTH_WALL;
   public static final EnumProperty<WallSide> EAST = BlockStateProperties.EAST_WALL;
   public static final EnumProperty<WallSide> SOUTH = BlockStateProperties.SOUTH_WALL;
   public static final EnumProperty<WallSide> WEST = BlockStateProperties.WEST_WALL;
   public static final Map<Direction, EnumProperty<WallSide>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(
      Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST))
   );
   private final Function<BlockState, VoxelShape> shapes;

   @Override
   public MapCodec<MossyCarpetBlock> codec() {
      return CODEC;
   }

   public MossyCarpetBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(BASE, true)
            .setValue(NORTH, WallSide.NONE)
            .setValue(EAST, WallSide.NONE)
            .setValue(SOUTH, WallSide.NONE)
            .setValue(WEST, WallSide.NONE)
      );
      this.shapes = this.makeShapes();
   }

   public Function<BlockState, VoxelShape> makeShapes() {
      Map<Direction, VoxelShape> map = Shapes.rotateHorizontal(Block.boxZ(16.0, 0.0, 10.0, 0.0, 1.0));
      Map<Direction, VoxelShape> map2 = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
      return this.getShapeForEachState(blockState -> {
         VoxelShape voxelShape = blockState.getValue(BASE) ? map2.get(Direction.DOWN) : Shapes.empty();

         for (Entry<Direction, EnumProperty<WallSide>> entry : PROPERTY_BY_DIRECTION.entrySet()) {
            switch ((WallSide)blockState.getValue(entry.getValue())) {
               case NONE:
               default:
                  break;
               case LOW:
                  voxelShape = Shapes.or(voxelShape, map.get(entry.getKey()));
                  break;
               case TALL:
                  voxelShape = Shapes.or(voxelShape, map2.get(entry.getKey()));
            }
         }

         return voxelShape.isEmpty() ? Shapes.block() : voxelShape;
      });
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.shapes.apply(state);
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.getValue(BASE) ? this.shapes.apply(this.defaultBlockState()) : Shapes.empty();
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState state) {
      return true;
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      BlockState blockState = level.getBlockState(pos.below());
      return state.getValue(BASE) ? !blockState.isAir() : blockState.is(this) && blockState.getValue(BASE);
   }

   private static boolean hasFaces(BlockState state) {
      if (state.getValue(BASE)) {
         return true;
      } else {
         for (EnumProperty<WallSide> enumProperty : PROPERTY_BY_DIRECTION.values()) {
            if (state.getValue(enumProperty) != WallSide.NONE) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean canSupportAtFace(BlockGetter level, BlockPos pos, Direction direction) {
      return direction == Direction.UP ? false : MultifaceBlock.canAttachTo(level, pos, direction);
   }

   private static BlockState getUpdatedState(BlockState state, BlockGetter level, BlockPos pos, boolean tip) {
      BlockState blockState = null;
      BlockState blockState2 = null;
      tip |= state.getValue(BASE);

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         EnumProperty<WallSide> enumProperty = getPropertyForFace(direction);
         WallSide wallSide = canSupportAtFace(level, pos, direction) ? (tip ? WallSide.LOW : state.getValue(enumProperty)) : WallSide.NONE;
         if (wallSide == WallSide.LOW) {
            if (blockState == null) {
               blockState = level.getBlockState(pos.above());
            }

            if (blockState.is(Blocks.PALE_MOSS_CARPET) && blockState.getValue(enumProperty) != WallSide.NONE && !blockState.getValue(BASE)) {
               wallSide = WallSide.TALL;
            }

            if (!state.getValue(BASE)) {
               if (blockState2 == null) {
                  blockState2 = level.getBlockState(pos.below());
               }

               if (blockState2.is(Blocks.PALE_MOSS_CARPET) && blockState2.getValue(enumProperty) == WallSide.NONE) {
                  wallSide = WallSide.NONE;
               }
            }
         }

         state = state.setValue(enumProperty, wallSide);
      }

      return state;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return getUpdatedState(this.defaultBlockState(), context.getLevel(), context.getClickedPos(), true);
   }

   public static void placeAt(LevelAccessor level, BlockPos pos, RandomSource random, @Block.UpdateFlags int flags) {
      BlockState blockState = Blocks.PALE_MOSS_CARPET.defaultBlockState();
      BlockState blockState2 = getUpdatedState(blockState, level, pos, true);
      level.setBlock(pos, blockState2, flags);
      BlockState blockState3 = createTopperWithSideChance(level, pos, random::nextBoolean);
      if (!blockState3.isAir()) {
         level.setBlock(pos.above(), blockState3, flags);
         BlockState blockState4 = getUpdatedState(blockState2, level, pos, true);
         level.setBlock(pos, blockState4, flags);
      }
   }

   @Override
   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      if (!level.isClientSide()) {
         RandomSource randomSource = level.getRandom();
         BlockState blockState = createTopperWithSideChance(level, pos, randomSource::nextBoolean);
         if (!blockState.isAir()) {
            level.setBlock(pos.above(), blockState, 3);
         }
      }
   }

   private static BlockState createTopperWithSideChance(BlockGetter level, BlockPos pos, BooleanSupplier placeSide) {
      BlockPos blockPos = pos.above();
      BlockState blockState = level.getBlockState(blockPos);
      boolean bl = blockState.is(Blocks.PALE_MOSS_CARPET);
      if ((!bl || !blockState.getValue(BASE)) && (bl || blockState.canBeReplaced())) {
         BlockState blockState2 = Blocks.PALE_MOSS_CARPET.defaultBlockState().setValue(BASE, false);
         BlockState blockState3 = getUpdatedState(blockState2, level, pos.above(), true);

         for (Direction direction : Direction.Plane.HORIZONTAL) {
            EnumProperty<WallSide> enumProperty = getPropertyForFace(direction);
            if (blockState3.getValue(enumProperty) != WallSide.NONE && !placeSide.getAsBoolean()) {
               blockState3 = blockState3.setValue(enumProperty, WallSide.NONE);
            }
         }

         return hasFaces(blockState3) && blockState3 != blockState ? blockState3 : Blocks.AIR.defaultBlockState();
      } else {
         return Blocks.AIR.defaultBlockState();
      }
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
      if (!state.canSurvive(level, pos)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         BlockState blockState = getUpdatedState(state, level, pos, false);
         return !hasFaces(blockState) ? Blocks.AIR.defaultBlockState() : blockState;
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(BASE, NORTH, EAST, SOUTH, WEST);
   }

   @Override
   protected BlockState rotate(BlockState state, Rotation rotation) {
      return switch (rotation) {
         case CLOCKWISE_180 -> (BlockState)state.setValue(NORTH, state.getValue(SOUTH))
            .setValue(EAST, state.getValue(WEST))
            .setValue(SOUTH, state.getValue(NORTH))
            .setValue(WEST, state.getValue(EAST));
         case COUNTERCLOCKWISE_90 -> (BlockState)state.setValue(NORTH, state.getValue(EAST))
            .setValue(EAST, state.getValue(SOUTH))
            .setValue(SOUTH, state.getValue(WEST))
            .setValue(WEST, state.getValue(NORTH));
         case CLOCKWISE_90 -> (BlockState)state.setValue(NORTH, state.getValue(WEST))
            .setValue(EAST, state.getValue(NORTH))
            .setValue(SOUTH, state.getValue(EAST))
            .setValue(WEST, state.getValue(SOUTH));
         default -> state;
      };
   }

   @Override
   protected BlockState mirror(BlockState state, Mirror mirror) {
      return switch (mirror) {
         case LEFT_RIGHT -> (BlockState)state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
         case FRONT_BACK -> (BlockState)state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
         default -> super.mirror(state, mirror);
      };
   }

   @Nullable
   public static EnumProperty<WallSide> getPropertyForFace(Direction direction) {
      return PROPERTY_BY_DIRECTION.get(direction);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
      return state.getValue(BASE) && !createTopperWithSideChance(level, pos, () -> true).isAir();
   }

   @Override
   public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
      BlockState blockState = createTopperWithSideChance(level, pos, () -> true);
      if (!blockState.isAir()) {
         level.setBlock(pos.above(), blockState, 3);
      }
   }
}
