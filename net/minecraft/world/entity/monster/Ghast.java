package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Ghast extends Mob implements Enemy {
   private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING = SynchedEntityData.defineId(Ghast.class, EntityDataSerializers.BOOLEAN);
   private static final byte DEFAULT_EXPLOSION_POWER = 1;
   private int explosionPower = 1;

   public Ghast(EntityType<? extends Ghast> p_32725_, Level p_32726_) {
      super(p_32725_, p_32726_);
      this.xpReward = 5;
      this.moveControl = new Ghast.GhastMoveControl(this, false, () -> false);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(5, new Ghast.RandomFloatAroundGoal(this));
      this.goalSelector.addGoal(7, new Ghast.GhastLookGoal(this));
      this.goalSelector.addGoal(7, new Ghast.GhastShootFireballGoal(this));
      this.targetSelector
         .addGoal(
            1,
            new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (p_427100_, p_427101_) -> Math.abs(p_427100_.getY() - this.getY()) <= 4.0)
         );
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

   private static boolean isReflectedFireball(DamageSource p_238408_) {
      return p_238408_.getDirectEntity() instanceof LargeFireball && p_238408_.getEntity() instanceof Player;
   }

   @Override
   public boolean isInvulnerableTo(ServerLevel p_376822_, DamageSource p_238289_) {
      return this.isInvulnerable() && !p_238289_.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         || !isReflectedFireball(p_238289_) && super.isInvulnerableTo(p_376822_, p_238289_);
   }

   @Override
   protected void checkFallDamage(double p_416043_, boolean p_416735_, BlockState p_416029_, BlockPos p_415730_) {
   }

   @Override
   public boolean onClimbable() {
      return false;
   }

   @Override
   public void travel(Vec3 p_415638_) {
      this.travelFlying(p_415638_, 0.02F);
   }

   @Override
   public boolean hurtServer(ServerLevel p_376618_, DamageSource p_376819_, float p_376363_) {
      if (isReflectedFireball(p_376819_)) {
         super.hurtServer(p_376618_, p_376819_, 1000.0F);
         return true;
      } else {
         return this.isInvulnerableTo(p_376618_, p_376819_) ? false : super.hurtServer(p_376618_, p_376819_, p_376363_);
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_326063_) {
      super.defineSynchedData(p_326063_);
      p_326063_.define(DATA_IS_CHARGING, false);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 10.0)
         .add(Attributes.FOLLOW_RANGE, 100.0)
         .add(Attributes.CAMERA_DISTANCE, 8.0)
         .add(Attributes.FLYING_SPEED, 0.06);
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
      EntityType<Ghast> p_218985_, LevelAccessor p_218986_, EntitySpawnReason p_360741_, BlockPos p_218988_, RandomSource p_218989_
   ) {
      return p_218986_.getDifficulty() != Difficulty.PEACEFUL
         && p_218989_.nextInt(20) == 0
         && checkMobSpawnRules(p_218985_, p_218986_, p_360741_, p_218988_, p_218989_);
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return 1;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_422069_) {
      super.addAdditionalSaveData(p_422069_);
      p_422069_.putByte("ExplosionPower", (byte)this.explosionPower);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_421477_) {
      super.readAdditionalSaveData(p_421477_);
      this.explosionPower = p_421477_.getByteOr("ExplosionPower", (byte)1);
   }

   @Override
   public boolean supportQuadLeashAsHolder() {
      return true;
   }

   @Override
   public double leashElasticDistance() {
      return 10.0;
   }

   @Override
   public double leashSnapDistance() {
      return 16.0;
   }

   public static void faceMovementDirection(Mob p_416504_) {
      if (p_416504_.getTarget() == null) {
         Vec3 vec3 = p_416504_.getDeltaMovement();
         p_416504_.setYRot(-((float)Mth.atan2(vec3.x, vec3.z)) * (180.0F / (float)Math.PI));
         p_416504_.yBodyRot = p_416504_.getYRot();
      } else {
         LivingEntity livingentity = p_416504_.getTarget();
         double d0 = 64.0;
         if (livingentity.distanceToSqr(p_416504_) < 4096.0) {
            double d1 = livingentity.getX() - p_416504_.getX();
            double d2 = livingentity.getZ() - p_416504_.getZ();
            p_416504_.setYRot(-((float)Mth.atan2(d1, d2)) * (180.0F / (float)Math.PI));
            p_416504_.yBodyRot = p_416504_.getYRot();
         }
      }
   }

   public static class GhastLookGoal extends Goal {
      private final Mob ghast;

      public GhastLookGoal(Mob p_416384_) {
         this.ghast = p_416384_;
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
         Ghast.faceMovementDirection(this.ghast);
      }
   }

   public static class GhastMoveControl extends MoveControl {
      private final Mob ghast;
      private int floatDuration;
      private final boolean careful;
      private final BooleanSupplier shouldBeStopped;

      public GhastMoveControl(Mob p_416658_, boolean p_415882_, BooleanSupplier p_423656_) {
         super(p_416658_);
         this.ghast = p_416658_;
         this.careful = p_415882_;
         this.shouldBeStopped = p_423656_;
      }

      @Override
      public void tick() {
         if (this.shouldBeStopped.getAsBoolean()) {
            this.operation = MoveControl.Operation.WAIT;
            this.ghast.stopInPlace();
         }

         if (this.operation == MoveControl.Operation.MOVE_TO && this.floatDuration-- <= 0) {
            this.floatDuration = this.floatDuration + this.ghast.getRandom().nextInt(5) + 2;
            Vec3 vec3 = new Vec3(this.wantedX - this.ghast.getX(), this.wantedY - this.ghast.getY(), this.wantedZ - this.ghast.getZ());
            if (this.canReach(vec3)) {
               this.ghast
                  .setDeltaMovement(
                     this.ghast.getDeltaMovement().add(vec3.normalize().scale(this.ghast.getAttributeValue(Attributes.FLYING_SPEED) * 5.0 / 3.0))
                  );
            } else {
               this.operation = MoveControl.Operation.WAIT;
            }
         }
      }

      private boolean canReach(Vec3 p_32771_) {
         AABB aabb = this.ghast.getBoundingBox();
         AABB aabb1 = aabb.move(p_32771_);
         if (this.careful) {
            for (BlockPos blockpos : BlockPos.betweenClosed(aabb1.inflate(1.0))) {
               if (!this.blockTraversalPossible(this.ghast.level(), null, null, blockpos, false, false)) {
                  return false;
               }
            }
         }

         boolean flag = this.ghast.isInWater();
         boolean flag1 = this.ghast.isInLava();
         Vec3 vec3 = this.ghast.position();
         Vec3 vec31 = vec3.add(p_32771_);
         return BlockGetter.forEachBlockIntersectedBetween(
            vec3,
            vec31,
            aabb1,
            (p_423335_, p_423336_) -> aabb.intersects(p_423335_) ? true : this.blockTraversalPossible(this.ghast.level(), vec3, vec31, p_423335_, flag, flag1)
         );
      }

      private boolean blockTraversalPossible(
         BlockGetter p_419713_, @Nullable Vec3 p_422066_, @Nullable Vec3 p_421561_, BlockPos p_419924_, boolean p_419936_, boolean p_419980_
      ) {
         BlockState blockstate = p_419713_.getBlockState(p_419924_);
         if (blockstate.isAir()) {
            return true;
         } else {
            boolean flag = p_422066_ != null && p_421561_ != null;
            boolean flag1 = flag
               ? !this.ghast
                  .collidedWithShapeMovingFrom(p_422066_, p_421561_, blockstate.getCollisionShape(p_419713_, p_419924_).move(new Vec3(p_419924_)).toAabbs())
               : blockstate.getCollisionShape(p_419713_, p_419924_).isEmpty();
            if (!this.careful) {
               return flag1;
            } else if (blockstate.is(BlockTags.HAPPY_GHAST_AVOIDS)) {
               return false;
            } else {
               FluidState fluidstate = p_419713_.getFluidState(p_419924_);
               if (!fluidstate.isEmpty() && (!flag || this.ghast.collidedWithFluid(fluidstate, p_419924_, p_422066_, p_421561_))) {
                  if (fluidstate.is(FluidTags.WATER)) {
                     return p_419936_;
                  }

                  if (fluidstate.is(FluidTags.LAVA)) {
                     return p_419980_;
                  }
               }

               return flag1;
            }
         }
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
         LivingEntity livingentity = this.ghast.getTarget();
         if (livingentity != null) {
            double d0 = 64.0;
            if (livingentity.distanceToSqr(this.ghast) < 4096.0 && this.ghast.hasLineOfSight(livingentity)) {
               Level level = this.ghast.level();
               this.chargeTime++;
               if (this.chargeTime == 10 && !this.ghast.isSilent()) {
                  level.levelEvent(null, 1015, this.ghast.blockPosition(), 0);
               }

               if (this.chargeTime == 20) {
                  double d1 = 4.0;
                  Vec3 vec3 = this.ghast.getViewVector(1.0F);
                  double d2 = livingentity.getX() - (this.ghast.getX() + vec3.x * 4.0);
                  double d3 = livingentity.getY(0.5) - (0.5 + this.ghast.getY(0.5));
                  double d4 = livingentity.getZ() - (this.ghast.getZ() + vec3.z * 4.0);
                  Vec3 vec31 = new Vec3(d2, d3, d4);
                  if (!this.ghast.isSilent()) {
                     level.levelEvent(null, 1016, this.ghast.blockPosition(), 0);
                  }

                  LargeFireball largefireball = new LargeFireball(level, this.ghast, vec31.normalize(), this.ghast.getExplosionPower());
                  largefireball.setPos(this.ghast.getX() + vec3.x * 4.0, this.ghast.getY(0.5) + 0.5, largefireball.getZ() + vec3.z * 4.0);
                  level.addFreshEntity(largefireball);
                  this.chargeTime = -40;
               }
            } else if (this.chargeTime > 0) {
               this.chargeTime--;
            }

            this.ghast.setCharging(this.chargeTime > 10);
         }
      }
   }

   public static class RandomFloatAroundGoal extends Goal {
      private static final int MAX_ATTEMPTS = 64;
      private final Mob ghast;
      private final int distanceToBlocks;

      public RandomFloatAroundGoal(Mob p_416337_) {
         this(p_416337_, 0);
      }

      public RandomFloatAroundGoal(Mob p_415816_, int p_416473_) {
         this.ghast = p_415816_;
         this.distanceToBlocks = p_416473_;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public boolean canUse() {
         MoveControl movecontrol = this.ghast.getMoveControl();
         if (!movecontrol.hasWanted()) {
            return true;
         } else {
            double d0 = movecontrol.getWantedX() - this.ghast.getX();
            double d1 = movecontrol.getWantedY() - this.ghast.getY();
            double d2 = movecontrol.getWantedZ() - this.ghast.getZ();
            double d3 = d0 * d0 + d1 * d1 + d2 * d2;
            return d3 < 1.0 || d3 > 3600.0;
         }
      }

      @Override
      public boolean canContinueToUse() {
         return false;
      }

      @Override
      public void start() {
         Vec3 vec3 = getSuitableFlyToPosition(this.ghast, this.distanceToBlocks);
         this.ghast.getMoveControl().setWantedPosition(vec3.x(), vec3.y(), vec3.z(), 1.0);
      }

      public static Vec3 getSuitableFlyToPosition(Mob p_415820_, int p_415837_) {
         Level level = p_415820_.level();
         RandomSource randomsource = p_415820_.getRandom();
         Vec3 vec3 = p_415820_.position();
         Vec3 vec31 = null;

         for (int i = 0; i < 64; i++) {
            vec31 = chooseRandomPositionWithRestriction(p_415820_, vec3, randomsource);
            if (vec31 != null && isGoodTarget(level, vec31, p_415837_)) {
               return vec31;
            }
         }

         if (vec31 == null) {
            vec31 = chooseRandomPosition(vec3, randomsource);
         }

         BlockPos blockpos = BlockPos.containing(vec31);
         int j = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockpos.getX(), blockpos.getZ());
         if (j < blockpos.getY() && j > level.getMinY()) {
            vec31 = new Vec3(vec31.x(), p_415820_.getY() - Math.abs(p_415820_.getY() - vec31.y()), vec31.z());
         }

         return vec31;
      }

      private static boolean isGoodTarget(Level p_415688_, Vec3 p_416409_, int p_415738_) {
         if (p_415738_ <= 0) {
            return true;
         } else {
            BlockPos blockpos = BlockPos.containing(p_416409_);
            if (!p_415688_.getBlockState(blockpos).isAir()) {
               return false;
            } else {
               for (Direction direction : Direction.values()) {
                  for (int i = 1; i < p_415738_; i++) {
                     BlockPos blockpos1 = blockpos.relative(direction, i);
                     if (!p_415688_.getBlockState(blockpos1).isAir()) {
                        return true;
                     }
                  }
               }

               return false;
            }
         }
      }

      private static Vec3 chooseRandomPosition(Vec3 p_416643_, RandomSource p_416184_) {
         double d0 = p_416643_.x() + (p_416184_.nextFloat() * 2.0F - 1.0F) * 16.0F;
         double d1 = p_416643_.y() + (p_416184_.nextFloat() * 2.0F - 1.0F) * 16.0F;
         double d2 = p_416643_.z() + (p_416184_.nextFloat() * 2.0F - 1.0F) * 16.0F;
         return new Vec3(d0, d1, d2);
      }

      @Nullable
      private static Vec3 chooseRandomPositionWithRestriction(Mob p_415643_, Vec3 p_415583_, RandomSource p_416214_) {
         Vec3 vec3 = chooseRandomPosition(p_415583_, p_416214_);
         return p_415643_.hasHome() && !p_415643_.isWithinHome(vec3) ? null : vec3;
      }
   }
}
