package net.minecraft.client.particle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
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
   private final Map<ParticleRenderType, Function<ParticleEngine, ParticleGroup<?>>> particleGroupFactories;
   private final List<ParticleRenderType> particleRenderOrder;

   public ParticleEngine(ClientLevel p_107299_, ParticleResources p_446012_) {
      this.level = p_107299_;
      this.resourceManager = p_446012_;
      Reference2ObjectOpenHashMap<ParticleRenderType, Function<ParticleEngine, ParticleGroup<?>>> particleGroupFactories = new Reference2ObjectOpenHashMap();
      ArrayList<ParticleRenderType> particleRenderOrder = new ArrayList<>(RENDER_ORDER);
      ModLoader.postEvent(new RegisterParticleGroupsEvent(particleGroupFactories, particleRenderOrder));
      this.particleGroupFactories = Reference2ObjectMaps.unmodifiable(particleGroupFactories);
      this.particleRenderOrder = List.copyOf(particleRenderOrder);
   }

   public void createTrackingEmitter(Entity p_107330_, ParticleOptions p_107331_) {
      this.trackingEmitters.add(new TrackingEmitter(this.level, p_107330_, p_107331_));
   }

   public void createTrackingEmitter(Entity p_107333_, ParticleOptions p_107334_, int p_107335_) {
      this.trackingEmitters.add(new TrackingEmitter(this.level, p_107333_, p_107334_, p_107335_));
   }

   @Nullable
   public Particle createParticle(
      ParticleOptions p_107371_, double p_107372_, double p_107373_, double p_107374_, double p_107375_, double p_107376_, double p_107377_
   ) {
      Particle particle = this.makeParticle(p_107371_, p_107372_, p_107373_, p_107374_, p_107375_, p_107376_, p_107377_);
      if (particle != null) {
         this.add(particle);
         return particle;
      } else {
         return null;
      }
   }

   @Nullable
   private <T extends ParticleOptions> Particle makeParticle(
      T p_107396_, double p_107397_, double p_107398_, double p_107399_, double p_107400_, double p_107401_, double p_107402_
   ) {
      ParticleProvider<T> particleprovider = (ParticleProvider<T>)this.resourceManager
         .getProviders()
         .get(BuiltInRegistries.PARTICLE_TYPE.getKey(p_107396_.getType()));
      return particleprovider == null
         ? null
         : particleprovider.createParticle(p_107396_, this.level, p_107397_, p_107398_, p_107399_, p_107400_, p_107401_, p_107402_, this.random);
   }

   public void add(Particle p_107345_) {
      Optional<ParticleLimit> optional = p_107345_.getParticleLimit();
      if (optional.isPresent()) {
         if (this.hasSpaceInParticleLimit(optional.get())) {
            this.particlesToAdd.add(p_107345_);
            this.updateCount(optional.get(), 1);
         }
      } else {
         this.particlesToAdd.add(p_107345_);
      }
   }

   public void tick() {
      this.particles.forEach((p_445197_, p_445198_) -> {
         Profiler.get().push(p_445197_.name());
         p_445198_.tickParticles();
         Profiler.get().pop();
      });
      if (!this.trackingEmitters.isEmpty()) {
         List<TrackingEmitter> list = Lists.newArrayList();

         for (TrackingEmitter trackingemitter : this.trackingEmitters) {
            trackingemitter.tick();
            if (!trackingemitter.isAlive()) {
               list.add(trackingemitter);
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

   private ParticleGroup<?> createParticleGroup(ParticleRenderType p_446516_) {
      if (p_446516_ == ParticleRenderType.ITEM_PICKUP) {
         return new ItemPickupParticleGroup(this);
      } else if (p_446516_ == ParticleRenderType.ELDER_GUARDIANS) {
         return new ElderGuardianParticleGroup(this);
      } else if (this.particleGroupFactories.containsKey(p_446516_)) {
         return this.particleGroupFactories.get(p_446516_).apply(this);
      } else {
         return (ParticleGroup<?>)(p_446516_ == ParticleRenderType.NO_RENDER ? new NoRenderParticleGroup(this) : new QuadParticleGroup(this, p_446516_));
      }
   }

   protected void updateCount(ParticleLimit p_445453_, int p_172283_) {
      this.trackedParticleCounts.addTo(p_445453_, p_172283_);
   }

   public void extract(ParticlesRenderState p_451468_, Frustum p_446230_, Camera p_446261_, float p_446702_) {
      for (ParticleRenderType particlerendertype : this.particleRenderOrder) {
         ParticleGroup<?> particlegroup = this.particles.get(particlerendertype);
         if (particlegroup != null && !particlegroup.isEmpty()) {
            p_451468_.add(particlegroup.extractRenderState(p_446230_, p_446261_, p_446702_));
         }
      }
   }

   public void setLevel(@Nullable ClientLevel p_107343_) {
      this.level = p_107343_;
      this.clearParticles();
      this.trackingEmitters.clear();
   }

   public String countParticles() {
      return String.valueOf(this.particles.values().stream().mapToInt(ParticleGroup::size).sum());
   }

   private boolean hasSpaceInParticleLimit(ParticleLimit p_446420_) {
      return this.trackedParticleCounts.getInt(p_446420_) < p_446420_.limit();
   }

   public void clearParticles() {
      this.particles.clear();
      this.particlesToAdd.clear();
      this.trackingEmitters.clear();
      this.trackedParticleCounts.clear();
   }
}
