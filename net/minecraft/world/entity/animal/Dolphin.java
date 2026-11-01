package net.minecraft.world.entity.animal;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreathAirGoal;
import net.minecraft.world.entity.ai.goal.DolphinJumpGoal;
import net.minecraft.world.entity.ai.goal.FollowBoatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

public class Dolphin extends WaterAnimal {
   private static final EntityDataAccessor<BlockPos> TREASURE_POS = SynchedEntityData.defineId(Dolphin.class, EntityDataSerializers.BLOCK_POS);
   private static final EntityDataAccessor<Boolean> GOT_FISH = SynchedEntityData.defineId(Dolphin.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> MOISTNESS_LEVEL = SynchedEntityData.defineId(Dolphin.class, EntityDataSerializers.INT);
   static final TargetingConditions SWIM_WITH_PLAYER_TARGETING = TargetingConditions.forNonCombat().range(10.0).ignoreLineOfSight();
   public static final int TOTAL_AIR_SUPPLY = 4800;
   private static final int TOTAL_MOISTNESS_LEVEL = 2400;
   public static final Predicate<ItemEntity> ALLOWED_ITEMS = p_350083_ -> !p_350083_.hasPickUpDelay() && p_350083_.isAlive() && p_350083_.isInWater();

   public Dolphin(EntityType<? extends Dolphin> p_28316_, Level p_28317_) {
      super(p_28316_, p_28317_);
      this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
      this.lookControl = new SmoothSwimmingLookControl(this, 10);
      this.setCanPickUpLoot(true);
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_28332_, DifficultyInstance p_28333_, MobSpawnType p_28334_, @Nullable SpawnGroupData p_28335_) {
      this.setAirSupply(this.getMaxAirSupply());
      this.setXRot(0.0F);
      return super.finalizeSpawn(p_28332_, p_28333_, p_28334_, p_28335_);
   }

   @Override
   protected void handleAirSupply(int p_28326_) {
   }

   public void setTreasurePos(BlockPos p_28385_) {
      this.entityData.set(TREASURE_POS, p_28385_);
   }

   public BlockPos getTreasurePos() {
      return this.entityData.get(TREASURE_POS);
   }

   public boolean gotFish() {
      return this.entityData.get(GOT_FISH);
   }

   public void setGotFish(boolean p_28394_) {
      this.entityData.set(GOT_FISH, p_28394_);
   }

   public int getMoistnessLevel() {
      return this.entityData.get(MOISTNESS_LEVEL);
   }

   public void setMoisntessLevel(int p_28344_) {
      this.entityData.set(MOISTNESS_LEVEL, p_28344_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_326152_) {
      super.defineSynchedData(p_326152_);
      p_326152_.define(TREASURE_POS, BlockPos.ZERO);
      p_326152_.define(GOT_FISH, false);
      p_326152_.define(MOISTNESS_LEVEL, 2400);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag p_28364_) {
      super.addAdditionalSaveData(p_28364_);
      p_28364_.putInt("TreasurePosX", this.getTreasurePos().getX());
      p_28364_.putInt("TreasurePosY", this.getTreasurePos().getY());
      p_28364_.putInt("TreasurePosZ", this.getTreasurePos().getZ());
      p_28364_.putBoolean("GotFish", this.gotFish());
      p_28364_.putInt("Moistness", this.getMoistnessLevel());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag p_28340_) {
      int $$1 = p_28340_.getInt("TreasurePosX");
      int $$2 = p_28340_.getInt("TreasurePosY");
      int $$3 = p_28340_.getInt("TreasurePosZ");
      this.setTreasurePos(new BlockPos($$1, $$2, $$3));
      super.readAdditionalSaveData(p_28340_);
      this.setGotFish(p_28340_.getBoolean("GotFish"));
      this.setMoisntessLevel(p_28340_.getInt("Moistness"));
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new BreathAirGoal(this));
      this.goalSelector.addGoal(0, new TryFindWaterGoal(this));
      this.goalSelector.addGoal(1, new Dolphin.DolphinSwimToTreasureGoal(this));
      this.goalSelector.addGoal(2, new Dolphin.DolphinSwimWithPlayerGoal(this, 4.0));
      this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 10));
      this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
      this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(5, new DolphinJumpGoal(this, 10));
      this.goalSelector.addGoal(6, new MeleeAttackGoal(this, 1.2F, true));
      this.goalSelector.addGoal(8, new Dolphin.PlayWithItemsGoal());
      this.goalSelector.addGoal(8, new FollowBoatGoal(this));
      this.goalSelector.addGoal(9, new AvoidEntityGoal<>(this, Guardian.class, 8.0F, 1.0, 1.0));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Guardian.class).setAlertOthers());
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 1.2F).add(Attributes.ATTACK_DAMAGE, 3.0);
   }

   @Override
   protected PathNavigation createNavigation(Level p_28362_) {
      return new WaterBoundPathNavigation(this, p_28362_);
   }

   @Override
   public void playAttackSound() {
      this.playSound(SoundEvents.DOLPHIN_ATTACK, 1.0F, 1.0F);
   }

   @Override
   public int getMaxAirSupply() {
      return 4800;
   }

   @Override
   protected int increaseAirSupply(int p_28389_) {
      return this.getMaxAirSupply();
   }

   @Override
   public int getMaxHeadXRot() {
      return 1;
   }

   @Override
   public int getMaxHeadYRot() {
      return 1;
   }

   @Override
   protected boolean canRide(Entity p_28391_) {
      return true;
   }

   @Override
   public boolean canTakeItem(ItemStack p_28376_) {
      EquipmentSlot $$1 = this.getEquipmentSlotForItem(p_28376_);
      return !this.getItemBySlot($$1).isEmpty() ? false : $$1 == EquipmentSlot.MAINHAND && super.canTakeItem(p_28376_);
   }

   @Override
   protected void pickUpItem(ItemEntity p_28357_) {
      if (this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
         ItemStack $$1 = p_28357_.getItem();
         if (this.canHoldItem($$1)) {
            this.onItemPickup(p_28357_);
            this.setItemSlot(EquipmentSlot.MAINHAND, $$1);
            this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            this.take(p_28357_, $$1.getCount());
            p_28357_.discard();
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.isNoAi()) {
         this.setAirSupply(this.getMaxAirSupply());
      } else {
         if (this.isInWaterRainOrBubble()) {
            this.setMoisntessLevel(2400);
         } else {
            this.setMoisntessLevel(this.getMoistnessLevel() - 1);
            if (this.getMoistnessLevel() <= 0) {
               this.hurt(this.damageSources().dryOut(), 1.0F);
            }

            if (this.onGround()) {
               this.setDeltaMovement(
                  this.getDeltaMovement().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F, 0.5, (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F)
               );
               this.setYRot(this.random.nextFloat() * 360.0F);
               this.setOnGround(false);
               this.hasImpulse = true;
            }
         }

         if (this.level().isClientSide && this.isInWater() && this.getDeltaMovement().lengthSqr() > 0.03) {
            Vec3 $$0 = this.getViewVector(0.0F);
            float $$1 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)) * 0.3F;
            float $$2 = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)) * 0.3F;
            float $$3 = 1.2F - this.random.nextFloat() * 0.7F;

            for (int $$4 = 0; $$4 < 2; $$4++) {
               this.level()
                  .addParticle(ParticleTypes.DOLPHIN, this.getX() - $$0.x * $$3 + $$1, this.getY() - $$0.y, this.getZ() - $$0.z * $$3 + $$2, 0.0, 0.0, 0.0);
               this.level()
                  .addParticle(ParticleTypes.DOLPHIN, this.getX() - $$0.x * $$3 - $$1, this.getY() - $$0.y, this.getZ() - $$0.z * $$3 - $$2, 0.0, 0.0, 0.0);
            }
         }
      }
   }

   @Override
   public void handleEntityEvent(byte p_28324_) {
      if (p_28324_ == 38) {
         this.addParticlesAroundSelf(ParticleTypes.HAPPY_VILLAGER);
      } else {
         super.handleEntityEvent(p_28324_);
      }
   }

   private void addParticlesAroundSelf(ParticleOptions p_28338_) {
      for (int $$1 = 0; $$1 < 7; $$1++) {
         double $$2 = this.random.nextGaussian() * 0.01;
         double $$3 = this.random.nextGaussian() * 0.01;
         double $$4 = this.random.nextGaussian() * 0.01;
         this.level().addParticle(p_28338_, this.getRandomX(1.0), this.getRandomY() + 0.2, this.getRandomZ(1.0), $$2, $$3, $$4);
      }
   }

   @Override
   protected InteractionResult mobInteract(Player p_28359_, InteractionHand p_28360_) {
      ItemStack $$2 = p_28359_.getItemInHand(p_28360_);
      if (!$$2.isEmpty() && $$2.is(ItemTags.FISHES)) {
         if (!this.level().isClientSide) {
            this.playSound(SoundEvents.DOLPHIN_EAT, 1.0F, 1.0F);
         }

         this.setGotFish(true);
         $$2.consume(1, p_28359_);
         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else {
         return super.mobInteract(p_28359_, p_28360_);
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_28374_) {
      return SoundEvents.DOLPHIN_HURT;
   }

   @Nullable
   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.DOLPHIN_DEATH;
   }

   @Nullable
   @Override
   protected SoundEvent getAmbientSound() {
      return this.isInWater() ? SoundEvents.DOLPHIN_AMBIENT_WATER : SoundEvents.DOLPHIN_AMBIENT;
   }

   @Override
   protected SoundEvent getSwimSplashSound() {
      return SoundEvents.DOLPHIN_SPLASH;
   }

   @Override
   protected SoundEvent getSwimSound() {
      return SoundEvents.DOLPHIN_SWIM;
   }

   protected boolean closeToNextPos() {
      BlockPos $$0 = this.getNavigation().getTargetPos();
      return $$0 != null ? $$0.closerToCenterThan(this.position(), 12.0) : false;
   }

   @Override
   public void travel(Vec3 p_28383_) {
      if (this.isEffectiveAi() && this.isInWater()) {
         this.moveRelative(this.getSpeed(), p_28383_);
         this.move(MoverType.SELF, this.getDeltaMovement());
         this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
         if (this.getTarget() == null) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
         }
      } else {
         super.travel(p_28383_);
      }
   }

   @Override
   public boolean canBeLeashed() {
      return true;
   }

   static class DolphinSwimToTreasureGoal extends Goal {
      private final Dolphin dolphin;
      private boolean stuck;

      DolphinSwimToTreasureGoal(Dolphin p_28402_) {
         this.dolphin = p_28402_;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      }

      @Override
      public boolean isInterruptable() {
         return false;
      }

      @Override
      public boolean canUse() {
         return this.dolphin.gotFish() && this.dolphin.getAirSupply() >= 100;
      }

      @Override
      public boolean canContinueToUse() {
         BlockPos $$0 = this.dolphin.getTreasurePos();
         return !BlockPos.containing($$0.getX(), this.dolphin.getY(), $$0.getZ()).closerToCenterThan(this.dolphin.position(), 4.0)
            && !this.stuck
            && this.dolphin.getAirSupply() >= 100;
      }

      @Override
      public void start() {
         if (this.dolphin.level() instanceof ServerLevel) {
            ServerLevel $$0 = (ServerLevel)this.dolphin.level();
            this.stuck = false;
            this.dolphin.getNavigation().stop();
            BlockPos $$1 = this.dolphin.blockPosition();
            BlockPos $$2 = $$0.findNearestMapStructure(StructureTags.DOLPHIN_LOCATED, $$1, 50, false);
            if ($$2 != null) {
               this.dolphin.setTreasurePos($$2);
               $$0.broadcastEntityEvent(this.dolphin, (byte)38);
            } else {
               this.stuck = true;
            }
         }
      }

      @Override
      public void stop() {
         BlockPos $$0 = this.dolphin.getTreasurePos();
         if (BlockPos.containing($$0.getX(), this.dolphin.getY(), $$0.getZ()).closerToCenterThan(this.dolphin.position(), 4.0) || this.stuck) {
            this.dolphin.setGotFish(false);
         }
      }

      @Override
      public void tick() {
         Level $$0 = this.dolphin.level();
         if (this.dolphin.closeToNextPos() || this.dolphin.getNavigation().isDone()) {
            Vec3 $$1 = Vec3.atCenterOf(this.dolphin.getTreasurePos());
            Vec3 $$2 = DefaultRandomPos.getPosTowards(this.dolphin, 16, 1, $$1, (float) (Math.PI / 8));
            if ($$2 == null) {
               $$2 = DefaultRandomPos.getPosTowards(this.dolphin, 8, 4, $$1, (float) (Math.PI / 2));
            }

            if ($$2 != null) {
               BlockPos $$3 = BlockPos.containing($$2);
               if (!$$0.getFluidState($$3).is(FluidTags.WATER) || !$$0.getBlockState($$3).isPathfindable(PathComputationType.WATER)) {
                  $$2 = DefaultRandomPos.getPosTowards(this.dolphin, 8, 5, $$1, (float) (Math.PI / 2));
               }
            }

            if ($$2 == null) {
               this.stuck = true;
               return;
            }

            this.dolphin.getLookControl().setLookAt($$2.x, $$2.y, $$2.z, this.dolphin.getMaxHeadYRot() + 20, this.dolphin.getMaxHeadXRot());
            this.dolphin.getNavigation().moveTo($$2.x, $$2.y, $$2.z, 1.3);
            if ($$0.random.nextInt(this.adjustedTickDelay(80)) == 0) {
               $$0.broadcastEntityEvent(this.dolphin, (byte)38);
            }
         }
      }
   }

   static class DolphinSwimWithPlayerGoal extends Goal {
      private final Dolphin dolphin;
      private final double speedModifier;
      @Nullable
      private Player player;

      DolphinSwimWithPlayerGoal(Dolphin p_28413_, double p_28414_) {
         this.dolphin = p_28413_;
         this.speedModifier = p_28414_;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         this.player = this.dolphin.level().getNearestPlayer(Dolphin.SWIM_WITH_PLAYER_TARGETING, this.dolphin);
         return this.player == null ? false : this.player.isSwimming() && this.dolphin.getTarget() != this.player;
      }

      @Override
      public boolean canContinueToUse() {
         return this.player != null && this.player.isSwimming() && this.dolphin.distanceToSqr(this.player) < 256.0;
      }

      @Override
      public void start() {
         this.player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100), this.dolphin);
      }

      @Override
      public void stop() {
         this.player = null;
         this.dolphin.getNavigation().stop();
      }

      @Override
      public void tick() {
         this.dolphin.getLookControl().setLookAt(this.player, this.dolphin.getMaxHeadYRot() + 20, this.dolphin.getMaxHeadXRot());
         if (this.dolphin.distanceToSqr(this.player) < 6.25) {
            this.dolphin.getNavigation().stop();
         } else {
            this.dolphin.getNavigation().moveTo(this.player, this.speedModifier);
         }

         if (this.player.isSwimming() && this.player.level().random.nextInt(6) == 0) {
            this.player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100), this.dolphin);
         }
      }
   }

   class PlayWithItemsGoal extends Goal {
      private int cooldown;

      @Override
      public boolean canUse() {
         if (this.cooldown > Dolphin.this.tickCount) {
            return false;
         } else {
            List<ItemEntity> $$0 = Dolphin.this.level()
               .getEntitiesOfClass(ItemEntity.class, Dolphin.this.getBoundingBox().inflate(8.0, 8.0, 8.0), Dolphin.ALLOWED_ITEMS);
            return !$$0.isEmpty() || !Dolphin.this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty();
         }
      }

      @Override
      public void start() {
         List<ItemEntity> $$0 = Dolphin.this.level()
            .getEntitiesOfClass(ItemEntity.class, Dolphin.this.getBoundingBox().inflate(8.0, 8.0, 8.0), Dolphin.ALLOWED_ITEMS);
         if (!$$0.isEmpty()) {
            Dolphin.this.getNavigation().moveTo($$0.get(0), 1.2F);
            Dolphin.this.playSound(SoundEvents.DOLPHIN_PLAY, 1.0F, 1.0F);
         }

         this.cooldown = 0;
      }

      @Override
      public void stop() {
         ItemStack $$0 = Dolphin.this.getItemBySlot(EquipmentSlot.MAINHAND);
         if (!$$0.isEmpty()) {
            this.drop($$0);
            Dolphin.this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.cooldown = Dolphin.this.tickCount + Dolphin.this.random.nextInt(100);
         }
      }

      @Override
      public void tick() {
         List<ItemEntity> $$0 = Dolphin.this.level()
            .getEntitiesOfClass(ItemEntity.class, Dolphin.this.getBoundingBox().inflate(8.0, 8.0, 8.0), Dolphin.ALLOWED_ITEMS);
         ItemStack $$1 = Dolphin.this.getItemBySlot(EquipmentSlot.MAINHAND);
         if (!$$1.isEmpty()) {
            this.drop($$1);
            Dolphin.this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
         } else if (!$$0.isEmpty()) {
            Dolphin.this.getNavigation().moveTo($$0.get(0), 1.2F);
         }
      }

      private void drop(ItemStack p_28429_) {
         if (!p_28429_.isEmpty()) {
            double $$1 = Dolphin.this.getEyeY() - 0.3F;
            ItemEntity $$2 = new ItemEntity(Dolphin.this.level(), Dolphin.this.getX(), $$1, Dolphin.this.getZ(), p_28429_);
            $$2.setPickUpDelay(40);
            $$2.setThrower(Dolphin.this);
            float $$3 = 0.3F;
            float $$4 = Dolphin.this.random.nextFloat() * (float) (Math.PI * 2);
            float $$5 = 0.02F * Dolphin.this.random.nextFloat();
            $$2.setDeltaMovement(
               0.3F * -Mth.sin(Dolphin.this.getYRot() * (float) (Math.PI / 180.0)) * Mth.cos(Dolphin.this.getXRot() * (float) (Math.PI / 180.0))
                  + Mth.cos($$4) * $$5,
               0.3F * Mth.sin(Dolphin.this.getXRot() * (float) (Math.PI / 180.0)) * 1.5F,
               0.3F * Mth.cos(Dolphin.this.getYRot() * (float) (Math.PI / 180.0)) * Mth.cos(Dolphin.this.getXRot() * (float) (Math.PI / 180.0))
                  + Mth.sin($$4) * $$5
            );
            Dolphin.this.level().addFreshEntity($$2);
         }
      }
   }
}
