package io.github.pirut.chunks3.gen;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

/**
 * The Cubic world type's generator: vanilla's overworld at vanilla's heights (its terrain, sea level and
 * biomes, from {@link VanillaTerrain}), with no floor at Y -64. The rock, caves and ores carry on down for
 * thousands of blocks to a bedrock floor at {@link #BEDROCK_Y} ({@code -Dchunks3.cubic.bedrockY}). Vanilla's
 * own layers lie at its own heights: deepslate from Y 8 down, lava in the caves from Y -54 to -64, then a
 * few blocks of sealed rock under them, and below that open caves again down to a layer of lava lakes just
 * above the bedrock.
 *
 * <p>The surface cover (grass, sand, snow, trees and plants) follows each vanilla biome through the engine's
 * {@link SurfaceGenerator} rules rather than vanilla's surface rules and features, so the ground looks
 * vanilla without being block for block what a vanilla world of the same seed would hold.
 */
public final class OverworldGenerator extends SurfaceGenerator {
    public static final String ID = "overworld-v1";
    /** The lowest block of the bedrock floor. */
    public static final int BEDROCK_Y = Integer.getInteger("chunks3.cubic.bedrockY", -4096);
    /** Vanilla's sea: water up to Y 62, under its sea level of 63. */
    public static final int SEA = 62;
    /** Vanilla's heights of the deep lava: caves from Y -54 down to -64 hold it, and rock seals it from below. */
    private static final int LAVA_TOP = -54, LAVA_BOTTOM = -64, SEAL = 4;
    /** How far above the bedrock its lava lakes reach. */
    private static final int DEEP_LAVA = 10;

    /**
     * The first far level drawn without vanilla's 3D terrain noise ({@code -Dchunks3.cubic.smoothLevel}): from
     * 32-block voxels out its lumps are smaller than a voxel, and heights move by a few blocks at most.
     */
    static final int SMOOTH_LEVEL = Integer.getInteger("chunks3.cubic.smoothLevel", 5);

    private final VanillaTerrain vanilla;

    public OverworldGenerator(long seed, RegistryAccess registries) {
        this(seed, registries.registryOrThrow(Registries.BIOME), registries);
    }

    /** From any worldgen lookup (vanilla's built-in one in tests), with the cubes' biomes from {@code biomes}. */
    OverworldGenerator(long seed, Registry<Biome> biomes, net.minecraft.core.HolderLookup.Provider lookup) {
        this(seed, biomes, new VanillaTerrain(lookup, biomes, seed));
    }

    OverworldGenerator(long seed, Registry<Biome> biomes, VanillaTerrain vanilla) {
        super(seed, biomes, new LegacyRandomSource(seed ^ 0x4F56455257L));
        this.vanilla = vanilla;
    }

    @Override public String id() { return ID; }
    @Override public int seaLevel() { return SEA; }
    @Override protected int shoreLevel() { return SEA; }
    @Override protected int datum() { return 64; }
    @Override protected int bedrockY() { return BEDROCK_Y; }
    @Override public int highestGenerated() { return VanillaTerrain.TOP + 64; }
    @Override protected boolean emeralds(int maxGround) { return maxGround > 140; }
    /** Mountains and snow are no place to start; nor is the ground of a biome that freezes. */
    @Override public boolean barren(int x, int z, int y) { return y > 140 || biome(x, z, y, DRY).value().getBaseTemperature() < 0.15f; }

    @Override protected void ground(int x, int z, int[] out) {
        int y = vanilla.height(x, z);
        out[0] = y;
        out[1] = y < SEA ? SEA : DRY;
    }

    /**
     * Far terrain reads vanilla's noise only as finely as each level needs, after SeedGen's way of filling
     * distant terrain straight from the seed: from 4-block voxels out, one {@linkplain VanillaTerrain#farHeight
     * column search} per voxel, at the voxel's corner, so each level's samples are also every coarser level's.
     */
    @Override protected void farGround(int level, int x, int z, int[] out) {
        if (level < 2) { ground(x, z, out); return; }
        int v = 1 << level, sx = Math.floorDiv(x, v) * v, sz = Math.floorDiv(z, v) * v;
        int y = vanilla.farHeight(sx, sz, level >= SMOOTH_LEVEL);
        out[0] = y;
        out[1] = y < SEA ? SEA : DRY;
        // The biome now, while the column's continents, erosion and ridges are still at hand: farSurface asks for it next.
        biome(sx, sz, out[0], out[1]);
    }

    /** A far voxel's cover, from one biome lookup at the same corner its height came from. */
    @Override protected Surface farSurface(int level, int x, int z, int ground, int water, int slope) {
        if (level < 2) return surface(x, z, ground, water, slope);
        int v = 1 << level;
        return surface(Math.floorDiv(x, v) * v, Math.floorDiv(z, v) * v, ground, water, slope);
    }

    @Override @Nullable protected BlockState cavity(int y, int floor) {
        if (y < BEDROCK_Y + DEEP_LAVA) return LAVA;
        if (y < LAVA_TOP && y >= LAVA_BOTTOM) return LAVA;
        if (y < LAVA_BOTTOM && y >= LAVA_BOTTOM - SEAL) return null;
        return AIR;
    }

    /** Vanilla's biome over a column: at its ground, or at the sea's surface over water. */
    private Holder<Biome> biome(int x, int z, int ground, int water) {
        return vanilla.biome(x, Math.max(ground, water == DRY ? ground : water), z);
    }

    private static final BlockState PODZOL = Blocks.PODZOL.defaultBlockState(), MYCELIUM = Blocks.MYCELIUM.defaultBlockState(),
            COARSE_DIRT = Blocks.COARSE_DIRT.defaultBlockState(), RED_SAND = Blocks.RED_SAND.defaultBlockState(),
            SANDSTONE = Blocks.SANDSTONE.defaultBlockState(), TERRACOTTA = Blocks.TERRACOTTA.defaultBlockState(),
            ORANGE_TERRACOTTA = Blocks.ORANGE_TERRACOTTA.defaultBlockState(), PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState();

    @Override protected Surface surface(int x, int z, int y, int water, int slope) {
        Holder<Biome> b = biome(x, z, y, water);
        Surface land = land(b, x, z, y, slope);
        if (water <= y) return land;
        // Under water: vanilla's seabeds are gravel and sand, its riverbeds and lake floors sand and dirt.
        BlockState floor = b.is(Biomes.WARM_OCEAN) || b.is(Biomes.LUKEWARM_OCEAN) || b.is(Biomes.DEEP_LUKEWARM_OCEAN) || b.is(BiomeTags.IS_BEACH)
                ? SAND : b.is(BiomeTags.IS_OCEAN) ? (water - y > 12 ? GRAVEL : SAND) : b.is(BiomeTags.IS_RIVER) ? SAND
                : land.top() == GRASS || land.top() == PODZOL || land.top() == MYCELIUM || land.top() == SNOW_BLOCK ? DIRT : land.top();
        BlockState under = floor == DIRT ? DIRT : floor == SAND ? SAND : land.under();
        return new Surface(b, floor, under, b.is(Biomes.MANGROVE_SWAMP) ? land.trees() : 0, 0, 0, 0, 0, false, land.broadleaf());
    }

    /** What covers a dry column of a vanilla biome. */
    private Surface land(Holder<Biome> b, int x, int z, int y, int slope) {
        boolean steep = slope > 6;
        long roll = mix(x, 0x5355524CL, z);
        if (b.is(Biomes.DESERT)) return new Surface(b, SAND, SANDSTONE, 0, 0, 0, 20, 26, false);
        if (b.is(Biomes.BEACH) || b.is(Biomes.SNOWY_BEACH)) return new Surface(b, SAND, SAND, 0, 0, 0, 0, 0, false);
        if (b.is(Biomes.STONY_SHORE) || b.is(Biomes.STONY_PEAKS)) return new Surface(b, STONE, STONE, 0, 0, 0, 0, 0, false);
        if (b.is(Biomes.BADLANDS) || b.is(Biomes.ERODED_BADLANDS))
            return new Surface(b, steep ? (Math.floorMod(y, 5) < 2 ? ORANGE_TERRACOTTA : TERRACOTTA) : RED_SAND, TERRACOTTA, 0, 0, 0, 10, 14, false);
        if (b.is(Biomes.WOODED_BADLANDS))
            return new Surface(b, y > 100 ? (Math.floorMod(roll, 3) == 0 ? COARSE_DIRT : GRASS) : RED_SAND, TERRACOTTA, y > 100 ? 8 : 0, 0, 0, 120, 120, false);
        if (b.is(Biomes.FROZEN_PEAKS)) return new Surface(b, steep ? PACKED_ICE : SNOW_BLOCK, STONE, 0, 0, 0, 0, 0, false);
        if (b.is(Biomes.JAGGED_PEAKS) || b.is(Biomes.SNOWY_SLOPES))
            return new Surface(b, steep ? STONE : SNOW_BLOCK, STONE, 0, 0, 0, 0, 0, false);
        if (b.is(Biomes.ICE_SPIKES)) return new Surface(b, SNOW_BLOCK, DIRT, 0, 0, 0, 0, 0, false);
        if (b.is(Biomes.GROVE)) return new Surface(b, SNOW_BLOCK, DIRT, steep ? 0 : 30, 10, 0, 0, 0, false);
        if (b.is(Biomes.MUSHROOM_FIELDS)) return new Surface(b, MYCELIUM, DIRT, 0, 0, 0, 0, 0, false);
        if (b.is(Biomes.WINDSWEPT_GRAVELLY_HILLS)) return new Surface(b, steep ? STONE : GRAVEL, GRAVEL, 2, 7, 0, 60, 60, false);
        if (b.is(Biomes.WINDSWEPT_HILLS)) return new Surface(b, steep ? STONE : GRASS, steep ? STONE : DIRT, 3, 7, 0, 200, 210, false);
        if (b.is(Biomes.WINDSWEPT_FOREST)) return new Surface(b, steep ? STONE : GRASS, DIRT, 35, 7, 0, 200, 210, false);
        if (b.is(Biomes.MEADOW)) return new Surface(b, GRASS, DIRT, 1, 0, 3, 420, 500, false);
        if (b.is(Biomes.CHERRY_GROVE)) return new Surface(b, GRASS, DIRT, 20, 0, 0, 380, 460, false, Tree.CHERRY);
        if (b.is(Biomes.SAVANNA) || b.is(Biomes.SAVANNA_PLATEAU)) return new Surface(b, GRASS, DIRT, 6, 0, 0, 450, 460, false, Tree.ACACIA);
        if (b.is(Biomes.WINDSWEPT_SAVANNA)) return new Surface(b, steep ? STONE : Math.floorMod(roll, 4) == 0 ? COARSE_DIRT : GRASS, DIRT, 4, 0, 0, 350, 360, false, Tree.ACACIA);
        if (b.is(Biomes.JUNGLE)) return new Surface(b, GRASS, DIRT, 90, 0, 0, 500, 520, true, Tree.JUNGLE);
        if (b.is(Biomes.SPARSE_JUNGLE)) return new Surface(b, GRASS, DIRT, 25, 0, 0, 450, 470, true, Tree.JUNGLE);
        if (b.is(Biomes.BAMBOO_JUNGLE)) return new Surface(b, Math.floorMod(roll, 3) == 0 ? PODZOL : GRASS, DIRT, 60, 0, 0, 500, 510, true, Tree.JUNGLE);
        if (b.is(Biomes.MANGROVE_SWAMP)) return new Surface(b, MUD, MUD, 45, 0, 0, 0, 0, false, Tree.MANGROVE);
        if (b.is(Biomes.SWAMP)) return new Surface(b, GRASS, DIRT, 12, 0, 0, 400, 420, false);
        if (b.is(Biomes.DARK_FOREST)) return new Surface(b, GRASS, DIRT, 85, 0, 1, 150, 170, false, Tree.DARK_OAK);
        if (b.is(Biomes.BIRCH_FOREST) || b.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) return new Surface(b, GRASS, DIRT, 60, 0, 10, 200, 240, false);
        if (b.is(Biomes.FLOWER_FOREST)) return new Surface(b, GRASS, DIRT, 35, 0, 2, 150, 450, false);
        if (b.is(Biomes.FOREST)) return new Surface(b, GRASS, DIRT, 60, 0, 2, 200, 230, false);
        if (b.is(Biomes.OLD_GROWTH_PINE_TAIGA) || b.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA))
            return new Surface(b, Math.floorMod(roll, 3) == 0 ? COARSE_DIRT : PODZOL, DIRT, 70, 10, 0, 300, 310, true);
        if (b.is(Biomes.SNOWY_TAIGA)) return new Surface(b, GRASS, DIRT, 50, 10, 0, 200, 205, true);
        if (b.is(Biomes.TAIGA)) return new Surface(b, GRASS, DIRT, 60, 10, 0, 300, 310, true);
        if (b.is(Biomes.SNOWY_PLAINS)) return new Surface(b, GRASS, DIRT, 1, 10, 0, 100, 102, false);
        if (b.is(Biomes.SUNFLOWER_PLAINS)) return new Surface(b, GRASS, DIRT, 1, 0, 1, 320, 380, false);
        // Plains, and any biome another mod adds: grassland with the odd tree, stony where it is steep and high.
        BlockState top = steep && y > 110 ? STONE : GRASS;
        return new Surface(b, top, top == STONE ? STONE : DIRT, 1, 0, 1, 300, 330, false);
    }
}
