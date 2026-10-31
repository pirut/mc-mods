package io.github.pirut.seaworthy.sable;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import io.github.pirut.seaworthy.hull.HullGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;

/**
 * Copies a ship's blocks into a {@link HullGrid} on the game thread, so the analysis can run on another one.
 *
 * @param origin  plot position of grid cell (0, 0, 0): one block below and outside the ship's bounds
 * @param blocks  how many cells hold a block, to notice a ship whose chunks haven't arrived yet
 */
public record HullSnapshot(HullGrid grid, BlockPos origin, int blocks) {

    /** A snapshot of the sub-level's blocks, or null if it has none or is bigger than {@code maxCells}. */
    public static @Nullable HullSnapshot take(SubLevel subLevel, long maxCells) {
        BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        int minX = bounds.minX(), minY = bounds.minY(), minZ = bounds.minZ();
        int maxX = bounds.maxX(), maxY = bounds.maxY(), maxZ = bounds.maxZ();
        if (maxX < minX || maxY < minY || maxZ < minZ) return null;
        int sx = maxX - minX + 3, sy = maxY - minY + 3, sz = maxZ - minZ + 3;
        if ((long) sx * sy * sz > maxCells) return null;

        Level level = subLevel.getLevel();
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        HullGrid grid = new HullGrid(sx, sy, sz);
        BlockPos origin = new BlockPos(minX - 1, minY - 1, minZ - 1);
        int blocks = 0;

        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                LevelChunk chunk = chunk(level, container, cx, cz);
                if (chunk == null) continue;
                int x0 = Math.max(minX, cx << 4), x1 = Math.min(maxX, (cx << 4) + 15);
                int z0 = Math.max(minZ, cz << 4), z1 = Math.min(maxZ, (cz << 4) + 15);
                for (int y = minY; y <= maxY; y++) {
                    int sectionIndex = chunk.getSectionIndex(y);
                    if (sectionIndex < 0 || sectionIndex >= chunk.getSectionsCount()) continue;
                    LevelChunkSection section = chunk.getSection(sectionIndex);
                    if (section.hasOnlyAir()) continue;
                    for (int x = x0; x <= x1; x++) {
                        for (int z = z0; z <= z1; z++) {
                            BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                            if (state.isAir()) continue;
                            int packed = BlockFlags.of(state);
                            grid.set(x - origin.getX(), y - origin.getY(), z - origin.getZ(),
                                    BlockFlags.flags(packed), BlockFlags.air(packed));
                            blocks++;
                        }
                    }
                }
            }
        }
        return new HullSnapshot(grid, origin, blocks);
    }

    private static @Nullable LevelChunk chunk(Level level, @Nullable SubLevelContainer container, int cx, int cz) {
        if (container != null) {
            LevelChunk chunk = container.getChunk(new ChunkPos(cx, cz));
            if (chunk != null) return chunk;
        }
        return level.getChunkSource().getChunkNow(cx, cz);
    }
}
