package net.minecraft.world.entity.monster.hoglin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public interface HoglinBase {
   int ATTACK_ANIMATION_DURATION = 10;
   float PROBABILITY_OF_SPAWNING_AS_BABY = 0.2F;

   int getAttackAnimationRemainingTicks();

   static boolean hurtAndThrowTarget(ServerLevel level, LivingEntity entity, LivingEntity target) {
      float f = (float)entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
      float g;
      if (!entity.isBaby() && (int)f > 0) {
         g = f / 2.0F + level.random.nextInt((int)f);
      } else {
         g = f;
      }

      DamageSource damageSource = entity.damageSources().mobAttack(entity);
      boolean bl = target.hurtServer(level, damageSource, g);
      if (bl) {
         EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
         if (!entity.isBaby()) {
            throwTarget(entity, target);
         }
      }

      return bl;
   }

   static void throwTarget(LivingEntity hoglin, LivingEntity target) {
      double d = hoglin.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
      double e = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
      double f = d - e;
      if (!(f <= 0.0)) {
         double g = target.getX() - hoglin.getX();
         double h = target.getZ() - hoglin.getZ();
         float i = hoglin.level().random.nextInt(21) - 10;
         double j = f * (hoglin.level().random.nextFloat() * 0.5F + 0.2F);
         Vec3 vec3 = new Vec3(g, 0.0, h).normalize().scale(j).yRot(i);
         double k = f * hoglin.level().random.nextFloat() * 0.5;
         target.push(vec3.x, k, vec3.z);
         target.hurtMarked = true;
      }
   }
}
