package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RedstoneWallTorchBlock extends RedstoneTorchBlock {
   public static final MapCodec<RedstoneWallTorchBlock> CODEC = simpleCodec(RedstoneWallTorchBlock::new);
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

   @Override
   public MapCodec<RedstoneWallTorchBlock> codec() {
      return CODEC;
   }

   public RedstoneWallTorchBlock(BlockBehaviour.Properties p_55744_) {
      super(p_55744_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, true));
   }

   @Override
   public String getDescriptionId() {
      return this.asItem().getDescriptionId();
   }

   @Override
   protected VoxelShape getShape(BlockState p_55781_, BlockGetter p_55782_, BlockPos p_55783_, CollisionContext p_55784_) {
      return WallTorchBlock.getShape(p_55781_);
   }

   @Override
   protected boolean canSurvive(BlockState p_55762_, LevelReader p_55763_, BlockPos p_55764_) {
      return WallTorchBlock.canSurvive(p_55763_, p_55764_, p_55762_.getValue(FACING));
   }

   @Override
   protected BlockState updateShape(BlockState p_55772_, Direction p_55773_, BlockState p_55774_, LevelAccessor p_55775_, BlockPos p_55776_, BlockPos p_55777_) {
      return p_55773_.getOpposite() == p_55772_.getValue(FACING) && !p_55772_.canSurvive(p_55775_, p_55776_) ? Blocks.AIR.defaultBlockState() : p_55772_;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_55746_) {
      BlockState $$1 = Blocks.WALL_TORCH.getStateForPlacement(p_55746_);
      return $$1 == null ? null : this.defaultBlockState().setValue(FACING, $$1.getValue(FACING));
   }

   @Override
   public void animateTick(BlockState p_221959_, Level p_221960_, BlockPos p_221961_, RandomSource p_221962_) {
      if (p_221959_.getValue(LIT)) {
         Direction $$4 = p_221959_.getValue(FACING).getOpposite();
         double $$5 = 0.27;
         double $$6 = p_221961_.getX() + 0.5 + (p_221962_.nextDouble() - 0.5) * 0.2 + 0.27 * $$4.getStepX();
         double $$7 = p_221961_.getY() + 0.7 + (p_221962_.nextDouble() - 0.5) * 0.2 + 0.22;
         double $$8 = p_221961_.getZ() + 0.5 + (p_221962_.nextDouble() - 0.5) * 0.2 + 0.27 * $$4.getStepZ();
         p_221960_.addParticle(DustParticleOptions.REDSTONE, $$6, $$7, $$8, 0.0, 0.0, 0.0);
      }
   }

   @Override
   protected boolean hasNeighborSignal(Level p_55748_, BlockPos p_55749_, BlockState p_55750_) {
      Direction $$3 = p_55750_.getValue(FACING).getOpposite();
      return p_55748_.hasSignal(p_55749_.relative($$3), $$3);
   }

   @Override
   protected int getSignal(BlockState p_55752_, BlockGetter p_55753_, BlockPos p_55754_, Direction p_55755_) {
      return p_55752_.getValue(LIT) && p_55752_.getValue(FACING) != p_55755_ ? 15 : 0;
   }

   @Override
   protected BlockState rotate(BlockState p_55769_, Rotation p_55770_) {
      return p_55769_.setValue(FACING, p_55770_.rotate(p_55769_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_55766_, Mirror p_55767_) {
      return p_55766_.rotate(p_55767_.getRotation(p_55766_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_55779_) {
      p_55779_.add(FACING, LIT);
   }
}
