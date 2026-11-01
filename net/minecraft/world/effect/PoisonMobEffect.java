package net.minecraft.world.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForgeMod;

public class PoisonMobEffect extends MobEffect {
   public static final int DAMAGE_INTERVAL = 25;

   protected PoisonMobEffect(MobEffectCategory p_295076_, int p_295615_) {
      super(p_295076_, p_295615_);
   }

   @Override
   public boolean applyEffectTick(ServerLevel p_376442_, LivingEntity p_296276_, int p_296233_) {
      if (p_296276_.getHealth() > 1.0F) {
         Registry<DamageType> dTypeReg = p_296276_.damageSources().damageTypes;
         Holder.Reference<DamageType> dType = dTypeReg.get(NeoForgeMod.POISON_DAMAGE).orElse(dTypeReg.getOrThrow(DamageTypes.MAGIC));
         p_296276_.hurtServer(p_376442_, new DamageSource(dType), 1.0F);
      }

      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int p_295368_, int p_294232_) {
      int i = 25 >> p_294232_;
      return i > 0 ? p_295368_ % i == 0 : true;
   }
}
