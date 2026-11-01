package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ScaffoldingBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<ScaffoldingBlock> CODEC = simpleCodec(ScaffoldingBlock::new);
   private static final int TICK_DELAY = 1;
   private static final VoxelShape STABLE_SHAPE;
   private static final VoxelShape UNSTABLE_SHAPE;
   private static final VoxelShape UNSTABLE_SHAPE_BOTTOM = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
   private static final VoxelShape BELOW_BLOCK = Shapes.block().move(0.0, -1.0, 0.0);
   public static final int STABILITY_MAX_DISTANCE = 7;
   public static final IntegerProperty DISTANCE = BlockStateProperties.STABILITY_DISTANCE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;

   @Override
   public MapCodec<ScaffoldingBlock> codec() {
      return CODEC;
   }

   public ScaffoldingBlock(BlockBehaviour.Properties p_56021_) {
      super(p_56021_);
      this.registerDefaultState(this.stateDefinition.any().setValue(DISTANCE, 7).setValue(WATERLOGGED, false).setValue(BOTTOM, false));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_56051_) {
      p_56051_.add(DISTANCE, WATERLOGGED, BOTTOM);
   }

   @Override
   protected VoxelShape getShape(BlockState p_56057_, BlockGetter p_56058_, BlockPos p_56059_, CollisionContext p_56060_) {
      if (!p_56060_.isHoldingItem(p_56057_.getBlock().asItem())) {
         return p_56057_.getValue(BOTTOM) ? UNSTABLE_SHAPE : STABLE_SHAPE;
      } else {
         return Shapes.block();
      }
   }

   @Override
   protected VoxelShape getInteractionShape(BlockState p_56053_, BlockGetter p_56054_, BlockPos p_56055_) {
      return Shapes.block();
   }

   @Override
   protected boolean canBeReplaced(BlockState p_56037_, BlockPlaceContext p_56038_) {
      return p_56038_.getItemInHand().is(this.asItem());
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_56023_) {
      BlockPos $$1 = p_56023_.getClickedPos();
      Level $$2 = p_56023_.getLevel();
      int $$3 = getDistance($$2, $$1);
      return this.defaultBlockState()
         .setValue(WATERLOGGED, $$2.getFluidState($$1).getType() == Fluids.WATER)
         .setValue(DISTANCE, $$3)
         .setValue(BOTTOM, this.isBottom($$2, $$1, $$3));
   }

   @Override
   protected void onPlace(BlockState p_56062_, Level p_56063_, BlockPos p_56064_, BlockState p_56065_, boolean p_56066_) {
      if (!p_56063_.isClientSide) {
         p_56063_.scheduleTick(p_56064_, this, 1);
      }
   }

   @Override
   protected BlockState updateShape(BlockState p_56044_, Direction p_56045_, BlockState p_56046_, LevelAccessor p_56047_, BlockPos p_56048_, BlockPos p_56049_) {
      if (p_56044_.getValue(WATERLOGGED)) {
         p_56047_.scheduleTick(p_56048_, Fluids.WATER, Fluids.WATER.getTickDelay(p_56047_));
      }

      if (!p_56047_.isClientSide()) {
         p_56047_.scheduleTick(p_56048_, this, 1);
      }

      return p_56044_;
   }

   @Override
   protected void tick(BlockState p_222019_, ServerLevel p_222020_, BlockPos p_222021_, RandomSource p_222022_) {
      int $$4 = getDistance(p_222020_, p_222021_);
      BlockState $$5 = p_222019_.setValue(DISTANCE, $$4).setValue(BOTTOM, this.isBottom(p_222020_, p_222021_, $$4));
      if ($$5.getValue(DISTANCE) == 7) {
         if (p_222019_.getValue(DISTANCE) == 7) {
            FallingBlockEntity.fall(p_222020_, p_222021_, $$5);
         } else {
            p_222020_.destroyBlock(p_222021_, true);
         }
      } else if (p_222019_ != $$5) {
         p_222020_.setBlock(p_222021_, $$5, 3);
      }
   }

   @Override
   protected boolean canSurvive(BlockState p_56040_, LevelReader p_56041_, BlockPos p_56042_) {
      return getDistance(p_56041_, p_56042_) < 7;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState p_56068_, BlockGetter p_56069_, BlockPos p_56070_, CollisionContext p_56071_) {
      if (p_56071_.isAbove(Shapes.block(), p_56070_, true) && !p_56071_.isDescending()) {
         return STABLE_SHAPE;
      } else {
         return p_56068_.getValue(DISTANCE) != 0 && p_56068_.getValue(BOTTOM) && p_56071_.isAbove(BELOW_BLOCK, p_56070_, true)
            ? UNSTABLE_SHAPE_BOTTOM
            : Shapes.empty();
      }
   }

   @Override
   protected FluidState getFluidState(BlockState p_56073_) {
      return p_56073_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_56073_);
   }

   private boolean isBottom(BlockGetter p_56028_, BlockPos p_56029_, int p_56030_) {
      return p_56030_ > 0 && !p_56028_.getBlockState(p_56029_.below()).is(this);
   }

   public static int getDistance(BlockGetter p_56025_, BlockPos p_56026_) {
      BlockPos.MutableBlockPos $$2 = p_56026_.mutable().move(Direction.DOWN);
      BlockState $$3 = p_56025_.getBlockState($$2);
      int $$4 = 7;
      if ($$3.is(Blocks.SCAFFOLDING)) {
         $$4 = $$3.getValue(DISTANCE);
      } else if ($$3.isFaceSturdy(p_56025_, $$2, Direction.UP)) {
         return 0;
      }

      for (Direction $$5 : Direction.Plane.HORIZONTAL) {
         BlockState $$6 = p_56025_.getBlockState($$2.setWithOffset(p_56026_, $$5));
         if ($$6.is(Blocks.SCAFFOLDING)) {
            $$4 = Math.min($$4, $$6.getValue(DISTANCE) + 1);
            if ($$4 == 1) {
               break;
            }
         }
      }

      return $$4;
   }

   static {
      VoxelShape $$0 = Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0);
      VoxelShape $$1 = Block.box(0.0, 0.0, 0.0, 2.0, 16.0, 2.0);
      VoxelShape $$2 = Block.box(14.0, 0.0, 0.0, 16.0, 16.0, 2.0);
      VoxelShape $$3 = Block.box(0.0, 0.0, 14.0, 2.0, 16.0, 16.0);
      VoxelShape $$4 = Block.box(14.0, 0.0, 14.0, 16.0, 16.0, 16.0);
      STABLE_SHAPE = Shapes.or($$0, $$1, $$2, $$3, $$4);
      VoxelShape $$5 = Block.box(0.0, 0.0, 0.0, 2.0, 2.0, 16.0);
      VoxelShape $$6 = Block.box(14.0, 0.0, 0.0, 16.0, 2.0, 16.0);
      VoxelShape $$7 = Block.box(0.0, 0.0, 14.0, 16.0, 2.0, 16.0);
      VoxelShape $$8 = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 2.0);
      UNSTABLE_SHAPE = Shapes.or(ScaffoldingBlock.UNSTABLE_SHAPE_BOTTOM, STABLE_SHAPE, $$6, $$5, $$8, $$7);
   }
}
