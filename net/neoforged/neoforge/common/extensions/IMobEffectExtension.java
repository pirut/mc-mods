package net.neoforged.neoforge.common.extensions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public interface IMobEffectExtension {
   private MobEffect self() {
      return (MobEffect)this;
   }

   default int getSortOrder(MobEffectInstance effectInstance) {
      return this.self().getColor();
   }
}
