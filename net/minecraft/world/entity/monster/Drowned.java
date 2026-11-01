package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

public class Drowned extends Zombie implements RangedAttackMob {
   public static final float NAUTILUS_SHELL_CHANCE = 0.03F;
   boolean searchingForLand;
   protected final WaterBoundPathNavigation waterNavigation;
   protected final GroundPathNavigation groundNavigation;

   public Drowned(EntityType<? extends Drowned> p_32344_, Level p_32345_) {
      super(p_32344_, p_32345_);
      this.moveControl = new Drowned.DrownedMoveControl(this);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
      this.waterNavigation = new WaterBoundPathNavigation(this, p_32345_);
      this.groundNavigation = new GroundPathNavigation(this, p_32345_);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Zombie.createAttributes().add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   protected void addBehaviourGoals() {
      this.goalSelector.addGoal(1, new Drowned.DrownedGoToWaterGoal(this, 1.0));
      this.goalSelector.addGoal(2, new Drowned.DrownedTridentAttackGoal(this, 1.0, 40, 10.0F));
      this.goalSelector.addGoal(2, new Drowned.DrownedAttackGoal(this, 1.0, false));
      this.goalSelector.addGoal(5, new Drowned.DrownedGoToBeachGoal(this, 1.0));
      this.goalSelector.addGoal(6, new Drowned.DrownedSwimUpGoal(this, 1.0, this.level().getSeaLevel()));
      this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Drowned.class).setAlertOthers(ZombifiedPiglin.class));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::okTarget));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Axolotl.class, true, false));
      this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_32372_, DifficultyInstance p_32373_, MobSpawnType p_32374_, @Nullable SpawnGroupData p_32375_) {
      p_32375_ = super.finalizeSpawn(p_32372_, p_32373_, p_32374_, p_32375_);
      if (this.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty() && p_32372_.getRandom().nextFloat() < 0.03F) {
         this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.NAUTILUS_SHELL));
         this.setGuaranteedDrop(EquipmentSlot.OFFHAND);
      }

      return p_32375_;
   }

   public static boolean checkDrownedSpawnRules(
      EntityType<Drowned> p_218956_, ServerLevelAccessor p_218957_, MobSpawnType p_218958_, BlockPos p_218959_, RandomSource p_218960_
   ) {
      if (!p_218957_.getFluidState(p_218959_.below()).is(FluidTags.WATER) && !MobSpawnType.isSpawner(p_218958_)) {
         return false;
      } else {
         Holder<Biome> $$5 = p_218957_.getBiome(p_218959_);
         boolean $$6 = p_218957_.getDifficulty() != Difficulty.PEACEFUL
            && (MobSpawnType.ignoresLightRequirements(p_218958_) || isDarkEnoughToSpawn(p_218957_, p_218959_, p_218960_))
            && (MobSpawnType.isSpawner(p_218958_) || p_218957_.getFluidState(p_218959_).is(FluidTags.WATER));
         if ($$6 && MobSpawnType.isSpawner(p_218958_)) {
            return true;
         } else {
            return $$5.is(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS)
               ? p_218960_.nextInt(15) == 0 && $$6
               : p_218960_.nextInt(40) == 0 && isDeepEnoughToSpawn(p_218957_, p_218959_) && $$6;
         }
      }
   }

   private static boolean isDeepEnoughToSpawn(LevelAccessor p_32367_, BlockPos p_32368_) {
      return p_32368_.getY() < p_32367_.getSeaLevel() - 5;
   }

   @Override
   protected boolean supportsBreakDoorGoal() {
      return false;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isInWater() ? SoundEvents.DROWNED_AMBIENT_WATER : SoundEvents.DROWNED_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_32386_) {
      return this.isInWater() ? SoundEvents.DROWNED_HURT_WATER : SoundEvents.DROWNED_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return this.isInWater() ? SoundEvents.DROWNED_DEATH_WATER : SoundEvents.DROWNED_DEATH;
   }

   @Override
   protected SoundEvent getStepSound() {
      return SoundEvents.DROWNED_STEP;
   }

   @Override
   protected SoundEvent getSwimSound() {
      return SoundEvents.DROWNED_SWIM;
   }

   @Override
   protected ItemStack getSkull() {
      return ItemStack.EMPTY;
   }

   @Override
   protected void populateDefaultEquipmentSlots(RandomSource p_218953_, DifficultyInstance p_218954_) {
      if (p_218953_.nextFloat() > 0.9) {
         int $$2 = p_218953_.nextInt(16);
         if ($$2 < 10) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
         } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
         }
      }
   }

   @Override
   protected boolean canReplaceCurrentItem(ItemStack p_32364_, ItemStack p_32365_) {
      if (p_32365_.is(Items.NAUTILUS_SHELL)) {
         return false;
      } else if (p_32365_.is(Items.TRIDENT)) {
         return p_32364_.is(Items.TRIDENT) ? p_32364_.getDamageValue() < p_32365_.getDamageValue() : false;
      } else {
         return p_32364_.is(Items.TRIDENT) ? true : super.canReplaceCurrentItem(p_32364_, p_32365_);
      }
   }

   @Override
   protected boolean convertsInWater() {
      return false;
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader p_32370_) {
      return p_32370_.isUnobstructed(this);
   }

   public boolean okTarget(@Nullable LivingEntity p_32396_) {
      return p_32396_ != null ? !this.level().isDay() || p_32396_.isInWater() : false;
   }

   @Override
   public boolean isPushedByFluid() {
      return !this.isSwimming();
   }

   boolean wantsToSwim() {
      if (this.searchingForLand) {
         return true;
      } else {
         LivingEntity $$0 = this.getTarget();
         return $$0 != null && $$0.isInWater();
      }
   }

   @Override
   public void travel(Vec3 p_32394_) {
      if (this.isControlledByLocalInstance() && this.isInWater() && this.wantsToSwim()) {
         this.moveRelative(0.01F, p_32394_);
         this.move(MoverType.SELF, this.getDeltaMovement());
         this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
      } else {
         super.travel(p_32394_);
      }
   }

   @Override
   public void updateSwimming() {
      if (!this.level().isClientSide) {
         if (this.isEffectiveAi() && this.isInWater() && this.wantsToSwim()) {
            this.navigation = this.waterNavigation;
            this.setSwimming(true);
         } else {
            this.navigation = this.groundNavigation;
            this.setSwimming(false);
         }
      }
   }

   @Override
   public boolean isVisuallySwimming() {
      return this.isSwimming();
   }

   protected boolean closeToNextPos() {
      Path $$0 = this.getNavigation().getPath();
      if ($$0 != null) {
         BlockPos $$1 = $$0.getTarget();
         if ($$1 != null) {
            double $$2 = this.distanceToSqr($$1.getX(), $$1.getY(), $$1.getZ());
            if ($$2 < 4.0) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public void performRangedAttack(LivingEntity p_32356_, float p_32357_) {
      ThrownTrident $$2 = new ThrownTrident(this.level(), this, new ItemStack(Items.TRIDENT));
      double $$3 = p_32356_.getX() - this.getX();
      double $$4 = p_32356_.getY(0.3333333333333333) - $$2.getY();
      double $$5 = p_32356_.getZ() - this.getZ();
      double $$6 = Math.sqrt($$3 * $$3 + $$5 * $$5);
      $$2.shoot($$3, $$4 + $$6 * 0.2F, $$5, 1.6F, 14 - this.level().getDifficulty().getId() * 4);
      this.playSound(SoundEvents.DROWNED_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
      this.level().addFreshEntity($$2);
   }

   public void setSearchingForLand(boolean p_32399_) {
      this.searchingForLand = p_32399_;
   }

   static class DrownedAttackGoal extends ZombieAttackGoal {
      private final Drowned drowned;

      public DrownedAttackGoal(Drowned p_32402_, double p_32403_, boolean p_32404_) {
         super(p_32402_, p_32403_, p_32404_);
         this.drowned = p_32402_;
      }

      @Override
      public boolean canUse() {
         return super.canUse() && this.drowned.okTarget(this.drowned.getTarget());
      }

      @Override
      public boolean canContinueToUse() {
         return super.canContinueToUse() && this.drowned.okTarget(this.drowned.getTarget());
      }
   }

   static class DrownedGoToBeachGoal extends MoveToBlockGoal {
      private final Drowned drowned;

      public DrownedGoToBeachGoal(Drowned p_32409_, double p_32410_) {
         super(p_32409_, p_32410_, 8, 2);
         this.drowned = p_32409_;
      }

      @Override
      public boolean canUse() {
         return super.canUse() && !this.drowned.level().isDay() && this.drowned.isInWater() && this.drowned.getY() >= this.drowned.level().getSeaLevel() - 3;
      }

      @Override
      public boolean canContinueToUse() {
         return super.canContinueToUse();
      }

      @Override
      protected boolean isValidTarget(LevelReader p_32413_, BlockPos p_32414_) {
         BlockPos $$2 = p_32414_.above();
         return p_32413_.isEmptyBlock($$2) && p_32413_.isEmptyBlock($$2.above())
            ? p_32413_.getBlockState(p_32414_).entityCanStandOn(p_32413_, p_32414_, this.drowned)
            : false;
      }

      @Override
      public void start() {
         this.drowned.setSearchingForLand(false);
         this.drowned.navigation = this.drowned.groundNavigation;
         super.start();
      }

      @Override
      public void stop() {
         super.stop();
      }
   }

   static class DrownedGoToWaterGoal extends Goal {
      private final PathfinderMob mob;
      private double wantedX;
      private double wantedY;
      private double wantedZ;
      private final double speedModifier;
      private final Level level;

      public DrownedGoToWaterGoal(PathfinderMob p_32425_, double p_32426_) {
         this.mob = p_32425_;
         this.speedModifier = p_32426_;
         this.level = p_32425_.level();
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         if (!this.level.isDay()) {
            return false;
         } else if (this.mob.isInWater()) {
            return false;
         } else {
            Vec3 $$0 = this.getWaterPos();
            if ($$0 == null) {
               return false;
            } else {
               this.wantedX = $$0.x;
               this.wantedY = $$0.y;
               this.wantedZ = $$0.z;
               return true;
            }
         }
      }

      @Override
      public boolean canContinueToUse() {
         return !this.mob.getNavigation().isDone();
      }

      @Override
      public void start() {
         this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
      }

      @Nullable
      private Vec3 getWaterPos() {
         RandomSource $$0 = this.mob.getRandom();
         BlockPos $$1 = this.mob.blockPosition();

         for (int $$2 = 0; $$2 < 10; $$2++) {
            BlockPos $$3 = $$1.offset($$0.nextInt(20) - 10, 2 - $$0.nextInt(8), $$0.nextInt(20) - 10);
            if (this.level.getBlockState($$3).is(Blocks.WATER)) {
               return Vec3.atBottomCenterOf($$3);
            }
         }

         return null;
      }
   }

   static class DrownedMoveControl extends MoveControl {
      private final Drowned drowned;

      public DrownedMoveControl(Drowned p_32433_) {
         super(p_32433_);
         this.drowned = p_32433_;
      }

      @Override
      public void tick() {
         LivingEntity $$0 = this.drowned.getTarget();
         if (this.drowned.wantsToSwim() && this.drowned.isInWater()) {
            if ($$0 != null && $$0.getY() > this.drowned.getY() || this.drowned.searchingForLand) {
               this.drowned.setDeltaMovement(this.drowned.getDeltaMovement().add(0.0, 0.002, 0.0));
            }

            if (this.operation != MoveControl.Operation.MOVE_TO || this.drowned.getNavigation().isDone()) {
               this.drowned.setSpeed(0.0F);
               return;
            }

            double $$1 = this.wantedX - this.drowned.getX();
            double $$2 = this.wantedY - this.drowned.getY();
            double $$3 = this.wantedZ - this.drowned.getZ();
            double $$4 = Math.sqrt($$1 * $$1 + $$2 * $$2 + $$3 * $$3);
            $$2 /= $$4;
            float $$5 = (float)(Mth.atan2($$3, $$1) * 180.0F / (float)Math.PI) - 90.0F;
            this.drowned.setYRot(this.rotlerp(this.drowned.getYRot(), $$5, 90.0F));
            this.drowned.yBodyRot = this.drowned.getYRot();
            float $$6 = (float)(this.speedModifier * this.drowned.getAttributeValue(Attributes.MOVEMENT_SPEED));
            float $$7 = Mth.lerp(0.125F, this.drowned.getSpeed(), $$6);
            this.drowned.setSpeed($$7);
            this.drowned.setDeltaMovement(this.drowned.getDeltaMovement().add($$7 * $$1 * 0.005, $$7 * $$2 * 0.1, $$7 * $$3 * 0.005));
         } else {
            if (!this.drowned.onGround()) {
               this.drowned.setDeltaMovement(this.drowned.getDeltaMovement().add(0.0, -0.008, 0.0));
            }

            super.tick();
         }
      }
   }

   static class DrownedSwimUpGoal extends Goal {
      private final Drowned drowned;
      private final double speedModifier;
      private final int seaLevel;
      private boolean stuck;

      public DrownedSwimUpGoal(Drowned p_32440_, double p_32441_, int p_32442_) {
         this.drowned = p_32440_;
         this.speedModifier = p_32441_;
         this.seaLevel = p_32442_;
      }

      @Override
      public boolean canUse() {
         return !this.drowned.level().isDay() && this.drowned.isInWater() && this.drowned.getY() < this.seaLevel - 2;
      }

      @Override
      public boolean canContinueToUse() {
         return this.canUse() && !this.stuck;
      }

      @Override
      public void tick() {
         if (this.drowned.getY() < this.seaLevel - 1 && (this.drowned.getNavigation().isDone() || this.drowned.closeToNextPos())) {
            Vec3 $$0 = DefaultRandomPos.getPosTowards(
               this.drowned, 4, 8, new Vec3(this.drowned.getX(), this.seaLevel - 1, this.drowned.getZ()), (float) (Math.PI / 2)
            );
            if ($$0 == null) {
               this.stuck = true;
               return;
            }

            this.drowned.getNavigation().moveTo($$0.x, $$0.y, $$0.z, this.speedModifier);
         }
      }

      @Override
      public void start() {
         this.drowned.setSearchingForLand(true);
         this.stuck = false;
      }

      @Override
      public void stop() {
         this.drowned.setSearchingForLand(false);
      }
   }

   static class DrownedTridentAttackGoal extends RangedAttackGoal {
      private final Drowned drowned;

      public DrownedTridentAttackGoal(RangedAttackMob p_32450_, double p_32451_, int p_32452_, float p_32453_) {
         super(p_32450_, p_32451_, p_32452_, p_32453_);
         this.drowned = (Drowned)p_32450_;
      }

      @Override
      public boolean canUse() {
         return super.canUse() && this.drowned.getMainHandItem().is(Items.TRIDENT);
      }

      @Override
      public void start() {
         super.start();
         this.drowned.setAggressive(true);
         this.drowned.startUsingItem(InteractionHand.MAIN_HAND);
      }

      @Override
      public void stop() {
         super.stop();
         this.drowned.stopUsingItem();
         this.drowned.setAggressive(false);
      }
   }
}
