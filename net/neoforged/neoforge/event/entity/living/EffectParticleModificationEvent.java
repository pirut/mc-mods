package net.neoforged.neoforge.event.entity.living;

import java.util.Objects;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public class EffectParticleModificationEvent extends LivingEvent {
   private final MobEffectInstance effect;
   private final ParticleOptions originalOptions;
   private ParticleOptions options;
   private boolean isVisible;

   public EffectParticleModificationEvent(LivingEntity entity, MobEffectInstance effect) {
      super(entity);
      this.effect = effect;
      this.isVisible = effect.isVisible();
      this.originalOptions = effect.getParticleOptions();
      this.options = this.originalOptions;
   }

   public MobEffectInstance getEffect() {
      return this.effect;
   }

   public ParticleOptions getOriginalParticleOptions() {
      return this.originalOptions;
   }

   public ParticleOptions getParticleOptions() {
      return this.options;
   }

   public void setParticleOptions(@Nullable ParticleOptions options) {
      this.options = Objects.requireNonNullElse(options, this.originalOptions);
   }

   public boolean isVisible() {
      return this.isVisible;
   }

   public void setVisible(boolean visible) {
      this.isVisible = visible;
   }
}
