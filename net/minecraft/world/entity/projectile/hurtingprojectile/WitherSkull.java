package net.minecraft.world.entity.projectile.hurtingprojectile;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class WitherSkull extends AbstractHurtingProjectile {
   private static final EntityDataAccessor<Boolean> DATA_DANGEROUS = SynchedEntityData.defineId(WitherSkull.class, EntityDataSerializers.BOOLEAN);
   private static final boolean DEFAULT_DANGEROUS = false;

   public WitherSkull(EntityType<? extends WitherSkull> p_481759_, Level p_481927_) {
      super(p_481759_, p_481927_);
   }

   public WitherSkull(Level p_480249_, LivingEntity p_482160_, Vec3 p_479046_) {
      super(EntityType.WITHER_SKULL, p_482160_, p_479046_, p_480249_);
   }

   @Override
   protected float getInertia() {
      return this.isDangerous() ? 0.73F : super.getInertia();
   }

   @Override
   public boolean isOnFire() {
      return false;
   }

   @Override
   public float getBlockExplosionResistance(
      Explosion p_479858_, BlockGetter p_481750_, BlockPos p_480351_, BlockState p_480179_, FluidState p_480229_, float p_480544_
   ) {
      return this.isDangerous() && p_480179_.canEntityDestroy(p_481750_, p_480351_, this) ? Math.min(0.8F, p_480544_) : p_480544_;
   }

   @Override
   protected void onHitEntity(EntityHitResult p_480809_) {
      super.onHitEntity(p_480809_);
      if (this.level() instanceof ServerLevel serverlevel) {
         Entity entity = p_480809_.getEntity();
         boolean flag;
         if (this.getOwner() instanceof LivingEntity livingentity) {
            DamageSource damagesource = this.damageSources().witherSkull(this, livingentity);
            flag = entity.hurtServer(serverlevel, damagesource, 8.0F);
            if (flag) {
               if (entity.isAlive()) {
                  EnchantmentHelper.doPostAttackEffects(serverlevel, entity, damagesource);
               } else {
                  livingentity.heal(5.0F);
               }
            }
         } else {
            flag = entity.hurtServer(serverlevel, this.damageSources().magic(), 5.0F);
         }

         if (flag && entity instanceof LivingEntity livingentity1) {
            int i = 0;
            if (this.level().getDifficulty() == Difficulty.NORMAL) {
               i = 10;
            } else if (this.level().getDifficulty() == Difficulty.HARD) {
               i = 40;
            }

            if (i > 0) {
               livingentity1.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * i, 1), this.getEffectSource());
            }
         }
      }
   }

   @Override
   protected void onHit(HitResult p_478436_) {
      super.onHit(p_478436_);
      if (!this.level().isClientSide()) {
         this.level().explode(this, this.getX(), this.getY(), this.getZ(), 1.0F, false, Level.ExplosionInteraction.MOB);
         this.discard();
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_479198_) {
      p_479198_.define(DATA_DANGEROUS, false);
   }

   public boolean isDangerous() {
      return this.entityData.get(DATA_DANGEROUS);
   }

   public void setDangerous(boolean p_480368_) {
      this.entityData.set(DATA_DANGEROUS, p_480368_);
   }

   @Override
   protected boolean shouldBurn() {
      return false;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_479776_) {
      super.addAdditionalSaveData(p_479776_);
      p_479776_.putBoolean("dangerous", this.isDangerous());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_478691_) {
      super.readAdditionalSaveData(p_478691_);
      this.setDangerous(p_478691_.getBooleanOr("dangerous", false));
   }
}
