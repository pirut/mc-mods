package net.minecraft.world.entity.animal;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;

public abstract class WaterAnimal extends PathfinderMob {
   protected WaterAnimal(EntityType<? extends WaterAnimal> p_30341_, Level p_30342_) {
      super(p_30341_, p_30342_);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader p_30348_) {
      return p_30348_.isUnobstructed(this);
   }

   @Override
   public int getAmbientSoundInterval() {
      return 120;
   }

   @Override
   protected int getBaseExperienceReward() {
      return 1 + this.level().random.nextInt(3);
   }

   protected void handleAirSupply(int p_30344_) {
      if (this.isAlive() && !this.isInWaterOrBubble()) {
         this.setAirSupply(p_30344_ - 1);
         if (this.getAirSupply() == -20) {
            this.setAirSupply(0);
            this.hurt(this.damageSources().drown(), 2.0F);
         }
      } else {
         this.setAirSupply(300);
      }
   }

   @Override
   public void baseTick() {
      int $$0 = this.getAirSupply();
      super.baseTick();
      this.handleAirSupply($$0);
   }

   @Override
   public boolean isPushedByFluid() {
      return false;
   }

   @Override
   public boolean canBeLeashed() {
      return false;
   }

   public static boolean checkSurfaceWaterAnimalSpawnRules(
      EntityType<? extends WaterAnimal> p_218283_, LevelAccessor p_218284_, MobSpawnType p_218285_, BlockPos p_218286_, RandomSource p_218287_
   ) {
      int $$5 = p_218284_.getSeaLevel();
      int $$6 = $$5 - 13;
      return p_218286_.getY() >= $$6
         && p_218286_.getY() <= $$5
         && p_218284_.getFluidState(p_218286_.below()).is(FluidTags.WATER)
         && p_218284_.getBlockState(p_218286_.above()).is(Blocks.WATER);
   }
}
