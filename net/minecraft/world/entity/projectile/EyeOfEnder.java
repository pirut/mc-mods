package net.minecraft.world.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EyeOfEnder extends Entity implements ItemSupplier {
   private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(EyeOfEnder.class, EntityDataSerializers.ITEM_STACK);
   private double tx;
   private double ty;
   private double tz;
   private int life;
   private boolean surviveAfterDeath;

   public EyeOfEnder(EntityType<? extends EyeOfEnder> p_36957_, Level p_36958_) {
      super(p_36957_, p_36958_);
   }

   public EyeOfEnder(Level p_36960_, double p_36961_, double p_36962_, double p_36963_) {
      this(EntityType.EYE_OF_ENDER, p_36960_);
      this.setPos(p_36961_, p_36962_, p_36963_);
   }

   public void setItem(ItemStack p_36973_) {
      if (p_36973_.isEmpty()) {
         this.getEntityData().set(DATA_ITEM_STACK, this.getDefaultItem());
      } else {
         this.getEntityData().set(DATA_ITEM_STACK, p_36973_.copyWithCount(1));
      }
   }

   @Override
   public ItemStack getItem() {
      return this.getEntityData().get(DATA_ITEM_STACK);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_326310_) {
      p_326310_.define(DATA_ITEM_STACK, this.getDefaultItem());
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double p_36966_) {
      double $$1 = this.getBoundingBox().getSize() * 4.0;
      if (Double.isNaN($$1)) {
         $$1 = 4.0;
      }

      $$1 *= 64.0;
      return p_36966_ < $$1 * $$1;
   }

   public void signalTo(BlockPos p_36968_) {
      double $$1 = p_36968_.getX();
      int $$2 = p_36968_.getY();
      double $$3 = p_36968_.getZ();
      double $$4 = $$1 - this.getX();
      double $$5 = $$3 - this.getZ();
      double $$6 = Math.sqrt($$4 * $$4 + $$5 * $$5);
      if ($$6 > 12.0) {
         this.tx = this.getX() + $$4 / $$6 * 12.0;
         this.tz = this.getZ() + $$5 / $$6 * 12.0;
         this.ty = this.getY() + 8.0;
      } else {
         this.tx = $$1;
         this.ty = $$2;
         this.tz = $$3;
      }

      this.life = 0;
      this.surviveAfterDeath = this.random.nextInt(5) > 0;
   }

   @Override
   public void lerpMotion(double p_36984_, double p_36985_, double p_36986_) {
      this.setDeltaMovement(p_36984_, p_36985_, p_36986_);
      if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
         double $$3 = Math.sqrt(p_36984_ * p_36984_ + p_36986_ * p_36986_);
         this.setYRot((float)(Mth.atan2(p_36984_, p_36986_) * 180.0F / (float)Math.PI));
         this.setXRot((float)(Mth.atan2(p_36985_, $$3) * 180.0F / (float)Math.PI));
         this.yRotO = this.getYRot();
         this.xRotO = this.getXRot();
      }
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 $$0 = this.getDeltaMovement();
      double $$1 = this.getX() + $$0.x;
      double $$2 = this.getY() + $$0.y;
      double $$3 = this.getZ() + $$0.z;
      double $$4 = $$0.horizontalDistance();
      this.setXRot(Projectile.lerpRotation(this.xRotO, (float)(Mth.atan2($$0.y, $$4) * 180.0F / (float)Math.PI)));
      this.setYRot(Projectile.lerpRotation(this.yRotO, (float)(Mth.atan2($$0.x, $$0.z) * 180.0F / (float)Math.PI)));
      if (!this.level().isClientSide) {
         double $$5 = this.tx - $$1;
         double $$6 = this.tz - $$3;
         float $$7 = (float)Math.sqrt($$5 * $$5 + $$6 * $$6);
         float $$8 = (float)Mth.atan2($$6, $$5);
         double $$9 = Mth.lerp(0.0025, $$4, (double)$$7);
         double $$10 = $$0.y;
         if ($$7 < 1.0F) {
            $$9 *= 0.8;
            $$10 *= 0.8;
         }

         int $$11 = this.getY() < this.ty ? 1 : -1;
         $$0 = new Vec3(Math.cos($$8) * $$9, $$10 + ($$11 - $$10) * 0.015F, Math.sin($$8) * $$9);
         this.setDeltaMovement($$0);
      }

      float $$12 = 0.25F;
      if (this.isInWater()) {
         for (int $$13 = 0; $$13 < 4; $$13++) {
            this.level().addParticle(ParticleTypes.BUBBLE, $$1 - $$0.x * 0.25, $$2 - $$0.y * 0.25, $$3 - $$0.z * 0.25, $$0.x, $$0.y, $$0.z);
         }
      } else {
         this.level()
            .addParticle(
               ParticleTypes.PORTAL,
               $$1 - $$0.x * 0.25 + this.random.nextDouble() * 0.6 - 0.3,
               $$2 - $$0.y * 0.25 - 0.5,
               $$3 - $$0.z * 0.25 + this.random.nextDouble() * 0.6 - 0.3,
               $$0.x,
               $$0.y,
               $$0.z
            );
      }

      if (!this.level().isClientSide) {
         this.setPos($$1, $$2, $$3);
         this.life++;
         if (this.life > 80 && !this.level().isClientSide) {
            this.playSound(SoundEvents.ENDER_EYE_DEATH, 1.0F, 1.0F);
            this.discard();
            if (this.surviveAfterDeath) {
               this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), this.getItem()));
            } else {
               this.level().levelEvent(2003, this.blockPosition(), 0);
            }
         }
      } else {
         this.setPosRaw($$1, $$2, $$3);
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag p_36975_) {
      p_36975_.put("Item", this.getItem().save(this.registryAccess()));
   }

   @Override
   public void readAdditionalSaveData(CompoundTag p_36970_) {
      if (p_36970_.contains("Item", 10)) {
         this.setItem(ItemStack.parse(this.registryAccess(), p_36970_.getCompound("Item")).orElse(this.getDefaultItem()));
      } else {
         this.setItem(this.getDefaultItem());
      }
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
}
