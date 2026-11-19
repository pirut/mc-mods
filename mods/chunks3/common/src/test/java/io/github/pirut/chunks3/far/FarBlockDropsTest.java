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

    /** A waterfall saved in a cube: falling water (vanilla's level 8) down a cliff, under its source, a block wide. */
    @Test void aWaterfallStandsAsFullWaterVoxelsUnderItsSurface() {
        LevelChunkSection section = new LevelChunkSection(io.github.pirut.chunks3.gen.SurfacePacketFixtures.biomes());
        var falling = Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 8);
        var flowing = Blocks.WATER.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 3);
        section.setBlockState(0, 0, 0, Blocks.STONE.defaultBlockState(), false);
        for (int y = 1; y < 12; y++) section.setBlockState(0, y, 0, FarServer.blockVoxel(falling), false);
        section.setBlockState(0, 12, 0, FarServer.blockVoxel(Blocks.WATER.defaultBlockState()), false);
        section.setBlockState(1, 1, 0, FarServer.blockVoxel(flowing), false);
        var waterlogged = Blocks.OAK_FENCE.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true);
        section.setBlockState(2, 1, 0, FarServer.blockVoxel(waterlogged), false);
        section.recalcBlockCounts();
        for (int y = 1; y <= 12; y++) assertEquals(1.0F, Far.fill(section.getBlockState(0, y, 0)), "every waterfall cell a whole voxel, so their sides meet");
        assertEquals(Blocks.WATER.defaultBlockState(), section.getBlockState(1, 1, 0), "flowing water a whole voxel too");
        assertEquals(waterlogged, section.getBlockState(2, 1, 0), "a waterlogged block stays one");
        FarColumn column = new FarColumn();
        column.sections.add(section);
        FarServer.blockDrops(column);
        assertEquals(Math.round((1 - Far.WATER_SURFACE) * Far.DROP_UNIT), column.drops[0], "the surface from the top water alone");
        assertEquals(0, column.drops[2], "a fence on top: no drop");
    }
}
