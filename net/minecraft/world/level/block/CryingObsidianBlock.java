package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class CryingObsidianBlock extends Block {
   public static final MapCodec<CryingObsidianBlock> CODEC = simpleCodec(CryingObsidianBlock::new);

   @Override
   public MapCodec<CryingObsidianBlock> codec() {
      return CODEC;
   }

   public CryingObsidianBlock(BlockBehaviour.Properties p_52371_) {
      super(p_52371_);
   }

   @Override
   public void animateTick(BlockState p_221055_, Level p_221056_, BlockPos p_221057_, RandomSource p_221058_) {
      if (p_221058_.nextInt(5) == 0) {
         Direction $$4 = Direction.getRandom(p_221058_);
         if ($$4 != Direction.UP) {
            BlockPos $$5 = p_221057_.relative($$4);
            BlockState $$6 = p_221056_.getBlockState($$5);
            if (!p_221055_.canOcclude() || !$$6.isFaceSturdy(p_221056_, $$5, $$4.getOpposite())) {
               double $$7 = $$4.getStepX() == 0 ? p_221058_.nextDouble() : 0.5 + $$4.getStepX() * 0.6;
               double $$8 = $$4.getStepY() == 0 ? p_221058_.nextDouble() : 0.5 + $$4.getStepY() * 0.6;
               double $$9 = $$4.getStepZ() == 0 ? p_221058_.nextDouble() : 0.5 + $$4.getStepZ() * 0.6;
               p_221056_.addParticle(
                  ParticleTypes.DRIPPING_OBSIDIAN_TEAR, p_221057_.getX() + $$7, p_221057_.getY() + $$8, p_221057_.getZ() + $$9, 0.0, 0.0, 0.0
               );
            }
         }
      }
   }
}
