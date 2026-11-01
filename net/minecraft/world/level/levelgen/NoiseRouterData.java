package net.minecraft.world.level.levelgen;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseRouterData {
   public static final float GLOBAL_OFFSET = -0.50375F;
   private static final float ORE_THICKNESS = 0.08F;
   private static final double VEININESS_FREQUENCY = 1.5;
   private static final double NOODLE_SPACING_AND_STRAIGHTNESS = 1.5;
   private static final double SURFACE_DENSITY_THRESHOLD = 1.5625;
   private static final double CHEESE_NOISE_TARGET = -0.703125;
   public static final int ISLAND_CHUNK_DISTANCE = 64;
   public static final long ISLAND_CHUNK_DISTANCE_SQR = 4096L;
   private static final DensityFunction BLENDING_FACTOR = DensityFunctions.constant(10.0);
   private static final DensityFunction BLENDING_JAGGEDNESS = DensityFunctions.zero();
   private static final ResourceKey<DensityFunction> ZERO = createKey("zero");
   private static final ResourceKey<DensityFunction> Y = createKey("y");
   private static final ResourceKey<DensityFunction> SHIFT_X = createKey("shift_x");
   private static final ResourceKey<DensityFunction> SHIFT_Z = createKey("shift_z");
   private static final ResourceKey<DensityFunction> BASE_3D_NOISE_OVERWORLD = createKey("overworld/base_3d_noise");
   private static final ResourceKey<DensityFunction> BASE_3D_NOISE_NETHER = createKey("nether/base_3d_noise");
   private static final ResourceKey<DensityFunction> BASE_3D_NOISE_END = createKey("end/base_3d_noise");
   public static final ResourceKey<DensityFunction> CONTINENTS = createKey("overworld/continents");
   public static final ResourceKey<DensityFunction> EROSION = createKey("overworld/erosion");
   public static final ResourceKey<DensityFunction> RIDGES = createKey("overworld/ridges");
   public static final ResourceKey<DensityFunction> RIDGES_FOLDED = createKey("overworld/ridges_folded");
   public static final ResourceKey<DensityFunction> OFFSET = createKey("overworld/offset");
   public static final ResourceKey<DensityFunction> FACTOR = createKey("overworld/factor");
   public static final ResourceKey<DensityFunction> JAGGEDNESS = createKey("overworld/jaggedness");
   public static final ResourceKey<DensityFunction> DEPTH = createKey("overworld/depth");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE = createKey("overworld/sloped_cheese");
   public static final ResourceKey<DensityFunction> CONTINENTS_LARGE = createKey("overworld_large_biomes/continents");
   public static final ResourceKey<DensityFunction> EROSION_LARGE = createKey("overworld_large_biomes/erosion");
   private static final ResourceKey<DensityFunction> OFFSET_LARGE = createKey("overworld_large_biomes/offset");
   private static final ResourceKey<DensityFunction> FACTOR_LARGE = createKey("overworld_large_biomes/factor");
   private static final ResourceKey<DensityFunction> JAGGEDNESS_LARGE = createKey("overworld_large_biomes/jaggedness");
   private static final ResourceKey<DensityFunction> DEPTH_LARGE = createKey("overworld_large_biomes/depth");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE_LARGE = createKey("overworld_large_biomes/sloped_cheese");
   private static final ResourceKey<DensityFunction> OFFSET_AMPLIFIED = createKey("overworld_amplified/offset");
   private static final ResourceKey<DensityFunction> FACTOR_AMPLIFIED = createKey("overworld_amplified/factor");
   private static final ResourceKey<DensityFunction> JAGGEDNESS_AMPLIFIED = createKey("overworld_amplified/jaggedness");
   private static final ResourceKey<DensityFunction> DEPTH_AMPLIFIED = createKey("overworld_amplified/depth");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE_AMPLIFIED = createKey("overworld_amplified/sloped_cheese");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE_END = createKey("end/sloped_cheese");
   private static final ResourceKey<DensityFunction> SPAGHETTI_ROUGHNESS_FUNCTION = createKey("overworld/caves/spaghetti_roughness_function");
   private static final ResourceKey<DensityFunction> ENTRANCES = createKey("overworld/caves/entrances");
   private static final ResourceKey<DensityFunction> NOODLE = createKey("overworld/caves/noodle");
   private static final ResourceKey<DensityFunction> PILLARS = createKey("overworld/caves/pillars");
   private static final ResourceKey<DensityFunction> SPAGHETTI_2D_THICKNESS_MODULATOR = createKey("overworld/caves/spaghetti_2d_thickness_modulator");
   private static final ResourceKey<DensityFunction> SPAGHETTI_2D = createKey("overworld/caves/spaghetti_2d");

   private static ResourceKey<DensityFunction> createKey(String p_209537_) {
      return ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace(p_209537_));
   }

   public static Holder<? extends DensityFunction> bootstrap(BootstrapContext<DensityFunction> p_321740_) {
      HolderGetter<NormalNoise.NoiseParameters> $$1 = p_321740_.lookup(Registries.NOISE);
      HolderGetter<DensityFunction> $$2 = p_321740_.lookup(Registries.DENSITY_FUNCTION);
      p_321740_.register(ZERO, DensityFunctions.zero());
      int $$3 = DimensionType.MIN_Y * 2;
      int $$4 = DimensionType.MAX_Y * 2;
      p_321740_.register(Y, DensityFunctions.yClampedGradient($$3, $$4, $$3, $$4));
      DensityFunction $$5 = registerAndWrap(
         p_321740_, SHIFT_X, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftA($$1.getOrThrow(Noises.SHIFT))))
      );
      DensityFunction $$6 = registerAndWrap(
         p_321740_, SHIFT_Z, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftB($$1.getOrThrow(Noises.SHIFT))))
      );
      p_321740_.register(BASE_3D_NOISE_OVERWORLD, BlendedNoise.createUnseeded(0.25, 0.125, 80.0, 160.0, 8.0));
      p_321740_.register(BASE_3D_NOISE_NETHER, BlendedNoise.createUnseeded(0.25, 0.375, 80.0, 60.0, 8.0));
      p_321740_.register(BASE_3D_NOISE_END, BlendedNoise.createUnseeded(0.25, 0.25, 80.0, 160.0, 4.0));
      Holder<DensityFunction> $$7 = p_321740_.register(
         CONTINENTS, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d($$5, $$6, 0.25, $$1.getOrThrow(Noises.CONTINENTALNESS)))
      );
      Holder<DensityFunction> $$8 = p_321740_.register(
         EROSION, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d($$5, $$6, 0.25, $$1.getOrThrow(Noises.EROSION)))
      );
      DensityFunction $$9 = registerAndWrap(
         p_321740_, RIDGES, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d($$5, $$6, 0.25, $$1.getOrThrow(Noises.RIDGE)))
      );
      p_321740_.register(RIDGES_FOLDED, peaksAndValleys($$9));
      DensityFunction $$10 = DensityFunctions.noise($$1.getOrThrow(Noises.JAGGED), 1500.0, 0.0);
      registerTerrainNoises(p_321740_, $$2, $$10, $$7, $$8, OFFSET, FACTOR, JAGGEDNESS, DEPTH, SLOPED_CHEESE, false);
      Holder<DensityFunction> $$11 = p_321740_.register(
         CONTINENTS_LARGE, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d($$5, $$6, 0.25, $$1.getOrThrow(Noises.CONTINENTALNESS_LARGE)))
      );
      Holder<DensityFunction> $$12 = p_321740_.register(
         EROSION_LARGE, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d($$5, $$6, 0.25, $$1.getOrThrow(Noises.EROSION_LARGE)))
      );
      registerTerrainNoises(p_321740_, $$2, $$10, $$11, $$12, OFFSET_LARGE, FACTOR_LARGE, JAGGEDNESS_LARGE, DEPTH_LARGE, SLOPED_CHEESE_LARGE, false);
      registerTerrainNoises(
         p_321740_, $$2, $$10, $$7, $$8, OFFSET_AMPLIFIED, FACTOR_AMPLIFIED, JAGGEDNESS_AMPLIFIED, DEPTH_AMPLIFIED, SLOPED_CHEESE_AMPLIFIED, true
      );
      p_321740_.register(SLOPED_CHEESE_END, DensityFunctions.add(DensityFunctions.endIslands(0L), getFunction($$2, BASE_3D_NOISE_END)));
      p_321740_.register(SPAGHETTI_ROUGHNESS_FUNCTION, spaghettiRoughnessFunction($$1));
      p_321740_.register(
         SPAGHETTI_2D_THICKNESS_MODULATOR,
         DensityFunctions.cacheOnce(DensityFunctions.mappedNoise($$1.getOrThrow(Noises.SPAGHETTI_2D_THICKNESS), 2.0, 1.0, -0.6, -1.3))
      );
      p_321740_.register(SPAGHETTI_2D, spaghetti2D($$2, $$1));
      p_321740_.register(ENTRANCES, entrances($$2, $$1));
      p_321740_.register(NOODLE, noodle($$2, $$1));
      return p_321740_.register(PILLARS, pillars($$1));
   }

   private static void registerTerrainNoises(
      BootstrapContext<DensityFunction> p_321572_,
      HolderGetter<DensityFunction> p_256393_,
      DensityFunction p_224476_,
      Holder<DensityFunction> p_224477_,
      Holder<DensityFunction> p_224478_,
      ResourceKey<DensityFunction> p_224479_,
      ResourceKey<DensityFunction> p_224480_,
      ResourceKey<DensityFunction> p_224481_,
      ResourceKey<DensityFunction> p_224482_,
      ResourceKey<DensityFunction> p_224483_,
      boolean p_224484_
   ) {
      DensityFunctions.Spline.Coordinate $$11 = new DensityFunctions.Spline.Coordinate(p_224477_);
      DensityFunctions.Spline.Coordinate $$12 = new DensityFunctions.Spline.Coordinate(p_224478_);
      DensityFunctions.Spline.Coordinate $$13 = new DensityFunctions.Spline.Coordinate(p_256393_.getOrThrow(RIDGES));
      DensityFunctions.Spline.Coordinate $$14 = new DensityFunctions.Spline.Coordinate(p_256393_.getOrThrow(RIDGES_FOLDED));
      DensityFunction $$15 = registerAndWrap(
         p_321572_,
         p_224479_,
         splineWithBlending(
            DensityFunctions.add(DensityFunctions.constant(-0.50375F), DensityFunctions.spline(TerrainProvider.overworldOffset($$11, $$12, $$14, p_224484_))),
            DensityFunctions.blendOffset()
         )
      );
      DensityFunction $$16 = registerAndWrap(
         p_321572_, p_224480_, splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldFactor($$11, $$12, $$13, $$14, p_224484_)), BLENDING_FACTOR)
      );
      DensityFunction $$17 = registerAndWrap(p_321572_, p_224482_, DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 1.5, -1.5), $$15));
      DensityFunction $$18 = registerAndWrap(
         p_321572_,
         p_224481_,
         splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldJaggedness($$11, $$12, $$13, $$14, p_224484_)), BLENDING_JAGGEDNESS)
      );
      DensityFunction $$19 = DensityFunctions.mul($$18, p_224476_.halfNegative());
      DensityFunction $$20 = noiseGradientDensity($$16, DensityFunctions.add($$17, $$19));
      p_321572_.register(p_224483_, DensityFunctions.add($$20, getFunction(p_256393_, BASE_3D_NOISE_OVERWORLD)));
   }

   private static DensityFunction registerAndWrap(
      BootstrapContext<DensityFunction> p_321585_, ResourceKey<DensityFunction> p_255905_, DensityFunction p_255856_
   ) {
      return new DensityFunctions.HolderHolder(p_321585_.register(p_255905_, p_255856_));
   }

   private static DensityFunction getFunction(HolderGetter<DensityFunction> p_256312_, ResourceKey<DensityFunction> p_256077_) {
      return new DensityFunctions.HolderHolder(p_256312_.getOrThrow(p_256077_));
   }

   private static DensityFunction peaksAndValleys(DensityFunction p_224438_) {
      return DensityFunctions.mul(
         DensityFunctions.add(
            DensityFunctions.add(p_224438_.abs(), DensityFunctions.constant(-0.6666666666666666)).abs(), DensityFunctions.constant(-0.3333333333333333)
         ),
         DensityFunctions.constant(-3.0)
      );
   }

   public static float peaksAndValleys(float p_224436_) {
      return -(Math.abs(Math.abs(p_224436_) - 0.6666667F) - 0.33333334F) * 3.0F;
   }

   private static DensityFunction spaghettiRoughnessFunction(HolderGetter<NormalNoise.NoiseParameters> p_255763_) {
      DensityFunction $$1 = DensityFunctions.noise(p_255763_.getOrThrow(Noises.SPAGHETTI_ROUGHNESS));
      DensityFunction $$2 = DensityFunctions.mappedNoise(p_255763_.getOrThrow(Noises.SPAGHETTI_ROUGHNESS_MODULATOR), 0.0, -0.1);
      return DensityFunctions.cacheOnce(DensityFunctions.mul($$2, DensityFunctions.add($$1.abs(), DensityFunctions.constant(-0.4))));
   }

   private static DensityFunction entrances(HolderGetter<DensityFunction> p_256511_, HolderGetter<NormalNoise.NoiseParameters> p_255899_) {
      DensityFunction $$2 = DensityFunctions.cacheOnce(DensityFunctions.noise(p_255899_.getOrThrow(Noises.SPAGHETTI_3D_RARITY), 2.0, 1.0));
      DensityFunction $$3 = DensityFunctions.mappedNoise(p_255899_.getOrThrow(Noises.SPAGHETTI_3D_THICKNESS), -0.065, -0.088);
      DensityFunction $$4 = DensityFunctions.weirdScaledSampler(
         $$2, p_255899_.getOrThrow(Noises.SPAGHETTI_3D_1), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE1
      );
      DensityFunction $$5 = DensityFunctions.weirdScaledSampler(
         $$2, p_255899_.getOrThrow(Noises.SPAGHETTI_3D_2), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE1
      );
      DensityFunction $$6 = DensityFunctions.add(DensityFunctions.max($$4, $$5), $$3).clamp(-1.0, 1.0);
      DensityFunction $$7 = getFunction(p_256511_, SPAGHETTI_ROUGHNESS_FUNCTION);
      DensityFunction $$8 = DensityFunctions.noise(p_255899_.getOrThrow(Noises.CAVE_ENTRANCE), 0.75, 0.5);
      DensityFunction $$9 = DensityFunctions.add(
         DensityFunctions.add($$8, DensityFunctions.constant(0.37)), DensityFunctions.yClampedGradient(-10, 30, 0.3, 0.0)
      );
      return DensityFunctions.cacheOnce(DensityFunctions.min($$9, DensityFunctions.add($$7, $$6)));
   }

   private static DensityFunction noodle(HolderGetter<DensityFunction> p_256402_, HolderGetter<NormalNoise.NoiseParameters> p_255632_) {
      DensityFunction $$2 = getFunction(p_256402_, Y);
      int $$3 = -64;
      int $$4 = -60;
      int $$5 = 320;
      DensityFunction $$6 = yLimitedInterpolatable($$2, DensityFunctions.noise(p_255632_.getOrThrow(Noises.NOODLE), 1.0, 1.0), -60, 320, -1);
      DensityFunction $$7 = yLimitedInterpolatable(
         $$2, DensityFunctions.mappedNoise(p_255632_.getOrThrow(Noises.NOODLE_THICKNESS), 1.0, 1.0, -0.05, -0.1), -60, 320, 0
      );
      double $$8 = 2.6666666666666665;
      DensityFunction $$9 = yLimitedInterpolatable(
         $$2, DensityFunctions.noise(p_255632_.getOrThrow(Noises.NOODLE_RIDGE_A), 2.6666666666666665, 2.6666666666666665), -60, 320, 0
      );
      DensityFunction $$10 = yLimitedInterpolatable(
         $$2, DensityFunctions.noise(p_255632_.getOrThrow(Noises.NOODLE_RIDGE_B), 2.6666666666666665, 2.6666666666666665), -60, 320, 0
      );
      DensityFunction $$11 = DensityFunctions.mul(DensityFunctions.constant(1.5), DensityFunctions.max($$9.abs(), $$10.abs()));
      return DensityFunctions.rangeChoice($$6, -1000000.0, 0.0, DensityFunctions.constant(64.0), DensityFunctions.add($$7, $$11));
   }

   private static DensityFunction pillars(HolderGetter<NormalNoise.NoiseParameters> p_255985_) {
      double $$1 = 25.0;
      double $$2 = 0.3;
      DensityFunction $$3 = DensityFunctions.noise(p_255985_.getOrThrow(Noises.PILLAR), 25.0, 0.3);
      DensityFunction $$4 = DensityFunctions.mappedNoise(p_255985_.getOrThrow(Noises.PILLAR_RARENESS), 0.0, -2.0);
      DensityFunction $$5 = DensityFunctions.mappedNoise(p_255985_.getOrThrow(Noises.PILLAR_THICKNESS), 0.0, 1.1);
      DensityFunction $$6 = DensityFunctions.add(DensityFunctions.mul($$3, DensityFunctions.constant(2.0)), $$4);
      return DensityFunctions.cacheOnce(DensityFunctions.mul($$6, $$5.cube()));
   }

   private static DensityFunction spaghetti2D(HolderGetter<DensityFunction> p_256535_, HolderGetter<NormalNoise.NoiseParameters> p_255650_) {
      DensityFunction $$2 = DensityFunctions.noise(p_255650_.getOrThrow(Noises.SPAGHETTI_2D_MODULATOR), 2.0, 1.0);
      DensityFunction $$3 = DensityFunctions.weirdScaledSampler(
         $$2, p_255650_.getOrThrow(Noises.SPAGHETTI_2D), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE2
      );
      DensityFunction $$4 = DensityFunctions.mappedNoise(p_255650_.getOrThrow(Noises.SPAGHETTI_2D_ELEVATION), 0.0, Math.floorDiv(-64, 8), 8.0);
      DensityFunction $$5 = getFunction(p_256535_, SPAGHETTI_2D_THICKNESS_MODULATOR);
      DensityFunction $$6 = DensityFunctions.add($$4, DensityFunctions.yClampedGradient(-64, 320, 8.0, -40.0)).abs();
      DensityFunction $$7 = DensityFunctions.add($$6, $$5).cube();
      double $$8 = 0.083;
      DensityFunction $$9 = DensityFunctions.add($$3, DensityFunctions.mul(DensityFunctions.constant(0.083), $$5));
      return DensityFunctions.max($$9, $$7).clamp(-1.0, 1.0);
   }

   private static DensityFunction underground(
      HolderGetter<DensityFunction> p_256548_, HolderGetter<NormalNoise.NoiseParameters> p_256236_, DensityFunction p_256658_
   ) {
      DensityFunction $$3 = getFunction(p_256548_, SPAGHETTI_2D);
      DensityFunction $$4 = getFunction(p_256548_, SPAGHETTI_ROUGHNESS_FUNCTION);
      DensityFunction $$5 = DensityFunctions.noise(p_256236_.getOrThrow(Noises.CAVE_LAYER), 8.0);
      DensityFunction $$6 = DensityFunctions.mul(DensityFunctions.constant(4.0), $$5.square());
      DensityFunction $$7 = DensityFunctions.noise(p_256236_.getOrThrow(Noises.CAVE_CHEESE), 0.6666666666666666);
      DensityFunction $$8 = DensityFunctions.add(
         DensityFunctions.add(DensityFunctions.constant(0.27), $$7).clamp(-1.0, 1.0),
         DensityFunctions.add(DensityFunctions.constant(1.5), DensityFunctions.mul(DensityFunctions.constant(-0.64), p_256658_)).clamp(0.0, 0.5)
      );
      DensityFunction $$9 = DensityFunctions.add($$6, $$8);
      DensityFunction $$10 = DensityFunctions.min(DensityFunctions.min($$9, getFunction(p_256548_, ENTRANCES)), DensityFunctions.add($$3, $$4));
      DensityFunction $$11 = getFunction(p_256548_, PILLARS);
      DensityFunction $$12 = DensityFunctions.rangeChoice($$11, -1000000.0, 0.03, DensityFunctions.constant(-1000000.0), $$11);
      return DensityFunctions.max($$10, $$12);
   }

   private static DensityFunction postProcess(DensityFunction p_224493_) {
      DensityFunction $$1 = DensityFunctions.blendDensity(p_224493_);
      return DensityFunctions.mul(DensityFunctions.interpolated($$1), DensityFunctions.constant(0.64)).squeeze();
   }

   protected static NoiseRouter overworld(
      HolderGetter<DensityFunction> p_255681_, HolderGetter<NormalNoise.NoiseParameters> p_256005_, boolean p_255649_, boolean p_255617_
   ) {
      DensityFunction $$4 = DensityFunctions.noise(p_256005_.getOrThrow(Noises.AQUIFER_BARRIER), 0.5);
      DensityFunction $$5 = DensityFunctions.noise(p_256005_.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67);
      DensityFunction $$6 = DensityFunctions.noise(p_256005_.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143);
      DensityFunction $$7 = DensityFunctions.noise(p_256005_.getOrThrow(Noises.AQUIFER_LAVA));
      DensityFunction $$8 = getFunction(p_255681_, SHIFT_X);
      DensityFunction $$9 = getFunction(p_255681_, SHIFT_Z);
      DensityFunction $$10 = DensityFunctions.shiftedNoise2d($$8, $$9, 0.25, p_256005_.getOrThrow(p_255649_ ? Noises.TEMPERATURE_LARGE : Noises.TEMPERATURE));
      DensityFunction $$11 = DensityFunctions.shiftedNoise2d($$8, $$9, 0.25, p_256005_.getOrThrow(p_255649_ ? Noises.VEGETATION_LARGE : Noises.VEGETATION));
      DensityFunction $$12 = getFunction(p_255681_, p_255649_ ? FACTOR_LARGE : (p_255617_ ? FACTOR_AMPLIFIED : FACTOR));
      DensityFunction $$13 = getFunction(p_255681_, p_255649_ ? DEPTH_LARGE : (p_255617_ ? DEPTH_AMPLIFIED : DEPTH));
      DensityFunction $$14 = noiseGradientDensity(DensityFunctions.cache2d($$12), $$13);
      DensityFunction $$15 = getFunction(p_255681_, p_255649_ ? SLOPED_CHEESE_LARGE : (p_255617_ ? SLOPED_CHEESE_AMPLIFIED : SLOPED_CHEESE));
      DensityFunction $$16 = DensityFunctions.min($$15, DensityFunctions.mul(DensityFunctions.constant(5.0), getFunction(p_255681_, ENTRANCES)));
      DensityFunction $$17 = DensityFunctions.rangeChoice($$15, -1000000.0, 1.5625, $$16, underground(p_255681_, p_256005_, $$15));
      DensityFunction $$18 = DensityFunctions.min(postProcess(slideOverworld(p_255617_, $$17)), getFunction(p_255681_, NOODLE));
      DensityFunction $$19 = getFunction(p_255681_, Y);
      int $$20 = Stream.of(OreVeinifier.VeinType.values()).mapToInt(p_224495_ -> p_224495_.minY).min().orElse(-DimensionType.MIN_Y * 2);
      int $$21 = Stream.of(OreVeinifier.VeinType.values()).mapToInt(p_224457_ -> p_224457_.maxY).max().orElse(-DimensionType.MIN_Y * 2);
      DensityFunction $$22 = yLimitedInterpolatable($$19, DensityFunctions.noise(p_256005_.getOrThrow(Noises.ORE_VEININESS), 1.5, 1.5), $$20, $$21, 0);
      float $$23 = 4.0F;
      DensityFunction $$24 = yLimitedInterpolatable($$19, DensityFunctions.noise(p_256005_.getOrThrow(Noises.ORE_VEIN_A), 4.0, 4.0), $$20, $$21, 0).abs();
      DensityFunction $$25 = yLimitedInterpolatable($$19, DensityFunctions.noise(p_256005_.getOrThrow(Noises.ORE_VEIN_B), 4.0, 4.0), $$20, $$21, 0).abs();
      DensityFunction $$26 = DensityFunctions.add(DensityFunctions.constant(-0.08F), DensityFunctions.max($$24, $$25));
      DensityFunction $$27 = DensityFunctions.noise(p_256005_.getOrThrow(Noises.ORE_GAP));
      return new NoiseRouter(
         $$4,
         $$5,
         $$6,
         $$7,
         $$10,
         $$11,
         getFunction(p_255681_, p_255649_ ? CONTINENTS_LARGE : CONTINENTS),
         getFunction(p_255681_, p_255649_ ? EROSION_LARGE : EROSION),
         $$13,
         getFunction(p_255681_, RIDGES),
         slideOverworld(p_255617_, DensityFunctions.add($$14, DensityFunctions.constant(-0.703125)).clamp(-64.0, 64.0)),
         $$18,
         $$22,
         $$26,
         $$27
      );
   }

   private static NoiseRouter noNewCaves(
      HolderGetter<DensityFunction> p_255724_, HolderGetter<NormalNoise.NoiseParameters> p_255986_, DensityFunction p_256378_
   ) {
      DensityFunction $$3 = getFunction(p_255724_, SHIFT_X);
      DensityFunction $$4 = getFunction(p_255724_, SHIFT_Z);
      DensityFunction $$5 = DensityFunctions.shiftedNoise2d($$3, $$4, 0.25, p_255986_.getOrThrow(Noises.TEMPERATURE));
      DensityFunction $$6 = DensityFunctions.shiftedNoise2d($$3, $$4, 0.25, p_255986_.getOrThrow(Noises.VEGETATION));
      DensityFunction $$7 = postProcess(p_256378_);
      return new NoiseRouter(
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         $$5,
         $$6,
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         $$7,
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero()
      );
   }

   private static DensityFunction slideOverworld(boolean p_224490_, DensityFunction p_224491_) {
      return slide(p_224491_, -64, 384, p_224490_ ? 16 : 80, p_224490_ ? 0 : 64, -0.078125, 0, 24, p_224490_ ? 0.4 : 0.1171875);
   }

   private static DensityFunction slideNetherLike(HolderGetter<DensityFunction> p_256084_, int p_255802_, int p_255834_) {
      return slide(getFunction(p_256084_, BASE_3D_NOISE_NETHER), p_255802_, p_255834_, 24, 0, 0.9375, -8, 24, 2.5);
   }

   private static DensityFunction slideEndLike(DensityFunction p_224440_, int p_224441_, int p_224442_) {
      return slide(p_224440_, p_224441_, p_224442_, 72, -184, -23.4375, 4, 32, -0.234375);
   }

   protected static NoiseRouter nether(HolderGetter<DensityFunction> p_256256_, HolderGetter<NormalNoise.NoiseParameters> p_256169_) {
      return noNewCaves(p_256256_, p_256169_, slideNetherLike(p_256256_, 0, 128));
   }

   protected static NoiseRouter caves(HolderGetter<DensityFunction> p_256088_, HolderGetter<NormalNoise.NoiseParameters> p_255675_) {
      return noNewCaves(p_256088_, p_255675_, slideNetherLike(p_256088_, -64, 192));
   }

   protected static NoiseRouter floatingIslands(HolderGetter<DensityFunction> p_256633_, HolderGetter<NormalNoise.NoiseParameters> p_255902_) {
      return noNewCaves(p_256633_, p_255902_, slideEndLike(getFunction(p_256633_, BASE_3D_NOISE_END), 0, 256));
   }

   private static DensityFunction slideEnd(DensityFunction p_224506_) {
      return slideEndLike(p_224506_, 0, 128);
   }

   protected static NoiseRouter end(HolderGetter<DensityFunction> p_256079_) {
      DensityFunction $$1 = DensityFunctions.cache2d(DensityFunctions.endIslands(0L));
      DensityFunction $$2 = postProcess(slideEnd(getFunction(p_256079_, SLOPED_CHEESE_END)));
      return new NoiseRouter(
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         $$1,
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         slideEnd(DensityFunctions.add($$1, DensityFunctions.constant(-0.703125))),
         $$2,
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero()
      );
   }

   protected static NoiseRouter none() {
      return new NoiseRouter(
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero(),
         DensityFunctions.zero()
      );
   }

   private static DensityFunction splineWithBlending(DensityFunction p_224454_, DensityFunction p_224455_) {
      DensityFunction $$2 = DensityFunctions.lerp(DensityFunctions.blendAlpha(), p_224455_, p_224454_);
      return DensityFunctions.flatCache(DensityFunctions.cache2d($$2));
   }

   private static DensityFunction noiseGradientDensity(DensityFunction p_212272_, DensityFunction p_212273_) {
      DensityFunction $$2 = DensityFunctions.mul(p_212273_, p_212272_);
      return DensityFunctions.mul(DensityFunctions.constant(4.0), $$2.quarterNegative());
   }

   private static DensityFunction yLimitedInterpolatable(DensityFunction p_209472_, DensityFunction p_209473_, int p_209474_, int p_209475_, int p_209476_) {
      return DensityFunctions.interpolated(DensityFunctions.rangeChoice(p_209472_, p_209474_, p_209475_ + 1, p_209473_, DensityFunctions.constant(p_209476_)));
   }

   private static DensityFunction slide(
      DensityFunction p_224444_, int p_224445_, int p_224446_, int p_224447_, int p_224448_, double p_224449_, int p_224450_, int p_224451_, double p_224452_
   ) {
      DensityFunction $$10 = DensityFunctions.yClampedGradient(p_224445_ + p_224446_ - p_224447_, p_224445_ + p_224446_ - p_224448_, 1.0, 0.0);
      DensityFunction $$9 = DensityFunctions.lerp($$10, p_224449_, p_224444_);
      DensityFunction $$11 = DensityFunctions.yClampedGradient(p_224445_ + p_224450_, p_224445_ + p_224451_, 0.0, 1.0);
      return DensityFunctions.lerp($$11, p_224452_, $$9);
   }

   protected static final class QuantizedSpaghettiRarity {
      protected static double getSphaghettiRarity2D(double p_209564_) {
         if (p_209564_ < -0.75) {
            return 0.5;
         } else if (p_209564_ < -0.5) {
            return 0.75;
         } else if (p_209564_ < 0.5) {
            return 1.0;
         } else {
            return p_209564_ < 0.75 ? 2.0 : 3.0;
         }
      }

      protected static double getSpaghettiRarity3D(double p_209566_) {
         if (p_209566_ < -0.5) {
            return 0.75;
         } else if (p_209566_ < 0.0) {
            return 1.0;
         } else {
            return p_209566_ < 0.5 ? 1.5 : 2.0;
         }
      }
   }
}
