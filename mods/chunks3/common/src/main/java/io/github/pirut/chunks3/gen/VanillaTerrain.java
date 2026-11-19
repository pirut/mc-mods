package io.github.pirut.chunks3.gen;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;

/**
 * Vanilla's overworld, read a column at a time: the ground height of its terrain and the biome at a
 * spot, from vanilla's own noise router and biome source for the world's seed.
 *
 * <p>The height is the top block where vanilla's terrain density, {@code minecraft:overworld/sloped_cheese}
 * under the overworld's top and bottom slides, is above zero, with the density sampled at the corners of
 * vanilla's 4 by 8 by 4 cells and interpolated between them as vanilla's noise chunk does. Vanilla's caves
 * (which the final density cuts out of that) are left to the engine's own {@link Underground}, so the
 * ground is vanilla's ground without its cave mouths. Thread safe.
 */
final class VanillaTerrain {
    /** Vanilla overworld's noise cells, in blocks. */
    static final int CELL_WIDTH = 4, CELL_HEIGHT = 8;
    /** Vanilla overworld's floor and the top of its terrain: above Y 256 the top slide leaves only air. */
    static final int MIN_Y = -64, TOP = 256;
    private static final int CELLS = (TOP - MIN_Y) / CELL_HEIGHT;
    private static final ResourceKey<DensityFunction> SLOPED_CHEESE =
            ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace("overworld/sloped_cheese"));

    private final DensityFunction terrain, smooth, depth;
    private final Climate.Sampler climate;
    private final MultiNoiseBiomeSource biomes;
    private final Registry<Biome> biomeRegistry;
    /** Densities at a cell corner column, by cell from the bottom, NaN until asked for. */
    private final ConcurrentHashMap<Long, double[]> corners = new ConcurrentHashMap<>();
    /** Far terrain's heights by column: far levels sample on nested grids, so one sample serves every coarser level too. */
    private final ConcurrentHashMap<Long, Integer> farHeights = new ConcurrentHashMap<>(), smoothHeights = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Holder<Biome>> biomeCache = new ConcurrentHashMap<>();

    /**
     * Vanilla's overworld for a seed, from the worldgen data in {@code lookup} (the server's registries, data
     * packs included). Biomes are handed out as {@code biomeRegistry}'s holders.
     */
    VanillaTerrain(HolderLookup.Provider lookup, Registry<Biome> biomeRegistry, long seed) {
        NoiseGeneratorSettings settings = lookup.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
        RandomState state = RandomState.create(settings, lookup.lookupOrThrow(Registries.NOISE), seed);
        PositionalRandomFactory random = settings.getRandomSource().newInstance(seed).forkPositional();
        // One wiring for the terrain and the climate, so the parts they share (continents, erosion, ridges and the
        // splines over them) are the same functions and their per-column caches serve both.
        Wiring wiring = new Wiring(state, random);
        DensityFunction cheese = lookup.lookupOrThrow(Registries.DENSITY_FUNCTION).getOrThrow(SLOPED_CHEESE).value();
        this.terrain = cheese.mapAll(wiring);
        // The same terrain without its 3D noise, whose lumps are smaller than the coarse far levels' voxels.
        this.smooth = terrain.mapAll(f -> f instanceof BlendedNoise ? DensityFunctions.zero() : f);
        NoiseRouter router = settings.noiseRouter();
        this.depth = router.depth().mapAll(wiring);
        this.climate = new Climate.Sampler(router.temperature().mapAll(wiring), router.vegetation().mapAll(wiring),
                router.continents().mapAll(wiring), router.erosion().mapAll(wiring), depth, router.ridges().mapAll(wiring),
                state.sampler().spawnTarget());
        this.biomes = MultiNoiseBiomeSource.createFromPreset(lookup.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        this.biomeRegistry = biomeRegistry;
    }

    /**
     * Wires a density function to the world's noises as {@link RandomState} wires its router, and swaps
     * vanilla's per-chunk caches (which only work inside a noise chunk) for small per-thread ones.
     */
    private static final class Wiring implements DensityFunction.Visitor {
        private final RandomState state;
        private final PositionalRandomFactory random;
        private final Map<DensityFunction, DensityFunction> wired = new HashMap<>();

        Wiring(RandomState state, PositionalRandomFactory random) { this.state = state; this.random = random; }

        @Override public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder holder) {
            return new DensityFunction.NoiseHolder(holder.noiseData(), state.getOrCreateNoise(holder.noiseData().unwrapKey().orElseThrow()));
        }

        @Override public DensityFunction apply(DensityFunction function) { return wired.computeIfAbsent(function, this::wire); }

        private DensityFunction wire(DensityFunction function) {
            if (function instanceof BlendedNoise blended) return blended.withNewRandom(random.fromHashOf(ResourceLocation.withDefaultNamespace("terrain")));
            if (function instanceof DensityFunctions.MarkerOrMarked marker) {
                String type = ((Enum<?>) (Object) marker.type()).name();
                return type.equals("FlatCache") || type.equals("Cache2D") ? new Flat(marker.wrapped()) : marker.wrapped();
            }
            return function;
        }
    }

    /**
     * A function of X and Z only, remembered per thread for the last column asked: a column's densities
     * are read top down, so its continents, erosion and splines are worked out once. Like vanilla's flat
     * cache it reads its input at Y 0.
     */
    private static final class Flat implements DensityFunction.SimpleFunction {
        private final DensityFunction wrapped;
        private final ThreadLocal<double[]> last = ThreadLocal.withInitial(() -> new double[]{Double.NaN, Double.NaN, 0});

        Flat(DensityFunction wrapped) { this.wrapped = wrapped; }

        @Override public double compute(DensityFunction.FunctionContext context) {
            double[] memo = last.get();
            int x = context.blockX(), z = context.blockZ();
            if (memo[0] == x && memo[1] == z) return memo[2];
            double value = wrapped.compute(new DensityFunction.SinglePointContext(x, 0, z));
            memo[0] = x; memo[1] = z; memo[2] = value;
            return value;
        }
        @Override public double minValue() { return wrapped.minValue(); }
        @Override public double maxValue() { return wrapped.maxValue(); }
        @Override public net.minecraft.util.KeyDispatchDataCodec<? extends DensityFunction> codec() { throw new UnsupportedOperationException("not serialised"); }
    }

    // Heights -------------------------------------------------------------------------------------

    /** The overworld's top and bottom slides, as {@code NoiseRouterData.slideOverworld}, at a height. */
    static double slide(double density, int y) {
        double top = Mth.clampedMap(y, TOP - 16, TOP, 1.0, 0.0);
        density = -0.078125 + top * (density + 0.078125);
        double bottom = Mth.clampedMap(y, MIN_Y, MIN_Y + 24, 0.0, 1.0);
        return 0.1171875 + bottom * (density - 0.1171875);
    }

    /** A column of densities by cell over vanilla's floor. */
    private interface Cells { double at(int cell); }

    /** Cells a height search steps at a time before it looks closer. */
    private static final int STRIDE = 4;

    /**
     * Where a column's search starts: vanilla's depth is 0 at its plain surface, before the mountains'
     * jaggedness and the 3D noise, and falls by 1 every 128 blocks up, so that surface is 128 times the depth
     * at Y 0. Read once per column from functions the terrain shares.
     */
    private int guessCell(int x, int z) {
        double surface = 128 * depth.compute(new DensityFunction.SinglePointContext(x, 0, z));
        return Mth.clamp((int) Math.floor((surface + CELL_HEIGHT - MIN_Y) / CELL_HEIGHT), 0, CELLS);
    }

    /**
     * The highest cell of a column with its density above zero: from the guess, {@link #STRIDE} cells at a
     * time up or down until the density changes sign, then cell by cell within that stride. A few reads where
     * a search from the top reads dozens, but it can stop on an overhang or ledge with land above it, so only
     * far terrain uses it; the near ground reads from the top. -1 where no cell is solid.
     */
    private static int topCell(Cells column, int guess) {
        int solid, air;
        if (column.at(guess) > 0) {
            solid = guess;
            air = Math.min(CELLS, guess + STRIDE);
            while (column.at(air) > 0) {
                if (air == CELLS) return CELLS;
                solid = air;
                air = Math.min(CELLS, air + STRIDE);
            }
        } else {
            air = guess;
            solid = guess - STRIDE;
            while (true) {
                if (solid <= 0) { solid = 0; if (column.at(0) <= 0) return -1; break; }
                if (column.at(solid) > 0) break;
                air = solid;
                solid -= STRIDE;
            }
        }
        for (int cell = air - 1; cell > solid; cell--) if (column.at(cell) > 0) return cell;
        return solid;
    }

    /** The slid terrain density at a column, {@code cell} cells over vanilla's floor. */
    private double density(int x, int z, int cell) { return density(terrain, x, z, cell); }

    private static double density(DensityFunction function, int x, int z, int cell) {
        int y = MIN_Y + cell * CELL_HEIGHT;
        return slide(function.compute(new DensityFunction.SinglePointContext(x, y, z)), y);
    }

    /** A cell corner column's densities, by cell; NaN until asked for. */
    private double[] corner(int x, int z) {
        long key = it.unimi.dsi.fastutil.HashCommon.mix(((long) x << 32) | (z & 0xFFFFFFFFL));
        double[] column = corners.get(key);
        if (column == null) {
            double[] fresh = new double[CELLS + 1];
            java.util.Arrays.fill(fresh, Double.NaN);
            if (corners.size() > 65536) corners.clear();
            column = corners.putIfAbsent(key, fresh);
            if (column == null) column = fresh;
        }
        return column;
    }

    private double corner(double[] column, int x, int z, int cell) {
        double value = column[cell];
        if (Double.isNaN(value)) column[cell] = value = density(x, z, cell);
        return value;
    }

    /**
     * The top solid block of vanilla's terrain in a block column, from the cells' corners interpolated as
     * vanilla does; vanilla's floor ({@link #MIN_Y}) where there is none, as its bottom slide fills it.
     * Read down from the top of vanilla's terrain, so land over an overhang or a detached ledge is found
     * (a search from a guess can stop on the ledge, putting sea where the land stands).
     */
    int height(int x, int z) {
        int x0 = Math.floorDiv(x, CELL_WIDTH) * CELL_WIDTH, z0 = Math.floorDiv(z, CELL_WIDTH) * CELL_WIDTH, x1 = x0 + CELL_WIDTH, z1 = z0 + CELL_WIDTH;
        double tx = (x - x0) / (double) CELL_WIDTH, tz = (z - z0) / (double) CELL_WIDTH;
        double[] a = corner(x0, z0), b = corner(x1, z0), c = corner(x0, z1), d = corner(x1, z1);
        for (int cell = CELLS - 1; cell >= 0; cell--) {
            double a0 = corner(a, x0, z0, cell), b0 = corner(b, x1, z0, cell), c0 = corner(c, x0, z1, cell), d0 = corner(d, x1, z1, cell);
            double a1 = corner(a, x0, z0, cell + 1), b1 = corner(b, x1, z0, cell + 1), c1 = corner(c, x0, z1, cell + 1), d1 = corner(d, x1, z1, cell + 1);
            // Interpolating values none of which is above zero gives none above zero: the whole cell is air.
            if (Math.max(Math.max(Math.max(a0, b0), Math.max(c0, d0)), Math.max(Math.max(a1, b1), Math.max(c1, d1))) <= 0) continue;
            double low = Mth.lerp2(tx, tz, a0, b0, c0, d0), high = Mth.lerp2(tx, tz, a1, b1, c1, d1);
            for (int dy = CELL_HEIGHT - 1; dy >= 0; dy--) {
                if (Mth.lerp(dy / (double) CELL_HEIGHT, low, high) > 0) return MIN_Y + cell * CELL_HEIGHT + dy;
            }
        }
        return MIN_Y;
    }

    /**
     * The terrain height at a column for far terrain, whose voxels are wider than vanilla's cells: the
     * {@linkplain #topCell top solid cell} of this one column, the top placed where the density crosses zero
     * between it and the cell above. Remembered per column, so a sample serves every far level whose grid
     * holds it, and its densities are not kept. {@code smooth} leaves out vanilla's 3D noise, for voxels wider
     * than its lumps: the reads are then 2D work the column shares, a few times cheaper.
     */
    int farHeight(int x, int z, boolean smooth) {
        long key = it.unimi.dsi.fastutil.HashCommon.mix(((long) x << 32) | (z & 0xFFFFFFFFL));
        ConcurrentHashMap<Long, Integer> heights = smooth ? smoothHeights : farHeights;
        Integer known = heights.get(key);
        if (known != null) return known;
        DensityFunction function = smooth ? this.smooth : terrain;
        double[] read = new double[CELLS + 1];
        java.util.Arrays.fill(read, Double.NaN);
        Cells column = cell -> { double v = read[cell]; if (Double.isNaN(v)) read[cell] = v = density(function, x, z, cell); return v; };
        int top = topCell(column, guessCell(x, z)), found;
        if (top < 0) found = MIN_Y;
        else if (top == CELLS) found = TOP;
        else {
            double here = column.at(top), upper = column.at(top + 1);
            double t = upper >= 0 ? 1 : here / (here - upper);
            found = MIN_Y + top * CELL_HEIGHT + Math.min(CELL_HEIGHT - 1, (int) (t * CELL_HEIGHT));
        }
        if (heights.size() > 262144) heights.clear();
        heights.put(key, found);
        return found;
    }

    // Biomes --------------------------------------------------------------------------------------

    /**
     * Vanilla's biome at a block, by its 4-block quart as vanilla's biome source gives it. Near the
     * surface a cave biome (vanilla can put lush caves under a shallow seabed) gives way to the one
     * above it.
     */
    Holder<Biome> biome(int x, int y, int z) {
        int qx = QuartPos.fromBlock(x), qy = QuartPos.fromBlock(y), qz = QuartPos.fromBlock(z);
        long key = net.minecraft.core.SectionPos.asLong(qx, qy, qz);
        Holder<Biome> cached = biomeCache.get(key);
        if (cached != null) return cached;
        Holder<Biome> biome = biomes.getNoiseBiome(qx, qy, qz, climate);
        for (int up = 1; up <= 8 && cave(biome); up++) biome = biomes.getNoiseBiome(qx, qy + up * 2, qz, climate);
        // The biome source's holders may come from another lookup than the registry the cubes' palettes use.
        Holder<Biome> source = biome;
        biome = biome.unwrapKey().flatMap(biomeRegistry::getHolder).<Holder<Biome>>map(h -> h).orElseGet(() -> biomeRegistry.wrapAsHolder(source.value()));
        if (biomeCache.size() > 65536) biomeCache.clear();
        biomeCache.putIfAbsent(key, biome);
        return biome;
    }

    private static boolean cave(Holder<Biome> biome) {
        return biome.is(Biomes.LUSH_CAVES) || biome.is(Biomes.DRIPSTONE_CAVES) || biome.is(Biomes.DEEP_DARK);
    }
}
