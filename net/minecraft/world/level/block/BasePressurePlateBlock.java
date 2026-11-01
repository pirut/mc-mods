package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class BasePressurePlateBlock extends Block {
   private static final VoxelShape SHAPE_PRESSED = Block.column(14.0, 0.0, 0.5);
   private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 1.0);
   protected static final AABB TOUCH_AABB = Block.column(14.0, 0.0, 4.0).toAabbs().getFirst();
   protected final BlockSetType type;

   protected BasePressurePlateBlock(BlockBehaviour.Properties properties, BlockSetType type) {
      super(properties.sound(type.soundType()));
      this.type = type;
   }

   @Override
   protected abstract MapCodec<? extends BasePressurePlateBlock> codec();

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.getSignalForState(state) > 0 ? SHAPE_PRESSED : SHAPE;
   }

   protected int getPressedTime() {
      return 20;
   }

   @Override
   public boolean isPossibleToRespawnInThis(BlockState state) {
      return true;
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
      return direction == Direction.DOWN && !state.canSurvive(level, pos)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      BlockPos blockPos = pos.below();
      return canSupportRigidBlock(level, blockPos) || canSupportCenter(level, blockPos, Direction.UP);
   }

   @Override
   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      int i = this.getSignalForState(state);
      if (i > 0) {
         this.checkPressed(null, level, pos, state, i);
      }
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
      if (!level.isClientSide()) {
         int i = this.getSignalForState(state);
         if (i == 0) {
            this.checkPressed(entity, level, pos, state, i);
         }
      }
   }

   private void checkPressed(@Nullable Entity entity, Level level, BlockPos pos, BlockState state, int currentSignal) {
      int i = this.getSignalStrength(level, pos);
      boolean bl = currentSignal > 0;
      boolean bl2 = i > 0;
      if (currentSignal != i) {
         BlockState blockState = this.setSignalForState(state, i);
         level.setBlock(pos, blockState, 2);
         this.updateNeighbours(level, pos);
         level.setBlocksDirty(pos, state, blockState);
      }

      if (!bl2 && bl) {
         level.playSound(null, pos, this.type.pressurePlateClickOff(), SoundSource.BLOCKS);
         level.gameEvent(entity, GameEvent.BLOCK_DEACTIVATE, pos);
      } else if (bl2 && !bl) {
         level.playSound(null, pos, this.type.pressurePlateClickOn(), SoundSource.BLOCKS);
         level.gameEvent(entity, GameEvent.BLOCK_ACTIVATE, pos);
      }

      if (bl2) {
         level.scheduleTick(new BlockPos(pos), this, this.getPressedTime());
      }
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
      if (!movedByPiston && this.getSignalForState(state) > 0) {
         this.updateNeighbours(level, pos);
      }
   }

   protected void updateNeighbours(Level level, BlockPos pos) {
      level.updateNeighborsAt(pos, this);
      level.updateNeighborsAt(pos.below(), this);
   }

   @Override
   protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return this.getSignalForState(state);
   }

   @Override
   protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return direction == Direction.UP ? this.getSignalForState(state) : 0;
   }

   @Override
   protected boolean isSignalSource(BlockState state) {
      return true;
   }

   protected static int getEntityCount(Level level, AABB box, Class<? extends Entity> entityClass) {
      return level.getEntitiesOfClass(entityClass, box, EntitySelector.NO_SPECTATORS.and(entity -> !entity.isIgnoringBlockTriggers())).size();
   }

   protected abstract int getSignalStrength(Level level, BlockPos pos);

   protected abstract int getSignalForState(BlockState state);

   protected abstract BlockState setSignalForState(BlockState state, int signal);
}
