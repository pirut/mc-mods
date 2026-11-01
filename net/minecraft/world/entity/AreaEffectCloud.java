package net.minecraft.world.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import org.slf4j.Logger;

public class AreaEffectCloud extends Entity implements TraceableEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int TIME_BETWEEN_APPLICATIONS = 5;
   private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> DATA_WAITING = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<ParticleOptions> DATA_PARTICLE = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.PARTICLE);
   private static final float MAX_RADIUS = 32.0F;
   private static final float MINIMAL_RADIUS = 0.5F;
   private static final float DEFAULT_RADIUS = 3.0F;
   public static final float DEFAULT_WIDTH = 6.0F;
   public static final float HEIGHT = 0.5F;
   private PotionContents potionContents = PotionContents.EMPTY;
   private final Map<Entity, Integer> victims = Maps.newHashMap();
   private int duration = 600;
   private int waitTime = 20;
   private int reapplicationDelay = 20;
   private int durationOnUse;
   private float radiusOnUse;
   private float radiusPerTick;
   @Nullable
   private LivingEntity owner;
   @Nullable
   private UUID ownerUUID;

   public AreaEffectCloud(EntityType<? extends AreaEffectCloud> p_19704_, Level p_19705_) {
      super(p_19704_, p_19705_);
      this.noPhysics = true;
   }

   public AreaEffectCloud(Level p_19707_, double p_19708_, double p_19709_, double p_19710_) {
      this(EntityType.AREA_EFFECT_CLOUD, p_19707_);
      this.setPos(p_19708_, p_19709_, p_19710_);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_326332_) {
      p_326332_.define(DATA_RADIUS, 3.0F);
      p_326332_.define(DATA_WAITING, false);
      p_326332_.define(DATA_PARTICLE, ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, -1));
   }

   public void setRadius(float p_19713_) {
      if (!this.level().isClientSide) {
         this.getEntityData().set(DATA_RADIUS, Mth.clamp(p_19713_, 0.0F, 32.0F));
      }
   }

   @Override
   public void refreshDimensions() {
      double $$0 = this.getX();
      double $$1 = this.getY();
      double $$2 = this.getZ();
      super.refreshDimensions();
      this.setPos($$0, $$1, $$2);
   }

   public float getRadius() {
      return this.getEntityData().get(DATA_RADIUS);
   }

   public void setPotionContents(PotionContents p_330869_) {
      this.potionContents = p_330869_;
      this.updateColor();
   }

   private void updateColor() {
      ParticleOptions $$0 = this.entityData.get(DATA_PARTICLE);
      if ($$0 instanceof ColorParticleOption $$1) {
         int $$2 = this.potionContents.equals(PotionContents.EMPTY) ? 0 : this.potionContents.getColor();
         this.entityData.set(DATA_PARTICLE, ColorParticleOption.create($$1.getType(), FastColor.ARGB32.opaque($$2)));
      }
   }

   public void addEffect(MobEffectInstance p_19717_) {
      this.setPotionContents(this.potionContents.withEffectAdded(p_19717_));
   }

   public ParticleOptions getParticle() {
      return this.getEntityData().get(DATA_PARTICLE);
   }

   public void setParticle(ParticleOptions p_19725_) {
      this.getEntityData().set(DATA_PARTICLE, p_19725_);
   }

   protected void setWaiting(boolean p_19731_) {
      this.getEntityData().set(DATA_WAITING, p_19731_);
   }

   public boolean isWaiting() {
      return this.getEntityData().get(DATA_WAITING);
   }

   public int getDuration() {
      return this.duration;
   }

   public void setDuration(int p_19735_) {
      this.duration = p_19735_;
   }

   @Override
   public void tick() {
      super.tick();
      boolean $$0 = this.isWaiting();
      float $$1 = this.getRadius();
      if (this.level().isClientSide) {
         if ($$0 && this.random.nextBoolean()) {
            return;
         }

         ParticleOptions $$2 = this.getParticle();
         int $$3;
         float $$4;
         if ($$0) {
            $$3 = 2;
            $$4 = 0.2F;
         } else {
            $$3 = Mth.ceil((float) Math.PI * $$1 * $$1);
            $$4 = $$1;
         }

         for (int $$7 = 0; $$7 < $$3; $$7++) {
            float $$8 = this.random.nextFloat() * (float) (Math.PI * 2);
            float $$9 = Mth.sqrt(this.random.nextFloat()) * $$4;
            double $$10 = this.getX() + Mth.cos($$8) * $$9;
            double $$11 = this.getY();
            double $$12 = this.getZ() + Mth.sin($$8) * $$9;
            if ($$2.getType() == ParticleTypes.ENTITY_EFFECT) {
               if ($$0 && this.random.nextBoolean()) {
                  this.level().addAlwaysVisibleParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, -1), $$10, $$11, $$12, 0.0, 0.0, 0.0);
               } else {
                  this.level().addAlwaysVisibleParticle($$2, $$10, $$11, $$12, 0.0, 0.0, 0.0);
               }
            } else if ($$0) {
               this.level().addAlwaysVisibleParticle($$2, $$10, $$11, $$12, 0.0, 0.0, 0.0);
            } else {
               this.level()
                  .addAlwaysVisibleParticle($$2, $$10, $$11, $$12, (0.5 - this.random.nextDouble()) * 0.15, 0.01F, (0.5 - this.random.nextDouble()) * 0.15);
            }
         }
      } else {
         if (this.tickCount >= this.waitTime + this.duration) {
            this.discard();
            return;
         }

         boolean $$13 = this.tickCount < this.waitTime;
         if ($$0 != $$13) {
            this.setWaiting($$13);
         }

         if ($$13) {
            return;
         }

         if (this.radiusPerTick != 0.0F) {
            $$1 += this.radiusPerTick;
            if ($$1 < 0.5F) {
               this.discard();
               return;
            }

            this.setRadius($$1);
         }

         if (this.tickCount % 5 == 0) {
            this.victims.entrySet().removeIf(p_287380_ -> this.tickCount >= p_287380_.getValue());
            if (!this.potionContents.hasEffects()) {
               this.victims.clear();
            } else {
               List<MobEffectInstance> $$14 = Lists.newArrayList();
               if (this.potionContents.potion().isPresent()) {
                  for (MobEffectInstance $$15 : this.potionContents.potion().get().value().getEffects()) {
                     $$14.add(
                        new MobEffectInstance(
                           $$15.getEffect(), $$15.mapDuration(p_267926_ -> p_267926_ / 4), $$15.getAmplifier(), $$15.isAmbient(), $$15.isVisible()
                        )
                     );
                  }
               }

               $$14.addAll(this.potionContents.customEffects());
               List<LivingEntity> $$16 = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox());
               if (!$$16.isEmpty()) {
                  for (LivingEntity $$17 : $$16) {
                     if (!this.victims.containsKey($$17) && $$17.isAffectedByPotions() && !$$14.stream().noneMatch($$17::canBeAffected)) {
                        double $$18 = $$17.getX() - this.getX();
                        double $$19 = $$17.getZ() - this.getZ();
                        double $$20 = $$18 * $$18 + $$19 * $$19;
                        if ($$20 <= $$1 * $$1) {
                           this.victims.put($$17, this.tickCount + this.reapplicationDelay);

                           for (MobEffectInstance $$21 : $$14) {
                              if ($$21.getEffect().value().isInstantenous()) {
                                 $$21.getEffect().value().applyInstantenousEffect(this, this.getOwner(), $$17, $$21.getAmplifier(), 0.5);
                              } else {
                                 $$17.addEffect(new MobEffectInstance($$21), this);
                              }
                           }

                           if (this.radiusOnUse != 0.0F) {
                              $$1 += this.radiusOnUse;
                              if ($$1 < 0.5F) {
                                 this.discard();
                                 return;
                              }

                              this.setRadius($$1);
                           }

                           if (this.durationOnUse != 0) {
                              this.duration = this.duration + this.durationOnUse;
                              if (this.duration <= 0) {
                                 this.discard();
                                 return;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public float getRadiusOnUse() {
      return this.radiusOnUse;
   }

   public void setRadiusOnUse(float p_19733_) {
      this.radiusOnUse = p_19733_;
   }

   public float getRadiusPerTick() {
      return this.radiusPerTick;
   }

   public void setRadiusPerTick(float p_19739_) {
      this.radiusPerTick = p_19739_;
   }

   public int getDurationOnUse() {
      return this.durationOnUse;
   }

   public void setDurationOnUse(int p_146786_) {
      this.durationOnUse = p_146786_;
   }

   public int getWaitTime() {
      return this.waitTime;
   }

   public void setWaitTime(int p_19741_) {
      this.waitTime = p_19741_;
   }

   public void setOwner(@Nullable LivingEntity p_19719_) {
      this.owner = p_19719_;
      this.ownerUUID = p_19719_ == null ? null : p_19719_.getUUID();
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
   protected void readAdditionalSaveData(CompoundTag p_19727_) {
      this.tickCount = p_19727_.getInt("Age");
      this.duration = p_19727_.getInt("Duration");
      this.waitTime = p_19727_.getInt("WaitTime");
      this.reapplicationDelay = p_19727_.getInt("ReapplicationDelay");
      this.durationOnUse = p_19727_.getInt("DurationOnUse");
      this.radiusOnUse = p_19727_.getFloat("RadiusOnUse");
      this.radiusPerTick = p_19727_.getFloat("RadiusPerTick");
      this.setRadius(p_19727_.getFloat("Radius"));
      if (p_19727_.hasUUID("Owner")) {
         this.ownerUUID = p_19727_.getUUID("Owner");
      }

      RegistryOps<Tag> $$1 = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
      if (p_19727_.contains("Particle", 10)) {
         ParticleTypes.CODEC
            .parse($$1, p_19727_.get("Particle"))
            .resultOrPartial(p_329991_ -> LOGGER.warn("Failed to parse area effect cloud particle options: '{}'", p_329991_))
            .ifPresent(this::setParticle);
      }

      if (p_19727_.contains("potion_contents")) {
         PotionContents.CODEC
            .parse($$1, p_19727_.get("potion_contents"))
            .resultOrPartial(p_340707_ -> LOGGER.warn("Failed to parse area effect cloud potions: '{}'", p_340707_))
            .ifPresent(this::setPotionContents);
      }
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag p_19737_) {
      p_19737_.putInt("Age", this.tickCount);
      p_19737_.putInt("Duration", this.duration);
      p_19737_.putInt("WaitTime", this.waitTime);
      p_19737_.putInt("ReapplicationDelay", this.reapplicationDelay);
      p_19737_.putInt("DurationOnUse", this.durationOnUse);
      p_19737_.putFloat("RadiusOnUse", this.radiusOnUse);
      p_19737_.putFloat("RadiusPerTick", this.radiusPerTick);
      p_19737_.putFloat("Radius", this.getRadius());
      RegistryOps<Tag> $$1 = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
      p_19737_.put("Particle", (Tag)ParticleTypes.CODEC.encodeStart($$1, this.getParticle()).getOrThrow());
      if (this.ownerUUID != null) {
         p_19737_.putUUID("Owner", this.ownerUUID);
      }

      if (!this.potionContents.equals(PotionContents.EMPTY)) {
         Tag $$2 = (Tag)PotionContents.CODEC.encodeStart($$1, this.potionContents).getOrThrow();
         p_19737_.put("potion_contents", $$2);
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> p_19729_) {
      if (DATA_RADIUS.equals(p_19729_)) {
         this.refreshDimensions();
      }

      super.onSyncedDataUpdated(p_19729_);
   }

   @Override
   public PushReaction getPistonPushReaction() {
      return PushReaction.IGNORE;
   }

   @Override
   public EntityDimensions getDimensions(Pose p_19721_) {
      return EntityDimensions.scalable(this.getRadius() * 2.0F, 0.5F);
   }
}
