package net.minecraft.world.entity.projectile.throwableitemprojectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class ThrownLingeringPotion extends AbstractThrownPotion {
   public ThrownLingeringPotion(EntityType<? extends ThrownLingeringPotion> p_480890_, Level p_478046_) {
      super(p_480890_, p_478046_);
   }

   public ThrownLingeringPotion(Level p_479074_, LivingEntity p_479085_, ItemStack p_480850_) {
      super(EntityType.LINGERING_POTION, p_479074_, p_479085_, p_480850_);
   }

   public ThrownLingeringPotion(Level p_481095_, double p_479367_, double p_480014_, double p_480932_, ItemStack p_478413_) {
      super(EntityType.LINGERING_POTION, p_481095_, p_479367_, p_480014_, p_480932_, p_478413_);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.LINGERING_POTION;
   }

   @Override
   public void onHitAsPotion(ServerLevel p_479772_, ItemStack p_481164_, HitResult p_478476_) {
      AreaEffectCloud areaeffectcloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
      if (this.getOwner() instanceof LivingEntity livingentity) {
         areaeffectcloud.setOwner(livingentity);
      }

      areaeffectcloud.setRadius(3.0F);
      areaeffectcloud.setRadiusOnUse(-0.5F);
      areaeffectcloud.setDuration(600);
      areaeffectcloud.setWaitTime(10);
      areaeffectcloud.setRadiusPerTick(-areaeffectcloud.getRadius() / areaeffectcloud.getDuration());
      areaeffectcloud.applyComponentsFromItemStack(p_481164_);
      p_479772_.addFreshEntity(areaeffectcloud);
   }
}
