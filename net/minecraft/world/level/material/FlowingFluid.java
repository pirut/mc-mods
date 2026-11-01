package net.minecraft.world.level.material;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class FlowingFluid extends Fluid {
   public static final BooleanProperty FALLING = BlockStateProperties.FALLING;
   public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_FLOWING;
   private static final int CACHE_SIZE = 200;
   private static final ThreadLocal<Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(
      () -> {
         Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey> object2ByteLinkedOpenHashMap = new Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey>(
            200
         ) {
            protected void rehash(int i) {
            }
         };
         object2ByteLinkedOpenHashMap.defaultReturnValue((byte)127);
         return object2ByteLinkedOpenHashMap;
      }
   );
   private final Map<FluidState, VoxelShape> shapes = Maps.newIdentityHashMap();

   @Override
   protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
      builder.add(FALLING);
   }

   @Override
   public Vec3 getFlow(BlockGetter blockReader, BlockPos pos, FluidState fluidState) {
      double d = 0.0;
      double e = 0.0;
      BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         mutableBlockPos.setWithOffset(pos, direction);
         FluidState fluidState2 = blockReader.getFluidState(mutableBlockPos);
         if (this.affectsFlow(fluidState2)) {
            float f = fluidState2.getOwnHeight();
            float g = 0.0F;
            if (f == 0.0F) {
               if (!blockReader.getBlockState(mutableBlockPos).blocksMotion()) {
                  BlockPos blockPos = mutableBlockPos.below();
                  FluidState fluidState3 = blockReader.getFluidState(blockPos);
                  if (this.affectsFlow(fluidState3)) {
                     f = fluidState3.getOwnHeight();
                     if (f > 0.0F) {
                        g = fluidState.getOwnHeight() - (f - 0.8888889F);
                     }
                  }
               }
            } else if (f > 0.0F) {
               g = fluidState.getOwnHeight() - f;
            }

            if (g != 0.0F) {
               d += direction.getStepX() * g;
               e += direction.getStepZ() * g;
            }
         }
      }

      Vec3 vec3 = new Vec3(d, 0.0, e);
      if (fluidState.getValue(FALLING)) {
         for (Direction direction2 : Direction.Plane.HORIZONTAL) {
            mutableBlockPos.setWithOffset(pos, direction2);
            if (this.isSolidFace(blockReader, mutableBlockPos, direction2) || this.isSolidFace(blockReader, mutableBlockPos.above(), direction2)) {
               vec3 = vec3.normalize().add(0.0, -6.0, 0.0);
               break;
            }
         }
      }

      return vec3.normalize();
   }

   private boolean affectsFlow(FluidState state) {
      return state.isEmpty() || state.getType().isSame(this);
   }

   protected boolean isSolidFace(BlockGetter level, BlockPos neighborPos, Direction side) {
      BlockState blockState = level.getBlockState(neighborPos);
      FluidState fluidState = level.getFluidState(neighborPos);
      if (fluidState.getType().isSame(this)) {
         return false;
      } else if (side == Direction.UP) {
         return true;
      } else {
         return blockState.getBlock() instanceof IceBlock ? false : blockState.isFaceSturdy(level, neighborPos, side);
      }
   }

   protected void spread(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
      if (!fluidState.isEmpty()) {
         BlockPos blockPos = pos.below();
         BlockState blockState2 = level.getBlockState(blockPos);
         FluidState fluidState2 = blockState2.getFluidState();
         if (this.canMaybePassThrough(level, pos, blockState, Direction.DOWN, blockPos, blockState2, fluidState2)) {
            FluidState fluidState3 = this.getNewLiquid(level, blockPos, blockState2);
            Fluid fluid = fluidState3.getType();
            if (fluidState2.canBeReplacedWith(level, blockPos, fluid, Direction.DOWN) && canHoldSpecificFluid(level, blockPos, blockState2, fluid)) {
               this.spreadTo(level, blockPos, blockState2, Direction.DOWN, fluidState3);
               if (this.sourceNeighborCount(level, pos) >= 3) {
                  this.spreadToSides(level, pos, fluidState, blockState);
               }

               return;
            }
         }

         if (fluidState.isSource() || !this.isWaterHole(level, pos, blockState, blockPos, blockState2)) {
            this.spreadToSides(level, pos, fluidState, blockState);
         }
      }
   }

   private void spreadToSides(ServerLevel level, BlockPos pos, FluidState fluidState, BlockState blockState) {
      int i = fluidState.getAmount() - this.getDropOff(level);
      if (fluidState.getValue(FALLING)) {
         i = 7;
      }

      if (i > 0) {
         Map<Direction, FluidState> map = this.getSpread(level, pos, blockState);

         for (Entry<Direction, FluidState> entry : map.entrySet()) {
            Direction direction = entry.getKey();
            FluidState fluidState2 = entry.getValue();
            BlockPos blockPos = pos.relative(direction);
            this.spreadTo(level, blockPos, level.getBlockState(blockPos), direction, fluidState2);
         }
      }
   }

   protected FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState state) {
      int i = 0;
      int j = 0;
      BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos blockPos = mutableBlockPos.setWithOffset(pos, direction);
         BlockState blockState = level.getBlockState(blockPos);
         FluidState fluidState = blockState.getFluidState();
         if (fluidState.getType().isSame(this) && canPassThroughWall(direction, level, pos, state, blockPos, blockState)) {
            if (fluidState.isSource()) {
               j++;
            }

            i = Math.max(i, fluidState.getAmount());
         }
      }

      if (j >= 2 && this.canConvertToSource(level)) {
         BlockState blockState2 = level.getBlockState(mutableBlockPos.setWithOffset(pos, Direction.DOWN));
         FluidState fluidState2 = blockState2.getFluidState();
         if (blockState2.isSolid() || this.isSourceBlockOfThisType(fluidState2)) {
            return this.getSource(false);
         }
      }

      BlockPos blockPos2 = mutableBlockPos.setWithOffset(pos, Direction.UP);
      BlockState blockState3 = level.getBlockState(blockPos2);
      FluidState fluidState3 = blockState3.getFluidState();
      if (!fluidState3.isEmpty() && fluidState3.getType().isSame(this) && canPassThroughWall(Direction.UP, level, pos, state, blockPos2, blockState3)) {
         return this.getFlowing(8, true);
      } else {
         int k = i - this.getDropOff(level);
         return k <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(k, false);
      }
   }

   private static boolean canPassThroughWall(Direction direction, BlockGetter level, BlockPos pos, BlockState state, BlockPos spreadPos, BlockState spreadState) {
      if (!SharedConstants.DEBUG_DISABLE_LIQUID_SPREADING && (!SharedConstants.DEBUG_ONLY_GENERATE_HALF_THE_WORLD || spreadPos.getZ() >= 0)) {
         VoxelShape voxelShape = spreadState.getCollisionShape(level, spreadPos);
         if (voxelShape == Shapes.block()) {
            return false;
         } else {
            VoxelShape voxelShape2 = state.getCollisionShape(level, pos);
            if (voxelShape2 == Shapes.block()) {
               return false;
            } else if (voxelShape2 == Shapes.empty() && voxelShape == Shapes.empty()) {
               return true;
            } else {
               Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey> object2ByteLinkedOpenHashMap;
               if (!state.getBlock().hasDynamicShape() && !spreadState.getBlock().hasDynamicShape()) {
                  object2ByteLinkedOpenHashMap = OCCLUSION_CACHE.get();
               } else {
                  object2ByteLinkedOpenHashMap = null;
               }

               FlowingFluid.BlockStatePairKey blockStatePairKey;
               if (object2ByteLinkedOpenHashMap != null) {
                  blockStatePairKey = new FlowingFluid.BlockStatePairKey(state, spreadState, direction);
                  byte b = object2ByteLinkedOpenHashMap.getAndMoveToFirst(blockStatePairKey);
                  if (b != 127) {
                     return b != 0;
                  }
               } else {
                  blockStatePairKey = null;
               }

               boolean bl = !Shapes.mergedFaceOccludes(voxelShape2, voxelShape, direction);
               if (object2ByteLinkedOpenHashMap != null) {
                  if (object2ByteLinkedOpenHashMap.size() == 200) {
                     object2ByteLinkedOpenHashMap.removeLastByte();
                  }

                  object2ByteLinkedOpenHashMap.putAndMoveToFirst(blockStatePairKey, (byte)(bl ? 1 : 0));
               }

               return bl;
            }
         }
      } else {
         return false;
      }
   }

   public abstract Fluid getFlowing();

   public FluidState getFlowing(int level, boolean falling) {
      return this.getFlowing().defaultFluidState().setValue(LEVEL, level).setValue(FALLING, falling);
   }

   public abstract Fluid getSource();

   public FluidState getSource(boolean falling) {
      return this.getSource().defaultFluidState().setValue(FALLING, falling);
   }

   protected abstract boolean canConvertToSource(ServerLevel level);

   protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState blockState, Direction direction, FluidState fluidState) {
      if (blockState.getBlock() instanceof LiquidBlockContainer liquidBlockContainer) {
         liquidBlockContainer.placeLiquid(level, pos, blockState, fluidState);
      } else {
         if (!blockState.isAir()) {
            this.beforeDestroyingBlock(level, pos, blockState);
         }

         level.setBlock(pos, fluidState.createLegacyBlock(), 3);
      }
   }

   protected abstract void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state);

   protected int getSlopeDistance(LevelReader level, BlockPos pos, int depth, Direction direction, BlockState state, FlowingFluid.SpreadContext spreadContext) {
      int i = 1000;

      for (Direction direction2 : Direction.Plane.HORIZONTAL) {
         if (direction2 != direction) {
            BlockPos blockPos = pos.relative(direction2);
            BlockState blockState = spreadContext.getBlockState(blockPos);
            FluidState fluidState = blockState.getFluidState();
            if (this.canPassThrough(level, this.getFlowing(), pos, state, direction2, blockPos, blockState, fluidState)) {
               if (spreadContext.isHole(blockPos)) {
                  return depth;
               }

               if (depth < this.getSlopeFindDistance(level)) {
                  int j = this.getSlopeDistance(level, blockPos, depth + 1, direction2.getOpposite(), blockState, spreadContext);
                  if (j < i) {
                     i = j;
                  }
               }
            }
         }
      }

      return i;
   }

   boolean isWaterHole(BlockGetter level, BlockPos pos, BlockState state, BlockPos belowPos, BlockState belowState) {
      if (!canPassThroughWall(Direction.DOWN, level, pos, state, belowPos, belowState)) {
         return false;
      } else {
         return belowState.getFluidState().getType().isSame(this) ? true : canHoldFluid(level, belowPos, belowState, this.getFlowing());
      }
   }

   private boolean canPassThrough(
      BlockGetter level, Fluid fluid, BlockPos pos, BlockState state, Direction direction, BlockPos spreadPos, BlockState spreadState, FluidState fluidState
   ) {
      return this.canMaybePassThrough(level, pos, state, direction, spreadPos, spreadState, fluidState)
         && canHoldSpecificFluid(level, spreadPos, spreadState, fluid);
   }

   private boolean canMaybePassThrough(
      BlockGetter level, BlockPos pos, BlockState state, Direction direction, BlockPos spreadPos, BlockState spreadState, FluidState fluidState
   ) {
      return !this.isSourceBlockOfThisType(fluidState)
         && canHoldAnyFluid(spreadState)
         && canPassThroughWall(direction, level, pos, state, spreadPos, spreadState);
   }

   private boolean isSourceBlockOfThisType(FluidState state) {
      return state.getType().isSame(this) && state.isSource();
   }

   protected abstract int getSlopeFindDistance(LevelReader level);

   private int sourceNeighborCount(LevelReader level, BlockPos pos) {
      int i = 0;

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos blockPos = pos.relative(direction);
         FluidState fluidState = level.getFluidState(blockPos);
         if (this.isSourceBlockOfThisType(fluidState)) {
            i++;
         }
      }

      return i;
   }

   protected Map<Direction, FluidState> getSpread(ServerLevel level, BlockPos pos, BlockState state) {
      int i = 1000;
      Map<Direction, FluidState> map = Maps.newEnumMap(Direction.class);
      FlowingFluid.SpreadContext spreadContext = null;

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos blockPos = pos.relative(direction);
         BlockState blockState = level.getBlockState(blockPos);
         FluidState fluidState = blockState.getFluidState();
         if (this.canMaybePassThrough(level, pos, state, direction, blockPos, blockState, fluidState)) {
            FluidState fluidState2 = this.getNewLiquid(level, blockPos, blockState);
            if (canHoldSpecificFluid(level, blockPos, blockState, fluidState2.getType())) {
               if (spreadContext == null) {
                  spreadContext = new FlowingFluid.SpreadContext(level, pos);
               }

               int j;
               if (spreadContext.isHole(blockPos)) {
                  j = 0;
               } else {
                  j = this.getSlopeDistance(level, blockPos, 1, direction.getOpposite(), blockState, spreadContext);
               }

               if (j < i) {
                  map.clear();
               }

               if (j <= i) {
                  if (fluidState.canBeReplacedWith(level, blockPos, fluidState2.getType(), direction)) {
                     map.put(direction, fluidState2);
                  }

                  i = j;
               }
            }
         }
      }

      return map;
   }

   private static boolean canHoldAnyFluid(BlockState state) {
      Block block = state.getBlock();
      if (block instanceof LiquidBlockContainer) {
         return true;
      } else {
         return state.blocksMotion()
            ? false
            : !(block instanceof DoorBlock)
               && !state.is(BlockTags.SIGNS)
               && !state.is(Blocks.LADDER)
               && !state.is(Blocks.SUGAR_CANE)
               && !state.is(Blocks.BUBBLE_COLUMN)
               && !state.is(Blocks.NETHER_PORTAL)
               && !state.is(Blocks.END_PORTAL)
               && !state.is(Blocks.END_GATEWAY)
               && !state.is(Blocks.STRUCTURE_VOID);
      }
   }

   private static boolean canHoldFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
      return canHoldAnyFluid(state) && canHoldSpecificFluid(level, pos, state, fluid);
   }

   private static boolean canHoldSpecificFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
      return state.getBlock() instanceof LiquidBlockContainer liquidBlockContainer ? liquidBlockContainer.canPlaceLiquid(null, level, pos, state, fluid) : true;
   }

   protected abstract int getDropOff(LevelReader level);

   protected int getSpreadDelay(Level level, BlockPos pos, FluidState currentState, FluidState newState) {
      return this.getTickDelay(level);
   }

   @Override
   public void tick(ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
      if (!fluidState.isSource()) {
         FluidState fluidState2 = this.getNewLiquid(level, pos, level.getBlockState(pos));
         int i = this.getSpreadDelay(level, pos, fluidState, fluidState2);
         if (fluidState2.isEmpty()) {
            fluidState = fluidState2;
            blockState = Blocks.AIR.defaultBlockState();
            level.setBlock(pos, blockState, 3);
         } else if (fluidState2 != fluidState) {
            fluidState = fluidState2;
            blockState = fluidState2.createLegacyBlock();
            level.setBlock(pos, blockState, 3);
            level.scheduleTick(pos, fluidState2.getType(), i);
         }
      }

      this.spread(level, pos, blockState, fluidState);
   }

   protected static int getLegacyLevel(FluidState state) {
      return state.isSource() ? 0 : 8 - Math.min(state.getAmount(), 8) + (state.getValue(FALLING) ? 8 : 0);
   }

   private static boolean hasSameAbove(FluidState fluidState, BlockGetter level, BlockPos pos) {
      return fluidState.getType().isSame(level.getFluidState(pos.above()).getType());
   }

   @Override
   public float getHeight(FluidState state, BlockGetter level, BlockPos pos) {
      return hasSameAbove(state, level, pos) ? 1.0F : state.getOwnHeight();
   }

   @Override
   public float getOwnHeight(FluidState state) {
      return state.getAmount() / 9.0F;
   }

   @Override
   public abstract int getAmount(FluidState state);

   @Override
   public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
      return state.getAmount() == 9 && hasSameAbove(state, level, pos)
         ? Shapes.block()
         : this.shapes.computeIfAbsent(state, fluidState -> Shapes.box(0.0, 0.0, 0.0, 1.0, fluidState.getHeight(level, pos), 1.0));
   }

   record BlockStatePairKey(BlockState first, BlockState second, Direction direction) {
      @Override
      public boolean equals(Object object) {
         return object instanceof FlowingFluid.BlockStatePairKey blockStatePairKey
            && this.first == blockStatePairKey.first
            && this.second == blockStatePairKey.second
            && this.direction == blockStatePairKey.direction;
      }

      @Override
      public int hashCode() {
         int i = System.identityHashCode(this.first);
         i = 31 * i + System.identityHashCode(this.second);
         return 31 * i + this.direction.hashCode();
      }
   }

   protected class SpreadContext {
      private final BlockGetter level;
      private final BlockPos origin;
      private final Short2ObjectMap<BlockState> stateCache = new Short2ObjectOpenHashMap();
      private final Short2BooleanMap holeCache = new Short2BooleanOpenHashMap();

      SpreadContext(final BlockGetter level, final BlockPos origin) {
         this.level = level;
         this.origin = origin;
      }

      public BlockState getBlockState(BlockPos pos) {
         return this.getBlockState(pos, this.getCacheKey(pos));
      }

      private BlockState getBlockState(BlockPos pos, short cacheKey) {
         return (BlockState)this.stateCache.computeIfAbsent(cacheKey, s -> this.level.getBlockState(pos));
      }

      public boolean isHole(BlockPos pos) {
         return this.holeCache.computeIfAbsent(this.getCacheKey(pos), s -> {
            BlockState blockState = this.getBlockState(pos, s);
            BlockPos blockPos2 = pos.below();
            BlockState blockState2 = this.level.getBlockState(blockPos2);
            return FlowingFluid.this.isWaterHole(this.level, pos, blockState, blockPos2, blockState2);
         });
      }

      private short getCacheKey(BlockPos pos) {
         int i = pos.getX() - this.origin.getX();
         int j = pos.getZ() - this.origin.getZ();
         return (short)((i + 128 & 0xFF) << 8 | j + 128 & 0xFF);
      }
   }
}
