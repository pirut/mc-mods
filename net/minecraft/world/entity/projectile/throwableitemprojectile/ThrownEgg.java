package net.minecraft.world.entity.projectile.throwableitemprojectile;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownEgg extends ThrowableItemProjectile {
   private static final EntityDimensions ZERO_SIZED_DIMENSIONS = EntityDimensions.fixed(0.0F, 0.0F);

   public ThrownEgg(EntityType<? extends ThrownEgg> p_478861_, Level p_478341_) {
      super(p_478861_, p_478341_);
   }

   public ThrownEgg(Level p_478858_, LivingEntity p_481401_, ItemStack p_478487_) {
      super(EntityType.EGG, p_481401_, p_478858_, p_478487_);
   }

   public ThrownEgg(Level p_478584_, double p_479175_, double p_480677_, double p_478835_, ItemStack p_479665_) {
      super(EntityType.EGG, p_479175_, p_480677_, p_478835_, p_478584_, p_479665_);
   }

   @Override
   public void handleEntityEvent(byte p_481529_) {
      if (p_481529_ == 3) {
         double d0 = 0.08;

         for (int i = 0; i < 8; i++) {
            this.level()
               .addParticle(
                  new ItemParticleOption(ParticleTypes.ITEM, this.getItem()),
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  (this.random.nextFloat() - 0.5) * 0.08,
                  (this.random.nextFloat() - 0.5) * 0.08,
                  (this.random.nextFloat() - 0.5) * 0.08
               );
         }
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult p_479755_) {
      super.onHitEntity(p_479755_);
      p_479755_.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
   }

   @Override
   protected void onHit(HitResult p_478775_) {
      super.onHit(p_478775_);
      if (!this.level().isClientSide()) {
         if (this.random.nextInt(8) == 0) {
            int i = 1;
            if (this.random.nextInt(32) == 0) {
               i = 4;
            }

            for (int j = 0; j < i; j++) {
               Chicken chicken = EntityType.CHICKEN.create(this.level(), EntitySpawnReason.TRIGGERED);
               if (chicken != null) {
                  chicken.setAge(-24000);
                  chicken.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                  Optional.ofNullable(this.getItem().get(DataComponents.CHICKEN_VARIANT))
                     .flatMap(p_478195_ -> p_478195_.unwrap(this.registryAccess()))
                     .ifPresent(chicken::setVariant);
                  if (!chicken.fudgePositionAfterSizeChange(ZERO_SIZED_DIMENSIONS)) {
                     break;
                  }

                  this.level().addFreshEntity(chicken);
               }
            }
         }

         this.level().broadcastEntityEvent(this, (byte)3);
         this.discard();
      }
   }

   @Override
   protected Item getDefaultItem() {
      return Items.EGG;
   }
}
