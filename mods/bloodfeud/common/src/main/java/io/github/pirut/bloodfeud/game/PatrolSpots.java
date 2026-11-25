package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.game.camp.SiteFinder;
import io.github.pirut.lib.Spots;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Set;

/**
 * Where a patrol may come out: ground a mob can stand on, with no light from blocks on it (no torch-lit base, no
 * lit camp), and no player block within {@value #CLEAR} blocks, by the same check that keeps camps off player
 * builds. A column whose height isn't known yet (on the Earth, ground nobody keeps loaded) is never read.
 */
final class PatrolSpots {
    static final int CLEAR = 6;

    private PatrolSpots() {
    }

    /** A spot between {@code min} and {@code max} blocks from {@code near}, or null. */
    static BlockPos find(ServerLevel level, BlockPos near, int min, int max, RandomSource random) {
        for (int i = 0; i < 24; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = min + random.nextDouble() * Math.max(1, max - min);
            int x = near.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = near.getZ() + (int) Math.round(Math.sin(angle) * dist);
            BlockPos spot = inColumn(level, x, near.getY(), z);
            if (spot != null) return spot;
        }
        return null;
    }

    /** A spot in column {@code (x, z)} near height {@code y}, or null. */
    static BlockPos inColumn(ServerLevel level, int x, int y, int z) {
        if (!level.hasChunk(x >> 4, z >> 4) || !Earth.knowsHeight(level, x, z)) return null;
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int startY = y < top - 8 ? y : top;
        for (int dy = 0; dy <= 10; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos p = new BlockPos(x, startY + dy * sign, z);
                if (!Earth.isLoaded(level, p.below()) || !Earth.isLoaded(level, p.above())) continue;
                if (allowed(level, p)) return p;
                if (dy == 0) break;
            }
        }
        return null;
    }

    static boolean allowed(ServerLevel level, BlockPos p) {
        return Spots.standable(level, p, false) && level.getBrightness(LightLayer.BLOCK, p) == 0
                && !SiteFinder.hasPlayerBuild(level, p, CLEAR, Set.of(), Set.of());
    }
}
