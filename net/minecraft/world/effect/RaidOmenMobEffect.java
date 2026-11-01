package net.minecraft.world.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

class RaidOmenMobEffect extends MobEffect {
   protected RaidOmenMobEffect(MobEffectCategory p_338433_, int p_338414_, ParticleOptions p_338610_) {
      super(p_338433_, p_338414_, p_338610_);
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int p_338435_, int p_338504_) {
      return p_338435_ == 1;
   }

   @Override
   public boolean applyEffectTick(LivingEntity p_338728_, int p_338249_) {
      if (p_338728_ instanceof ServerPlayer $$2 && !p_338728_.isSpectator()) {
         ServerLevel $$3 = $$2.serverLevel();
         BlockPos $$4 = $$2.getRaidOmenPosition();
         if ($$4 != null) {
            $$3.getRaids().createOrExtendRaid($$2, $$4);
            $$2.clearRaidOmenPosition();
            return false;
         }
      }

      return true;
   }
}
