package net.minecraft.world.effect;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

class HealOrHarmMobEffect extends InstantenousMobEffect {
   private final boolean isHarm;

   public HealOrHarmMobEffect(MobEffectCategory p_296444_, int p_294226_, boolean p_295780_) {
      super(p_296444_, p_294226_);
      this.isHarm = p_295780_;
   }

   @Override
   public boolean applyEffectTick(LivingEntity p_295255_, int p_295147_) {
      if (this.isHarm == p_295255_.isInvertedHealAndHarm()) {
         p_295255_.heal(Math.max(4 << p_295147_, 0));
      } else {
         p_295255_.hurt(p_295255_.damageSources().magic(), 6 << p_295147_);
      }

      return true;
   }

   @Override
   public void applyInstantenousEffect(@Nullable Entity p_294574_, @Nullable Entity p_295692_, LivingEntity p_296483_, int p_296095_, double p_295178_) {
      if (this.isHarm == p_296483_.isInvertedHealAndHarm()) {
         int $$5 = (int)(p_295178_ * (4 << p_296095_) + 0.5);
         p_296483_.heal($$5);
      } else {
         int $$6 = (int)(p_295178_ * (6 << p_296095_) + 0.5);
         if (p_294574_ == null) {
            p_296483_.hurt(p_296483_.damageSources().magic(), $$6);
         } else {
            p_296483_.hurt(p_296483_.damageSources().indirectMagic(p_294574_, p_295692_), $$6);
         }
      }
   }
}
