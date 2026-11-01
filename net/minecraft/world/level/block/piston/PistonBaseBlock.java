package net.minecraft.world.level.block.piston;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PistonBaseBlock extends DirectionalBlock {
   public static final MapCodec<PistonBaseBlock> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(Codec.BOOL.fieldOf("sticky").forGetter(pistonBaseBlock -> pistonBaseBlock.isSticky), propertiesCodec())
         .apply(instance, PistonBaseBlock::new)
   );
   public static final BooleanProperty EXTENDED = BlockStateProperties.EXTENDED;
   public static final int TRIGGER_EXTEND = 0;
   public static final int TRIGGER_CONTRACT = 1;
   public static final int TRIGGER_DROP = 2;
   public static final int PLATFORM_THICKNESS = 4;
   private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateAll(Block.boxZ(16.0, 4.0, 16.0));
   private final boolean isSticky;

   @Override
   public MapCodec<PistonBaseBlock> codec() {
      return CODEC;
   }

   public PistonBaseBlock(boolean isSticky, BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(EXTENDED, false));
      this.isSticky = isSticky;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.getValue(EXTENDED) ? SHAPES.get(state.getValue(FACING)) : Shapes.block();
   }

   @Override
   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      if (!level.isClientSide()) {
         this.checkIfExtend(level, pos, state);
      }
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
      if (!level.isClientSide()) {
         this.checkIfExtend(level, pos, state);
      }
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      if (!oldState.is(state.getBlock())) {
         if (!level.isClientSide() && level.getBlockEntity(pos) == null) {
            this.checkIfExtend(level, pos, state);
         }
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite()).setValue(EXTENDED, false);
   }

   private void checkIfExtend(Level level, BlockPos pos, BlockState state) {
      Direction direction = state.getValue(FACING);
      boolean bl = this.getNeighborSignal(level, pos, direction);
      if (bl && !state.getValue(EXTENDED)) {
         if (new PistonStructureResolver(level, pos, direction, true).resolve()) {
            level.blockEvent(pos, this, 0, direction.get3DDataValue());
         }
      } else if (!bl && state.getValue(EXTENDED)) {
         BlockPos blockPos = pos.relative(direction, 2);
         BlockState blockState = level.getBlockState(blockPos);
         int i = 1;
         if (blockState.is(Blocks.MOVING_PISTON)
            && blockState.getValue(FACING) == direction
            && level.getBlockEntity(blockPos) instanceof PistonMovingBlockEntity pistonMovingBlockEntity
            && pistonMovingBlockEntity.isExtending()
            && (
               pistonMovingBlockEntity.getProgress(0.0F) < 0.5F
                  || level.getGameTime() == pistonMovingBlockEntity.getLastTicked()
                  || ((ServerLevel)level).isHandlingTick()
            )) {
            i = 2;
         }

         level.blockEvent(pos, this, i, direction.get3DDataValue());
      }
   }

   private boolean getNeighborSignal(SignalGetter signalGetter, BlockPos pos, Direction direction) {
      for (Direction direction2 : Direction.values()) {
         if (direction2 != direction && signalGetter.hasSignal(pos.relative(direction2), direction2)) {
            return true;
         }
      }

      if (signalGetter.hasSignal(pos, Direction.DOWN)) {
         return true;
      } else {
         BlockPos blockPos = pos.above();

         for (Direction direction3 : Direction.values()) {
            if (direction3 != Direction.DOWN && signalGetter.hasSignal(blockPos.relative(direction3), direction3)) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
      Direction direction = state.getValue(FACING);
      BlockState blockState = state.setValue(EXTENDED, true);
      if (!level.isClientSide()) {
         boolean bl = this.getNeighborSignal(level, pos, direction);
         if (bl && (id == 1 || id == 2)) {
            level.setBlock(pos, blockState, 2);
            return false;
         }

         if (!bl && id == 0) {
            return false;
         }
      }

      if (id == 0) {
         if (!this.moveBlocks(level, pos, direction, true)) {
            return false;
         }

         level.setBlock(pos, blockState, 67);
         level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.25F + 0.6F);
         level.gameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Context.of(blockState));
      } else if (id == 1 || id == 2) {
         BlockEntity blockEntity = level.getBlockEntity(pos.relative(direction));
         if (blockEntity instanceof PistonMovingBlockEntity) {
            ((PistonMovingBlockEntity)blockEntity).finalTick();
         }

         BlockState blockState2 = Blocks.MOVING_PISTON
            .defaultBlockState()
            .setValue(MovingPistonBlock.FACING, direction)
            .setValue(MovingPistonBlock.TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT);
         level.setBlock(pos, blockState2, 276);
         level.setBlockEntity(
            MovingPistonBlock.newMovingBlockEntity(
               pos, blockState2, this.defaultBlockState().setValue(FACING, Direction.from3DDataValue(param & 7)), direction, false, true
            )
         );
         level.updateNeighborsAt(pos, blockState2.getBlock());
         blockState2.updateNeighbourShapes(level, pos, 2);
         if (this.isSticky) {
            BlockPos blockPos = pos.offset(direction.getStepX() * 2, direction.getStepY() * 2, direction.getStepZ() * 2);
            BlockState blockState3 = level.getBlockState(blockPos);
            boolean bl2 = false;
            if (blockState3.is(Blocks.MOVING_PISTON)
               && level.getBlockEntity(blockPos) instanceof PistonMovingBlockEntity pistonMovingBlockEntity
               && pistonMovingBlockEntity.getDirection() == direction
               && pistonMovingBlockEntity.isExtending()) {
               pistonMovingBlockEntity.finalTick();
               bl2 = true;
            }

            if (!bl2) {
               if (id != 1
                  || blockState3.isAir()
                  || !isPushable(blockState3, level, blockPos, direction.getOpposite(), false, direction)
                  || blockState3.getPistonPushReaction() != PushReaction.NORMAL && !blockState3.is(Blocks.PISTON) && !blockState3.is(Blocks.STICKY_PISTON)) {
                  level.removeBlock(pos.relative(direction), false);
               } else {
                  this.moveBlocks(level, pos, direction, false);
               }
            }
         } else {
            level.removeBlock(pos.relative(direction), false);
         }

         level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.15F + 0.6F);
         level.gameEvent(GameEvent.BLOCK_DEACTIVATE, pos, GameEvent.Context.of(blockState2));
      }

      return true;
   }

   public static boolean isPushable(BlockState state, Level level, BlockPos pos, Direction movementDirection, boolean allowDestroy, Direction pistonFacing) {
      if (pos.getY() < level.getMinY() || pos.getY() > level.getMaxY() || !level.getWorldBorder().isWithinBounds(pos)) {
         return false;
      } else if (state.isAir()) {
         return true;
      } else if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.RESPAWN_ANCHOR) || state.is(Blocks.REINFORCED_DEEPSLATE)) {
         return false;
      } else if (movementDirection == Direction.DOWN && pos.getY() == level.getMinY()) {
         return false;
      } else if (movementDirection == Direction.UP && pos.getY() == level.getMaxY()) {
         return false;
      } else {
         if (!state.is(Blocks.PISTON) && !state.is(Blocks.STICKY_PISTON)) {
            if (state.getDestroySpeed(level, pos) == -1.0F) {
               return false;
            }

            switch (state.getPistonPushReaction()) {
               case BLOCK:
                  return false;
               case DESTROY:
                  return allowDestroy;
               case PUSH_ONLY:
                  return movementDirection == pistonFacing;
            }
         } else if (state.getValue(EXTENDED)) {
            return false;
         }

         return !state.hasBlockEntity();
      }
   }

   private boolean moveBlocks(Level level, BlockPos pos, Direction facing, boolean extending) {
      BlockPos blockPos = pos.relative(facing);
      if (!extending && level.getBlockState(blockPos).is(Blocks.PISTON_HEAD)) {
         level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 276);
      }

      PistonStructureResolver pistonStructureResolver = new PistonStructureResolver(level, pos, facing, extending);
      if (!pistonStructureResolver.resolve()) {
         return false;
      } else {
         Map<BlockPos, BlockState> map = Maps.newHashMap();
         List<BlockPos> list = pistonStructureResolver.getToPush();
         List<BlockState> list2 = Lists.newArrayList();

         for (BlockPos blockPos2 : list) {
            BlockState blockState = level.getBlockState(blockPos2);
            list2.add(blockState);
            map.put(blockPos2, blockState);
         }

         List<BlockPos> list3 = pistonStructureResolver.getToDestroy();
         BlockState[] blockStates = new BlockState[list.size() + list3.size()];
         Direction direction = extending ? facing : facing.getOpposite();
         int i = 0;

         for (int j = list3.size() - 1; j >= 0; j--) {
            BlockPos blockPos3 = list3.get(j);
            BlockState blockState2 = level.getBlockState(blockPos3);
            BlockEntity blockEntity = blockState2.hasBlockEntity() ? level.getBlockEntity(blockPos3) : null;
            dropResources(blockState2, level, blockPos3, blockEntity);
            if (!blockState2.is(BlockTags.FIRE) && level.isClientSide()) {
               level.levelEvent(2001, blockPos3, getId(blockState2));
            }

            level.setBlock(blockPos3, Blocks.AIR.defaultBlockState(), 18);
            level.gameEvent(GameEvent.BLOCK_DESTROY, blockPos3, GameEvent.Context.of(blockState2));
            blockStates[i++] = blockState2;
         }

         for (int j = list.size() - 1; j >= 0; j--) {
            BlockPos blockPos3 = list.get(j);
            BlockState blockState2 = level.getBlockState(blockPos3);
            blockPos3 = blockPos3.relative(direction);
            map.remove(blockPos3);
            BlockState blockState3 = Blocks.MOVING_PISTON.defaultBlockState().setValue(FACING, facing);
            level.setBlock(blockPos3, blockState3, 324);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(blockPos3, blockState3, list2.get(j), facing, extending, false));
            blockStates[i++] = blockState2;
         }

         if (extending) {
            PistonType pistonType = this.isSticky ? PistonType.STICKY : PistonType.DEFAULT;
            BlockState blockState4 = Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING, facing).setValue(PistonHeadBlock.TYPE, pistonType);
            BlockState blockState2 = Blocks.MOVING_PISTON
               .defaultBlockState()
               .setValue(MovingPistonBlock.FACING, facing)
               .setValue(MovingPistonBlock.TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT);
            map.remove(blockPos);
            level.setBlock(blockPos, blockState2, 324);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(blockPos, blockState2, blockState4, facing, true, true));
         }

         BlockState blockState5 = Blocks.AIR.defaultBlockState();

         for (BlockPos blockPos4 : map.keySet()) {
            level.setBlock(blockPos4, blockState5, 82);
         }

         for (Entry<BlockPos, BlockState> entry : map.entrySet()) {
            BlockPos blockPos5 = entry.getKey();
            BlockState blockState6 = entry.getValue();
            blockState6.updateIndirectNeighbourShapes(level, blockPos5, 2);
            blockState5.updateNeighbourShapes(level, blockPos5, 2);
            blockState5.updateIndirectNeighbourShapes(level, blockPos5, 2);
         }

         Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, pistonStructureResolver.getPushDirection(), null);
         i = 0;

         for (int k = list3.size() - 1; k >= 0; k--) {
            BlockState blockState3 = blockStates[i++];
            BlockPos blockPos6 = list3.get(k);
            if (level instanceof ServerLevel serverLevel) {
               blockState3.affectNeighborsAfterRemoval(serverLevel, blockPos6, false);
            }

            blockState3.updateIndirectNeighbourShapes(level, blockPos6, 2);
            level.updateNeighborsAt(blockPos6, blockState3.getBlock(), orientation);
         }

         for (int k = list.size() - 1; k >= 0; k--) {
            level.updateNeighborsAt(list.get(k), blockStates[i++].getBlock(), orientation);
         }

         if (extending) {
            level.updateNeighborsAt(blockPos, Blocks.PISTON_HEAD, orientation);
         }

         return true;
      }
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
      builder.add(FACING, EXTENDED);
   }

   @Override
   protected boolean useShapeForLightOcclusion(BlockState state) {
      return state.getValue(EXTENDED);
   }

   @Override
   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }
}
