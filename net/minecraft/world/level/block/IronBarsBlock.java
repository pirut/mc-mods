package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class IronBarsBlock extends CrossCollisionBlock {
   public static final MapCodec<IronBarsBlock> CODEC = simpleCodec(IronBarsBlock::new);

   @Override
   public MapCodec<? extends IronBarsBlock> codec() {
      return CODEC;
   }

   public IronBarsBlock(BlockBehaviour.Properties p_54198_) {
      super(1.0F, 1.0F, 16.0F, 16.0F, 16.0F, p_54198_);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false)
      );
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_54200_) {
      BlockGetter $$1 = p_54200_.getLevel();
      BlockPos $$2 = p_54200_.getClickedPos();
      FluidState $$3 = p_54200_.getLevel().getFluidState(p_54200_.getClickedPos());
      BlockPos $$4 = $$2.north();
      BlockPos $$5 = $$2.south();
      BlockPos $$6 = $$2.west();
      BlockPos $$7 = $$2.east();
      BlockState $$8 = $$1.getBlockState($$4);
      BlockState $$9 = $$1.getBlockState($$5);
      BlockState $$10 = $$1.getBlockState($$6);
      BlockState $$11 = $$1.getBlockState($$7);
      return this.defaultBlockState()
         .setValue(NORTH, this.attachsTo($$8, $$8.isFaceSturdy($$1, $$4, Direction.SOUTH)))
         .setValue(SOUTH, this.attachsTo($$9, $$9.isFaceSturdy($$1, $$5, Direction.NORTH)))
         .setValue(WEST, this.attachsTo($$10, $$10.isFaceSturdy($$1, $$6, Direction.EAST)))
         .setValue(EAST, this.attachsTo($$11, $$11.isFaceSturdy($$1, $$7, Direction.WEST)))
         .setValue(WATERLOGGED, $$3.getType() == Fluids.WATER);
   }

   @Override
   protected BlockState updateShape(BlockState p_54211_, Direction p_54212_, BlockState p_54213_, LevelAccessor p_54214_, BlockPos p_54215_, BlockPos p_54216_) {
      if (p_54211_.getValue(WATERLOGGED)) {
         p_54214_.scheduleTick(p_54215_, Fluids.WATER, Fluids.WATER.getTickDelay(p_54214_));
      }

      return p_54212_.getAxis().isHorizontal()
         ? p_54211_.setValue(PROPERTY_BY_DIRECTION.get(p_54212_), this.attachsTo(p_54213_, p_54213_.isFaceSturdy(p_54214_, p_54216_, p_54212_.getOpposite())))
         : super.updateShape(p_54211_, p_54212_, p_54213_, p_54214_, p_54215_, p_54216_);
   }

   @Override
   protected VoxelShape getVisualShape(BlockState p_54202_, BlockGetter p_54203_, BlockPos p_54204_, CollisionContext p_54205_) {
      return Shapes.empty();
   }

   @Override
   protected boolean skipRendering(BlockState p_54207_, BlockState p_54208_, Direction p_54209_) {
      if (p_54208_.is(this)) {
         if (!p_54209_.getAxis().isHorizontal()) {
            return true;
         }

         if (p_54207_.getValue(PROPERTY_BY_DIRECTION.get(p_54209_)) && p_54208_.getValue(PROPERTY_BY_DIRECTION.get(p_54209_.getOpposite()))) {
            return true;
         }
      }

      return super.skipRendering(p_54207_, p_54208_, p_54209_);
   }

   public final boolean attachsTo(BlockState p_54218_, boolean p_54219_) {
      return !isExceptionForConnection(p_54218_) && p_54219_ || p_54218_.getBlock() instanceof IronBarsBlock || p_54218_.is(BlockTags.WALLS);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54221_) {
      p_54221_.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }
}
