package net.minecraft.world.level.block;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class TripWireHookBlock extends Block {
   public static final MapCodec<TripWireHookBlock> CODEC = simpleCodec(TripWireHookBlock::new);
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
   protected static final int WIRE_DIST_MIN = 1;
   protected static final int WIRE_DIST_MAX = 42;
   private static final int RECHECK_PERIOD = 10;
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, 10.0, 10.0, 16.0));

   @Override
   public MapCodec<TripWireHookBlock> codec() {
      return CODEC;
   }

   public TripWireHookBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(ATTACHED, false));
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPES.get(state.getValue(FACING));
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      Direction direction = state.getValue(FACING);
      BlockPos blockPos = pos.relative(direction.getOpposite());
      BlockState blockState = level.getBlockState(blockPos);
      return direction.getAxis().isHorizontal() && blockState.isFaceSturdy(level, blockPos, direction);
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
      return direction.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, pos)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState blockState = this.defaultBlockState().setValue(POWERED, false).setValue(ATTACHED, false);
      LevelReader levelReader = context.getLevel();
      BlockPos blockPos = context.getClickedPos();
      Direction[] directions = context.getNearestLookingDirections();

      for (Direction direction : directions) {
         if (direction.getAxis().isHorizontal()) {
            Direction direction2 = direction.getOpposite();
            blockState = blockState.setValue(FACING, direction2);
            if (blockState.canSurvive(levelReader, blockPos)) {
               return blockState;
            }
         }
      }

      return null;
   }

   @Override
   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      calculateState(level, pos, state, false, false, -1, null);
   }

   public static void calculateState(
      Level level, BlockPos pos, BlockState hookState, boolean attaching, boolean shouldNotifyNeighbours, int searchRange, @Nullable BlockState state
   ) {
      Optional<Direction> optional = hookState.getOptionalValue(FACING);
      if (optional.isPresent()) {
         Direction direction = optional.get();
         boolean bl = hookState.getOptionalValue(ATTACHED).orElse(false);
         boolean bl2 = hookState.getOptionalValue(POWERED).orElse(false);
         Block block = hookState.getBlock();
         boolean bl3 = !attaching;
         boolean bl4 = false;
         int i = 0;
         BlockState[] blockStates = new BlockState[42];

         for (int j = 1; j < 42; j++) {
            BlockPos blockPos = pos.relative(direction, j);
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.is(Blocks.TRIPWIRE_HOOK)) {
               if (blockState.getValue(FACING) == direction.getOpposite()) {
                  i = j;
               }
               break;
            }

            if (!blockState.is(Blocks.TRIPWIRE) && j != searchRange) {
               blockStates[j] = null;
               bl3 = false;
            } else {
               if (j == searchRange) {
                  blockState = (BlockState)MoreObjects.firstNonNull(state, blockState);
               }

               boolean bl5 = !blockState.getValue(TripWireBlock.DISARMED);
               boolean bl6 = blockState.getValue(TripWireBlock.POWERED);
               bl4 |= bl5 && bl6;
               blockStates[j] = blockState;
               if (j == searchRange) {
                  level.scheduleTick(pos, block, 10);
                  bl3 &= bl5;
               }
            }
         }

         bl3 &= i > 1;
         bl4 &= bl3;
         BlockState blockState2 = block.defaultBlockState().trySetValue(ATTACHED, bl3).trySetValue(POWERED, bl4);
         if (i > 0) {
            BlockPos blockPosx = pos.relative(direction, i);
            Direction direction2 = direction.getOpposite();
            level.setBlock(blockPosx, blockState2.setValue(FACING, direction2), 3);
            notifyNeighbors(block, level, blockPosx, direction2);
            emitState(level, blockPosx, bl3, bl4, bl, bl2);
         }

         emitState(level, pos, bl3, bl4, bl, bl2);
         if (!attaching) {
            level.setBlock(pos, blockState2.setValue(FACING, direction), 3);
            if (shouldNotifyNeighbours) {
               notifyNeighbors(block, level, pos, direction);
            }
         }

         if (bl != bl3) {
            for (int k = 1; k < i; k++) {
               BlockPos blockPos2 = pos.relative(direction, k);
               BlockState blockState3 = blockStates[k];
               if (blockState3 != null) {
                  BlockState blockState4 = level.getBlockState(blockPos2);
                  if (blockState4.is(Blocks.TRIPWIRE) || blockState4.is(Blocks.TRIPWIRE_HOOK)) {
                     level.setBlock(blockPos2, blockState3.trySetValue(ATTACHED, bl3), 3);
                  }
               }
            }
         }
      }
   }

   @Override
   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      calculateState(level, pos, state, false, true, -1, null);
   }

   private static void emitState(Level level, BlockPos pos, boolean attached, boolean powered, boolean wasAttached, boolean wasPowered) {
      if (powered && !wasPowered) {
         level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.4F, 0.6F);
         level.gameEvent(null, GameEvent.BLOCK_ACTIVATE, pos);
      } else if (!powered && wasPowered) {
         level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.BLOCKS, 0.4F, 0.5F);
         level.gameEvent(null, GameEvent.BLOCK_DEACTIVATE, pos);
      } else if (attached && !wasAttached) {
         level.playSound(null, pos, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.4F, 0.7F);
         level.gameEvent(null, GameEvent.BLOCK_ATTACH, pos);
      } else if (!attached && wasAttached) {
         level.playSound(null, pos, SoundEvents.TRIPWIRE_DETACH, SoundSource.BLOCKS, 0.4F, 1.2F / (level.random.nextFloat() * 0.2F + 0.9F));
         level.gameEvent(null, GameEvent.BLOCK_DETACH, pos);
      }
   }

   private static void notifyNeighbors(Block block, Level level, BlockPos pos, Direction direction) {
      Direction direction2 = direction.getOpposite();
      Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, direction2, Direction.UP);
      level.updateNeighborsAt(pos, block, orientation);
      level.updateNeighborsAt(pos.relative(direction2), block, orientation);
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
      if (!movedByPiston) {
         boolean bl = state.getValue(ATTACHED);
         boolean bl2 = state.getValue(POWERED);
         if (bl || bl2) {
            calculateState(level, pos, state, true, false, -1, null);
         }

         if (bl2) {
            notifyNeighbors(this, level, pos, state.getValue(FACING));
         }
      }
   }

   @Override
   protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return state.getValue(POWERED) ? 15 : 0;
   }

   @Override
   protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      if (!state.getValue(POWERED)) {
         return 0;
      } else {
         return state.getValue(FACING) == direction ? 15 : 0;
      }
   }

   @Override
   protected boolean isSignalSource(BlockState state) {
      return true;
   }

   @Override
   protected BlockState rotate(BlockState state, Rotation rotation) {
      return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation(state.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, POWERED, ATTACHED);
   }
}
