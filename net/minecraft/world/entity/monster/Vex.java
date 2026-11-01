package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public class Vex extends Monster implements TraceableEntity {
   public static final float FLAP_DEGREES_PER_TICK = 45.836624F;
   public static final int TICKS_PER_FLAP = Mth.ceil((float) (Math.PI * 5.0 / 4.0));
   protected static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(Vex.class, EntityDataSerializers.BYTE);
   private static final int FLAG_IS_CHARGING = 1;
   @Nullable
   Mob owner;
   @Nullable
   private BlockPos boundOrigin;
   private boolean hasLimitedLife;
   private int limitedLifeTicks;

   public Vex(EntityType<? extends Vex> p_33984_, Level p_33985_) {
      super(p_33984_, p_33985_);
      this.moveControl = new Vex.VexMoveControl(this);
      this.xpReward = 3;
   }

   @Override
   public boolean isFlapping() {
      return this.tickCount % TICKS_PER_FLAP == 0;
   }

   @Override
   public void move(MoverType p_33997_, Vec3 p_33998_) {
      super.move(p_33997_, p_33998_);
      this.checkInsideBlocks();
   }

   @Override
   public void tick() {
      this.noPhysics = true;
      super.tick();
      this.noPhysics = false;
      this.setNoGravity(true);
      if (this.hasLimitedLife && --this.limitedLifeTicks <= 0) {
         this.limitedLifeTicks = 20;
         this.hurt(this.damageSources().starve(), 1.0F);
      }
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(4, new Vex.VexChargeAttackGoal());
      this.goalSelector.addGoal(8, new Vex.VexRandomMoveGoal());
      this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
      this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class).setAlertOthers());
      this.targetSelector.addGoal(2, new Vex.VexCopyOwnerTargetGoal(this));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.ATTACK_DAMAGE, 4.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_326059_) {
      super.defineSynchedData(p_326059_);
      p_326059_.define(DATA_FLAGS_ID, (byte)0);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag p_34008_) {
      super.readAdditionalSaveData(p_34008_);
      if (p_34008_.contains("BoundX")) {
         this.boundOrigin = new BlockPos(p_34008_.getInt("BoundX"), p_34008_.getInt("BoundY"), p_34008_.getInt("BoundZ"));
      }

      if (p_34008_.contains("LifeTicks")) {
         this.setLimitedLife(p_34008_.getInt("LifeTicks"));
      }
   }

   @Override
   public void restoreFrom(Entity p_306094_) {
      super.restoreFrom(p_306094_);
      if (p_306094_ instanceof Vex $$1) {
         this.owner = $$1.getOwner();
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag p_34015_) {
      super.addAdditionalSaveData(p_34015_);
      if (this.boundOrigin != null) {
         p_34015_.putInt("BoundX", this.boundOrigin.getX());
         p_34015_.putInt("BoundY", this.boundOrigin.getY());
         p_34015_.putInt("BoundZ", this.boundOrigin.getZ());
      }

      if (this.hasLimitedLife) {
         p_34015_.putInt("LifeTicks", this.limitedLifeTicks);
      }
   }

   @Nullable
   public Mob getOwner() {
      return this.owner;
   }

   @Nullable
   public BlockPos getBoundOrigin() {
      return this.boundOrigin;
   }

   public void setBoundOrigin(@Nullable BlockPos p_34034_) {
      this.boundOrigin = p_34034_;
   }

   private boolean getVexFlag(int p_34011_) {
      int $$1 = this.entityData.get(DATA_FLAGS_ID);
      return ($$1 & p_34011_) != 0;
   }

   private void setVexFlag(int p_33990_, boolean p_33991_) {
      int $$2 = this.entityData.get(DATA_FLAGS_ID);
      if (p_33991_) {
         $$2 |= p_33990_;
      } else {
         $$2 &= ~p_33990_;
      }

      this.entityData.set(DATA_FLAGS_ID, (byte)($$2 & 0xFF));
   }

   public boolean isCharging() {
      return this.getVexFlag(1);
   }

   public void setIsCharging(boolean p_34043_) {
      this.setVexFlag(1, p_34043_);
   }

   public void setOwner(Mob p_33995_) {
      this.owner = p_33995_;
   }

   public void setLimitedLife(int p_33988_) {
      this.hasLimitedLife = true;
      this.limitedLifeTicks = p_33988_;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.VEX_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.VEX_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_34023_) {
      return SoundEvents.VEX_HURT;
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_34002_, DifficultyInstance p_34003_, MobSpawnType p_34004_, @Nullable SpawnGroupData p_34005_) {
      RandomSource $$4 = p_34002_.getRandom();
      this.populateDefaultEquipmentSlots($$4, p_34003_);
      this.populateDefaultEquipmentEnchantments(p_34002_, $$4, p_34003_);
      return super.finalizeSpawn(p_34002_, p_34003_, p_34004_, p_34005_);
   }

   @Override
   protected void populateDefaultEquipmentSlots(RandomSource p_219135_, DifficultyInstance p_219136_) {
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
      this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
   }

   class VexChargeAttackGoal extends Goal {
      public VexChargeAttackGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         LivingEntity $$0 = Vex.this.getTarget();
         return $$0 != null && $$0.isAlive() && !Vex.this.getMoveControl().hasWanted() && Vex.this.random.nextInt(reducedTickDelay(7)) == 0
            ? Vex.this.distanceToSqr($$0) > 4.0
            : false;
      }

      @Override
      public boolean canContinueToUse() {
         return Vex.this.getMoveControl().hasWanted() && Vex.this.isCharging() && Vex.this.getTarget() != null && Vex.this.getTarget().isAlive();
      }

      @Override
      public void start() {
         LivingEntity $$0 = Vex.this.getTarget();
         if ($$0 != null) {
            Vec3 $$1 = $$0.getEyePosition();
            Vex.this.moveControl.setWantedPosition($$1.x, $$1.y, $$1.z, 1.0);
         }

         Vex.this.setIsCharging(true);
         Vex.this.playSound(SoundEvents.VEX_CHARGE, 1.0F, 1.0F);
      }

      @Override
      public void stop() {
         Vex.this.setIsCharging(false);
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         LivingEntity $$0 = Vex.this.getTarget();
         if ($$0 != null) {
            if (Vex.this.getBoundingBox().intersects($$0.getBoundingBox())) {
               Vex.this.doHurtTarget($$0);
               Vex.this.setIsCharging(false);
            } else {
               double $$1 = Vex.this.distanceToSqr($$0);
               if ($$1 < 9.0) {
                  Vec3 $$2 = $$0.getEyePosition();
                  Vex.this.moveControl.setWantedPosition($$2.x, $$2.y, $$2.z, 1.0);
               }
            }
         }
      }
   }

   class VexCopyOwnerTargetGoal extends TargetGoal {
      private final TargetingConditions copyOwnerTargeting = TargetingConditions.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

      public VexCopyOwnerTargetGoal(PathfinderMob p_34056_) {
         super(p_34056_, false);
      }

      @Override
      public boolean canUse() {
         return Vex.this.owner != null && Vex.this.owner.getTarget() != null && this.canAttack(Vex.this.owner.getTarget(), this.copyOwnerTargeting);
      }

      @Override
      public void start() {
         Vex.this.setTarget(Vex.this.owner.getTarget());
         super.start();
      }
   }

   class VexMoveControl extends MoveControl {
      public VexMoveControl(Vex p_34062_) {
         super(p_34062_);
      }

      @Override
      public void tick() {
         if (this.operation == MoveControl.Operation.MOVE_TO) {
            Vec3 $$0 = new Vec3(this.wantedX - Vex.this.getX(), this.wantedY - Vex.this.getY(), this.wantedZ - Vex.this.getZ());
            double $$1 = $$0.length();
            if ($$1 < Vex.this.getBoundingBox().getSize()) {
               this.operation = MoveControl.Operation.WAIT;
               Vex.this.setDeltaMovement(Vex.this.getDeltaMovement().scale(0.5));
            } else {
               Vex.this.setDeltaMovement(Vex.this.getDeltaMovement().add($$0.scale(this.speedModifier * 0.05 / $$1)));
               if (Vex.this.getTarget() == null) {
                  Vec3 $$2 = Vex.this.getDeltaMovement();
                  Vex.this.setYRot(-((float)Mth.atan2($$2.x, $$2.z)) * (180.0F / (float)Math.PI));
                  Vex.this.yBodyRot = Vex.this.getYRot();
               } else {
                  double $$3 = Vex.this.getTarget().getX() - Vex.this.getX();
                  double $$4 = Vex.this.getTarget().getZ() - Vex.this.getZ();
                  Vex.this.setYRot(-((float)Mth.atan2($$3, $$4)) * (180.0F / (float)Math.PI));
                  Vex.this.yBodyRot = Vex.this.getYRot();
               }
            }
         }
      }
   }

   class VexRandomMoveGoal extends Goal {
      public VexRandomMoveGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         return !Vex.this.getMoveControl().hasWanted() && Vex.this.random.nextInt(reducedTickDelay(7)) == 0;
      }

      @Override
      public boolean canContinueToUse() {
         return false;
      }

      @Override
      public void tick() {
         BlockPos $$0 = Vex.this.getBoundOrigin();
         if ($$0 == null) {
            $$0 = Vex.this.blockPosition();
         }

         for (int $$1 = 0; $$1 < 3; $$1++) {
            BlockPos $$2 = $$0.offset(Vex.this.random.nextInt(15) - 7, Vex.this.random.nextInt(11) - 5, Vex.this.random.nextInt(15) - 7);
            if (Vex.this.level().isEmptyBlock($$2)) {
               Vex.this.moveControl.setWantedPosition($$2.getX() + 0.5, $$2.getY() + 0.5, $$2.getZ() + 0.5, 0.25);
               if (Vex.this.getTarget() == null) {
                  Vex.this.getLookControl().setLookAt($$2.getX() + 0.5, $$2.getY() + 0.5, $$2.getZ() + 0.5, 180.0F, 20.0F);
               }
               break;
            }
         }
      }
   }
}
