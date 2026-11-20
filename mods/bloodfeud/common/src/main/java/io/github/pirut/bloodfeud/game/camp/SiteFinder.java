package io.github.pirut.bloodfeud.game.camp;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Finds safe terrain sites for pack settlements, guards against overwriting player builds,
 * and never force-loads chunks during automatic placement.
 */
public final class SiteFinder {
    private static final int MAX_CLIFF_SPREAD = 8;
    private static final int MAX_SPIRAL_RADIUS = 36;
    private static final int SPIRAL_STEP = 6;

    public record Site(BlockPos origin, int facing) {
    }

    private SiteFinder() {
    }

    /** Checks whether all chunks overlapping a square footprint around (cx, cz) are currently loaded. */
    public static boolean isFootprintLoaded(ServerLevel level, int cx, int cz, int radius) {
        int r = Math.max(1, radius + 2);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx += 8) {
            for (int dz = -r; dz <= r; dz += 8) {
                if (!level.isLoaded(pos.set(cx + dx, 64, cz + dz))) return false;
            }
        }
        return level.isLoaded(pos.set(cx + r, 64, cz + r))
                && level.isLoaded(pos.set(cx - r, 64, cz + r))
                && level.isLoaded(pos.set(cx + r, 64, cz - r))
                && level.isLoaded(pos.set(cx - r, 64, cz - r));
    }

    /**
     * Finds an automatic settlement site near {@code (pack.homeX, pack.homeZ)}.
     * Respects spawn safe radius, refuses water/lava/cliffs, avoids player-built blocks with spiral search
     * up to 36 blocks, and never force-loads chunks.
     */
    public static Site findAutoSite(ServerLevel level, Pack pack, SettlementTier tier) {
        int radius = Math.max(SettlementTier.CAMP.radius, tier == null ? SettlementTier.CAMP.radius : tier.radius);
        int facing = Math.floorMod(pack.id.hashCode(), 4);
        int baseX = pack.homeX;
        int baseZ = pack.homeZ;

        // First try the pack's own home coordinates, then spiral outward up to 36 blocks
        Site direct = evaluateAutoCandidate(level, baseX, baseZ, radius, facing);
        if (direct != null) return direct;

        for (int dist = SPIRAL_STEP; dist <= MAX_SPIRAL_RADIUS; dist += SPIRAL_STEP) {
            int[][] offsets = {
                    {dist, 0}, {-dist, 0}, {0, dist}, {0, -dist},
                    {dist, dist}, {-dist, dist}, {dist, -dist}, {-dist, -dist}
            };
            for (int[] off : offsets) {
                Site candidate = evaluateAutoCandidate(level, baseX + off[0], baseZ + off[1], radius, facing);
                if (candidate != null) return candidate;
            }
        }
        return null;
    }

    private static Site evaluateAutoCandidate(ServerLevel level, int cx, int cz, int radius, int facing) {
        if (tooCloseToWorldSpawn(level, cx, cz)) return null;
        if (!isFootprintLoaded(level, cx, cz, radius)) return null;

        Integer y = findSurfaceY(level, cx, cz);
        if (y == null) return null;

        // Check core terrain stability (reject water/lava and extreme cliffs > 8 blocks spread)
        int coreR = Math.min(radius, 9);
        int minY = y, maxY = y;
        int[][] samples = {
                {-coreR, 0}, {coreR, 0}, {0, -coreR}, {0, coreR},
                {-coreR / 2, -coreR / 2}, {coreR / 2, -coreR / 2}, {-coreR / 2, coreR / 2}, {coreR / 2, coreR / 2}
        };
        for (int[] s : samples) {
            Integer sy = findSurfaceY(level, cx + s[0], cz + s[1]);
            if (sy == null) return null;
            minY = Math.min(minY, sy);
            maxY = Math.max(maxY, sy);
            if (maxY - minY > MAX_CLIFF_SPREAD) return null;
        }

        // Player-build guard across the footprint
        if (hasPlayerBuild(level, cx, y, cz, radius, 0)) return null;

        return new Site(new BlockPos(cx, y, cz), facing);
    }

    private static boolean tooCloseToWorldSpawn(ServerLevel level, int x, int z) {
        if (!level.dimension().equals(Level.OVERWORLD)) return false;
        BlockPos spawn = level.getSharedSpawnPos();
        int safe = Math.max(0, BloodFeud.config().spawnSafeRadius) + 32;
        return Math.hypot(x - spawn.getX(), z - spawn.getZ()) < safe;
    }

    /**
     * Operator placement for {@code /bloodfeud camp ...}: places the settlement ahead of the player
     * ({@code tier.radius + 6} blocks along their horizontal look direction) with the main gate facing them.
     * Ignores spawn safe radius, adapts to terrain, and avoids void/fluids.
     */
    public static Site findCommandSite(ServerLevel level, ServerPlayer player, SettlementTier tier) {
        int radius = Math.max(SettlementTier.CAMP.radius, tier.radius);
        int dist = radius + 6;
        Vec3 look = player.getLookAngle();
        double lx = look.x, lz = look.z;
        double len = Math.hypot(lx, lz);
        if (len < 0.05) {
            lx = 0;
            lz = 1;
            len = 1;
        }
        lx /= len;
        lz /= len;

        // Facing so the South (+Z unrotated) Main Gatehouse faces back toward the player
        int facing;
        if (Math.abs(lx) >= Math.abs(lz)) {
            facing = lx > 0 ? 1 : 3; // looking East -> gate faces West (1); looking West -> gate faces East (3)
        } else {
            facing = lz > 0 ? 2 : 0; // looking South -> gate faces North (2); looking North -> gate faces South (0)
        }

        int cx = (int) Math.round(player.getX() + lx * dist);
        int cz = (int) Math.round(player.getZ() + lz * dist);

        Integer y = findSurfaceY(level, cx, cz);
        if (y == null) {
            // Try a couple small side/forward nudges to avoid placing directly over a pond or lava pocket
            int[][] nudges = {{0, 0}, {4, 0}, {-4, 0}, {0, 4}, {0, -4}, {8, 0}, {-8, 0}};
            for (int[] n : nudges) {
                Integer ny = findSurfaceY(level, cx + n[0], cz + n[1]);
                if (ny != null) {
                    cx += n[0];
                    cz += n[1];
                    y = ny;
                    break;
                }
            }
        }
        if (y == null) {
            int minY = level.getMinBuildHeight() + 6;
            int maxY = level.getMaxBuildHeight() - 20;
            y = Math.max(minY, Math.min(maxY, player.getBlockY()));
        }
        return new Site(new BlockPos(cx, y, cz), facing);
    }

    /**
     * Finds a dry, solid surface Y at (x, z), or null if water/lava/void.
     * In roofed dimensions (Nether), scans downward from y=100 to y=34 for solid floor with >= 8 air blocks above.
     */
    public static Integer findSurfaceY(ServerLevel level, int x, int z) {
        if (!level.isLoaded(new BlockPos(x, 64, z))) return null;
        if (level.dimensionType().hasCeiling()) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int airStreak = 0;
            for (int y = 100; y >= 34; y--) {
                BlockState state = level.getBlockState(pos.set(x, y, z));
                if (!state.getFluidState().isEmpty()) {
                    airStreak = 0;
                    continue;
                }
                if (state.isAir() || state.canBeReplaced()) {
                    airStreak++;
                    continue;
                }
                if (airStreak >= 8 && state.blocksMotion() && state.getFluidState().isEmpty()) {
                    return y + 1;
                }
                airStreak = 0;
            }
            return null;
        }

        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (surface <= level.getMinBuildHeight() + 2 || surface >= level.getMaxBuildHeight() - 18) return null;
        BlockPos at = new BlockPos(x, surface, z);
        BlockPos below = at.below();
        if (!level.getFluidState(at).isEmpty() || !level.getFluidState(below).isEmpty()) return null;
        BlockState ground = level.getBlockState(below);
        if (!ground.blocksMotion()) return null;
        return surface;
    }

    /**
     * Checks whether a newly expanding zone box collides with any player-placed indicator blocks.
     * Chests/barrels inside {@code previousBuiltRadius} are treated as our own settlement containers.
     */
    public static boolean isZoneBlockedByPlayer(ServerLevel level, BlockPos origin, CampBlueprint.Box box, int previousBuiltRadius) {
        int minX = origin.getX() + box.minX();
        int maxX = origin.getX() + box.maxX();
        int minY = Math.max(level.getMinBuildHeight() + 1, origin.getY() + Math.max(-2, box.minY()));
        int maxY = Math.min(level.getMaxBuildHeight() - 1, origin.getY() + Math.max(6, box.maxY()));
        int minZ = origin.getZ() + box.minZ();
        int maxZ = origin.getZ() + box.maxZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!level.isLoaded(pos.set(x, origin.getY(), z))) return true;
                boolean insideOld = previousBuiltRadius > 0
                        && Math.abs(x - origin.getX()) <= previousBuiltRadius
                        && Math.abs(z - origin.getZ()) <= previousBuiltRadius;
                for (int y = minY; y <= maxY; y++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (isPlayerIndicator(state, !insideOld)) return true;
                }
            }
        }
        return false;
    }

    private static boolean hasPlayerBuild(ServerLevel level, int cx, int cy, int cz, int radius, int previousBuiltRadius) {
        int minY = Math.max(level.getMinBuildHeight() + 1, cy - 3);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, cy + 10);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                boolean insideOld = previousBuiltRadius > 0
                        && Math.abs(dx) <= previousBuiltRadius
                        && Math.abs(dz) <= previousBuiltRadius;
                for (int y = minY; y <= maxY; y++) {
                    BlockState state = level.getBlockState(pos.set(cx + dx, y, cz + dz));
                    if (isPlayerIndicator(state, !insideOld)) return true;
                }
            }
        }
        return false;
    }

    /** True for blocks that strongly indicate a player-built base/contraption. */
    public static boolean isPlayerIndicator(BlockState state, boolean includeContainers) {
        if (state.isAir()) return false;
        Block b = state.getBlock();
        if (state.is(BlockTags.BEDS) || state.is(BlockTags.ANVIL) || state.is(BlockTags.SHULKER_BOXES)
                || state.is(BlockTags.DOORS)) {
            return true;
        }
        if (b == Blocks.CRAFTING_TABLE || b == Blocks.FURNACE || b == Blocks.BLAST_FURNACE || b == Blocks.SMOKER
                || b == Blocks.ENCHANTING_TABLE || b == Blocks.BEACON || b == Blocks.CONDUIT || b == Blocks.ENDER_CHEST
                || b == Blocks.REDSTONE_WIRE || b == Blocks.REPEATER || b == Blocks.COMPARATOR
                || b == Blocks.PISTON || b == Blocks.STICKY_PISTON || b == Blocks.HOPPER
                || b == Blocks.JUKEBOX || b == Blocks.LECTERN) {
            return true;
        }
        if (includeContainers && (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.BARREL)) {
            return true;
        }
        return false;
    }

    /** Never overwrite indestructible world blocks or player-build indicators during clearing. */
    public static boolean isProtectedBlock(BlockState state) {
        if (state.isAir()) return false;
        Block b = state.getBlock();
        if (b == Blocks.BEDROCK || b == Blocks.END_PORTAL || b == Blocks.END_PORTAL_FRAME
                || b == Blocks.NETHER_PORTAL || b == Blocks.COMMAND_BLOCK || b == Blocks.REPEATING_COMMAND_BLOCK
                || b == Blocks.CHAIN_COMMAND_BLOCK || b == Blocks.BARRIER) {
            return true;
        }
        return isPlayerIndicator(state, false);
    }
}
