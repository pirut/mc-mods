package net.neoforged.neoforge.client.event;

import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterParticleProvidersEvent extends Event implements IModBusEvent {
   private final ParticleResources particleResources;

   @Internal
   public RegisterParticleProvidersEvent(ParticleResources particleResources) {
      this.particleResources = particleResources;
   }

   public <T extends ParticleOptions> void registerSpecial(ParticleType<T> type, ParticleProvider<T> provider) {
      this.particleResources.register(type, provider);
   }

   public <T extends ParticleOptions> void registerSpriteSet(ParticleType<T> type, ParticleResources.SpriteParticleRegistration<T> registration) {
      this.particleResources.register(type, registration);
   }
}
