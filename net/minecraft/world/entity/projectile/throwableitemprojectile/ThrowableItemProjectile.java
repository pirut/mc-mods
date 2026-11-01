package net.minecraft.world.entity.projectile.throwableitemprojectile;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class ThrowableItemProjectile extends ThrowableProjectile implements ItemSupplier {
   private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(
      ThrowableItemProjectile.class, EntityDataSerializers.ITEM_STACK
   );

   public ThrowableItemProjectile(EntityType<? extends ThrowableItemProjectile> p_480397_, Level p_479814_) {
      super(p_480397_, p_479814_);
   }

   public ThrowableItemProjectile(
      EntityType<? extends ThrowableItemProjectile> p_479257_, double p_478602_, double p_478795_, double p_481997_, Level p_480225_, ItemStack p_480063_
   ) {
      super(p_479257_, p_478602_, p_478795_, p_481997_, p_480225_);
      this.setItem(p_480063_);
   }

   public ThrowableItemProjectile(EntityType<? extends ThrowableItemProjectile> p_479434_, LivingEntity p_480155_, Level p_480641_, ItemStack p_481187_) {
      this(p_479434_, p_480155_.getX(), p_480155_.getEyeY() - 0.1F, p_480155_.getZ(), p_480641_, p_481187_);
      this.setOwner(p_480155_);
   }

   public void setItem(ItemStack p_481842_) {
      this.getEntityData().set(DATA_ITEM_STACK, p_481842_.copyWithCount(1));
   }

   protected abstract Item getDefaultItem();

   @Override
   public ItemStack getItem() {
      return this.getEntityData().get(DATA_ITEM_STACK);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_481454_) {
      p_481454_.define(DATA_ITEM_STACK, new ItemStack(this.getDefaultItem()));
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_481398_) {
      super.addAdditionalSaveData(p_481398_);
      p_481398_.store("Item", ItemStack.CODEC, this.getItem());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_481766_) {
      super.readAdditionalSaveData(p_481766_);
      this.setItem(p_481766_.<ItemStack>read("Item", ItemStack.CODEC).orElseGet(() -> new ItemStack(this.getDefaultItem())));
   }
}
