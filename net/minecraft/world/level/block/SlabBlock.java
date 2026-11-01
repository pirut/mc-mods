package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SlabBlock extends Block implements SimpleWaterloggedBlock {
   public static final MapCodec<SlabBlock> CODEC = simpleCodec(SlabBlock::new);
   public static final EnumProperty<SlabType> TYPE = BlockStateProperties.SLAB_TYPE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   protected static final VoxelShape BOTTOM_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
   protected static final VoxelShape TOP_AABB = Block.box(0.0, 8.0, 0.0, 16.0, 16.0, 16.0);

   @Override
   public MapCodec<? extends SlabBlock> codec() {
      return CODEC;
   }

   public SlabBlock(BlockBehaviour.Properties p_56359_) {
      super(p_56359_);
      this.registerDefaultState(this.defaultBlockState().setValue(TYPE, SlabType.BOTTOM).setValue(WATERLOGGED, false));
   }

   @Override
   protected boolean useShapeForLightOcclusion(BlockState p_56395_) {
      return p_56395_.getValue(TYPE) != SlabType.DOUBLE;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_56388_) {
      p_56388_.add(TYPE, WATERLOGGED);
   }

   @Override
   protected VoxelShape getShape(BlockState p_56390_, BlockGetter p_56391_, BlockPos p_56392_, CollisionContext p_56393_) {
      SlabType $$4 = p_56390_.getValue(TYPE);
      switch ($$4) {
         case DOUBLE:
            return Shapes.block();
         case TOP:
            return TOP_AABB;
         default:
            return BOTTOM_AABB;
      }
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_56361_) {
      BlockPos $$1 = p_56361_.getClickedPos();
      BlockState $$2 = p_56361_.getLevel().getBlockState($$1);
      if ($$2.is(this)) {
         return $$2.setValue(TYPE, SlabType.DOUBLE).setValue(WATERLOGGED, false);
      } else {
         FluidState $$3 = p_56361_.getLevel().getFluidState($$1);
         BlockState $$4 = this.defaultBlockState().setValue(TYPE, SlabType.BOTTOM).setValue(WATERLOGGED, $$3.getType() == Fluids.WATER);
         Direction $$5 = p_56361_.getClickedFace();
         return $$5 != Direction.DOWN && ($$5 == Direction.UP || !(p_56361_.getClickLocation().y - $$1.getY() > 0.5)) ? $$4 : $$4.setValue(TYPE, SlabType.TOP);
      }
   }

   @Override
   protected boolean canBeReplaced(BlockState p_56373_, BlockPlaceContext p_56374_) {
      ItemStack $$2 = p_56374_.getItemInHand();
      SlabType $$3 = p_56373_.getValue(TYPE);
      if ($$3 == SlabType.DOUBLE || !$$2.is(this.asItem())) {
         return false;
      } else if (p_56374_.replacingClickedOnBlock()) {
         boolean $$4 = p_56374_.getClickLocation().y - p_56374_.getClickedPos().getY() > 0.5;
         Direction $$5 = p_56374_.getClickedFace();
         return $$3 == SlabType.BOTTOM
            ? $$5 == Direction.UP || $$4 && $$5.getAxis().isHorizontal()
            : $$5 == Direction.DOWN || !$$4 && $$5.getAxis().isHorizontal();
      } else {
         return true;
      }
   }

   @Override
   protected FluidState getFluidState(BlockState p_56397_) {
      return p_56397_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_56397_);
   }

   @Override
   public boolean placeLiquid(LevelAccessor p_56368_, BlockPos p_56369_, BlockState p_56370_, FluidState p_56371_) {
      return p_56370_.getValue(TYPE) != SlabType.DOUBLE ? SimpleWaterloggedBlock.super.placeLiquid(p_56368_, p_56369_, p_56370_, p_56371_) : false;
   }

   @Override
   public boolean canPlaceLiquid(@Nullable Player p_294165_, BlockGetter p_56363_, BlockPos p_56364_, BlockState p_56365_, Fluid p_56366_) {
      return p_56365_.getValue(TYPE) != SlabType.DOUBLE
         ? SimpleWaterloggedBlock.super.canPlaceLiquid(p_294165_, p_56363_, p_56364_, p_56365_, p_56366_)
         : false;
   }

   @Override
   protected BlockState updateShape(BlockState p_56381_, Direction p_56382_, BlockState p_56383_, LevelAccessor p_56384_, BlockPos p_56385_, BlockPos p_56386_) {
      if (p_56381_.getValue(WATERLOGGED)) {
         p_56384_.scheduleTick(p_56385_, Fluids.WATER, Fluids.WATER.getTickDelay(p_56384_));
      }

      return super.updateShape(p_56381_, p_56382_, p_56383_, p_56384_, p_56385_, p_56386_);
   }

   @Override
   protected boolean isPathfindable(BlockState p_56376_, PathComputationType p_56379_) {
      switch (p_56379_) {
         case LAND:
            return false;
         case WATER:
            return p_56376_.getFluidState().is(FluidTags.WATER);
         case AIR:
            return false;
         default:
            return false;
      }
   }
}
