package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.game.camp.SiteFinder;
import io.github.pirut.lib.Spots;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Where any mob a pack sends out may come out: its Brutes, escorts, Packcaller grunts and garrison grunts alike.
 * One rule: ground a mob can stand on, with no light from blocks on it, and nothing within {@value #CLEAR} blocks that
 * isn't natural (by the same check that keeps camps off player builds). Inside a pack's own settlement its own
 * blocks and its own light are allowed: the camp's blocks are the pack's by its record of what it placed (a
 * settlement from before that record goes by its footprint alone). A column whose height isn't known yet (on the
 * Earth, ground nobody keeps loaded) is never read.
 */
final class PatrolSpots {
    static final int CLEAR = 6;

    private PatrolSpots() {
    }

    static BlockPos find(ServerLevel level, BlockPos near, int min, int max, RandomSource random) {
        return find(level, near, min, max, random, null);
    }

    /** A spot between {@code min} and {@code max} blocks from {@code near}, or null. */
    static BlockPos find(ServerLevel level, BlockPos near, int min, int max, RandomSource random, Pack pack) {
        for (int i = 0; i < 24; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = min + random.nextDouble() * Math.max(1, max - min);
            int x = near.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = near.getZ() + (int) Math.round(Math.sin(angle) * dist);
            BlockPos spot = inColumn(level, x, near.getY(), z, pack);
            if (spot != null) return spot;
        }
        return null;
    }

    static BlockPos inColumn(ServerLevel level, int x, int y, int z) {
        return inColumn(level, x, y, z, null);
    }

    /** A spot in column {@code (x, z)} near height {@code y}, or null. */
    static BlockPos inColumn(ServerLevel level, int x, int y, int z, Pack pack) {
        if (!level.hasChunk(x >> 4, z >> 4) || !Earth.knowsHeight(level, x, z)) return null;
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int startY = y < top - 8 ? y : top;
        for (int dy = 0; dy <= 10; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos p = new BlockPos(x, startY + dy * sign, z);
                if (allowed(level, p, pack)) return p;
                if (dy == 0) break;
            }
        }
        return null;
    }

    static boolean allowed(ServerLevel level, BlockPos p) {
        return allowed(level, p, null);
    }

    /** The one rule, at one spot. */
    static boolean allowed(ServerLevel level, BlockPos p, Pack pack) {
        if (!Earth.knowsHeight(level, p.getX(), p.getZ())) return false;
        if (!Earth.isLoaded(level, p.below()) || !Earth.isLoaded(level, p.above())) return false;
        if (!Spots.standable(level, p, false)) return false;
        boolean home = inOwnSettlement(pack, p);
        if (!home && level.getBrightness(LightLayer.BLOCK, p) > 0) return false;
        BlockPos origin = home ? new BlockPos(pack.siteX, pack.siteY, pack.siteZ) : p;
        if (home && pack.placed.isEmpty()) return SiteFinder.areBoundsLoaded(level, p.getX() - CLEAR, p.getZ() - CLEAR,
                p.getX() + CLEAR, p.getZ() + CLEAR); // an older settlement: its footprint is its own
        SiteFinder.Owned own = home ? new SiteFinder.Owned(pack.placed) : null;
        java.util.Set<io.github.pirut.bloodfeud.game.camp.CampBlueprint.Spot> owned = own == null ? java.util.Set.of() : own;
        if (!SiteFinder.areBoundsLoaded(level, p.getX() - CLEAR, p.getZ() - CLEAR, p.getX() + CLEAR, p.getZ() + CLEAR)) return false;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int minY = Math.max(Earth.bottom(level) + 1, p.getY() - 3), maxY = Math.min(Earth.top(level) - 1, p.getY() + 10);
        for (int dx = -CLEAR; dx <= CLEAR; dx++) {
            for (int dz = -CLEAR; dz <= CLEAR; dz++) {
                for (int y = minY; y <= maxY; y++) {
                    if (SiteFinder.isPlayerBlockAt(level, origin, at.set(p.getX() + dx, y, p.getZ() + dz), owned, owned)) return false;
                }
            }
        }
        return true;
    }

    /** Whether {@code p} is inside the footprint of the settlement {@code pack} has built. */
    static boolean inOwnSettlement(Pack pack, BlockPos p) {
        if (pack == null || !pack.siteSet || pack.builtTier <= 0) return false;
        int r = SettlementTier.byLevel(pack.builtTier).radius + 2;
        return Math.abs(p.getX() - pack.siteX) <= r && Math.abs(p.getZ() - pack.siteZ) <= r;
    }
}
