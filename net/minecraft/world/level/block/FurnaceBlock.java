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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class FurnaceBlock extends AbstractFurnaceBlock {
   public static final MapCodec<FurnaceBlock> CODEC = simpleCodec(FurnaceBlock::new);

   @Override
   public MapCodec<FurnaceBlock> codec() {
      return CODEC;
   }

   public FurnaceBlock(BlockBehaviour.Properties p_53627_) {
      super(p_53627_);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_153277_, BlockState p_153278_) {
      return new FurnaceBlockEntity(p_153277_, p_153278_);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_153273_, BlockState p_153274_, BlockEntityType<T> p_153275_) {
      return createFurnaceTicker(p_153273_, p_153275_, BlockEntityType.FURNACE);
   }

   @Override
   protected void openContainer(Level p_53631_, BlockPos p_53632_, Player p_53633_) {
      BlockEntity $$3 = p_53631_.getBlockEntity(p_53632_);
      if ($$3 instanceof FurnaceBlockEntity) {
         p_53633_.openMenu((MenuProvider)$$3);
         p_53633_.awardStat(Stats.INTERACT_WITH_FURNACE);
      }
   }

   @Override
   public void animateTick(BlockState p_221253_, Level p_221254_, BlockPos p_221255_, RandomSource p_221256_) {
      if (p_221253_.getValue(LIT)) {
         double $$4 = p_221255_.getX() + 0.5;
         double $$5 = p_221255_.getY();
         double $$6 = p_221255_.getZ() + 0.5;
         if (p_221256_.nextDouble() < 0.1) {
            p_221254_.playLocalSound($$4, $$5, $$6, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         Direction $$7 = p_221253_.getValue(FACING);
         Direction.Axis $$8 = $$7.getAxis();
         double $$9 = 0.52;
         double $$10 = p_221256_.nextDouble() * 0.6 - 0.3;
         double $$11 = $$8 == Direction.Axis.X ? $$7.getStepX() * 0.52 : $$10;
         double $$12 = p_221256_.nextDouble() * 6.0 / 16.0;
         double $$13 = $$8 == Direction.Axis.Z ? $$7.getStepZ() * 0.52 : $$10;
         p_221254_.addParticle(ParticleTypes.SMOKE, $$4 + $$11, $$5 + $$12, $$6 + $$13, 0.0, 0.0, 0.0);
         p_221254_.addParticle(ParticleTypes.FLAME, $$4 + $$11, $$5 + $$12, $$6 + $$13, 0.0, 0.0, 0.0);
      }
   }
}
