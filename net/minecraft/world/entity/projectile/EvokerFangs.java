package net.minecraft.world.entity.projectile;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public class EvokerFangs extends Entity implements TraceableEntity {
   public static final int ATTACK_DURATION = 20;
   public static final int LIFE_OFFSET = 2;
   public static final int ATTACK_TRIGGER_TICKS = 14;
   private int warmupDelayTicks;
   private boolean sentSpikeEvent;
   private int lifeTicks = 22;
   private boolean clientSideAttackStarted;
   @Nullable
   private LivingEntity owner;
   @Nullable
   private UUID ownerUUID;

   public EvokerFangs(EntityType<? extends EvokerFangs> p_36923_, Level p_36924_) {
      super(p_36923_, p_36924_);
   }

   public EvokerFangs(Level p_36926_, double p_36927_, double p_36928_, double p_36929_, float p_36930_, int p_36931_, LivingEntity p_36932_) {
      this(EntityType.EVOKER_FANGS, p_36926_);
      this.warmupDelayTicks = p_36931_;
      this.setOwner(p_36932_);
      this.setYRot(p_36930_ * (180.0F / (float)Math.PI));
      this.setPos(p_36927_, p_36928_, p_36929_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_326219_) {
   }

   public void setOwner(@Nullable LivingEntity p_36939_) {
      this.owner = p_36939_;
      this.ownerUUID = p_36939_ == null ? null : p_36939_.getUUID();
   }

   @Nullable
   public LivingEntity getOwner() {
      if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel) {
         Entity $$0 = ((ServerLevel)this.level()).getEntity(this.ownerUUID);
         if ($$0 instanceof LivingEntity) {
            this.owner = (LivingEntity)$$0;
         }
      }

      return this.owner;
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag p_36941_) {
      this.warmupDelayTicks = p_36941_.getInt("Warmup");
      if (p_36941_.hasUUID("Owner")) {
         this.ownerUUID = p_36941_.getUUID("Owner");
      }
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag p_36943_) {
      p_36943_.putInt("Warmup", this.warmupDelayTicks);
      if (this.ownerUUID != null) {
         p_36943_.putUUID("Owner", this.ownerUUID);
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide) {
         if (this.clientSideAttackStarted) {
            this.lifeTicks--;
            if (this.lifeTicks == 14) {
               for (int $$0 = 0; $$0 < 12; $$0++) {
                  double $$1 = this.getX() + (this.random.nextDouble() * 2.0 - 1.0) * this.getBbWidth() * 0.5;
                  double $$2 = this.getY() + 0.05 + this.random.nextDouble();
                  double $$3 = this.getZ() + (this.random.nextDouble() * 2.0 - 1.0) * this.getBbWidth() * 0.5;
                  double $$4 = (this.random.nextDouble() * 2.0 - 1.0) * 0.3;
                  double $$5 = 0.3 + this.random.nextDouble() * 0.3;
                  double $$6 = (this.random.nextDouble() * 2.0 - 1.0) * 0.3;
                  this.level().addParticle(ParticleTypes.CRIT, $$1, $$2 + 1.0, $$3, $$4, $$5, $$6);
               }
            }
         }
      } else if (--this.warmupDelayTicks < 0) {
         if (this.warmupDelayTicks == -8) {
            for (LivingEntity $$8 : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.2, 0.0, 0.2))) {
               this.dealDamageTo($$8);
            }
         }

         if (!this.sentSpikeEvent) {
            this.level().broadcastEntityEvent(this, (byte)4);
            this.sentSpikeEvent = true;
         }

         if (--this.lifeTicks < 0) {
            this.discard();
         }
      }
   }

   private void dealDamageTo(LivingEntity p_36945_) {
      LivingEntity $$1 = this.getOwner();
      if (p_36945_.isAlive() && !p_36945_.isInvulnerable() && p_36945_ != $$1) {
         if ($$1 == null) {
            p_36945_.hurt(this.damageSources().magic(), 6.0F);
         } else {
            if ($$1.isAlliedTo(p_36945_)) {
               return;
            }

            DamageSource $$2 = this.damageSources().indirectMagic(this, $$1);
            if (p_36945_.hurt($$2, 6.0F) && this.level() instanceof ServerLevel $$3) {
               EnchantmentHelper.doPostAttackEffects($$3, p_36945_, $$2);
            }
         }
      }
   }

   @Override
   public void handleEntityEvent(byte p_36935_) {
      super.handleEntityEvent(p_36935_);
      if (p_36935_ == 4) {
         this.clientSideAttackStarted = true;
         if (!this.isSilent()) {
            this.level()
               .playLocalSound(
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  SoundEvents.EVOKER_FANGS_ATTACK,
                  this.getSoundSource(),
                  1.0F,
                  this.random.nextFloat() * 0.2F + 0.85F,
                  false
               );
         }
      }
   }

   public float getAnimationProgress(float p_36937_) {
      if (!this.clientSideAttackStarted) {
         return 0.0F;
      } else {
         int $$1 = this.lifeTicks - 2;
         return $$1 <= 0 ? 1.0F : 1.0F - ($$1 - p_36937_) / 20.0F;
      }
   }
}
