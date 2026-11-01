package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

public class SculkBlock extends DropExperienceBlock implements SculkBehaviour {
   public static final MapCodec<SculkBlock> CODEC = simpleCodec(SculkBlock::new);

   @Override
   public MapCodec<SculkBlock> codec() {
      return CODEC;
   }

   public SculkBlock(BlockBehaviour.Properties p_222063_) {
      super(ConstantInt.of(1), p_222063_);
   }

   @Override
   public int attemptUseCharge(
      SculkSpreader.ChargeCursor p_222073_, LevelAccessor p_222074_, BlockPos p_222075_, RandomSource p_222076_, SculkSpreader p_222077_, boolean p_222078_
   ) {
      int $$6 = p_222073_.getCharge();
      if ($$6 != 0 && p_222076_.nextInt(p_222077_.chargeDecayRate()) == 0) {
         BlockPos $$7 = p_222073_.getPos();
         boolean $$8 = $$7.closerThan(p_222075_, p_222077_.noGrowthRadius());
         if (!$$8 && canPlaceGrowth(p_222074_, $$7)) {
            int $$9 = p_222077_.growthSpawnCost();
            if (p_222076_.nextInt($$9) < $$6) {
               BlockPos $$10 = $$7.above();
               BlockState $$11 = this.getRandomGrowthState(p_222074_, $$10, p_222076_, p_222077_.isWorldGeneration());
               p_222074_.setBlock($$10, $$11, 3);
               p_222074_.playSound(null, $$7, $$11.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            return Math.max(0, $$6 - $$9);
         } else {
            return p_222076_.nextInt(p_222077_.additionalDecayRate()) != 0 ? $$6 : $$6 - ($$8 ? 1 : getDecayPenalty(p_222077_, $$7, p_222075_, $$6));
         }
      } else {
         return $$6;
      }
   }

   private static int getDecayPenalty(SculkSpreader p_222080_, BlockPos p_222081_, BlockPos p_222082_, int p_222083_) {
      int $$4 = p_222080_.noGrowthRadius();
      float $$5 = Mth.square((float)Math.sqrt(p_222081_.distSqr(p_222082_)) - $$4);
      int $$6 = Mth.square(24 - $$4);
      float $$7 = Math.min(1.0F, $$5 / $$6);
      return Math.max(1, (int)(p_222083_ * $$7 * 0.5F));
   }

   private BlockState getRandomGrowthState(LevelAccessor p_222068_, BlockPos p_222069_, RandomSource p_222070_, boolean p_222071_) {
      BlockState $$4;
      if (p_222070_.nextInt(11) == 0) {
         $$4 = Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, p_222071_);
      } else {
         $$4 = Blocks.SCULK_SENSOR.defaultBlockState();
      }

      return $$4.hasProperty(BlockStateProperties.WATERLOGGED) && !p_222068_.getFluidState(p_222069_).isEmpty()
         ? $$4.setValue(BlockStateProperties.WATERLOGGED, true)
         : $$4;
   }

   private static boolean canPlaceGrowth(LevelAccessor p_222065_, BlockPos p_222066_) {
      BlockState $$2 = p_222065_.getBlockState(p_222066_.above());
      if ($$2.isAir() || $$2.is(Blocks.WATER) && $$2.getFluidState().is(Fluids.WATER)) {
         int $$3 = 0;

         for (BlockPos $$4 : BlockPos.betweenClosed(p_222066_.offset(-4, 0, -4), p_222066_.offset(4, 2, 4))) {
            BlockState $$5 = p_222065_.getBlockState($$4);
            if ($$5.is(Blocks.SCULK_SENSOR) || $$5.is(Blocks.SCULK_SHRIEKER)) {
               $$3++;
            }

            if ($$3 > 2) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean canChangeBlockStateOnSpread() {
      return false;
   }
}
