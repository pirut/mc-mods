package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WetSpongeBlock extends Block {
   public static final MapCodec<WetSpongeBlock> CODEC = simpleCodec(WetSpongeBlock::new);

   @Override
   public MapCodec<WetSpongeBlock> codec() {
      return CODEC;
   }

   public WetSpongeBlock(BlockBehaviour.Properties p_58222_) {
      super(p_58222_);
   }

   @Override
   protected void onPlace(BlockState p_58229_, Level p_58230_, BlockPos p_58231_, BlockState p_58232_, boolean p_58233_) {
      if (p_58230_.dimensionType().ultraWarm()) {
         p_58230_.setBlock(p_58231_, Blocks.SPONGE.defaultBlockState(), 3);
         p_58230_.levelEvent(2009, p_58231_, 0);
         p_58230_.playSound(null, p_58231_, SoundEvents.WET_SPONGE_DRIES, SoundSource.BLOCKS, 1.0F, (1.0F + p_58230_.getRandom().nextFloat() * 0.2F) * 0.7F);
      }
   }

   @Override
   public void animateTick(BlockState p_222682_, Level p_222683_, BlockPos p_222684_, RandomSource p_222685_) {
      Direction $$4 = Direction.getRandom(p_222685_);
      if ($$4 != Direction.UP) {
         BlockPos $$5 = p_222684_.relative($$4);
         BlockState $$6 = p_222683_.getBlockState($$5);
         if (!p_222682_.canOcclude() || !$$6.isFaceSturdy(p_222683_, $$5, $$4.getOpposite())) {
            double $$7 = p_222684_.getX();
            double $$8 = p_222684_.getY();
            double $$9 = p_222684_.getZ();
            if ($$4 == Direction.DOWN) {
               $$8 -= 0.05;
               $$7 += p_222685_.nextDouble();
               $$9 += p_222685_.nextDouble();
            } else {
               $$8 += p_222685_.nextDouble() * 0.8;
               if ($$4.getAxis() == Direction.Axis.X) {
                  $$9 += p_222685_.nextDouble();
                  if ($$4 == Direction.EAST) {
                     $$7++;
                  } else {
                     $$7 += 0.05;
                  }
               } else {
                  $$7 += p_222685_.nextDouble();
                  if ($$4 == Direction.SOUTH) {
                     $$9++;
                  } else {
                     $$9 += 0.05;
                  }
               }
            }

            p_222683_.addParticle(ParticleTypes.DRIPPING_WATER, $$7, $$8, $$9, 0.0, 0.0, 0.0);
         }
      }
   }
}
