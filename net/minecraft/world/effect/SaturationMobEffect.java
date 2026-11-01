package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

class SaturationMobEffect extends InstantenousMobEffect {
   protected SaturationMobEffect(MobEffectCategory mobEffectCategory, int i) {
      super(mobEffectCategory, i);
   }

   @Override
   public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
      if (entity instanceof Player player) {
         player.getFoodData().eat(amplifier + 1, 1.0F);
      }

      return true;
   }
}
