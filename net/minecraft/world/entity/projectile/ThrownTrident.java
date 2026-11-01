package net.minecraft.world.entity.projectile;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class ThrownTrident extends AbstractArrow {
   private static final EntityDataAccessor<Byte> ID_LOYALTY = SynchedEntityData.defineId(ThrownTrident.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Boolean> ID_FOIL = SynchedEntityData.defineId(ThrownTrident.class, EntityDataSerializers.BOOLEAN);
   private boolean dealtDamage;
   public int clientSideReturnTridentTickCount;

   public ThrownTrident(EntityType<? extends ThrownTrident> p_37561_, Level p_37562_) {
      super(p_37561_, p_37562_);
   }

   public ThrownTrident(Level p_37569_, LivingEntity p_37570_, ItemStack p_37571_) {
      super(EntityType.TRIDENT, p_37570_, p_37569_, p_37571_, null);
      this.entityData.set(ID_LOYALTY, this.getLoyaltyFromItem(p_37571_));
      this.entityData.set(ID_FOIL, p_37571_.hasFoil());
   }

   public ThrownTrident(Level p_338686_, double p_338771_, double p_338674_, double p_338477_, ItemStack p_338255_) {
      super(EntityType.TRIDENT, p_338771_, p_338674_, p_338477_, p_338686_, p_338255_, p_338255_);
      this.entityData.set(ID_LOYALTY, this.getLoyaltyFromItem(p_338255_));
      this.entityData.set(ID_FOIL, p_338255_.hasFoil());
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_326249_) {
      super.defineSynchedData(p_326249_);
      p_326249_.define(ID_LOYALTY, (byte)0);
      p_326249_.define(ID_FOIL, false);
   }

   @Override
   public void tick() {
      if (this.inGroundTime > 4) {
         this.dealtDamage = true;
      }

      Entity $$0 = this.getOwner();
      int $$1 = this.entityData.get(ID_LOYALTY);
      if ($$1 > 0 && (this.dealtDamage || this.isNoPhysics()) && $$0 != null) {
         if (!this.isAcceptibleReturnOwner()) {
            if (!this.level().isClientSide && this.pickup == AbstractArrow.Pickup.ALLOWED) {
               this.spawnAtLocation(this.getPickupItem(), 0.1F);
            }

            this.discard();
         } else {
            this.setNoPhysics(true);
            Vec3 $$2 = $$0.getEyePosition().subtract(this.position());
            this.setPosRaw(this.getX(), this.getY() + $$2.y * 0.015 * $$1, this.getZ());
            if (this.level().isClientSide) {
               this.yOld = this.getY();
            }

            double $$3 = 0.05 * $$1;
            this.setDeltaMovement(this.getDeltaMovement().scale(0.95).add($$2.normalize().scale($$3)));
            if (this.clientSideReturnTridentTickCount == 0) {
               this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
            }

            this.clientSideReturnTridentTickCount++;
         }
      }

      super.tick();
   }

   private boolean isAcceptibleReturnOwner() {
      Entity $$0 = this.getOwner();
      return $$0 == null || !$$0.isAlive() ? false : !($$0 instanceof ServerPlayer) || !$$0.isSpectator();
   }

   public boolean isFoil() {
      return this.entityData.get(ID_FOIL);
   }

   @Nullable
   @Override
   protected EntityHitResult findHitEntity(Vec3 p_37575_, Vec3 p_37576_) {
      return this.dealtDamage ? null : super.findHitEntity(p_37575_, p_37576_);
   }

   @Override
   protected void onHitEntity(EntityHitResult p_37573_) {
      Entity $$1 = p_37573_.getEntity();
      float $$2 = 8.0F;
      Entity $$3 = this.getOwner();
      DamageSource $$4 = this.damageSources().trident(this, (Entity)($$3 == null ? this : $$3));
      if (this.level() instanceof ServerLevel $$5) {
         $$2 = EnchantmentHelper.modifyDamage($$5, this.getWeaponItem(), $$1, $$4, $$2);
      }

      this.dealtDamage = true;
      if ($$1.hurt($$4, $$2)) {
         if ($$1.getType() == EntityType.ENDERMAN) {
            return;
         }

         if (this.level() instanceof ServerLevel $$6) {
            EnchantmentHelper.doPostAttackEffectsWithItemSource($$6, $$1, $$4, this.getWeaponItem());
         }

         if ($$1 instanceof LivingEntity $$7) {
            this.doKnockback($$7, $$4);
            this.doPostHurtEffects($$7);
         }
      }

      this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01, -0.1, -0.01));
      this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
   }

   @Override
   protected void hitBlockEnchantmentEffects(ServerLevel p_344953_, BlockHitResult p_346320_, ItemStack p_344999_) {
      Vec3 $$3 = p_346320_.getBlockPos().clampLocationWithin(p_346320_.getLocation());
      EnchantmentHelper.onHitBlock(
         p_344953_,
         p_344999_,
         this.getOwner() instanceof LivingEntity $$4 ? $$4 : null,
         this,
         null,
         $$3,
         p_344953_.getBlockState(p_346320_.getBlockPos()),
         p_348680_ -> this.kill()
      );
   }

   @Override
   public ItemStack getWeaponItem() {
      return this.getPickupItemStackOrigin();
   }

   @Override
   protected boolean tryPickup(Player p_150196_) {
      return super.tryPickup(p_150196_) || this.isNoPhysics() && this.ownedBy(p_150196_) && p_150196_.getInventory().add(this.getPickupItem());
   }

   @Override
   protected ItemStack getDefaultPickupItem() {
      return new ItemStack(Items.TRIDENT);
   }

   @Override
   protected SoundEvent getDefaultHitGroundSoundEvent() {
      return SoundEvents.TRIDENT_HIT_GROUND;
   }

   @Override
   public void playerTouch(Player p_37580_) {
      if (this.ownedBy(p_37580_) || this.getOwner() == null) {
         super.playerTouch(p_37580_);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag p_37578_) {
      super.readAdditionalSaveData(p_37578_);
      this.dealtDamage = p_37578_.getBoolean("DealtDamage");
      this.entityData.set(ID_LOYALTY, this.getLoyaltyFromItem(this.getPickupItemStackOrigin()));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag p_37582_) {
      super.addAdditionalSaveData(p_37582_);
      p_37582_.putBoolean("DealtDamage", this.dealtDamage);
   }

   private byte getLoyaltyFromItem(ItemStack p_345571_) {
      return this.level() instanceof ServerLevel $$1 ? (byte)Mth.clamp(EnchantmentHelper.getTridentReturnToOwnerAcceleration($$1, p_345571_, this), 0, 127) : 0;
   }

   @Override
   public void tickDespawn() {
      int $$0 = this.entityData.get(ID_LOYALTY);
      if (this.pickup != AbstractArrow.Pickup.ALLOWED || $$0 <= 0) {
         super.tickDespawn();
      }
   }

   @Override
   protected float getWaterInertia() {
      return 0.99F;
   }

   @Override
   public boolean shouldRender(double p_37588_, double p_37589_, double p_37590_) {
      return true;
   }
}
