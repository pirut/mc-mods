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
import net.neoforged.neoforge.event.EventHooks;

public abstract class FlowingFluid extends Fluid {
   public static final BooleanProperty FALLING = BlockStateProperties.FALLING;
   public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_FLOWING;
   private static final int CACHE_SIZE = 200;
   private static final ThreadLocal<Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(
      () -> {
         Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey> object2bytelinkedopenhashmap = new Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey>(
            200
         ) {
            protected void rehash(int p_76102_) {
            }
         };
         object2bytelinkedopenhashmap.defaultReturnValue((byte)127);
         return object2bytelinkedopenhashmap;
      }
   );
   private final Map<FluidState, VoxelShape> shapes = Maps.newIdentityHashMap();

   @Override
   protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> p_76046_) {
      p_76046_.add(FALLING);
   }

   @Override
   public Vec3 getFlow(BlockGetter p_75987_, BlockPos p_75988_, FluidState p_75989_) {
      double d0 = 0.0;
      double d1 = 0.0;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         blockpos$mutableblockpos.setWithOffset(p_75988_, direction);
         FluidState fluidstate = p_75987_.getFluidState(blockpos$mutableblockpos);
         if (this.affectsFlow(fluidstate)) {
            float f = fluidstate.getOwnHeight();
            float f1 = 0.0F;
            if (f == 0.0F) {
               if (!p_75987_.getBlockState(blockpos$mutableblockpos).blocksMotion()) {
                  BlockPos blockpos = blockpos$mutableblockpos.below();
                  FluidState fluidstate1 = p_75987_.getFluidState(blockpos);
                  if (this.affectsFlow(fluidstate1)) {
                     f = fluidstate1.getOwnHeight();
                     if (f > 0.0F) {
                        f1 = p_75989_.getOwnHeight() - (f - 0.8888889F);
                     }
                  }
               }
            } else if (f > 0.0F) {
               f1 = p_75989_.getOwnHeight() - f;
            }

            if (f1 != 0.0F) {
               d0 += direction.getStepX() * f1;
               d1 += direction.getStepZ() * f1;
            }
         }
      }

      Vec3 vec3 = new Vec3(d0, 0.0, d1);
      if (p_75989_.getValue(FALLING)) {
         for (Direction direction1 : Direction.Plane.HORIZONTAL) {
            blockpos$mutableblockpos.setWithOffset(p_75988_, direction1);
            if (this.isSolidFace(p_75987_, blockpos$mutableblockpos, direction1) || this.isSolidFace(p_75987_, blockpos$mutableblockpos.above(), direction1)) {
               vec3 = vec3.normalize().add(0.0, -6.0, 0.0);
               break;
            }
         }
      }

      return vec3.normalize();
   }

   private boolean affectsFlow(FluidState p_76095_) {
      return p_76095_.isEmpty() || p_76095_.getType().isSame(this);
   }

   protected boolean isSolidFace(BlockGetter p_75991_, BlockPos p_75992_, Direction p_75993_) {
      BlockState blockstate = p_75991_.getBlockState(p_75992_);
      FluidState fluidstate = p_75991_.getFluidState(p_75992_);
      if (fluidstate.getType().isSame(this)) {
         return false;
      } else if (p_75993_ == Direction.UP) {
         return true;
      } else {
         return blockstate.getBlock() instanceof IceBlock ? false : blockstate.isFaceSturdy(p_75991_, p_75992_, p_75993_);
      }
   }

   protected void spread(ServerLevel p_376507_, BlockPos p_76012_, BlockState p_361407_, FluidState p_76013_) {
      if (!p_76013_.isEmpty()) {
         BlockPos blockpos = p_76012_.below();
         BlockState blockstate = p_376507_.getBlockState(blockpos);
         FluidState fluidstate = blockstate.getFluidState();
         if (this.canMaybePassThrough(p_376507_, p_76012_, p_361407_, Direction.DOWN, blockpos, blockstate, fluidstate)) {
            FluidState fluidstate1 = this.getNewLiquid(p_376507_, blockpos, blockstate);
            Fluid fluid = fluidstate1.getType();
            if (fluidstate.canBeReplacedWith(p_376507_, blockpos, fluid, Direction.DOWN) && canHoldSpecificFluid(p_376507_, blockpos, blockstate, fluid)) {
               this.spreadTo(p_376507_, blockpos, blockstate, Direction.DOWN, fluidstate1);
               if (this.sourceNeighborCount(p_376507_, p_76012_) >= 3) {
                  this.spreadToSides(p_376507_, p_76012_, p_76013_, p_361407_);
               }

               return;
            }
         }

         if (p_76013_.isSource() || !this.isWaterHole(p_376507_, p_76012_, p_361407_, blockpos, blockstate)) {
            this.spreadToSides(p_376507_, p_76012_, p_76013_, p_361407_);
         }
      }
   }

   private void spreadToSides(ServerLevel p_376224_, BlockPos p_76016_, FluidState p_76017_, BlockState p_76018_) {
      int i = p_76017_.getAmount() - this.getDropOff(p_376224_);
      if (p_76017_.getValue(FALLING)) {
         i = 7;
      }

      if (i > 0) {
         Map<Direction, FluidState> map = this.getSpread(p_376224_, p_76016_, p_76018_);

         for (Entry<Direction, FluidState> entry : map.entrySet()) {
            Direction direction = entry.getKey();
            FluidState fluidstate = entry.getValue();
            BlockPos blockpos = p_76016_.relative(direction);
            this.spreadTo(p_376224_, blockpos, p_376224_.getBlockState(blockpos), direction, fluidstate);
         }
      }
   }

   protected FluidState getNewLiquid(ServerLevel p_376839_, BlockPos p_76037_, BlockState p_76038_) {
      int i = 0;
      int j = 0;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos blockpos = blockpos$mutableblockpos.setWithOffset(p_76037_, direction);
         BlockState blockstate = p_376839_.getBlockState(blockpos);
         FluidState fluidstate = blockstate.getFluidState();
         if (fluidstate.getType().isSame(this) && canPassThroughWall(direction, p_376839_, p_76037_, p_76038_, blockpos, blockstate)) {
            if (fluidstate.isSource() && EventHooks.canCreateFluidSource(p_376839_, blockpos, blockstate)) {
               j++;
            }

            i = Math.max(i, fluidstate.getAmount());
         }
      }

      if (j >= 2) {
         BlockState blockstate1 = p_376839_.getBlockState(blockpos$mutableblockpos.setWithOffset(p_76037_, Direction.DOWN));
         FluidState fluidstate1 = blockstate1.getFluidState();
         if (blockstate1.isSolid() || this.isSourceBlockOfThisType(fluidstate1)) {
            return this.getSource(false);
         }
      }

      BlockPos blockpos1 = blockpos$mutableblockpos.setWithOffset(p_76037_, Direction.UP);
      BlockState blockstate2 = p_376839_.getBlockState(blockpos1);
      FluidState fluidstate2 = blockstate2.getFluidState();
      if (!fluidstate2.isEmpty()
         && fluidstate2.getType().isSame(this)
         && canPassThroughWall(Direction.UP, p_376839_, p_76037_, p_76038_, blockpos1, blockstate2)) {
         return this.getFlowing(8, true);
      } else {
         int k = i - this.getDropOff(p_376839_);
         return k <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(k, false);
      }
   }

   private static boolean canPassThroughWall(
      Direction p_76062_, BlockGetter p_76063_, BlockPos p_76064_, BlockState p_76065_, BlockPos p_76066_, BlockState p_76067_
   ) {
      if (!SharedConstants.DEBUG_DISABLE_LIQUID_SPREADING && (!SharedConstants.DEBUG_ONLY_GENERATE_HALF_THE_WORLD || p_76066_.getZ() >= 0)) {
         VoxelShape voxelshape = p_76067_.getCollisionShape(p_76063_, p_76066_);
         if (voxelshape == Shapes.block()) {
            return false;
         } else {
            VoxelShape voxelshape1 = p_76065_.getCollisionShape(p_76063_, p_76064_);
            if (voxelshape1 == Shapes.block()) {
               return false;
            } else if (voxelshape1 == Shapes.empty() && voxelshape == Shapes.empty()) {
               return true;
            } else {
               Object2ByteLinkedOpenHashMap<FlowingFluid.BlockStatePairKey> object2bytelinkedopenhashmap;
               if (!p_76065_.getBlock().hasDynamicShape() && !p_76067_.getBlock().hasDynamicShape()) {
                  object2bytelinkedopenhashmap = OCCLUSION_CACHE.get();
               } else {
                  object2bytelinkedopenhashmap = null;
               }

               FlowingFluid.BlockStatePairKey flowingfluid$blockstatepairkey;
               if (object2bytelinkedopenhashmap != null) {
                  flowingfluid$blockstatepairkey = new FlowingFluid.BlockStatePairKey(p_76065_, p_76067_, p_76062_);
                  byte b0 = object2bytelinkedopenhashmap.getAndMoveToFirst(flowingfluid$blockstatepairkey);
                  if (b0 != 127) {
                     return b0 != 0;
                  }
               } else {
                  flowingfluid$blockstatepairkey = null;
               }

               boolean flag = !Shapes.mergedFaceOccludes(voxelshape1, voxelshape, p_76062_);
               if (object2bytelinkedopenhashmap != null) {
                  if (object2bytelinkedopenhashmap.size() == 200) {
                     object2bytelinkedopenhashmap.removeLastByte();
                  }

                  object2bytelinkedopenhashmap.putAndMoveToFirst(flowingfluid$blockstatepairkey, (byte)(flag ? 1 : 0));
               }

               return flag;
            }
         }
      } else {
         return false;
      }
   }

   public abstract Fluid getFlowing();

   public FluidState getFlowing(int p_75954_, boolean p_75955_) {
      return this.getFlowing().defaultFluidState().setValue(LEVEL, p_75954_).setValue(FALLING, p_75955_);
   }

   public abstract Fluid getSource();

   public FluidState getSource(boolean p_76069_) {
      return this.getSource().defaultFluidState().setValue(FALLING, p_76069_);
   }

   @Override
   public boolean canConvertToSource(FluidState state, ServerLevel level, BlockPos pos) {
      return this.canConvertToSource(level);
   }

   @Deprecated
   protected abstract boolean canConvertToSource(ServerLevel var1);

   protected void spreadTo(LevelAccessor p_76005_, BlockPos p_76006_, BlockState p_76007_, Direction p_76008_, FluidState p_76009_) {
      if (p_76007_.getBlock() instanceof LiquidBlockContainer liquidblockcontainer) {
         liquidblockcontainer.placeLiquid(p_76005_, p_76006_, p_76007_, p_76009_);
      } else {
         if (!p_76007_.isAir()) {
            this.beforeDestroyingBlock(p_76005_, p_76006_, p_76007_);
         }

         p_76005_.setBlock(p_76006_, p_76009_.createLegacyBlock(), 3);
      }
   }

   protected abstract void beforeDestroyingBlock(LevelAccessor var1, BlockPos var2, BlockState var3);

   protected int getSlopeDistance(
      LevelReader p_76027_, BlockPos p_76028_, int p_76029_, Direction p_76030_, BlockState p_76031_, FlowingFluid.SpreadContext p_364491_
   ) {
      int i = 1000;

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         if (direction != p_76030_) {
            BlockPos blockpos = p_76028_.relative(direction);
            BlockState blockstate = p_364491_.getBlockState(blockpos);
            FluidState fluidstate = blockstate.getFluidState();
            if (this.canPassThrough(p_76027_, this.getFlowing(), p_76028_, p_76031_, direction, blockpos, blockstate, fluidstate)) {
               if (p_364491_.isHole(blockpos)) {
                  return p_76029_;
               }

               if (p_76029_ < this.getSlopeFindDistance(p_76027_)) {
                  int j = this.getSlopeDistance(p_76027_, blockpos, p_76029_ + 1, direction.getOpposite(), blockstate, p_364491_);
                  if (j < i) {
                     i = j;
                  }
               }
            }
         }
      }

      return i;
   }

   boolean isWaterHole(BlockGetter p_75957_, BlockPos p_75959_, BlockState p_75960_, BlockPos p_75961_, BlockState p_75962_) {
      if (!canPassThroughWall(Direction.DOWN, p_75957_, p_75959_, p_75960_, p_75961_, p_75962_)) {
         return false;
      } else {
         return p_75962_.getFluidState().getType().isSame(this) ? true : canHoldFluid(p_75957_, p_75961_, p_75962_, this.getFlowing());
      }
   }

   private boolean canPassThrough(
      BlockGetter p_75964_,
      Fluid p_75965_,
      BlockPos p_75966_,
      BlockState p_75967_,
      Direction p_75968_,
      BlockPos p_75969_,
      BlockState p_75970_,
      FluidState p_75971_
   ) {
      return this.canMaybePassThrough(p_75964_, p_75966_, p_75967_, p_75968_, p_75969_, p_75970_, p_75971_)
         && canHoldSpecificFluid(p_75964_, p_75969_, p_75970_, p_75965_);
   }

   private boolean canMaybePassThrough(
      BlockGetter p_361039_, BlockPos p_364099_, BlockState p_363055_, Direction p_361101_, BlockPos p_362307_, BlockState p_363139_, FluidState p_362538_
   ) {
      return !this.isSourceBlockOfThisType(p_362538_)
         && canHoldAnyFluid(p_363139_)
         && canPassThroughWall(p_361101_, p_361039_, p_364099_, p_363055_, p_362307_, p_363139_);
   }

   private boolean isSourceBlockOfThisType(FluidState p_76097_) {
      return p_76097_.getType().isSame(this) && p_76097_.isSource();
   }

   protected abstract int getSlopeFindDistance(LevelReader var1);

   private int sourceNeighborCount(LevelReader p_76020_, BlockPos p_76021_) {
      int i = 0;

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos blockpos = p_76021_.relative(direction);
         FluidState fluidstate = p_76020_.getFluidState(blockpos);
         if (this.isSourceBlockOfThisType(fluidstate)) {
            i++;
         }
      }

      return i;
   }

   protected Map<Direction, FluidState> getSpread(ServerLevel p_376283_, BlockPos p_76081_, BlockState p_76082_) {
      int i = 1000;
      Map<Direction, FluidState> map = Maps.newEnumMap(Direction.class);
      FlowingFluid.SpreadContext flowingfluid$spreadcontext = null;

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos blockpos = p_76081_.relative(direction);
         BlockState blockstate = p_376283_.getBlockState(blockpos);
         FluidState fluidstate = blockstate.getFluidState();
         if (this.canMaybePassThrough(p_376283_, p_76081_, p_76082_, direction, blockpos, blockstate, fluidstate)) {
            FluidState fluidstate1 = this.getNewLiquid(p_376283_, blockpos, blockstate);
            if (canHoldSpecificFluid(p_376283_, blockpos, blockstate, fluidstate1.getType())) {
               if (flowingfluid$spreadcontext == null) {
                  flowingfluid$spreadcontext = new FlowingFluid.SpreadContext(p_376283_, p_76081_);
               }

               int j;
               if (flowingfluid$spreadcontext.isHole(blockpos)) {
                  j = 0;
               } else {
                  j = this.getSlopeDistance(p_376283_, blockpos, 1, direction.getOpposite(), blockstate, flowingfluid$spreadcontext);
               }

               if (j < i) {
                  map.clear();
               }

               if (j <= i) {
                  if (fluidstate.canBeReplacedWith(p_376283_, blockpos, fluidstate1.getType(), direction)) {
                     map.put(direction, fluidstate1);
                  }

                  i = j;
               }
            }
         }
      }

      return map;
   }

   private static boolean canHoldAnyFluid(BlockState p_362073_) {
      Block block = p_362073_.getBlock();
      if (block instanceof LiquidBlockContainer) {
         return true;
      } else {
         return p_362073_.blocksMotion()
            ? false
            : !(block instanceof DoorBlock)
               && !p_362073_.is(BlockTags.SIGNS)
               && !p_362073_.is(Blocks.LADDER)
               && !p_362073_.is(Blocks.SUGAR_CANE)
               && !p_362073_.is(Blocks.BUBBLE_COLUMN)
               && !p_362073_.is(Blocks.NETHER_PORTAL)
               && !p_362073_.is(Blocks.END_PORTAL)
               && !p_362073_.is(Blocks.END_GATEWAY)
               && !p_362073_.is(Blocks.STRUCTURE_VOID);
      }
   }

   private static boolean canHoldFluid(BlockGetter p_75973_, BlockPos p_75974_, BlockState p_75975_, Fluid p_75976_) {
      return canHoldAnyFluid(p_75975_) && canHoldSpecificFluid(p_75973_, p_75974_, p_75975_, p_75976_);
   }

   private static boolean canHoldSpecificFluid(BlockGetter p_361904_, BlockPos p_360557_, BlockState p_365254_, Fluid p_365308_) {
      return p_365254_.getBlock() instanceof LiquidBlockContainer liquidblockcontainer
         ? liquidblockcontainer.canPlaceLiquid(null, p_361904_, p_360557_, p_365254_, p_365308_)
         : true;
   }

   protected abstract int getDropOff(LevelReader var1);

   protected int getSpreadDelay(Level p_75998_, BlockPos p_75999_, FluidState p_76000_, FluidState p_76001_) {
      return this.getTickDelay(p_75998_);
   }

   @Override
   public void tick(ServerLevel p_376710_, BlockPos p_75996_, BlockState p_360412_, FluidState p_75997_) {
      if (!p_75997_.isSource()) {
         FluidState fluidstate = this.getNewLiquid(p_376710_, p_75996_, p_376710_.getBlockState(p_75996_));
         int i = this.getSpreadDelay(p_376710_, p_75996_, p_75997_, fluidstate);
         if (fluidstate.isEmpty()) {
            p_75997_ = fluidstate;
            p_360412_ = Blocks.AIR.defaultBlockState();
            p_376710_.setBlock(p_75996_, p_360412_, 3);
         } else if (fluidstate != p_75997_) {
            p_75997_ = fluidstate;
            p_360412_ = fluidstate.createLegacyBlock();
            p_376710_.setBlock(p_75996_, p_360412_, 3);
            p_376710_.scheduleTick(p_75996_, fluidstate.getType(), i);
         }
      }

      this.spread(p_376710_, p_75996_, p_360412_, p_75997_);
   }

   protected static int getLegacyLevel(FluidState p_76093_) {
      return p_76093_.isSource() ? 0 : 8 - Math.min(p_76093_.getAmount(), 8) + (p_76093_.getValue(FALLING) ? 8 : 0);
   }

   private static boolean hasSameAbove(FluidState p_76089_, BlockGetter p_76090_, BlockPos p_76091_) {
      return p_76089_.getType().isSame(p_76090_.getFluidState(p_76091_.above()).getType());
   }

   @Override
   public float getHeight(FluidState p_76050_, BlockGetter p_76051_, BlockPos p_76052_) {
      return hasSameAbove(p_76050_, p_76051_, p_76052_) ? 1.0F : p_76050_.getOwnHeight();
   }

   @Override
   public float getOwnHeight(FluidState p_76048_) {
      return p_76048_.getAmount() / 9.0F;
   }

   @Override
   public abstract int getAmount(FluidState var1);

   @Override
   public VoxelShape getShape(FluidState p_76084_, BlockGetter p_76085_, BlockPos p_76086_) {
      return p_76084_.getAmount() == 9 && hasSameAbove(p_76084_, p_76085_, p_76086_)
         ? Shapes.block()
         : this.shapes.computeIfAbsent(p_76084_, p_76073_ -> Shapes.box(0.0, 0.0, 0.0, 1.0, p_76073_.getHeight(p_76085_, p_76086_), 1.0));
   }

   record BlockStatePairKey(BlockState first, BlockState second, Direction direction) {
      @Override
      public boolean equals(Object p_368747_) {
         return p_368747_ instanceof FlowingFluid.BlockStatePairKey flowingfluid$blockstatepairkey
            && this.first == flowingfluid$blockstatepairkey.first
            && this.second == flowingfluid$blockstatepairkey.second
            && this.direction == flowingfluid$blockstatepairkey.direction;
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

      SpreadContext(BlockGetter p_364617_, BlockPos p_361136_) {
         this.level = p_364617_;
         this.origin = p_361136_;
      }

      public BlockState getBlockState(BlockPos p_361206_) {
         return this.getBlockState(p_361206_, this.getCacheKey(p_361206_));
      }

      private BlockState getBlockState(BlockPos p_360376_, short p_364307_) {
         return (BlockState)this.stateCache.computeIfAbsent(p_364307_, p_361149_ -> this.level.getBlockState(p_360376_));
      }

      public boolean isHole(BlockPos p_362369_) {
         return this.holeCache.computeIfAbsent(this.getCacheKey(p_362369_), p_363178_ -> {
            BlockState blockstate = this.getBlockState(p_362369_, p_363178_);
            BlockPos blockpos = p_362369_.below();
            BlockState blockstate1 = this.level.getBlockState(blockpos);
            return FlowingFluid.this.isWaterHole(this.level, p_362369_, blockstate, blockpos, blockstate1);
         });
      }

      private short getCacheKey(BlockPos p_362251_) {
         int i = p_362251_.getX() - this.origin.getX();
         int j = p_362251_.getZ() - this.origin.getZ();
         return (short)((i + 128 & 0xFF) << 8 | j + 128 & 0xFF);
      }
   }
}
