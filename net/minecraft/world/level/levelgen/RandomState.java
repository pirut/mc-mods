package net.minecraft.world.level.levelgen;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class RandomState {
   final PositionalRandomFactory random;
   private final HolderGetter<NormalNoise.NoiseParameters> noises;
   private final NoiseRouter router;
   private final Climate.Sampler sampler;
   private final SurfaceSystem surfaceSystem;
   private final PositionalRandomFactory aquiferRandom;
   private final PositionalRandomFactory oreRandom;
   private final Map<ResourceKey<NormalNoise.NoiseParameters>, NormalNoise> noiseIntances;
   private final Map<ResourceLocation, PositionalRandomFactory> positionalRandoms;

   public static RandomState create(HolderGetter.Provider p_255935_, ResourceKey<NoiseGeneratorSettings> p_256314_, long p_256595_) {
      return create(p_255935_.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(p_256314_).value(), p_255935_.lookupOrThrow(Registries.NOISE), p_256595_);
   }

   public static RandomState create(NoiseGeneratorSettings p_255761_, HolderGetter<NormalNoise.NoiseParameters> p_256649_, long p_255965_) {
      return new RandomState(p_255761_, p_256649_, p_255965_);
   }

   private RandomState(NoiseGeneratorSettings p_255668_, HolderGetter<NormalNoise.NoiseParameters> p_256663_, final long p_255691_) {
      this.random = p_255668_.getRandomSource().newInstance(p_255691_).forkPositional();
      this.noises = p_256663_;
      this.aquiferRandom = this.random.fromHashOf(ResourceLocation.withDefaultNamespace("aquifer")).forkPositional();
      this.oreRandom = this.random.fromHashOf(ResourceLocation.withDefaultNamespace("ore")).forkPositional();
      this.noiseIntances = new ConcurrentHashMap<>();
      this.positionalRandoms = new ConcurrentHashMap<>();
      this.surfaceSystem = new SurfaceSystem(this, p_255668_.defaultBlock(), p_255668_.seaLevel(), this.random);
      final boolean $$3 = p_255668_.useLegacyRandomSource();

      class NoiseWiringHelper implements DensityFunction.Visitor {
         private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();

         private RandomSource newLegacyInstance(long p_224592_) {
            return new LegacyRandomSource(p_255691_ + p_224592_);
         }

         @Override
         public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder p_224594_) {
            Holder<NormalNoise.NoiseParameters> $$1 = p_224594_.noiseData();
            if ($$3) {
               if ($$1.is(Noises.TEMPERATURE)) {
                  NormalNoise $$2 = NormalNoise.createLegacyNetherBiome(this.newLegacyInstance(0L), new NormalNoise.NoiseParameters(-7, 1.0, 1.0));
                  return new DensityFunction.NoiseHolder($$1, $$2);
               }

               if ($$1.is(Noises.VEGETATION)) {
                  NormalNoise $$3 = NormalNoise.createLegacyNetherBiome(this.newLegacyInstance(1L), new NormalNoise.NoiseParameters(-7, 1.0, 1.0));
                  return new DensityFunction.NoiseHolder($$1, $$3);
               }

               if ($$1.is(Noises.SHIFT)) {
                  NormalNoise $$4 = NormalNoise.create(RandomState.this.random.fromHashOf(Noises.SHIFT.location()), new NormalNoise.NoiseParameters(0, 0.0));
                  return new DensityFunction.NoiseHolder($$1, $$4);
               }
            }

            NormalNoise $$5 = RandomState.this.getOrCreateNoise($$1.unwrapKey().orElseThrow());
            return new DensityFunction.NoiseHolder($$1, $$5);
         }

         private DensityFunction wrapNew(DensityFunction p_224596_) {
            if (p_224596_ instanceof BlendedNoise $$1) {
               RandomSource $$2 = $$3 ? this.newLegacyInstance(0L) : RandomState.this.random.fromHashOf(ResourceLocation.withDefaultNamespace("terrain"));
               return $$1.withNewRandom($$2);
            } else {
               return (DensityFunction)(p_224596_ instanceof DensityFunctions.EndIslandDensityFunction
                  ? new DensityFunctions.EndIslandDensityFunction(p_255691_)
                  : p_224596_);
            }
         }

         @Override
         public DensityFunction apply(DensityFunction p_224598_) {
            return this.wrapped.computeIfAbsent(p_224598_, this::wrapNew);
         }
      }

      this.router = p_255668_.noiseRouter().mapAll(new NoiseWiringHelper());
      DensityFunction.Visitor $$4 = new DensityFunction.Visitor() {
         private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();

         private DensityFunction wrapNew(DensityFunction p_249732_) {
            if (p_249732_ instanceof DensityFunctions.HolderHolder $$1) {
               return $$1.function().value();
            } else {
               return p_249732_ instanceof DensityFunctions.Marker $$2 ? $$2.wrapped() : p_249732_;
            }
         }

         @Override
         public DensityFunction apply(DensityFunction p_248616_) {
            return this.wrapped.computeIfAbsent(p_248616_, this::wrapNew);
         }
      };
      this.sampler = new Climate.Sampler(
         this.router.temperature().mapAll($$4),
         this.router.vegetation().mapAll($$4),
         this.router.continents().mapAll($$4),
         this.router.erosion().mapAll($$4),
         this.router.depth().mapAll($$4),
         this.router.ridges().mapAll($$4),
         p_255668_.spawnTarget()
      );
   }

   public NormalNoise getOrCreateNoise(ResourceKey<NormalNoise.NoiseParameters> p_224561_) {
      return this.noiseIntances.computeIfAbsent(p_224561_, p_255589_ -> Noises.instantiate(this.noises, this.random, p_224561_));
   }

   public PositionalRandomFactory getOrCreateRandomFactory(ResourceLocation p_224566_) {
      return this.positionalRandoms.computeIfAbsent(p_224566_, p_224569_ -> this.random.fromHashOf(p_224566_).forkPositional());
   }

   public NoiseRouter router() {
      return this.router;
   }

   public Climate.Sampler sampler() {
      return this.sampler;
   }

   public SurfaceSystem surfaceSystem() {
      return this.surfaceSystem;
   }

   public PositionalRandomFactory aquiferRandom() {
      return this.aquiferRandom;
   }

   public PositionalRandomFactory oreRandom() {
      return this.oreRandom;
   }
}
