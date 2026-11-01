package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class RepeaterBlock extends DiodeBlock {
   public static final MapCodec<RepeaterBlock> CODEC = simpleCodec(RepeaterBlock::new);
   public static final BooleanProperty LOCKED = BlockStateProperties.LOCKED;
   public static final IntegerProperty DELAY = BlockStateProperties.DELAY;

   @Override
   public MapCodec<RepeaterBlock> codec() {
      return CODEC;
   }

   public RepeaterBlock(BlockBehaviour.Properties p_55801_) {
      super(p_55801_);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DELAY, 1).setValue(LOCKED, false).setValue(POWERED, false)
      );
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_55809_, Level p_55810_, BlockPos p_55811_, Player p_55812_, BlockHitResult p_55814_) {
      if (!p_55812_.getAbilities().mayBuild) {
         return InteractionResult.PASS;
      } else {
         p_55810_.setBlock(p_55811_, p_55809_.cycle(DELAY), 3);
         return InteractionResult.sidedSuccess(p_55810_.isClientSide);
      }
   }

   @Override
   protected int getDelay(BlockState p_55830_) {
      return p_55830_.getValue(DELAY) * 2;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_55803_) {
      BlockState $$1 = super.getStateForPlacement(p_55803_);
      return $$1.setValue(LOCKED, this.isLocked(p_55803_.getLevel(), p_55803_.getClickedPos(), $$1));
   }

   @Override
   protected BlockState updateShape(BlockState p_55821_, Direction p_55822_, BlockState p_55823_, LevelAccessor p_55824_, BlockPos p_55825_, BlockPos p_55826_) {
      if (p_55822_ == Direction.DOWN && !this.canSurviveOn(p_55824_, p_55826_, p_55823_)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         return !p_55824_.isClientSide() && p_55822_.getAxis() != p_55821_.getValue(FACING).getAxis()
            ? p_55821_.setValue(LOCKED, this.isLocked(p_55824_, p_55825_, p_55821_))
            : super.updateShape(p_55821_, p_55822_, p_55823_, p_55824_, p_55825_, p_55826_);
      }
   }

   @Override
   public boolean isLocked(LevelReader p_55805_, BlockPos p_55806_, BlockState p_55807_) {
      return this.getAlternateSignal(p_55805_, p_55806_, p_55807_) > 0;
   }

   @Override
   protected boolean sideInputDiodesOnly() {
      return true;
   }

   @Override
   public void animateTick(BlockState p_221964_, Level p_221965_, BlockPos p_221966_, RandomSource p_221967_) {
      if (p_221964_.getValue(POWERED)) {
         Direction $$4 = p_221964_.getValue(FACING);
         double $$5 = p_221966_.getX() + 0.5 + (p_221967_.nextDouble() - 0.5) * 0.2;
         double $$6 = p_221966_.getY() + 0.4 + (p_221967_.nextDouble() - 0.5) * 0.2;
         double $$7 = p_221966_.getZ() + 0.5 + (p_221967_.nextDouble() - 0.5) * 0.2;
         float $$8 = -5.0F;
         if (p_221967_.nextBoolean()) {
            $$8 = p_221964_.getValue(DELAY) * 2 - 1;
         }

         $$8 /= 16.0F;
         double $$9 = $$8 * $$4.getStepX();
         double $$10 = $$8 * $$4.getStepZ();
         p_221965_.addParticle(DustParticleOptions.REDSTONE, $$5 + $$9, $$6, $$7 + $$10, 0.0, 0.0, 0.0);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_55828_) {
      p_55828_.add(FACING, DELAY, LOCKED, POWERED);
   }
}
