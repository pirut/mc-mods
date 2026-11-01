package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.redstone.DefaultRedstoneWireEvaluator;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.ExperimentalRedstoneWireEvaluator;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.redstone.RedstoneWireEvaluator;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class RedStoneWireBlock extends Block {
   public static final MapCodec<RedStoneWireBlock> CODEC = simpleCodec(RedStoneWireBlock::new);
   public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
   public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
   public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
   public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
   public static final IntegerProperty POWER = BlockStateProperties.POWER;
   public static final Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(
      Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST))
   );
   private static final int[] COLORS = Util.make(new int[16], is -> {
      for (int i = 0; i <= 15; i++) {
         float f = i / 15.0F;
         float g = f * 0.6F + (f > 0.0F ? 0.4F : 0.3F);
         float h = Mth.clamp(f * f * 0.7F - 0.5F, 0.0F, 1.0F);
         float j = Mth.clamp(f * f * 0.6F - 0.7F, 0.0F, 1.0F);
         is[i] = ARGB.colorFromFloat(1.0F, g, h, j);
      }
   });
   private static final float PARTICLE_DENSITY = 0.2F;
   private final Function<BlockState, VoxelShape> shapes;
   private final BlockState crossState;
   private final RedstoneWireEvaluator evaluator = new DefaultRedstoneWireEvaluator(this);
   private boolean shouldSignal = true;

   @Override
   public MapCodec<RedStoneWireBlock> codec() {
      return CODEC;
   }

   public RedStoneWireBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(NORTH, RedstoneSide.NONE)
            .setValue(EAST, RedstoneSide.NONE)
            .setValue(SOUTH, RedstoneSide.NONE)
            .setValue(WEST, RedstoneSide.NONE)
            .setValue(POWER, 0)
      );
      this.shapes = this.makeShapes();
      this.crossState = this.defaultBlockState()
         .setValue(NORTH, RedstoneSide.SIDE)
         .setValue(EAST, RedstoneSide.SIDE)
         .setValue(SOUTH, RedstoneSide.SIDE)
         .setValue(WEST, RedstoneSide.SIDE);
   }

   private Function<BlockState, VoxelShape> makeShapes() {
      int i = 1;
      int j = 10;
      VoxelShape voxelShape = Block.column(10.0, 0.0, 1.0);
      Map<Direction, VoxelShape> map = Shapes.rotateHorizontal(Block.boxZ(10.0, 0.0, 1.0, 0.0, 8.0));
      Map<Direction, VoxelShape> map2 = Shapes.rotateHorizontal(Block.boxZ(10.0, 16.0, 0.0, 1.0));
      return this.getShapeForEachState(blockState -> {
         VoxelShape voxelShape2 = voxelShape;

         for (Entry<Direction, EnumProperty<RedstoneSide>> entry : PROPERTY_BY_DIRECTION.entrySet()) {
            voxelShape2 = switch ((RedstoneSide)blockState.getValue(entry.getValue())) {
               case UP -> Shapes.or(voxelShape2, map.get(entry.getKey()), map2.get(entry.getKey()));
               case SIDE -> Shapes.or(voxelShape2, map.get(entry.getKey()));
               case NONE -> voxelShape2;
            };
         }

         return voxelShape2;
      }, new Property[]{POWER});
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.shapes.apply(state);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return this.getConnectionState(context.getLevel(), this.crossState, context.getClickedPos());
   }

   private BlockState getConnectionState(BlockGetter level, BlockState state, BlockPos pos) {
      boolean bl = isDot(state);
      state = this.getMissingConnections(level, this.defaultBlockState().setValue(POWER, state.getValue(POWER)), pos);
      if (bl && isDot(state)) {
         return state;
      } else {
         boolean bl2 = state.getValue(NORTH).isConnected();
         boolean bl3 = state.getValue(SOUTH).isConnected();
         boolean bl4 = state.getValue(EAST).isConnected();
         boolean bl5 = state.getValue(WEST).isConnected();
         boolean bl6 = !bl2 && !bl3;
         boolean bl7 = !bl4 && !bl5;
         if (!bl5 && bl6) {
            state = state.setValue(WEST, RedstoneSide.SIDE);
         }

         if (!bl4 && bl6) {
            state = state.setValue(EAST, RedstoneSide.SIDE);
         }

         if (!bl2 && bl7) {
            state = state.setValue(NORTH, RedstoneSide.SIDE);
         }

         if (!bl3 && bl7) {
            state = state.setValue(SOUTH, RedstoneSide.SIDE);
         }

         return state;
      }
   }

   private BlockState getMissingConnections(BlockGetter level, BlockState state, BlockPos pos) {
      boolean bl = !level.getBlockState(pos.above()).isRedstoneConductor(level, pos);

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         if (!state.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected()) {
            RedstoneSide redstoneSide = this.getConnectingSide(level, pos, direction, bl);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), redstoneSide);
         }
      }

      return state;
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
      if (direction == Direction.DOWN) {
         return !this.canSurviveOn(level, neighborPos, neighborState) ? Blocks.AIR.defaultBlockState() : state;
      } else if (direction == Direction.UP) {
         return this.getConnectionState(level, state, pos);
      } else {
         RedstoneSide redstoneSide = this.getConnectingSide(level, pos, direction);
         return redstoneSide.isConnected() == state.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected() && !isCross(state)
            ? state.setValue(PROPERTY_BY_DIRECTION.get(direction), redstoneSide)
            : this.getConnectionState(
               level, this.crossState.setValue(POWER, state.getValue(POWER)).setValue(PROPERTY_BY_DIRECTION.get(direction), redstoneSide), pos
            );
      }
   }

   private static boolean isCross(BlockState state) {
      return state.getValue(NORTH).isConnected()
         && state.getValue(SOUTH).isConnected()
         && state.getValue(EAST).isConnected()
         && state.getValue(WEST).isConnected();
   }

   private static boolean isDot(BlockState state) {
      return !state.getValue(NORTH).isConnected()
         && !state.getValue(SOUTH).isConnected()
         && !state.getValue(EAST).isConnected()
         && !state.getValue(WEST).isConnected();
   }

   @Override
   protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, @Block.UpdateFlags int flags, int recursionLeft) {
      BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         RedstoneSide redstoneSide = state.getValue(PROPERTY_BY_DIRECTION.get(direction));
         if (redstoneSide != RedstoneSide.NONE && !level.getBlockState(mutableBlockPos.setWithOffset(pos, direction)).is(this)) {
            mutableBlockPos.move(Direction.DOWN);
            BlockState blockState = level.getBlockState(mutableBlockPos);
            if (blockState.is(this)) {
               BlockPos blockPos = mutableBlockPos.relative(direction.getOpposite());
               level.neighborShapeChanged(direction.getOpposite(), mutableBlockPos, blockPos, level.getBlockState(blockPos), flags, recursionLeft);
            }

            mutableBlockPos.setWithOffset(pos, direction).move(Direction.UP);
            BlockState blockState2 = level.getBlockState(mutableBlockPos);
            if (blockState2.is(this)) {
               BlockPos blockPos2 = mutableBlockPos.relative(direction.getOpposite());
               level.neighborShapeChanged(direction.getOpposite(), mutableBlockPos, blockPos2, level.getBlockState(blockPos2), flags, recursionLeft);
            }
         }
      }
   }

   private RedstoneSide getConnectingSide(BlockGetter level, BlockPos pos, Direction face) {
      return this.getConnectingSide(level, pos, face, !level.getBlockState(pos.above()).isRedstoneConductor(level, pos));
   }

   private RedstoneSide getConnectingSide(BlockGetter level, BlockPos pos, Direction direction, boolean nonNormalCubeAbove) {
      BlockPos blockPos = pos.relative(direction);
      BlockState blockState = level.getBlockState(blockPos);
      if (nonNormalCubeAbove) {
         boolean bl = blockState.getBlock() instanceof TrapDoorBlock || this.canSurviveOn(level, blockPos, blockState);
         if (bl && shouldConnectTo(level.getBlockState(blockPos.above()))) {
            if (blockState.isFaceSturdy(level, blockPos, direction.getOpposite())) {
               return RedstoneSide.UP;
            }

            return RedstoneSide.SIDE;
         }
      }

      return !shouldConnectTo(blockState, direction)
            && (blockState.isRedstoneConductor(level, blockPos) || !shouldConnectTo(level.getBlockState(blockPos.below())))
         ? RedstoneSide.NONE
         : RedstoneSide.SIDE;
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      BlockPos blockPos = pos.below();
      BlockState blockState = level.getBlockState(blockPos);
      return this.canSurviveOn(level, blockPos, blockState);
   }

   private boolean canSurviveOn(BlockGetter level, BlockPos pos, BlockState state) {
      return state.isFaceSturdy(level, pos, Direction.UP) || state.is(Blocks.HOPPER);
   }

   private void updatePowerStrength(Level level, BlockPos pos, BlockState state, @Nullable Orientation orientation, boolean updateShape) {
      if (useExperimentalEvaluator(level)) {
         new ExperimentalRedstoneWireEvaluator(this).updatePowerStrength(level, pos, state, orientation, updateShape);
      } else {
         this.evaluator.updatePowerStrength(level, pos, state, orientation, updateShape);
      }
   }

   public int getBlockSignal(Level level, BlockPos pos) {
      this.shouldSignal = false;
      int i = level.getBestNeighborSignal(pos);
      this.shouldSignal = true;
      return i;
   }

   private void checkCornerChangeAt(Level level, BlockPos pos) {
      if (level.getBlockState(pos).is(this)) {
         level.updateNeighborsAt(pos, this);

         for (Direction direction : Direction.values()) {
            level.updateNeighborsAt(pos.relative(direction), this);
         }
      }
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      if (!oldState.is(state.getBlock()) && !level.isClientSide()) {
         this.updatePowerStrength(level, pos, state, null, true);

         for (Direction direction : Direction.Plane.VERTICAL) {
            level.updateNeighborsAt(pos.relative(direction), this);
         }

         this.updateNeighborsOfNeighboringWires(level, pos);
      }
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
      if (!movedByPiston) {
         for (Direction direction : Direction.values()) {
            level.updateNeighborsAt(pos.relative(direction), this);
         }

         this.updatePowerStrength(level, pos, state, null, false);
         this.updateNeighborsOfNeighboringWires(level, pos);
      }
   }

   private void updateNeighborsOfNeighboringWires(Level level, BlockPos pos) {
      for (Direction direction : Direction.Plane.HORIZONTAL) {
         this.checkCornerChangeAt(level, pos.relative(direction));
      }

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos blockPos = pos.relative(direction);
         if (level.getBlockState(blockPos).isRedstoneConductor(level, blockPos)) {
            this.checkCornerChangeAt(level, blockPos.above());
         } else {
            this.checkCornerChangeAt(level, blockPos.below());
         }
      }
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
      if (!level.isClientSide()) {
         if (neighborBlock != this || !useExperimentalEvaluator(level)) {
            if (state.canSurvive(level, pos)) {
               this.updatePowerStrength(level, pos, state, orientation, false);
            } else {
               dropResources(state, level, pos);
               level.removeBlock(pos, false);
            }
         }
      }
   }

   private static boolean useExperimentalEvaluator(Level level) {
      return level.enabledFeatures().contains(FeatureFlags.REDSTONE_EXPERIMENTS);
   }

   @Override
   protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return !this.shouldSignal ? 0 : state.getSignal(level, pos, direction);
   }

   @Override
   protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      if (this.shouldSignal && direction != Direction.DOWN) {
         int i = state.getValue(POWER);
         if (i == 0) {
            return 0;
         } else {
            return direction != Direction.UP
                  && !this.getConnectionState(level, state, pos).getValue(PROPERTY_BY_DIRECTION.get(direction.getOpposite())).isConnected()
               ? 0
               : i;
         }
      } else {
         return 0;
      }
   }

   protected static boolean shouldConnectTo(BlockState state) {
      return shouldConnectTo(state, null);
   }

   protected static boolean shouldConnectTo(BlockState state, @Nullable Direction direction) {
      if (state.is(Blocks.REDSTONE_WIRE)) {
         return true;
      } else if (state.is(Blocks.REPEATER)) {
         Direction direction2 = state.getValue(RepeaterBlock.FACING);
         return direction2 == direction || direction2.getOpposite() == direction;
      } else {
         return state.is(Blocks.OBSERVER) ? direction == state.getValue(ObserverBlock.FACING) : state.isSignalSource() && direction != null;
      }
   }

   @Override
   protected boolean isSignalSource(BlockState state) {
      return this.shouldSignal;
   }

   public static int getColorForPower(int power) {
      return COLORS[power];
   }

   private static void spawnParticlesAlongLine(
      Level level, RandomSource random, BlockPos pos, int color, Direction direction, Direction perpendicularDirection, float start, float end
   ) {
      float f = end - start;
      if (!(random.nextFloat() >= 0.2F * f)) {
         float g = 0.4375F;
         float h = start + f * random.nextFloat();
         double d = 0.5 + 0.4375F * direction.getStepX() + h * perpendicularDirection.getStepX();
         double e = 0.5 + 0.4375F * direction.getStepY() + h * perpendicularDirection.getStepY();
         double i = 0.5 + 0.4375F * direction.getStepZ() + h * perpendicularDirection.getStepZ();
         level.addParticle(new DustParticleOptions(color, 1.0F), pos.getX() + d, pos.getY() + e, pos.getZ() + i, 0.0, 0.0, 0.0);
      }
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      int i = state.getValue(POWER);
      if (i != 0) {
         for (Direction direction : Direction.Plane.HORIZONTAL) {
            RedstoneSide redstoneSide = state.getValue(PROPERTY_BY_DIRECTION.get(direction));
            switch (redstoneSide) {
               case UP:
                  spawnParticlesAlongLine(level, random, pos, COLORS[i], direction, Direction.UP, -0.5F, 0.5F);
               case SIDE:
                  spawnParticlesAlongLine(level, random, pos, COLORS[i], Direction.DOWN, direction, 0.0F, 0.5F);
                  break;
               case NONE:
               default:
                  spawnParticlesAlongLine(level, random, pos, COLORS[i], Direction.DOWN, direction, 0.0F, 0.3F);
            }
         }
      }
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

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(NORTH, EAST, SOUTH, WEST, POWER);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (!player.getAbilities().mayBuild) {
         return InteractionResult.PASS;
      } else {
         if (isCross(state) || isDot(state)) {
            BlockState blockState = isCross(state) ? this.defaultBlockState() : this.crossState;
            blockState = blockState.setValue(POWER, state.getValue(POWER));
            blockState = this.getConnectionState(level, blockState, pos);
            if (blockState != state) {
               level.setBlock(pos, blockState, 3);
               this.updatesOnShapeChange(level, pos, state, blockState);
               return InteractionResult.SUCCESS;
            }
         }

         return InteractionResult.PASS;
      }
   }

   private void updatesOnShapeChange(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
      Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, null, Direction.UP);

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos blockPos = pos.relative(direction);
         if (oldState.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected() != newState.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected()
            && level.getBlockState(blockPos).isRedstoneConductor(level, blockPos)) {
            level.updateNeighborsAtExceptFromFacing(
               blockPos, newState.getBlock(), direction.getOpposite(), ExperimentalRedstoneUtils.withFront(orientation, direction)
            );
         }
      }
   }
}
