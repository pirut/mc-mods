package net.minecraft.world.entity;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

public class OminousItemSpawner extends Entity {
   private static final int SPAWN_ITEM_DELAY_MIN = 60;
   private static final int SPAWN_ITEM_DELAY_MAX = 120;
   private static final String TAG_SPAWN_ITEM_AFTER_TICKS = "spawn_item_after_ticks";
   private static final String TAG_ITEM = "item";
   private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(OminousItemSpawner.class, EntityDataSerializers.ITEM_STACK);
   public static final int TICKS_BEFORE_ABOUT_TO_SPAWN_SOUND = 36;
   private long spawnItemAfterTicks;

   public OminousItemSpawner(EntityType<? extends OminousItemSpawner> p_338198_, Level p_338269_) {
      super(p_338198_, p_338269_);
      this.noPhysics = true;
   }

   public static OminousItemSpawner create(Level p_338234_, ItemStack p_338571_) {
      OminousItemSpawner $$2 = new OminousItemSpawner(EntityType.OMINOUS_ITEM_SPAWNER, p_338234_);
      $$2.spawnItemAfterTicks = p_338234_.random.nextIntBetweenInclusive(60, 120);
      $$2.setItem(p_338571_);
      return $$2;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide) {
         this.tickClient();
      } else {
         this.tickServer();
      }
   }

   private void tickServer() {
      if (this.tickCount == this.spawnItemAfterTicks - 36L) {
         this.level().playSound(null, this.blockPosition(), SoundEvents.TRIAL_SPAWNER_ABOUT_TO_SPAWN_ITEM, SoundSource.NEUTRAL);
      }

      if (this.tickCount >= this.spawnItemAfterTicks) {
         this.spawnItem();
         this.kill();
      }
   }

   private void tickClient() {
      if (this.level().getGameTime() % 5L == 0L) {
         this.addParticles();
      }
   }

   private void spawnItem() {
      Level $$0 = this.level();
      ItemStack $$1 = this.getItem();
      if (!$$1.isEmpty()) {
         Entity $$6;
         if ($$1.getItem() instanceof ProjectileItem $$2) {
            Direction $$3 = Direction.DOWN;
            Projectile $$4 = $$2.asProjectile($$0, this.position(), $$1, $$3);
            $$4.setOwner(this);
            ProjectileItem.DispenseConfig $$5 = $$2.createDispenseConfig();
            $$2.shoot($$4, $$3.getStepX(), $$3.getStepY(), $$3.getStepZ(), $$5.power(), $$5.uncertainty());
            $$5.overrideDispenseEvent().ifPresent(p_352709_ -> $$0.levelEvent(p_352709_, this.blockPosition(), 0));
            $$6 = $$4;
         } else {
            $$6 = new ItemEntity($$0, this.getX(), this.getY(), this.getZ(), $$1);
         }

         $$0.addFreshEntity($$6);
         $$0.levelEvent(3021, this.blockPosition(), 1);
         $$0.gameEvent($$6, GameEvent.ENTITY_PLACE, this.position());
         this.setItem(ItemStack.EMPTY);
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_338496_) {
      p_338496_.define(DATA_ITEM, ItemStack.EMPTY);
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag p_338507_) {
      ItemStack $$1 = p_338507_.contains("item", 10)
         ? ItemStack.parse(this.registryAccess(), p_338507_.getCompound("item")).orElse(ItemStack.EMPTY)
         : ItemStack.EMPTY;
      this.setItem($$1);
      this.spawnItemAfterTicks = p_338507_.getLong("spawn_item_after_ticks");
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag p_338411_) {
      if (!this.getItem().isEmpty()) {
         p_338411_.put("item", this.getItem().save(this.registryAccess()).copy());
      }

      p_338411_.putLong("spawn_item_after_ticks", this.spawnItemAfterTicks);
   }

   @Override
   protected boolean canAddPassenger(Entity p_338282_) {
      return false;
   }

   @Override
   protected boolean couldAcceptPassenger() {
      return false;
   }

   @Override
   protected void addPassenger(Entity p_338681_) {
      throw new IllegalStateException("Should never addPassenger without checking couldAcceptPassenger()");
   }

   @Override
   public PushReaction getPistonPushReaction() {
      return PushReaction.IGNORE;
   }

   @Override
   public boolean isIgnoringBlockTriggers() {
      return true;
   }

   public void addParticles() {
      Vec3 $$0 = this.position();
      int $$1 = this.random.nextIntBetweenInclusive(1, 3);

      for (int $$2 = 0; $$2 < $$1; $$2++) {
         double $$3 = 0.4;
         Vec3 $$4 = new Vec3(
            this.getX() + 0.4 * (this.random.nextGaussian() - this.random.nextGaussian()),
            this.getY() + 0.4 * (this.random.nextGaussian() - this.random.nextGaussian()),
            this.getZ() + 0.4 * (this.random.nextGaussian() - this.random.nextGaussian())
         );
         Vec3 $$5 = $$0.vectorTo($$4);
         this.level().addParticle(ParticleTypes.OMINOUS_SPAWNING, $$0.x(), $$0.y(), $$0.z(), $$5.x(), $$5.y(), $$5.z());
      }
   }

   public ItemStack getItem() {
      return this.getEntityData().get(DATA_ITEM);
   }

   private void setItem(ItemStack p_338789_) {
      this.getEntityData().set(DATA_ITEM, p_338789_);
   }
}
