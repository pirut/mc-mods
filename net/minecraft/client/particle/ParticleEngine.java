package net.minecraft.client.particle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticlesRenderState;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ParticleEngine {
   private static final List<ParticleRenderType> RENDER_ORDER = List.of(
      ParticleRenderType.SINGLE_QUADS, ParticleRenderType.ITEM_PICKUP, ParticleRenderType.ELDER_GUARDIANS
   );
   protected ClientLevel level;
   private final Map<ParticleRenderType, ParticleGroup<?>> particles = Maps.newIdentityHashMap();
   private final Queue<TrackingEmitter> trackingEmitters = Queues.newArrayDeque();
   private final Queue<Particle> particlesToAdd = Queues.newArrayDeque();
   private final Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts = new Object2IntOpenHashMap();
   private final ParticleResources resourceManager;
   private final RandomSource random = RandomSource.create();

   public ParticleEngine(ClientLevel level, ParticleResources resourceManager) {
      this.level = level;
      this.resourceManager = resourceManager;
   }

   public void createTrackingEmitter(Entity entity, ParticleOptions particleData) {
      this.trackingEmitters.add(new TrackingEmitter(this.level, entity, particleData));
   }

   public void createTrackingEmitter(Entity entity, ParticleOptions data, int lifetime) {
      this.trackingEmitters.add(new TrackingEmitter(this.level, entity, data, lifetime));
   }

   @Nullable
   public Particle createParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
      Particle particle = this.makeParticle(particleData, x, y, z, xSpeed, ySpeed, zSpeed);
      if (particle != null) {
         this.add(particle);
         return particle;
      } else {
         return null;
      }
   }

   @Nullable
   private <T extends ParticleOptions> Particle makeParticle(T particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
      ParticleProvider<T> particleProvider = (ParticleProvider<T>)this.resourceManager
         .getProviders()
         .get(BuiltInRegistries.PARTICLE_TYPE.getId(particleData.getType()));
      return particleProvider == null ? null : particleProvider.createParticle(particleData, this.level, x, y, z, xSpeed, ySpeed, zSpeed, this.random);
   }

   public void add(Particle effect) {
      Optional<ParticleLimit> optional = effect.getParticleLimit();
      if (optional.isPresent()) {
         if (this.hasSpaceInParticleLimit(optional.get())) {
            this.particlesToAdd.add(effect);
            this.updateCount(optional.get(), 1);
         }
      } else {
         this.particlesToAdd.add(effect);
      }
   }

   public void tick() {
      this.particles.forEach((particleRenderType, particleGroup) -> {
         Profiler.get().push(particleRenderType.name());
         particleGroup.tickParticles();
         Profiler.get().pop();
      });
      if (!this.trackingEmitters.isEmpty()) {
         List<TrackingEmitter> list = Lists.newArrayList();

         for (TrackingEmitter trackingEmitter : this.trackingEmitters) {
            trackingEmitter.tick();
            if (!trackingEmitter.isAlive()) {
               list.add(trackingEmitter);
            }
         }

         this.trackingEmitters.removeAll(list);
      }

      Particle particle;
      if (!this.particlesToAdd.isEmpty()) {
         while ((particle = this.particlesToAdd.poll()) != null) {
            this.particles.computeIfAbsent(particle.getGroup(), this::createParticleGroup).add(particle);
         }
      }
   }

   private ParticleGroup<?> createParticleGroup(ParticleRenderType renderType) {
      if (renderType == ParticleRenderType.ITEM_PICKUP) {
         return new ItemPickupParticleGroup(this);
      } else if (renderType == ParticleRenderType.ELDER_GUARDIANS) {
         return new ElderGuardianParticleGroup(this);
      } else {
         return (ParticleGroup<?>)(renderType == ParticleRenderType.NO_RENDER ? new NoRenderParticleGroup(this) : new QuadParticleGroup(this, renderType));
      }
   }

   protected void updateCount(ParticleLimit limit, int count) {
      this.trackedParticleCounts.addTo(limit, count);
   }

   public void extract(ParticlesRenderState reusedState, Frustum frustum, Camera camera, float partialTick) {
      for (ParticleRenderType particleRenderType : RENDER_ORDER) {
         ParticleGroup<?> particleGroup = this.particles.get(particleRenderType);
         if (particleGroup != null && !particleGroup.isEmpty()) {
            reusedState.add(particleGroup.extractRenderState(frustum, camera, partialTick));
         }
      }
   }

   public void setLevel(@Nullable ClientLevel level) {
      this.level = level;
      this.clearParticles();
      this.trackingEmitters.clear();
   }

   public String countParticles() {
      return String.valueOf(this.particles.values().stream().mapToInt(ParticleGroup::size).sum());
   }

   private boolean hasSpaceInParticleLimit(ParticleLimit limit) {
      return this.trackedParticleCounts.getInt(limit) < limit.limit();
   }

   public void clearParticles() {
      this.particles.clear();
      this.particlesToAdd.clear();
      this.trackingEmitters.clear();
      this.trackedParticleCounts.clear();
   }
}
