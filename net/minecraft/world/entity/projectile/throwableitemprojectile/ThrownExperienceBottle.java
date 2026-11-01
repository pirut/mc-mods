package net.minecraft.world.entity.projectile.throwableitemprojectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ThrownExperienceBottle extends ThrowableItemProjectile {
   public ThrownExperienceBottle(EntityType<? extends ThrownExperienceBottle> p_481008_, Level p_478988_) {
      super(p_481008_, p_478988_);
   }

   public ThrownExperienceBottle(Level p_480564_, LivingEntity p_478782_, ItemStack p_478054_) {
      super(EntityType.EXPERIENCE_BOTTLE, p_478782_, p_480564_, p_478054_);
   }

   public ThrownExperienceBottle(Level p_477961_, double p_482109_, double p_480380_, double p_480147_, ItemStack p_481142_) {
      super(EntityType.EXPERIENCE_BOTTLE, p_482109_, p_480380_, p_480147_, p_477961_, p_481142_);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.EXPERIENCE_BOTTLE;
   }

   @Override
   protected double getDefaultGravity() {
      return 0.07;
   }

   @Override
   protected void onHit(HitResult p_480361_) {
      super.onHit(p_480361_);
      if (this.level() instanceof ServerLevel serverlevel) {
         serverlevel.levelEvent(2002, this.blockPosition(), -13083194);
         int i = 3 + serverlevel.random.nextInt(5) + serverlevel.random.nextInt(5);
         if (p_480361_ instanceof BlockHitResult blockhitresult) {
            Vec3 vec3 = blockhitresult.getDirection().getUnitVec3();
            ExperienceOrb.awardWithDirection(serverlevel, p_480361_.getLocation(), vec3, i);
         } else {
            ExperienceOrb.awardWithDirection(serverlevel, p_480361_.getLocation(), this.getDeltaMovement().scale(-1.0), i);
         }

         this.discard();
      }
   }
}
