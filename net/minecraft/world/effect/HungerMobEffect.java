package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

class HungerMobEffect extends MobEffect {
   protected HungerMobEffect(MobEffectCategory mobEffectCategory, int i) {
      super(mobEffectCategory, i);
   }

   @Override
   public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
      if (entity instanceof Player player) {
         player.causeFoodExhaustion(0.005F * (amplifier + 1));
      }

      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }
}
