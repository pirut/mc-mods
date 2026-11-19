package io.github.pirut.chunks3.gen;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.junit.jupiter.api.Test;

class OverworldLodTest {
    private static final int[][] REGIONS = {{0, 0}, {3000, -2000}, {-6000, 4000}, {9000, 9000}, {-12000, -3000}, {20000, 5000}};

    @Test void centredHeightsAndBiomesUseTheActualVoxelPosition() {
        var biomes = SurfacePacketFixtures.biomes();
        var terrain = new VanillaTerrain(VanillaRegistries.createLookup(), biomes, 12345L);
        var g = new OverworldGenerator(12345L, biomes, terrain);
        int[] far = new int[2];
        int differentHeights = 0;
        for (int z = -6014; z <= -5970; z += 4) for (int x = -3230; x <= -3186; x += 4) {
            int expected = terrain.farHeight(x, z, false);
            if (expected != terrain.farHeight(x - 2, z - 2, false)) differentHeights++;
            g.farGround(2, x, z, far);
            assertEquals(expected, far[0]);
            assertEquals(terrain.biome(x, Math.max(expected, OverworldGenerator.SEA), z),
                    g.farSurface(2, x, z, far[0], far[1], 0).biome());
        }
        assertTrue(differentHeights > 0, "the fixture distinguishes centres from corners");
    }

    @Test void farTerrainAgreesWithNearCoverAndShorelines() {
        var g = OverworldGeneratorTest.generator();
        for (int level = 3; level <= 6; level++) {
            var result = compare(g, level, 8);
            assertTrue(result.surface > 90, result.toString());
            assertTrue(result.biome > 90, result.toString());
            assertTrue(result.water > 90, result.toString());
            assertTrue(result.farWaterCount > 0 && result.farWaterCount < result.count, "the fixture includes land and water");
        }
    }

    @Test void forestHasStandsOfItsOwnTreesAndClearings() {
        var g = OverworldGeneratorTest.generator();
        var b = SurfaceGenerator.holder(SurfacePacketFixtures.biomes(), Biomes.FOREST);
        var forest = new SurfaceGenerator.Surface(b, SurfaceGenerator.GRASS, SurfaceGenerator.DIRT, 60, 0, 2, 0, 0, false);
        int v = 16, size = 64, oak = 0, birch = 0, edges = 0;
        BlockState[] cover = new BlockState[size * size];
        for (int z = 0; z < size; z++) for (int x = 0; x < size; x++) {
            int i = z * size + x;
            cover[i] = g.canopy(forest, (x - size / 2) * v + v / 2, (z - size / 2) * v + v / 2, v);
            if (cover[i] != null) {
                assertTrue(cover[i].is(Blocks.OAK_LEAVES) || cover[i].is(Blocks.BIRCH_LEAVES));
                if (cover[i].is(Blocks.OAK_LEAVES)) oak++; else birch++;
            }
            if (x > 0) edges += forestEdge(cover[i], cover[i - 1], x % 4 != 0);
            if (z > 0) edges += forestEdge(cover[i], cover[i - size], z % 4 != 0);
        }
        int covered = oak + birch;
        assertTrue(oak > birch && birch > 0, "both oak and birch, with oak dominant");
        assertTrue(covered > size * size / 3 && covered < 3 * size * size / 4, "room for stands and clearings: " + covered);
        double chanceOfCover = covered / (double) cover.length;
        double randomEdges = 2 * chanceOfCover * (1 - chanceOfCover) * 2 * size * (size - 1);
        assertTrue(edges < randomEdges * 0.7, "cover and clearings cluster: " + edges + " edges vs " + randomEdges);
        for (int level = 3; level <= 4; level++) {
            assertTrue(forestColumn(g, level).sections.stream().mapToInt(OverworldLodTest::countLeaves).sum() > 0,
                    "forest canopy at level " + level);
        }
    }

    private static int forestEdge(BlockState a, BlockState b, boolean sameStand) {
        if (a != null && b != null && sameStand) assertSame(a.getBlock(), b.getBlock());
        return (a == null) != (b == null) ? 1 : 0;
    }

    @Test void vanillaDirtTagAllowsRealTreesInNearCubesAndFineLods() {
        var biomes = SurfacePacketFixtures.biomes();
        var blocks = BuiltInRegistries.BLOCK;
        var previous = blocks.getTags().collect(Collectors.toMap(p -> p.getFirst(), p -> p.getSecond().stream().toList()));
        var tags = new java.util.HashMap<>(previous);
        List<Holder<Block>> dirt = List.of(Blocks.DIRT, Blocks.GRASS_BLOCK,
                Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.MYCELIUM, Blocks.ROOTED_DIRT, Blocks.MOSS_BLOCK,
                Blocks.MUD, Blocks.MUDDY_MANGROVE_ROOTS).stream().map(blocks::wrapAsHolder).toList();
        tags.put(BlockTags.DIRT, dirt);
        blocks.bindTags(tags);
        try {
            var g = new OverworldGenerator(12345L, biomes, VanillaRegistries.createLookup());
            int nearLeaves = 0;
            for (int cz = -375; cz < -371; cz++) for (int cx = -201; cx < -197; cx++) {
                int cy = g.surfaceY(cx * 16 + 8, cz * 16 + 8) >> 4;
                for (int y = cy - 1; y <= cy + 2; y++) nearLeaves += countLeaves(g.generate(cx, y, cz));
            }
            assertTrue(nearLeaves > 0, "near leaves");
            for (int level = 1; level <= 2; level++) {
                FarColumn col = forestColumn(g, level);
                int leaves = col.sections.stream().mapToInt(OverworldLodTest::countLeaves).sum();
                assertTrue(leaves > 0, "rasterised leaves at level " + level);
                System.out.printf(Locale.ROOT, "LOD_REAL_TREES level=%d leaves=%d near=%d%n", level, leaves, nearLeaves);
            }
        } finally {
            blocks.bindTags(previous);
        }
    }

    private static int countLeaves(LevelChunkSection section) {
        if (section == null) return 0;
        int leaves = 0;
        for (int i = 0; i < 4096; i++) if (leaf(section.getBlockState(i & 15, i >> 8, i >> 4 & 15))) leaves++;
        return leaves;
    }

    @Test void audit() {
        assumeTrue("1".equals(System.getenv("CHUNKS3_LOD_BENCH")));
        var g = OverworldGeneratorTest.generator();
        for (int level = 2; level <= 8; level++) {
            compare(g, level, 12);
            forest(g, level);
        }
    }

    @Test void benchmarkColumns() {
        assumeTrue("1".equals(System.getenv("CHUNKS3_LOD_BENCH")));
        var biomes = SurfacePacketFixtures.biomes();
        var g = new OverworldGenerator(777L, biomes, VanillaRegistries.createLookup());
        for (int i = 0; i < 12; i++) g.farColumn(3, -20 + i, 19, new FarColumn());
        for (int level = 2; level <= 8; level++) {
            int columns = 24;
            long start = System.nanoTime();
            for (int i = 0; i < columns; i++) g.farColumn(level, 40 + i * 3, -17 + i * 5, new FarColumn());
            double cold = (System.nanoTime() - start) / 1e6 / columns;
            start = System.nanoTime();
            for (int i = 0; i < columns; i++) g.farColumn(level, 40 + i * 3, -17 + i * 5, new FarColumn());
            double warm = (System.nanoTime() - start) / 1e6 / columns;
            System.out.printf(Locale.ROOT, "LOD_TIME level=%d first=%.2f warm=%.2f ms/column%n", level, cold, warm);
        }
    }

    private record Agreement(double surface, double biome, double water, int farWaterCount, int count) {}

    private static Agreement compare(OverworldGenerator g, int level, int size) {
        int v = 1 << level, count = 0, land = 0, top = 0, biome = 0, wet = 0, wetCentre = 0, farWaterCount = 0;
        double meanError = 0, maxError = 0, centreError = 0, peakMiss = 0;
        int[] far = new int[2], near = new int[2], neighbour = new int[2];
        for (int[] region : REGIONS) {
            int startX = (region[0] >> level) << level, startZ = (region[1] >> level) << level;
            for (int j = 0; j < size; j++) for (int i = 0; i < size; i++) {
                int x = startX + i * v, z = startZ + j * v, cx = x + v / 2, cz = z + v / 2;
                g.farGround(level, cx, cz, far);
                int sum = 0, wetCount = 0, peak = Integer.MIN_VALUE;
                int divisions = Math.min(v, 4);
                for (int b = 0; b < divisions; b++) for (int a = 0; a < divisions; a++) {
                    int step = v / divisions;
                    g.ground(x + a * step + step / 2, z + b * step + step / 2, near);
                    sum += near[0];
                    peak = Math.max(peak, near[0]);
                    if (near[1] != SurfaceGenerator.DRY) wetCount++;
                }
                int points = divisions * divisions;
                g.ground(cx, cz, near);
                double error = Math.abs(far[0] - sum / (double) points);
                meanError += error;
                maxError = Math.max(error, maxError);
                centreError += Math.abs(far[0] - near[0]);
                peakMiss += Math.max(0, peak - far[0]);
                boolean farWet = far[1] != SurfaceGenerator.DRY, nearWet = near[1] != SurfaceGenerator.DRY;
                if (farWet) farWaterCount++;
                if (farWet == (wetCount * 2 == points ? nearWet : wetCount * 2 > points)) wet++;
                if (farWet == nearWet) wetCentre++;
                if (!farWet && !nearWet) {
                    land++;
                    int slope = slope(g, level, cx, cz, v, neighbour);
                    var fs = g.farSurface(level, cx, cz, far[0], far[1], slope);
                    var ns = g.coverAt(cx, cz, near[0], near[1]);
                    if (fs.top() == ns.top()) top++;
                    if (fs.biome().is(ns.biome().unwrapKey().orElseThrow())) biome++;
                }
                count++;
            }
        }
        var agreement = new Agreement(top * 100.0 / land, biome * 100.0 / land, wet * 100.0 / count, farWaterCount, count);
        System.out.printf(Locale.ROOT, "LOD_AGREEMENT level=%d n=%d mean=%.2f max=%.2f centre=%.2f peakMiss=%.2f surface=%.1f%% biome=%.1f%% water=%.1f%% waterCentre=%.1f%%%n",
                level, count, meanError / count, maxError, centreError / count, peakMiss / count,
                agreement.surface, agreement.biome, agreement.water, wetCentre * 100.0 / count);
        return agreement;
    }

    private static int slope(OverworldGenerator g, int level, int x, int z, int v, int[] scratch) {
        g.farGround(level, x - v, z, scratch); int west = scratch[0];
        g.farGround(level, x + v, z, scratch); int east = scratch[0];
        g.farGround(level, x, z - v, scratch); int north = scratch[0];
        g.farGround(level, x, z + v, scratch); int south = scratch[0];
        return (Math.abs(east - west) + Math.abs(south - north)) / v;
    }

    private static FarColumn forestColumn(OverworldGenerator g, int level) {
        int width = 16 << level;
        FarColumn col = new FarColumn();
        g.farColumn(level, Math.floorDiv(-3216, width), Math.floorDiv(-6000, width), col, false);
        return col;
    }

    private static void forest(OverworldGenerator g, int level) {
        FarColumn col = forestColumn(g, level);
        BlockState[] tops = new BlockState[256];
        int leaves = 0, edges = 0, both = 0, same = 0;
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            tops[z * 16 + x] = top(col, x, z);
            if (leaf(tops[z * 16 + x])) leaves++;
        }
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int i = z * 16 + x;
            if (x < 15) {
                if (leaf(tops[i]) != leaf(tops[i + 1])) edges++;
                if (leaf(tops[i]) && leaf(tops[i + 1])) { both++; if (tops[i].is(tops[i + 1].getBlock())) same++; }
            }
            if (z < 15) {
                if (leaf(tops[i]) != leaf(tops[i + 16])) edges++;
                if (leaf(tops[i]) && leaf(tops[i + 16])) { both++; if (tops[i].is(tops[i + 16].getBlock())) same++; }
            }
        }
        System.out.printf(Locale.ROOT, "LOD_FOREST level=%d leaves=%d edges=%d sameSpecies=%d/%d%n", level, leaves, edges, same, both);
    }

    private static boolean leaf(BlockState state) { return state != null && state.getBlock() instanceof LeavesBlock; }

    private static BlockState top(FarColumn column, int x, int z) {
        for (int i = column.sections.size() - 1; i >= 0; i--) {
            var section = column.sections.get(i);
            if (section == null) continue;
            for (int y = 15; y >= 0; y--) {
                BlockState state = section.getBlockState(x, y, z);
                if (!state.isAir()) return state;
            }
        }
        return null;
    }
}
