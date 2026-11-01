package net.minecraft.world.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

class RaidOmenMobEffect extends MobEffect {
   protected RaidOmenMobEffect(MobEffectCategory mobEffectCategory, int i, ParticleOptions particleOptions) {
      super(mobEffectCategory, i, particleOptions);
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return duration == 1;
   }

   @Override
   public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
      if (entity instanceof ServerPlayer serverPlayer && !entity.isSpectator()) {
         BlockPos blockPos = serverPlayer.getRaidOmenPosition();
         if (blockPos != null) {
            level.getRaids().createOrExtendRaid(serverPlayer, blockPos);
            serverPlayer.clearRaidOmenPosition();
            return false;
         }
      }

      return true;
   }
}
