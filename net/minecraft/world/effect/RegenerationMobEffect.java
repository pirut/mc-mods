package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

class RegenerationMobEffect extends MobEffect {
   protected RegenerationMobEffect(MobEffectCategory mobEffectCategory, int i) {
      super(mobEffectCategory, i);
   }

   @Override
   public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
      if (entity.getHealth() < entity.getMaxHealth()) {
         entity.heal(1.0F);
      }

      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      int i = 50 >> amplifier;
      return i > 0 ? duration % i == 0 : true;
   }
}
