package io.github.pirut.chunks3.gen;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Locale;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.junit.jupiter.api.Test;

/** The Cubic world type's generator: vanilla's heights and sea on top, deep rock below, bedrock far down. */
class OverworldGeneratorTest {
    private static OverworldGenerator generator;

    static synchronized OverworldGenerator generator() {
        if (generator == null) generator = new OverworldGenerator(12345L, SurfacePacketFixtures.biomes(), VanillaRegistries.createLookup());
        return generator;
    }

    @Test void groundStandsAtVanillasHeights() {
        OverworldGenerator g = generator();
        int land = 0, sea = 0, low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int z = -4096; z <= 4096; z += 256) for (int x = -4096; x <= 4096; x += 256) {
            int y = g.surfaceY(x, z);
            low = Math.min(low, y); high = Math.max(high, y);
            if (g.waterY(x, z) >= y) { sea++; assertEquals(OverworldGenerator.SEA, g.waterY(x, z)); } else land++;
        }
        assertTrue(low >= -64 && high <= 256, "heights within vanilla's: " + low + " to " + high);
        assertTrue(land > 50 && sea > 10, "both land and sea: " + land + " land, " + sea + " sea");
        assertTrue(high > 100, "some hills or mountains: highest " + high);
    }

    /**
     * The near ground is the true top of vanilla's terrain, over overhangs and detached ledges too: at this
     * column of seed 12345 a search from a guess found a ledge at Y 49 under land standing at Y 152.
     */
    @Test void nearGroundIsTheTopOverLedges() {
        OverworldGenerator g = generator();
        int y = g.surfaceY(1248, 1600);
        System.out.println("GEN_LEDGE surfaceY(1248, 1600) = " + y + ", water " + g.waterY(1248, 1600));
        assertEquals(152, y);
        assertTrue(g.waterY(1248, 1600) < y, "dry land, not sea");
    }

    @Test void farHeightsFollowTheNearGround() {
        OverworldGenerator g = generator();
        for (int level : new int[]{3, OverworldGenerator.SMOOTH_LEVEL}) {
            long total = 0;
            int n = 0;
            for (int z = -2048; z <= 2048; z += 64) for (int x = -2048; x <= 2048; x += 64) {
                int[] near = new int[2], far = new int[2];
                g.ground(x, z, near);
                g.farGround(level, x, z, far);
                total += Math.abs(near[0] - far[0]);
                n++;
            }
            double mean = total / (double) n;
            System.out.printf(Locale.ROOT, "GEN_FAR_ERROR level=%d meanBlocks=%.2f%n", level, mean);
            assertTrue(mean < (level >= OverworldGenerator.SMOOTH_LEVEL ? 6 : 3), "far heights near the near ground at level " + level + ": " + mean);
        }
    }

    private static int count(LevelChunkSection section, BlockState state) {
        int found = 0;
        for (int i = 0; i < 4096; i++) if (section.getBlockState(i & 15, i >> 8, i >> 4 & 15) == state) found++;
        return found;
    }

    @Test void rockGoesOnDownToADeepBedrockFloor() {
        OverworldGenerator g = generator();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        // Vanilla's floor is gone: no bedrock at Y -64, and rock (deepslate) under it.
        LevelChunkSection oldFloor = g.generate(0, -64 >> 4, 0);
        assertEquals(0, count(oldFloor, bedrock));
        LevelChunkSection deep = g.generate(3, -1000 >> 4, -7);
        assertEquals(0, count(deep, bedrock));
        assertTrue(count(deep, Blocks.DEEPSLATE.defaultBlockState()) > 1000, "deep rock is deepslate");
        // The floor: the cube holding its lowest block is mostly bedrock, and everything under it is.
        int floor = OverworldGenerator.BEDROCK_Y;
        assertTrue(count(g.generate(0, floor >> 4, 0), bedrock) > 256);
        assertEquals(4096, count(g.generate(0, (floor >> 4) - 1, 0), bedrock));
    }

    /** Opt-in: what one far sample costs, its height search against its biome lookup. */
    @Test void benchParts() {
        assumeTrue("1".equals(System.getenv("CHUNKS3_GEN_BENCH")));
        VanillaTerrain t = new VanillaTerrain(VanillaRegistries.createLookup(), SurfacePacketFixtures.biomes(), 99L);
        for (int round = 0; round < 3; round++) {
            int n = 4000, base = round * 1_000_000;
            long start = System.nanoTime();
            for (int i = 0; i < n; i++) t.farHeight(base + i * 64, i * 32, false);
            double full = (System.nanoTime() - start) / 1e3 / n;
            start = System.nanoTime();
            for (int i = 0; i < n; i++) t.farHeight(base + i * 64 + 32, i * 32, true);
            double height = (System.nanoTime() - start) / 1e3 / n;
            start = System.nanoTime();
            for (int i = 0; i < n; i++) t.biome(base + i * 64, 70, i * 32 + 7);
            double biome = (System.nanoTime() - start) / 1e3 / n;
            start = System.nanoTime();
            for (int i = 0; i < n / 10; i++) t.height(base + 500_000 + i * 64, i * 32);
            double near = (System.nanoTime() - start) / 1e3 / (n / 10);
            System.out.printf(Locale.ROOT, "GEN_PARTS round=%d farHeightUs=%.1f smoothFarHeightUs=%.1f biomeUs=%.1f nearHeightUs=%.1f%n", round, full, height, biome, near);
        }
    }

    /** Opt-in timings (CHUNKS3_GEN_BENCH=1): far columns and near cube columns, vanilla terrain against Highlands. */
    @Test void bench() {
        assumeTrue("1".equals(System.getenv("CHUNKS3_GEN_BENCH")));
        var biomes = SurfacePacketFixtures.biomes();
        for (String name : new String[]{"highlands", "overworld"}) {
            SurfaceGenerator g = name.equals("highlands") ? new HighlandsGenerator(777L, biomes)
                    : new OverworldGenerator(777L, biomes, VanillaRegistries.createLookup());
            for (int level = 1; level <= 8; level++) {
                int columns = 24;
                long start = System.nanoTime();
                for (int i = 0; i < columns; i++) g.farColumn(level, 40 + i * 3, -17 + i * 5, new FarColumn());
                double cold = (System.nanoTime() - start) / 1e6 / columns;
                start = System.nanoTime();
                for (int i = 0; i < columns; i++) g.farColumn(level, 40 + i * 3, -17 + i * 5, new FarColumn());
                double warm = (System.nanoTime() - start) / 1e6 / columns;
                System.out.printf(Locale.ROOT, "GEN_BENCH %s far level=%d firstMsPerColumn=%.2f againMsPerColumn=%.2f%n", name, level, cold, warm);
            }
            long start = System.nanoTime();
            int cubes = 0;
            for (int c = 0; c < 6; c++) {
                int cx = 200 + c * 7, cz = -90 + c * 3, top = g.surfaceY((cx << 4) + 8, (cz << 4) + 8) >> 4;
                for (int cy = top - 3; cy <= top + 1; cy++) { g.generate(cx, cy, cz); cubes++; }
            }
            System.out.printf(Locale.ROOT, "GEN_BENCH %s near msPerCube=%.2f (surface cubes, first visit)%n", name, (System.nanoTime() - start) / 1e6 / cubes);
        }
    }
}
