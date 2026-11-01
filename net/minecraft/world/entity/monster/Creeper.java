package net.minecraft.world.entity.monster;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class Creeper extends Monster implements PowerableMob {
   private static final EntityDataAccessor<Integer> DATA_SWELL_DIR = SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_IS_POWERED = SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_IS_IGNITED = SynchedEntityData.defineId(Creeper.class, EntityDataSerializers.BOOLEAN);
   private int oldSwell;
   private int swell;
   private int maxSwell = 30;
   private int explosionRadius = 3;
   private int droppedSkulls;

   public Creeper(EntityType<? extends Creeper> p_32278_, Level p_32279_) {
      super(p_32278_, p_32279_);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(2, new SwellGoal(this));
      this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, 6.0F, 1.0, 1.2));
      this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, 6.0F, 1.0, 1.2));
      this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
      this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.25);
   }

   @Override
   public int getMaxFallDistance() {
      return this.getTarget() == null ? this.getComfortableFallDistance(0.0F) : this.getComfortableFallDistance(this.getHealth() - 1.0F);
   }

   @Override
   public boolean causeFallDamage(float p_149687_, float p_149688_, DamageSource p_149689_) {
      boolean $$3 = super.causeFallDamage(p_149687_, p_149688_, p_149689_);
      this.swell += (int)(p_149687_ * 1.5F);
      if (this.swell > this.maxSwell - 5) {
         this.swell = this.maxSwell - 5;
      }

      return $$3;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_326212_) {
      super.defineSynchedData(p_326212_);
      p_326212_.define(DATA_SWELL_DIR, -1);
      p_326212_.define(DATA_IS_POWERED, false);
      p_326212_.define(DATA_IS_IGNITED, false);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag p_32304_) {
      super.addAdditionalSaveData(p_32304_);
      if (this.entityData.get(DATA_IS_POWERED)) {
         p_32304_.putBoolean("powered", true);
      }

      p_32304_.putShort("Fuse", (short)this.maxSwell);
      p_32304_.putByte("ExplosionRadius", (byte)this.explosionRadius);
      p_32304_.putBoolean("ignited", this.isIgnited());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag p_32296_) {
      super.readAdditionalSaveData(p_32296_);
      this.entityData.set(DATA_IS_POWERED, p_32296_.getBoolean("powered"));
      if (p_32296_.contains("Fuse", 99)) {
         this.maxSwell = p_32296_.getShort("Fuse");
      }

      if (p_32296_.contains("ExplosionRadius", 99)) {
         this.explosionRadius = p_32296_.getByte("ExplosionRadius");
      }

      if (p_32296_.getBoolean("ignited")) {
         this.ignite();
      }
   }

   @Override
   public void tick() {
      if (this.isAlive()) {
         this.oldSwell = this.swell;
         if (this.isIgnited()) {
            this.setSwellDir(1);
         }

         int $$0 = this.getSwellDir();
         if ($$0 > 0 && this.swell == 0) {
            this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
            this.gameEvent(GameEvent.PRIME_FUSE);
         }

         this.swell += $$0;
         if (this.swell < 0) {
            this.swell = 0;
         }

         if (this.swell >= this.maxSwell) {
            this.swell = this.maxSwell;
            this.explodeCreeper();
         }
      }

      super.tick();
   }

   @Override
   public void setTarget(@Nullable LivingEntity p_149691_) {
      if (!(p_149691_ instanceof Goat)) {
         super.setTarget(p_149691_);
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_32309_) {
      return SoundEvents.CREEPER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.CREEPER_DEATH;
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel p_348566_, DamageSource p_32292_, boolean p_32294_) {
      super.dropCustomDeathLoot(p_348566_, p_32292_, p_32294_);
      Entity $$3 = p_32292_.getEntity();
      if ($$3 != this && $$3 instanceof Creeper $$4 && $$4.canDropMobsSkull()) {
         $$4.increaseDroppedSkulls();
         this.spawnAtLocation(Items.CREEPER_HEAD);
      }
   }

   @Override
   public boolean doHurtTarget(Entity p_32281_) {
      return true;
   }

   @Override
   public boolean isPowered() {
      return this.entityData.get(DATA_IS_POWERED);
   }

   public float getSwelling(float p_32321_) {
      return Mth.lerp(p_32321_, (float)this.oldSwell, (float)this.swell) / (this.maxSwell - 2);
   }

   public int getSwellDir() {
      return this.entityData.get(DATA_SWELL_DIR);
   }

   public void setSwellDir(int p_32284_) {
      this.entityData.set(DATA_SWELL_DIR, p_32284_);
   }

   @Override
   public void thunderHit(ServerLevel p_32286_, LightningBolt p_32287_) {
      super.thunderHit(p_32286_, p_32287_);
      this.entityData.set(DATA_IS_POWERED, true);
   }

   @Override
   protected InteractionResult mobInteract(Player p_32301_, InteractionHand p_32302_) {
      ItemStack $$2 = p_32301_.getItemInHand(p_32302_);
      if ($$2.is(ItemTags.CREEPER_IGNITERS)) {
         SoundEvent $$3 = $$2.is(Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE;
         this.level().playSound(p_32301_, this.getX(), this.getY(), this.getZ(), $$3, this.getSoundSource(), 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
         if (!this.level().isClientSide) {
            this.ignite();
            if (!$$2.isDamageableItem()) {
               $$2.shrink(1);
            } else {
               $$2.hurtAndBreak(1, p_32301_, getSlotForHand(p_32302_));
            }
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else {
         return super.mobInteract(p_32301_, p_32302_);
      }
   }

   private void explodeCreeper() {
      if (!this.level().isClientSide) {
         float $$0 = this.isPowered() ? 2.0F : 1.0F;
         this.dead = true;
         this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.explosionRadius * $$0, Level.ExplosionInteraction.MOB);
         this.spawnLingeringCloud();
         this.triggerOnDeathMobEffects(Entity.RemovalReason.KILLED);
         this.discard();
      }
   }

   private void spawnLingeringCloud() {
      Collection<MobEffectInstance> $$0 = this.getActiveEffects();
      if (!$$0.isEmpty()) {
         AreaEffectCloud $$1 = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
         $$1.setRadius(2.5F);
         $$1.setRadiusOnUse(-0.5F);
         $$1.setWaitTime(10);
         $$1.setDuration($$1.getDuration() / 2);
         $$1.setRadiusPerTick(-$$1.getRadius() / $$1.getDuration());

         for (MobEffectInstance $$2 : $$0) {
            $$1.addEffect(new MobEffectInstance($$2));
         }

         this.level().addFreshEntity($$1);
      }
   }

   public boolean isIgnited() {
      return this.entityData.get(DATA_IS_IGNITED);
   }

   public void ignite() {
      this.entityData.set(DATA_IS_IGNITED, true);
   }

   public boolean canDropMobsSkull() {
      return this.isPowered() && this.droppedSkulls < 1;
   }

   public void increaseDroppedSkulls() {
      this.droppedSkulls++;
   }
}
