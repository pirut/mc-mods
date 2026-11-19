package io.github.pirut.chunks3.far;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.pirut.chunks3.gen.FarColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.junit.jupiter.api.Test;

/** Level 0 columns with saved cubes folded in: each voxel column's drop follows what now stands on top. */
class FarBlockDropsTest {
    @Test void waterOnTopDropsANinthOfABlockAndGroundNone() {
        LevelChunkSection section = new LevelChunkSection(io.github.pirut.chunks3.gen.SurfacePacketFixtures.biomes());
        // A pond a player filled, over stone; and grass where a pond was drained.
        section.setBlockState(0, 4, 0, Blocks.STONE.defaultBlockState(), false);
        section.setBlockState(0, 5, 0, Blocks.WATER.defaultBlockState(), false);
        section.setBlockState(1, 3, 0, Blocks.GRASS_BLOCK.defaultBlockState(), false);
        section.recalcBlockCounts();
        FarColumn column = new FarColumn();
        column.sections.add(section);
        java.util.Arrays.fill(column.drops, (short) 99);
        FarServer.blockDrops(column);
        assertEquals(Math.round((1 - Far.WATER_SURFACE) * Far.DROP_UNIT), column.drops[0]);
        assertEquals(0, column.drops[1]);
        assertEquals(0, column.drops[2], "an empty voxel column has no drop");
    }
}
