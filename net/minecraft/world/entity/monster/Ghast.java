package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Ghast extends FlyingMob implements Enemy {
   private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING = SynchedEntityData.defineId(Ghast.class, EntityDataSerializers.BOOLEAN);
   private int explosionPower = 1;

   public Ghast(EntityType<? extends Ghast> p_32725_, Level p_32726_) {
      super(p_32725_, p_32726_);
      this.xpReward = 5;
      this.moveControl = new Ghast.GhastMoveControl(this);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(5, new Ghast.RandomFloatAroundGoal(this));
      this.goalSelector.addGoal(7, new Ghast.GhastLookGoal(this));
      this.goalSelector.addGoal(7, new Ghast.GhastShootFireballGoal(this));
      this.targetSelector
         .addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p_352811_ -> Math.abs(p_352811_.getY() - this.getY()) <= 4.0));
   }

   public boolean isCharging() {
      return this.entityData.get(DATA_IS_CHARGING);
   }

   public void setCharging(boolean p_32759_) {
      this.entityData.set(DATA_IS_CHARGING, p_32759_);
   }

   public int getExplosionPower() {
      return this.explosionPower;
   }

   @Override
   protected boolean shouldDespawnInPeaceful() {
      return true;
   }

   private static boolean isReflectedFireball(DamageSource p_238408_) {
      return p_238408_.getDirectEntity() instanceof LargeFireball && p_238408_.getEntity() instanceof Player;
   }

   @Override
   public boolean isInvulnerableTo(DamageSource p_238289_) {
      return this.isInvulnerable() && !p_238289_.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         || !isReflectedFireball(p_238289_) && super.isInvulnerableTo(p_238289_);
   }

   @Override
   public boolean hurt(DamageSource p_32730_, float p_32731_) {
      if (isReflectedFireball(p_32730_)) {
         super.hurt(p_32730_, 1000.0F);
         return true;
      } else {
         return this.isInvulnerableTo(p_32730_) ? false : super.hurt(p_32730_, p_32731_);
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_326063_) {
      super.defineSynchedData(p_326063_);
      p_326063_.define(DATA_IS_CHARGING, false);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.FOLLOW_RANGE, 100.0);
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.GHAST_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_32750_) {
      return SoundEvents.GHAST_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.GHAST_DEATH;
   }

   @Override
   protected float getSoundVolume() {
      return 5.0F;
   }

   public static boolean checkGhastSpawnRules(
      EntityType<Ghast> p_218985_, LevelAccessor p_218986_, MobSpawnType p_218987_, BlockPos p_218988_, RandomSource p_218989_
   ) {
      return p_218986_.getDifficulty() != Difficulty.PEACEFUL
         && p_218989_.nextInt(20) == 0
         && checkMobSpawnRules(p_218985_, p_218986_, p_218987_, p_218988_, p_218989_);
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return 1;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag p_32744_) {
      super.addAdditionalSaveData(p_32744_);
      p_32744_.putByte("ExplosionPower", (byte)this.explosionPower);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag p_32733_) {
      super.readAdditionalSaveData(p_32733_);
      if (p_32733_.contains("ExplosionPower", 99)) {
         this.explosionPower = p_32733_.getByte("ExplosionPower");
      }
   }

   static class GhastLookGoal extends Goal {
      private final Ghast ghast;

      public GhastLookGoal(Ghast p_32762_) {
         this.ghast = p_32762_;
         this.setFlags(EnumSet.of(Goal.Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         return true;
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         if (this.ghast.getTarget() == null) {
            Vec3 $$0 = this.ghast.getDeltaMovement();
            this.ghast.setYRot(-((float)Mth.atan2($$0.x, $$0.z)) * (180.0F / (float)Math.PI));
            this.ghast.yBodyRot = this.ghast.getYRot();
         } else {
            LivingEntity $$1 = this.ghast.getTarget();
            double $$2 = 64.0;
            if ($$1.distanceToSqr(this.ghast) < 4096.0) {
               double $$3 = $$1.getX() - this.ghast.getX();
               double $$4 = $$1.getZ() - this.ghast.getZ();
               this.ghast.setYRot(-((float)Mth.atan2($$3, $$4)) * (180.0F / (float)Math.PI));
               this.ghast.yBodyRot = this.ghast.getYRot();
            }
         }
      }
   }

   static class GhastMoveControl extends MoveControl {
      private final Ghast ghast;
      private int floatDuration;

      public GhastMoveControl(Ghast p_32768_) {
         super(p_32768_);
         this.ghast = p_32768_;
      }

      @Override
      public void tick() {
         if (this.operation == MoveControl.Operation.MOVE_TO) {
            if (this.floatDuration-- <= 0) {
               this.floatDuration = this.floatDuration + this.ghast.getRandom().nextInt(5) + 2;
               Vec3 $$0 = new Vec3(this.wantedX - this.ghast.getX(), this.wantedY - this.ghast.getY(), this.wantedZ - this.ghast.getZ());
               double $$1 = $$0.length();
               $$0 = $$0.normalize();
               if (this.canReach($$0, Mth.ceil($$1))) {
                  this.ghast.setDeltaMovement(this.ghast.getDeltaMovement().add($$0.scale(0.1)));
               } else {
                  this.operation = MoveControl.Operation.WAIT;
               }
            }
         }
      }

      private boolean canReach(Vec3 p_32771_, int p_32772_) {
         AABB $$2 = this.ghast.getBoundingBox();

         for (int $$3 = 1; $$3 < p_32772_; $$3++) {
            $$2 = $$2.move(p_32771_);
            if (!this.ghast.level().noCollision(this.ghast, $$2)) {
               return false;
            }
         }

         return true;
      }
   }

   static class GhastShootFireballGoal extends Goal {
      private final Ghast ghast;
      public int chargeTime;

      public GhastShootFireballGoal(Ghast p_32776_) {
         this.ghast = p_32776_;
      }

      @Override
      public boolean canUse() {
         return this.ghast.getTarget() != null;
      }

      @Override
      public void start() {
         this.chargeTime = 0;
      }

      @Override
      public void stop() {
         this.ghast.setCharging(false);
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         LivingEntity $$0 = this.ghast.getTarget();
         if ($$0 != null) {
            double $$1 = 64.0;
            if ($$0.distanceToSqr(this.ghast) < 4096.0 && this.ghast.hasLineOfSight($$0)) {
               Level $$2 = this.ghast.level();
               this.chargeTime++;
               if (this.chargeTime == 10 && !this.ghast.isSilent()) {
                  $$2.levelEvent(null, 1015, this.ghast.blockPosition(), 0);
               }

               if (this.chargeTime == 20) {
                  double $$3 = 4.0;
                  Vec3 $$4 = this.ghast.getViewVector(1.0F);
                  double $$5 = $$0.getX() - (this.ghast.getX() + $$4.x * 4.0);
                  double $$6 = $$0.getY(0.5) - (0.5 + this.ghast.getY(0.5));
                  double $$7 = $$0.getZ() - (this.ghast.getZ() + $$4.z * 4.0);
                  Vec3 $$8 = new Vec3($$5, $$6, $$7);
                  if (!this.ghast.isSilent()) {
                     $$2.levelEvent(null, 1016, this.ghast.blockPosition(), 0);
                  }

                  LargeFireball $$9 = new LargeFireball($$2, this.ghast, $$8.normalize(), this.ghast.getExplosionPower());
                  $$9.setPos(this.ghast.getX() + $$4.x * 4.0, this.ghast.getY(0.5) + 0.5, $$9.getZ() + $$4.z * 4.0);
                  $$2.addFreshEntity($$9);
                  this.chargeTime = -40;
               }
            } else if (this.chargeTime > 0) {
               this.chargeTime--;
            }

            this.ghast.setCharging(this.chargeTime > 10);
         }
      }
   }

   static class RandomFloatAroundGoal extends Goal {
      private final Ghast ghast;

      public RandomFloatAroundGoal(Ghast p_32783_) {
         this.ghast = p_32783_;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         MoveControl $$0 = this.ghast.getMoveControl();
         if (!$$0.hasWanted()) {
            return true;
         } else {
            double $$1 = $$0.getWantedX() - this.ghast.getX();
            double $$2 = $$0.getWantedY() - this.ghast.getY();
            double $$3 = $$0.getWantedZ() - this.ghast.getZ();
            double $$4 = $$1 * $$1 + $$2 * $$2 + $$3 * $$3;
            return $$4 < 1.0 || $$4 > 3600.0;
         }
      }

      @Override
      public boolean canContinueToUse() {
         return false;
      }

      @Override
      public void start() {
         RandomSource $$0 = this.ghast.getRandom();
         double $$1 = this.ghast.getX() + ($$0.nextFloat() * 2.0F - 1.0F) * 16.0F;
         double $$2 = this.ghast.getY() + ($$0.nextFloat() * 2.0F - 1.0F) * 16.0F;
         double $$3 = this.ghast.getZ() + ($$0.nextFloat() * 2.0F - 1.0F) * 16.0F;
         this.ghast.getMoveControl().setWantedPosition($$1, $$2, $$3, 1.0);
      }
   }
}
