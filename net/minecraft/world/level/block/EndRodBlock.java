package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class EndRodBlock extends RodBlock {
   public static final MapCodec<EndRodBlock> CODEC = simpleCodec(EndRodBlock::new);

   @Override
   public MapCodec<EndRodBlock> codec() {
      return CODEC;
   }

   public EndRodBlock(BlockBehaviour.Properties p_53085_) {
      super(p_53085_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_53087_) {
      Direction $$1 = p_53087_.getClickedFace();
      BlockState $$2 = p_53087_.getLevel().getBlockState(p_53087_.getClickedPos().relative($$1.getOpposite()));
      return $$2.is(this) && $$2.getValue(FACING) == $$1
         ? this.defaultBlockState().setValue(FACING, $$1.getOpposite())
         : this.defaultBlockState().setValue(FACING, $$1);
   }

   @Override
   public void animateTick(BlockState p_221107_, Level p_221108_, BlockPos p_221109_, RandomSource p_221110_) {
      Direction $$4 = p_221107_.getValue(FACING);
      double $$5 = p_221109_.getX() + 0.55 - p_221110_.nextFloat() * 0.1F;
      double $$6 = p_221109_.getY() + 0.55 - p_221110_.nextFloat() * 0.1F;
      double $$7 = p_221109_.getZ() + 0.55 - p_221110_.nextFloat() * 0.1F;
      double $$8 = 0.4F - (p_221110_.nextFloat() + p_221110_.nextFloat()) * 0.4F;
      if (p_221110_.nextInt(5) == 0) {
         p_221108_.addParticle(
            ParticleTypes.END_ROD,
            $$5 + $$4.getStepX() * $$8,
            $$6 + $$4.getStepY() * $$8,
            $$7 + $$4.getStepZ() * $$8,
            p_221110_.nextGaussian() * 0.005,
            p_221110_.nextGaussian() * 0.005,
            p_221110_.nextGaussian() * 0.005
         );
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_53105_) {
      p_53105_.add(FACING);
   }
}
