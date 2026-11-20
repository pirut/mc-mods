package io.github.pirut.chunks3.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.pirut.chunks3.far.Far;
import java.util.Locale;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.junit.jupiter.api.Test;

/**
 * Level 0, the block-sized far columns zoomed views ask for, in a Cubic world: the near ground's own blocks, and
 * how long each level's columns take to make on one thread.
 */
class FarBlockLevelTest {
    @Test void blockColumnsHoldTheNearGroundsBlocks() {
        OverworldGenerator g = OverworldGeneratorTest.generator();
        assertTrue(g.farBlocks());
        int same = 0, total = 0;
        for (int[] at : new int[][] {{1248, 1600}, {0, 0}, {-3000, 2200}, {5100, -700}, {800, 4000}}) {
            int columnX = at[0] >> 4, columnZ = at[1] >> 4;
            FarColumn far = new FarColumn();
            g.farColumn(Far.FINEST_LEVEL, columnX, columnZ, far);
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                int bx = (columnX << 4) + x, bz = (columnZ << 4) + z, y = g.surfaceY(bx, bz);
                LevelChunkSection cube = g.generate(columnX, y >> 4, columnZ);
                BlockState near = cube.getBlockState(x, y & 15, z);
                int index = (y >> 4) - far.bottom;
                LevelChunkSection section = index >= 0 && index < far.sections.size() ? far.sections.get(index) : null;
                BlockState block = section == null ? null : section.getBlockState(x, y & 15, z);
                total++;
                if (near.equals(block)) same++;
                // The column stands exactly at the ground: no drop on dry land.
                if (g.waterY(bx, bz) < y) assertTrue(far.drops[z * 16 + x] == 0, "dry ground is not lowered");
            }
        }
        System.out.printf(Locale.ROOT, "FAR_BLOCKS same top block as the near ground: %d of %d%n", same, total);
        // The cubes carve caves and set structures the far columns leave out; a cave mouth now and then opens the ground.
        assertTrue(same >= total * 0.95, same + " of " + total);
    }

    @Test void eachLevelsColumnsTakeThisLong() {
        OverworldGenerator g = OverworldGeneratorTest.generator();
        StringBuilder out = new StringBuilder();
        for (int level = Far.FINEST_LEVEL; level <= Far.MAX_LEVEL; level++) {
            int n = 24;
            // Columns away from the others' ground, so caches warmed by another level do not count.
            int base = 40_000 + level * 9_000;
            for (int i = 0; i < 16; i++) g.farColumnCompact(level, (base >> (level + 4)) - 20 - i, 0, new FarColumn());
            long start = System.nanoTime();
            for (int i = 0; i < n; i++) g.farColumnCompact(level, (base >> (level + 4)) + i, 3, new FarColumn());
            double ms = (System.nanoTime() - start) / 1e6 / n;
            String stand = "none (zoomed views only)";
            if (g.farPreviews(level)) {
                start = System.nanoTime();
                for (int i = 0; i < n; i++) g.farPreview(level, (base >> (level + 4)) + i, 7, new FarColumn());
                stand = String.format(Locale.ROOT, "%.1f ms", (System.nanoTime() - start) / 1e6 / n);
            }
            out.append(String.format(Locale.ROOT, "FAR_MAKE level %d (%d-block voxels): %.1f ms a column on one thread, its quick stand-in %s%n",
                    level, Far.voxel(level), ms, stand));
        }
        System.out.print(out);
    }
}
