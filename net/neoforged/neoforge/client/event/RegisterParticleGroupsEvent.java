package net.neoforged.neoforge.client.event;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterParticleGroupsEvent extends Event implements IModBusEvent {
   private final Map<ParticleRenderType, Function<ParticleEngine, ParticleGroup<?>>> particleGroupFactories;
   private final List<ParticleRenderType> particleRenderOrder;

   @Internal
   public RegisterParticleGroupsEvent(
      Map<ParticleRenderType, Function<ParticleEngine, ParticleGroup<?>>> particleGroupFactories, List<ParticleRenderType> particleRenderOrder
   ) {
      this.particleGroupFactories = particleGroupFactories;
      this.particleRenderOrder = particleRenderOrder;
   }

   public void register(ParticleRenderType group, Function<ParticleEngine, ParticleGroup<?>> factory) {
      if (this.particleGroupFactories.putIfAbsent(group, factory) != null) {
         throw new IllegalArgumentException("Factory already registered for provided particle render type: " + group.name());
      } else {
         this.particleRenderOrder.add(group);
      }
   }
}
