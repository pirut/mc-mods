package net.minecraft.world.entity.animal.fish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;

public abstract class WaterAnimal extends PathfinderMob {
   public static final int AMBIENT_SOUND_INTERVAL = 120;

   protected WaterAnimal(EntityType<? extends WaterAnimal> p_478178_, Level p_479819_) {
      super(p_478178_, p_479819_);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader p_478737_) {
      return p_478737_.isUnobstructed(this);
   }

   @Override
   public int getAmbientSoundInterval() {
      return 120;
   }

   @Override
   protected int getBaseExperienceReward(ServerLevel p_480056_) {
      return 1 + this.random.nextInt(3);
   }

   protected void handleAirSupply(ServerLevel p_481632_, int p_481208_) {
      if (this.isAlive() && !this.isInWater()) {
         this.setAirSupply(p_481208_ - 1);
         if (this.shouldTakeDrowningDamage()) {
            this.setAirSupply(0);
            this.hurtServer(p_481632_, this.damageSources().drown(), 2.0F);
         }
      } else {
         this.setAirSupply(300);
      }
   }

   @Override
   public void baseTick() {
      int i = this.getAirSupply();
      super.baseTick();
      if (this.level() instanceof ServerLevel serverlevel) {
         this.handleAirSupply(serverlevel, i);
      }
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
      EntityType<? extends WaterAnimal> p_479767_, LevelAccessor p_478818_, EntitySpawnReason p_481392_, BlockPos p_481951_, RandomSource p_480555_
   ) {
      int i = p_478818_.getSeaLevel();
      int j = i - 13;
      return p_481951_.getY() >= j
         && p_481951_.getY() <= i
         && p_478818_.getFluidState(p_481951_.below()).is(FluidTags.WATER)
         && p_478818_.getBlockState(p_481951_.above()).is(Blocks.WATER);
   }
}
