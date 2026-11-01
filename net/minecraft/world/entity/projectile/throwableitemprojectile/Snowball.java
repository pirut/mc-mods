package net.minecraft.world.entity.projectile.throwableitemprojectile;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class Snowball extends ThrowableItemProjectile {
   public Snowball(EntityType<? extends Snowball> p_479866_, Level p_479213_) {
      super(p_479866_, p_479213_);
   }

   public Snowball(Level p_480251_, LivingEntity p_479227_, ItemStack p_480441_) {
      super(EntityType.SNOWBALL, p_479227_, p_480251_, p_480441_);
   }

   public Snowball(Level p_479112_, double p_479158_, double p_481202_, double p_481691_, ItemStack p_479097_) {
      super(EntityType.SNOWBALL, p_479158_, p_481202_, p_481691_, p_479112_, p_479097_);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.SNOWBALL;
   }

   private ParticleOptions getParticle() {
      ItemStack itemstack = this.getItem();
      return (ParticleOptions)(itemstack.isEmpty() ? ParticleTypes.ITEM_SNOWBALL : new ItemParticleOption(ParticleTypes.ITEM, itemstack));
   }

   @Override
   public void handleEntityEvent(byte p_479454_) {
      if (p_479454_ == 3) {
         ParticleOptions particleoptions = this.getParticle();

         for (int i = 0; i < 8; i++) {
            this.level().addParticle(particleoptions, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult p_481286_) {
      super.onHitEntity(p_481286_);
      Entity entity = p_481286_.getEntity();
      int i = entity instanceof Blaze ? 3 : 0;
      entity.hurt(this.damageSources().thrown(this, this.getOwner()), i);
   }

   @Override
   protected void onHit(HitResult p_480151_) {
      super.onHit(p_480151_);
      if (!this.level().isClientSide()) {
         this.level().broadcastEntityEvent(this, (byte)3);
         this.discard();
      }
   }
}
