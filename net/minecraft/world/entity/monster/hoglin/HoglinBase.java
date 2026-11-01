package net.minecraft.world.entity.monster.hoglin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public interface HoglinBase {
   int ATTACK_ANIMATION_DURATION = 10;

   int getAttackAnimationRemainingTicks();

   static boolean hurtAndThrowTarget(LivingEntity p_34643_, LivingEntity p_34644_) {
      float $$2 = (float)p_34643_.getAttributeValue(Attributes.ATTACK_DAMAGE);
      float $$3;
      if (!p_34643_.isBaby() && (int)$$2 > 0) {
         $$3 = $$2 / 2.0F + p_34643_.level().random.nextInt((int)$$2);
      } else {
         $$3 = $$2;
      }

      DamageSource $$5 = p_34643_.damageSources().mobAttack(p_34643_);
      boolean $$6 = p_34644_.hurt($$5, $$3);
      if ($$6) {
         if (p_34643_.level() instanceof ServerLevel $$7) {
            EnchantmentHelper.doPostAttackEffects($$7, p_34644_, $$5);
         }

         if (!p_34643_.isBaby()) {
            throwTarget(p_34643_, p_34644_);
         }
      }

      return $$6;
   }

   static void throwTarget(LivingEntity p_34646_, LivingEntity p_34647_) {
      double $$2 = p_34646_.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
      double $$3 = p_34647_.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
      double $$4 = $$2 - $$3;
      if (!($$4 <= 0.0)) {
         double $$5 = p_34647_.getX() - p_34646_.getX();
         double $$6 = p_34647_.getZ() - p_34646_.getZ();
         float $$7 = p_34646_.level().random.nextInt(21) - 10;
         double $$8 = $$4 * (p_34646_.level().random.nextFloat() * 0.5F + 0.2F);
         Vec3 $$9 = new Vec3($$5, 0.0, $$6).normalize().scale($$8).yRot($$7);
         double $$10 = $$4 * p_34646_.level().random.nextFloat() * 0.5;
         p_34647_.push($$9.x, $$10, $$9.z);
         p_34647_.hurtMarked = true;
      }
   }
}
