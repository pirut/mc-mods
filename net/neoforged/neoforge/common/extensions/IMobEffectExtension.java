package net.neoforged.neoforge.common.extensions;

import java.util.Set;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.EffectCures;

public interface IMobEffectExtension {
   private MobEffect self() {
      return (MobEffect)this;
   }

   default void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
      cures.addAll(EffectCures.DEFAULT_CURES);
      if (this.self() == MobEffects.POISON.value()) {
         cures.add(EffectCures.HONEY);
      }
   }

   default int getSortOrder(MobEffectInstance effectInstance) {
      return this.self().getColor();
   }
}
