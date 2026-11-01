package net.minecraft.world.entity.monster.spider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Spider extends Monster {
   private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(Spider.class, EntityDataSerializers.BYTE);
   private static final float SPIDER_SPECIAL_EFFECT_CHANCE = 0.1F;

   public Spider(EntityType<? extends Spider> p_480009_, Level p_479459_) {
      super(p_480009_, p_479459_);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Armadillo.class, 6.0F, 1.0, 1.2, p_480115_ -> !((Armadillo)p_480115_).isScared()));
      this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
      this.goalSelector.addGoal(4, new Spider.SpiderAttackGoal(this));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new Spider.SpiderTargetGoal<>(this, Player.class));
      this.targetSelector.addGoal(3, new Spider.SpiderTargetGoal<>(this, IronGolem.class));
   }

   @Override
   protected PathNavigation createNavigation(Level p_478204_) {
      return new WallClimberNavigation(this, p_478204_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_479225_) {
      super.defineSynchedData(p_479225_);
      p_479225_.define(DATA_FLAGS_ID, (byte)0);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         this.setClimbing(this.horizontalCollision);
      }
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.3F);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.SPIDER_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_481209_) {
      return SoundEvents.SPIDER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.SPIDER_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos p_481647_, BlockState p_478964_) {
      this.playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.0F);
   }

   @Override
   public boolean onClimbable() {
      return this.isClimbing();
   }

   @Override
   public void makeStuckInBlock(BlockState p_478894_, Vec3 p_481381_) {
      if (!p_478894_.is(Blocks.COBWEB)) {
         super.makeStuckInBlock(p_478894_, p_481381_);
      }
   }

   @Override
   public boolean canBeAffected(MobEffectInstance p_479991_) {
      return p_479991_.is(MobEffects.POISON) ? false : super.canBeAffected(p_479991_);
   }

   public boolean isClimbing() {
      return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
   }

   public void setClimbing(boolean p_479138_) {
      byte b0 = this.entityData.get(DATA_FLAGS_ID);
      if (p_479138_) {
         b0 = (byte)(b0 | 1);
      } else {
         b0 = (byte)(b0 & -2);
      }

      this.entityData.set(DATA_FLAGS_ID, b0);
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor p_478692_, DifficultyInstance p_479487_, EntitySpawnReason p_481376_, @Nullable SpawnGroupData p_480575_
   ) {
      p_480575_ = super.finalizeSpawn(p_478692_, p_479487_, p_481376_, p_480575_);
      RandomSource randomsource = p_478692_.getRandom();
      if (randomsource.nextInt(100) == 0) {
         Skeleton skeleton = EntityType.SKELETON.create(this.level(), EntitySpawnReason.JOCKEY);
         if (skeleton != null) {
            skeleton.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            skeleton.finalizeSpawn(p_478692_, p_479487_, p_481376_, null);
            skeleton.startRiding(this, false, false);
         }
      }

      if (p_480575_ == null) {
         p_480575_ = new Spider.SpiderEffectsGroupData();
         if (p_478692_.getDifficulty() == Difficulty.HARD && randomsource.nextFloat() < 0.1F * p_479487_.getSpecialMultiplier()) {
            ((Spider.SpiderEffectsGroupData)p_480575_).setRandomEffect(randomsource);
         }
      }

      if (p_480575_ instanceof Spider.SpiderEffectsGroupData spider$spidereffectsgroupdata) {
         Holder<MobEffect> holder = spider$spidereffectsgroupdata.effect;
         if (holder != null) {
            this.addEffect(new MobEffectInstance(holder, -1));
         }
      }

      return p_480575_;
   }

   @Override
   public Vec3 getVehicleAttachmentPoint(Entity p_479261_) {
      return p_479261_.getBbWidth() <= this.getBbWidth() ? new Vec3(0.0, 0.3125 * this.getScale(), 0.0) : super.getVehicleAttachmentPoint(p_479261_);
   }

   static class SpiderAttackGoal extends MeleeAttackGoal {
      public SpiderAttackGoal(Spider p_480012_) {
         super(p_480012_, 1.0, true);
      }

      @Override
      public boolean canUse() {
         return super.canUse() && !this.mob.isVehicle();
      }

      @Override
      public boolean canContinueToUse() {
         float f = this.mob.getLightLevelDependentMagicValue();
         if (f >= 0.5F && this.mob.getRandom().nextInt(100) == 0) {
            this.mob.setTarget(null);
            return false;
         } else {
            return super.canContinueToUse();
         }
      }
   }

   public static class SpiderEffectsGroupData implements SpawnGroupData {
      @Nullable
      public Holder<MobEffect> effect;

      public void setRandomEffect(RandomSource p_481946_) {
         int i = p_481946_.nextInt(5);
         if (i <= 1) {
            this.effect = MobEffects.SPEED;
         } else if (i <= 2) {
            this.effect = MobEffects.STRENGTH;
         } else if (i <= 3) {
            this.effect = MobEffects.REGENERATION;
         } else if (i <= 4) {
            this.effect = MobEffects.INVISIBILITY;
         }
      }
   }

   static class SpiderTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
      public SpiderTargetGoal(Spider p_480041_, Class<T> p_478638_) {
         super(p_480041_, p_478638_, true);
      }

      @Override
      public boolean canUse() {
         float f = this.mob.getLightLevelDependentMagicValue();
         return f >= 0.5F ? false : super.canUse();
      }
   }
}
