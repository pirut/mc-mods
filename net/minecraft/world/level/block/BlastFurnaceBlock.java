package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlastFurnaceBlock extends AbstractFurnaceBlock {
   public static final MapCodec<BlastFurnaceBlock> CODEC = simpleCodec(BlastFurnaceBlock::new);

   @Override
   public MapCodec<BlastFurnaceBlock> codec() {
      return CODEC;
   }

   public BlastFurnaceBlock(BlockBehaviour.Properties p_49773_) {
      super(p_49773_);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_152386_, BlockState p_152387_) {
      return new BlastFurnaceBlockEntity(p_152386_, p_152387_);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_152382_, BlockState p_152383_, BlockEntityType<T> p_152384_) {
      return createFurnaceTicker(p_152382_, p_152384_, BlockEntityType.BLAST_FURNACE);
   }

   @Override
   protected void openContainer(Level p_49777_, BlockPos p_49778_, Player p_49779_) {
      BlockEntity $$3 = p_49777_.getBlockEntity(p_49778_);
      if ($$3 instanceof BlastFurnaceBlockEntity) {
         p_49779_.openMenu((MenuProvider)$$3);
         p_49779_.awardStat(Stats.INTERACT_WITH_BLAST_FURNACE);
      }
   }

   @Override
   public void animateTick(BlockState p_220818_, Level p_220819_, BlockPos p_220820_, RandomSource p_220821_) {
      if (p_220818_.getValue(LIT)) {
         double $$4 = p_220820_.getX() + 0.5;
         double $$5 = p_220820_.getY();
         double $$6 = p_220820_.getZ() + 0.5;
         if (p_220821_.nextDouble() < 0.1) {
            p_220819_.playLocalSound($$4, $$5, $$6, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         Direction $$7 = p_220818_.getValue(FACING);
         Direction.Axis $$8 = $$7.getAxis();
         double $$9 = 0.52;
         double $$10 = p_220821_.nextDouble() * 0.6 - 0.3;
         double $$11 = $$8 == Direction.Axis.X ? $$7.getStepX() * 0.52 : $$10;
         double $$12 = p_220821_.nextDouble() * 9.0 / 16.0;
         double $$13 = $$8 == Direction.Axis.Z ? $$7.getStepZ() * 0.52 : $$10;
         p_220819_.addParticle(ParticleTypes.SMOKE, $$4 + $$11, $$5 + $$12, $$6 + $$13, 0.0, 0.0, 0.0);
      }
   }
}
