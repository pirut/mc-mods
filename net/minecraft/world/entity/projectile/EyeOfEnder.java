package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EyeOfEnder extends Entity implements ItemSupplier {
   private static final float MIN_CAMERA_DISTANCE_SQUARED = 12.25F;
   private static final float TOO_FAR_SIGNAL_HEIGHT = 8.0F;
   private static final float TOO_FAR_DISTANCE = 12.0F;
   private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(EyeOfEnder.class, EntityDataSerializers.ITEM_STACK);
   @Nullable
   private Vec3 target;
   private int life;
   private boolean surviveAfterDeath;

   public EyeOfEnder(EntityType<? extends EyeOfEnder> entityType, Level level) {
      super(entityType, level);
   }

   public EyeOfEnder(Level level, double x, double y, double z) {
      this(EntityType.EYE_OF_ENDER, level);
      this.setPos(x, y, z);
   }

   public void setItem(ItemStack stack) {
      if (stack.isEmpty()) {
         this.getEntityData().set(DATA_ITEM_STACK, this.getDefaultItem());
      } else {
         this.getEntityData().set(DATA_ITEM_STACK, stack.copyWithCount(1));
      }
   }

   @Override
   public ItemStack getItem() {
      return this.getEntityData().get(DATA_ITEM_STACK);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(DATA_ITEM_STACK, this.getDefaultItem());
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      if (this.tickCount < 2 && distance < 12.25) {
         return false;
      } else {
         double d = this.getBoundingBox().getSize() * 4.0;
         if (Double.isNaN(d)) {
            d = 4.0;
         }

         d *= 64.0;
         return distance < d * d;
      }
   }

   public void signalTo(Vec3 pos) {
      Vec3 vec3 = pos.subtract(this.position());
      double d = vec3.horizontalDistance();
      if (d > 12.0) {
         this.target = this.position().add(vec3.x / d * 12.0, 8.0, vec3.z / d * 12.0);
      } else {
         this.target = pos;
      }

      this.life = 0;
      this.surviveAfterDeath = this.random.nextInt(5) > 0;
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 vec3 = this.position().add(this.getDeltaMovement());
      if (!this.level().isClientSide() && this.target != null) {
         this.setDeltaMovement(updateDeltaMovement(this.getDeltaMovement(), vec3, this.target));
      }

      if (this.level().isClientSide()) {
         Vec3 vec32 = vec3.subtract(this.getDeltaMovement().scale(0.25));
         this.spawnParticles(vec32, this.getDeltaMovement());
      }

      this.setPos(vec3);
      if (!this.level().isClientSide()) {
         this.life++;
         if (this.life > 80 && !this.level().isClientSide()) {
            this.playSound(SoundEvents.ENDER_EYE_DEATH, 1.0F, 1.0F);
            this.discard();
            if (this.surviveAfterDeath) {
               this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), this.getItem()));
            } else {
               this.level().levelEvent(2003, this.blockPosition(), 0);
            }
         }
      }
   }

   private void spawnParticles(Vec3 pos, Vec3 deltaMovement) {
      if (this.isInWater()) {
         for (int i = 0; i < 4; i++) {
            this.level().addParticle(ParticleTypes.BUBBLE, pos.x, pos.y, pos.z, deltaMovement.x, deltaMovement.y, deltaMovement.z);
         }
      } else {
         this.level()
            .addParticle(
               ParticleTypes.PORTAL,
               pos.x + this.random.nextDouble() * 0.6 - 0.3,
               pos.y - 0.5,
               pos.z + this.random.nextDouble() * 0.6 - 0.3,
               deltaMovement.x,
               deltaMovement.y,
               deltaMovement.z
            );
      }
   }

   private static Vec3 updateDeltaMovement(Vec3 deltaMovement, Vec3 pos, Vec3 target) {
      Vec3 vec3 = new Vec3(target.x - pos.x, 0.0, target.z - pos.z);
      double d = vec3.length();
      double e = Mth.lerp(0.0025, deltaMovement.horizontalDistance(), d);
      double f = deltaMovement.y;
      if (d < 1.0) {
         e *= 0.8;
         f *= 0.8;
      }

      double g = pos.y - deltaMovement.y < target.y ? 1.0 : -1.0;
      return vec3.scale(e / d).add(0.0, f + (g - f) * 0.015, 0.0);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      output.store("Item", ItemStack.CODEC, this.getItem());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      this.setItem(input.<ItemStack>read("Item", ItemStack.CODEC).orElse(this.getDefaultItem()));
   }

   private ItemStack getDefaultItem() {
      return new ItemStack(Items.ENDER_EYE);
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   @Override
   public boolean isAttackable() {
      return false;
   }

   @Override
   public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
      return false;
   }
}
