package io.github.pirut.bloodfeud.game.camp;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.game.Earth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds safe terrain sites for pack settlements, guards against overwriting player builds using a strict
 * natural/replaceable allowlist, and never force-loads chunks during automatic placement. On the Earth a block
 * whose cube isn't loaded always counts as protected, never as empty.
 */
public final class SiteFinder {
    static final int MAX_CLIFF_SPREAD = 8;
    static final int CORE_RADIUS = 9;
    /** Everything a settlement reads or writes around its origin, with room for the neighbours each write updates. */
    static final int SITE_SIDE_MARGIN = CampBlueprint.FOOTPRINT_MARGIN + 2;
    static final int SITE_DOWN = 20;
    static final int SITE_UP = 24;
    /** How far below a column's top to look for ground on the Earth, whose world floor is 16 km down. */
    private static final int EARTH_SURFACE_REACH = 256;
    public static final int MAX_SPIRAL_RADIUS = CampBlueprint.MAX_SEARCH_SPIRAL_RADIUS;
    private static final int SPIRAL_STEP = 6;
    private static final int MAX_CANDIDATES_PER_ATTEMPT = 8;

    static final List<int[]> SPIRAL_OFFSETS = buildSpiralOffsets();

    public record Site(BlockPos origin, int facing) {
    }

    private SiteFinder() {
    }

    private static List<int[]> buildSpiralOffsets() {
        List<int[]> list = new ArrayList<>();
        list.add(new int[]{0, 0});
        for (int dist = SPIRAL_STEP; dist <= MAX_SPIRAL_RADIUS; dist += SPIRAL_STEP) {
            list.add(new int[]{dist, 0});
            list.add(new int[]{-dist, 0});
            list.add(new int[]{0, dist});
            list.add(new int[]{0, -dist});
            list.add(new int[]{dist, dist});
            list.add(new int[]{-dist, dist});
            list.add(new int[]{dist, -dist});
            list.add(new int[]{-dist, -dist});
        }
        return List.copyOf(list);
    }

    /** Non-blocking check that chunk {@code (chunkX, chunkZ)} is loaded in memory at FULL status right now. */
    public static boolean isChunkReady(ServerLevel level, int chunkX, int chunkZ) {
        return level.getChunkSource().getChunkNow(chunkX, chunkZ) != null;
    }

    /** Checks every chunk intersecting {@code [minX..maxX] x [minZ..maxZ]} without triggering chunk loads. */
    public static boolean areBoundsLoaded(ServerLevel level, int minX, int minZ, int maxX, int maxZ) {
        int[] b = CampBlueprint.chunkBounds(minX, minZ, maxX, maxZ);
        for (int chunkX = b[0]; chunkX <= b[2]; chunkX++) {
            for (int chunkZ = b[1]; chunkZ <= b[3]; chunkZ++) {
                if (!isChunkReady(level, chunkX, chunkZ)) return false;
            }
        }
        return true;
    }

    /**
     * Whether a whole settlement site can be read and written on this tick. In a normal dimension that is its
     * chunks. On the Earth it is also its cubes, which this asks for when they aren't there.
     */
    public static boolean isSiteReady(ServerLevel level, BlockPos origin, int radius) {
        if (!isFootprintLoaded(level, origin.getX(), origin.getZ(), radius)) return false;
        int reach = radius + SITE_SIDE_MARGIN;
        return Earth.ready(level, origin.getX() - reach, origin.getY() - SITE_DOWN, origin.getZ() - reach,
                origin.getX() + reach, origin.getY() + SITE_UP, origin.getZ() + reach);
    }

    /**
     * Whether a site is loaded as it is, without asking for anything to load: its chunks, and on the Earth its cubes,
     * from {@value #SITE_DOWN} below to {@value #SITE_UP} above. A site a player is near passes.
     */
    public static boolean isSiteLoaded(ServerLevel level, BlockPos origin, int radius) {
        if (!isFootprintLoaded(level, origin.getX(), origin.getZ(), radius)) return false;
        int reach = radius + SITE_SIDE_MARGIN;
        return Earth.loaded(level, origin.getX() - reach, origin.getY() - SITE_DOWN, origin.getZ() - reach,
                origin.getX() + reach, origin.getY() + SITE_UP, origin.getZ() + reach);
    }

    /** True when a site on the Earth has to wait for its cubes. Never true in other dimensions. */
    public static boolean isWaitingForGround(ServerLevel level, BlockPos origin, int radius) {
        return Earth.is(level) && !isSiteReady(level, origin, radius);
    }

    /** Checks whether every chunk overlapping a square footprint around (cx, cz) plus margin is loaded. */
    public static boolean isFootprintLoaded(ServerLevel level, int cx, int cz, int radius) {
        int[] b = CampBlueprint.footprintChunkBounds(cx, cz, radius);
        for (int chunkX = b[0]; chunkX <= b[2]; chunkX++) {
            for (int chunkZ = b[1]; chunkZ <= b[3]; chunkZ++) {
                if (!isChunkReady(level, chunkX, chunkZ)) return false;
            }
        }
        return true;
    }

    /**
     * Finds an automatic settlement site near {@code (pack.homeX, pack.homeZ)}.
     * Evaluates only candidates whose footprint chunks are already loaded, capping loaded evaluations per attempt
     * ({@value #MAX_CANDIDATES_PER_ATTEMPT}) to avoid tick spikes, respecting spawn safe radius, refusing
     * water/lava/cliffs/trees, avoiding non-natural blocks, and never synchronously force-loading chunks.
     */
    public static Site findAutoSite(ServerLevel level, Pack pack, SettlementTier tier) {
        return findAutoSite(level, pack, tier, null);
    }

    /** As {@link #findAutoSite(ServerLevel, Pack, SettlementTier)}, keeping off sites other packs of {@code feud} hold. */
    public static Site findAutoSite(ServerLevel level, Pack pack, SettlementTier tier, Feud feud) {
        int radius = Math.max(SettlementTier.CAMP.radius, tier == null ? SettlementTier.CAMP.radius : tier.radius);
        int facing = Math.floorMod(pack.id.hashCode(), 4);
        int baseX = pack.homeX;
        int baseZ = pack.homeZ;

        int total = SPIRAL_OFFSETS.size();
        int start = Math.floorMod(pack.siteSearchOffset, total);
        int evaluated = 0;
        int scanned = 0;
        for (int i = 0; i < total && evaluated < MAX_CANDIDATES_PER_ATTEMPT; i++) {
            int idx = (start + i) % total;
            int[] off = SPIRAL_OFFSETS.get(idx);
            scanned = i + 1;
            int cx = baseX + off[0];
            int cz = baseZ + off[1];
            if (!isFootprintLoaded(level, cx, cz, radius)) continue;
            if (claimedByAnother(feud, pack, cx, cz, radius)) continue;
            evaluated++;
            Site candidate = evaluateAutoCandidate(level, cx, cz, radius, facing);
            if (candidate != null) {
                pack.siteSearchOffset = 0;
                return candidate;
            }
        }
        pack.siteSearchOffset = (start + scanned) % total;
        return null;
    }

    /**
     * How far a chosen site reserves the ground round it: a Stronghold's footprint, or more if it is already bigger.
     * Nothing else is built inside another pack's reservation.
     */
    public static int reservedRadius(Pack pack) {
        int tier = Math.max(pack.builtTier, pack.tier == null ? 1 : pack.tier.level);
        if (pack.works != null) tier = Math.max(tier, pack.works.toTier);
        return Math.max(SettlementTier.STRONGHOLD.radius, SettlementTier.byLevel(Math.max(1, tier)).radius)
                + CampBlueprint.FOOTPRINT_MARGIN;
    }

    /**
     * The ground other packs of {@code feud} hold, as {@code {minX, minZ, maxX, maxZ}}: their reservations, and
     * abandoned settlements. Null {@code feud} holds nothing.
     */
    public static List<int[]> heldByOthers(Feud feud, Pack pack) {
        List<int[]> held = new ArrayList<>();
        if (feud == null) return held;
        for (Pack other : feud.packs.values()) {
            if (other == pack || !other.siteSet || !other.dimension.equals(pack.dimension)) continue;
            int r = reservedRadius(other);
            held.add(new int[]{other.siteX - r, other.siteZ - r, other.siteX + r, other.siteZ + r});
        }
        for (Feud.AbandonedSite site : feud.abandonedSites) {
            if (!site.dimension().equals(pack.dimension)) continue;
            int r = SettlementTier.byLevel(Math.max(1, site.builtTier())).radius + CampBlueprint.FOOTPRINT_MARGIN;
            held.add(new int[]{site.siteX() - r, site.siteZ() - r, site.siteX() + r, site.siteZ() + r});
        }
        return held;
    }

    /** Whether a rectangle {@code {minX, minZ, maxX, maxZ}} overlaps any of {@code held}. */
    public static boolean overlapsAny(List<int[]> held, int minX, int minZ, int maxX, int maxZ) {
        for (int[] h : held) {
            if (minX <= h[2] && maxX >= h[0] && minZ <= h[3] && maxZ >= h[1]) return true;
        }
        return false;
    }

    /**
     * Whether a camp of {@code radius} centred on {@code (x, z)} would reach into ground another pack of {@code feud}
     * holds (see {@link #heldByOthers}). Null {@code feud} checks nothing.
     */
    public static boolean claimedByAnother(Feud feud, Pack pack, int x, int z, int radius) {
        if (feud == null) return false;
        int r = radius + CampBlueprint.FOOTPRINT_MARGIN;
        return overlapsAny(heldByOthers(feud, pack), x - r, z - r, x + r, z + r);
    }

    static Site evaluateAutoCandidate(ServerLevel level, int cx, int cz, int radius, int facing) {
        if (tooCloseToWorldSpawn(level, cx, cz)) return null;
        if (!isFootprintLoaded(level, cx, cz, radius)) return null;

        Integer y = findSurfaceY(level, cx, cz);
        if (y == null) return null;

        // 1. Inner core terrain stability (reject water/lava and steep cliffs > 8 blocks spread)
        int coreR = Math.min(radius, CORE_RADIUS);
        int minY = y, maxY = y;
        for (int[] s : coreSamples(coreR)) {
            Integer sy = findSurfaceY(level, cx + s[0], cz + s[1]);
            if (sy == null) return null;
            minY = Math.min(minY, sy);
            maxY = Math.max(maxY, sy);
            if (maxY - minY > MAX_CLIFF_SPREAD) return null;
        }

        // 2. Full-radius outer ring terrain sampling (reject deep drops/water outside core so foundations never float)
        if (radius > coreR) {
            int maxDrop = CampBuilder.MAX_FOUNDATION_DEPTH - 2;
            for (int[] s : outerSamples(radius)) {
                Integer sy = findSurfaceY(level, cx + s[0], cz + s[1]);
                if (sy == null) return null;
                if (Math.abs(sy - y) > maxDrop) return null;
            }
        }

        // 3. Fast sparse player-build rejection before full 1-block scan
        BlockPos origin = new BlockPos(cx, y, cz);
        if (hasPlayerBuildSparse(level, origin, radius)) return null;

        // 4. Full allowlist player-build guard across the footprint
        if (hasPlayerBuild(level, origin, radius, Set.of(), Set.of())) return null;

        return new Site(origin, facing);
    }

    static int[][] coreSamples(int r) {
        return new int[][]{
                {-r, 0}, {r, 0}, {0, -r}, {0, r},
                {-r / 2, -r / 2}, {r / 2, -r / 2}, {-r / 2, r / 2}, {r / 2, r / 2}
        };
    }

    static int[][] outerSamples(int r) {
        return new int[][]{
                {-r, 0}, {r, 0}, {0, -r}, {0, r},
                {-r, -r}, {r, -r}, {-r, r}, {r, r}
        };
    }

    private static boolean tooCloseToWorldSpawn(ServerLevel level, int x, int z) {
        boolean earth = Earth.is(level);
        if (!earth && !level.dimension().equals(Level.OVERWORLD)) return false;
        BlockPos spawn = earth ? Earth.arrival(level) : level.getSharedSpawnPos();
        if (spawn == null) return true;
        int safe = Math.max(0, BloodFeud.config().spawnSafeRadius) + 32;
        return Math.hypot(x - spawn.getX(), z - spawn.getZ()) < safe;
    }

    /**
     * Operator placement for {@code /bloodfeud camp ...}: places the settlement ahead of the player
     * ({@code tier.radius + 6} blocks along their horizontal look direction) with the main gate facing them.
     * Returns {@code null} if no dry solid surface is found ahead (e.g. open ocean or void).
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
            int[][] nudges = {{4, 0}, {-4, 0}, {0, 4}, {0, -4}, {8, 0}, {-8, 0}, {0, 8}, {0, -8}};
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
        if (y == null) return null;
        return new Site(new BlockPos(cx, y, cz), facing);
    }

    /**
     * Finds a dry, solid ground surface Y at (x, z), walking down past tree trunks, leaves, and replaceable cover,
     * or returns {@code null} if unloaded, water, lava, or void.
     */
    public static Integer findSurfaceY(ServerLevel level, int x, int z) {
        if (!isChunkReady(level, x >> 4, z >> 4)) return null;
        boolean earth = Earth.is(level);
        // getHeight on the Earth downloads map data on this thread unless the column is already known.
        if (earth && !Earth.knowsHeight(level, x, z)) return null;
        int minY = Earth.bottom(level) + 2;
        int maxY = Earth.top(level) - 18;

        if (level.dimensionType().hasCeiling()) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int airStreak = 0;
            for (int y = 100; y >= 34; y--) {
                BlockState state = level.getBlockState(pos.set(x, y, z));
                if (!state.getFluidState().isEmpty()) {
                    airStreak = 0;
                    continue;
                }
                if (state.isAir() || state.canBeReplaced() || isTreeOrFoliage(state)) {
                    airStreak++;
                    continue;
                }
                if (airStreak >= 8 && state.blocksMotion()) {
                    return y + 1;
                }
                airStreak = 0;
            }
            return null;
        }

        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        if (topY < minY || topY >= maxY) return null;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int floor = earth ? Math.max(minY, topY - EARTH_SURFACE_REACH) : minY;
        for (int y = topY; y >= floor; y--) {
            if (earth && !Earth.isLoaded(level, pos.set(x, y, z))) return null;
            BlockState state = level.getBlockState(pos.set(x, y, z));
            if (!state.getFluidState().isEmpty()) return null;
            if (state.isAir() || state.canBeReplaced() || isTreeOrFoliage(state)) {
                continue;
            }
            if (state.blocksMotion()) {
                int surface = y + 1;
                if (surface <= minY || surface >= maxY) return null;
                if (earth && !Earth.isLoaded(level, pos.set(x, surface, z))) return null;
                if (!level.getFluidState(pos.set(x, surface, z)).isEmpty()) return null;
                return surface;
            }
        }
        return null;
    }

    private static boolean isTreeOrFoliage(BlockState state) {
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.WART_BLOCKS)) {
            return true;
        }
        Block b = state.getBlock();
        return b == Blocks.MUSHROOM_STEM || b == Blocks.RED_MUSHROOM_BLOCK || b == Blocks.BROWN_MUSHROOM_BLOCK
                || b == Blocks.BAMBOO || b == Blocks.CACTUS || b == Blocks.MANGROVE_ROOTS;
    }

    /**
     * Checks whether a discrete zone box collides with any player-built block or foreign container.
     * Uses exact chunk-loaded checks and scans down to {@code Math.min(-2, box.minY())}.
     */
    public static boolean isZoneBlockedByPlayer(ServerLevel level, BlockPos origin, CampBlueprint.Box box,
                                                Set<CampBlueprint.Spot> ownedPositions,
                                                Set<CampBlueprint.Spot> ownedContainers) {
        int minX = origin.getX() + box.minX();
        int maxX = origin.getX() + box.maxX();
        int minY = Math.max(Earth.bottom(level) + 1, origin.getY() + Math.min(-2, box.minY()));
        int maxY = Math.min(Earth.top(level) - 1, origin.getY() + Math.max(6, box.maxY()));
        int minZ = origin.getZ() + box.minZ();
        int maxZ = origin.getZ() + box.maxZ();

        if (!areBoundsLoaded(level, minX, minZ, maxX, maxZ)) return true;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (holdsUpZone(level, origin, pos.set(x, y, z), ownedPositions, ownedContainers)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Checks a single vertical column at relative {@code (rx, rz)} for any player-built block or foreign container. */
    public static boolean hasPlayerBlockInColumn(ServerLevel level, BlockPos origin, int rx, int rz,
                                                 int relMinY, int relMaxY,
                                                 Set<CampBlueprint.Spot> ownedPositions,
                                                 Set<CampBlueprint.Spot> ownedContainers) {
        int wx = origin.getX() + rx;
        int wz = origin.getZ() + rz;
        if (!isChunkReady(level, wx >> 4, wz >> 4)) return true;
        int minY = Math.max(Earth.bottom(level) + 1, origin.getY() + Math.min(-2, relMinY));
        int maxY = Math.min(Earth.top(level) - 1, origin.getY() + Math.max(6, relMaxY));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = minY; y <= maxY; y++) {
            if (holdsUpZone(level, origin, pos.set(wx, y, wz), ownedPositions, ownedContainers)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a block keeps the settlement out of the zone or ring column around it. A player's block standing in the
     * settlement's own place (bricks where its campfire was, a chest where its banner was) is still never written over,
     * since every write checks the block it replaces; but it doesn't stop the rest of the zone from ruining or being
     * repaired.
     */
    private static boolean holdsUpZone(ServerLevel level, BlockPos origin, BlockPos pos,
                                       Set<CampBlueprint.Spot> ownedPositions,
                                       Set<CampBlueprint.Spot> ownedContainers) {
        if (!isPlayerBlockAt(level, origin, pos, ownedPositions, ownedContainers)) return false;
        if (!(ownedPositions instanceof Owned) || !Earth.isLoaded(level, pos)) return true;
        CampBlueprint.Spot rel = new CampBlueprint.Spot(
                pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ());
        return !ownedPositions.contains(rel);
    }

    /**
     * Preflight check for upgrade building zones: returns {@code true} if any corner, edge midpoint, or center
     * column sits over lava or has no solid non-fluid ground within {@code maxFoundationDepth} blocks below origin.
     */
    public static boolean isZoneOverAbyss(ServerLevel level, BlockPos origin, CampBlueprint.Box box,
                                          int maxFoundationDepth) {
        int minX = origin.getX() + box.minX();
        int maxX = origin.getX() + box.maxX();
        int minZ = origin.getZ() + box.minZ();
        int maxZ = origin.getZ() + box.maxZ();
        int midX = (minX + maxX) / 2;
        int midZ = (minZ + maxZ) / 2;

        int[][] samples = {
                {minX, minZ}, {maxX, minZ}, {minX, maxZ}, {maxX, maxZ},
                {midX, minZ}, {midX, maxZ}, {minX, midZ}, {maxX, midZ},
                {midX, midZ}
        };
        int minBuild = Earth.bottom(level) + 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int[] s : samples) {
            int wx = s[0], wz = s[1];
            if (!isChunkReady(level, wx >> 4, wz >> 4)) return true;
            if (!Earth.isLoaded(level, pos.set(wx, origin.getY() - 1, wz))
                    || !Earth.isLoaded(level, pos.set(wx, origin.getY() - 2, wz))) return true;
            if (level.getFluidState(pos.set(wx, origin.getY() - 1, wz)).is(FluidTags.LAVA)
                    || level.getFluidState(pos.set(wx, origin.getY() - 2, wz)).is(FluidTags.LAVA)) {
                return true;
            }
            boolean solidFound = false;
            for (int depth = 0; depth <= maxFoundationDepth; depth++) {
                int y = origin.getY() - 1 - depth;
                if (y <= minBuild || !Earth.isLoaded(level, pos.set(wx, y, wz))) break;
                BlockState state = level.getBlockState(pos.set(wx, y, wz));
                if (state.blocksMotion() && state.getFluidState().isEmpty() && !state.canBeReplaced()) {
                    solidFound = true;
                    break;
                }
            }
            if (!solidFound) return true;
        }
        return false;
    }

    /** Full footprint player-build scan (always checks chunk loadedness first). */
    public static boolean hasPlayerBuild(ServerLevel level, BlockPos origin, int radius,
                                         Set<CampBlueprint.Spot> ownedPositions,
                                         Set<CampBlueprint.Spot> ownedContainers) {
        int cx = origin.getX(), cy = origin.getY(), cz = origin.getZ();
        if (!areBoundsLoaded(level, cx - radius, cz - radius, cx + radius, cz + radius)) return true;
        int minY = Math.max(Earth.bottom(level) + 1, cy - 3);
        int maxY = Math.min(Earth.top(level) - 1, cy + 10);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int y = minY; y <= maxY; y++) {
                    if (isPlayerBlockAt(level, origin, pos.set(cx + dx, y, cz + dz), ownedPositions, ownedContainers)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasPlayerBuildSparse(ServerLevel level, BlockPos origin, int radius) {
        int cx = origin.getX(), cy = origin.getY(), cz = origin.getZ();
        int minY = Math.max(Earth.bottom(level) + 1, cy - 1);
        int maxY = Math.min(Earth.top(level) - 1, cy + 6);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx += 4) {
            for (int dz = -radius; dz <= radius; dz += 4) {
                for (int y = minY; y <= maxY; y += 2) {
                    if (isPlayerBlockAt(level, origin, pos.set(cx + dx, y, cz + dz), Set.of(), Set.of())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the block at {@code pos} must be treated as player-built or protected.
     * Any container is ALWAYS protected unless its exact relative position is in {@code ownedContainers}.
     * Any block not on the strict natural/replaceable allowlist is protected, unless it stands at a position in
     * {@code ownedPositions} and is a block the settlement itself puts there (see {@link Owned}).
     */
    public static boolean isPlayerBlockAt(ServerLevel level, BlockPos origin, BlockPos pos,
                                          Set<CampBlueprint.Spot> ownedPositions,
                                          Set<CampBlueprint.Spot> ownedContainers) {
        if (!Earth.isLoaded(level, pos)) return true; // it would read as void air, which is replaceable
        BlockState state = level.getBlockState(pos);
        if (isIndestructibleBlock(state)) return true;
        CampBlueprint.Spot rel = new CampBlueprint.Spot(
                pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ());
        if (isContainerBlock(level, pos, state)) {
            if (!ownedContainers.contains(rel)) return true;
            // On the camp's stash spot, only the camp's own stash is the camp's: a player's furnace there is theirs.
            return ownedPositions instanceof Owned owned && !owned.isSettlementBlock(rel, state);
        }
        if (isNaturalOrReplaceable(state)) return false;
        // A player can take a settlement's place: bricks where its campfire stood are theirs, not the camp's.
        return !(ownedPositions instanceof Owned owned && owned.isSettlementBlock(rel, state));
    }

    /**
     * A settlement's own positions. Exact (what the pack has put down, {@code Pack.placed}): a position is the pack's
     * only while the very block it placed still stands there. Legacy (a settlement built before 0.1.0-beta.2, with no
     * such record): the blocks its blueprint puts at each position, in any color.
     */
    public static final class Owned extends java.util.AbstractSet<CampBlueprint.Spot> {
        private final Map<CampBlueprint.Spot, Set<CampBlueprint.Material>> materials;
        private final FamilyPalette palette;
        private final Map<Long, String> placed;

        /** Legacy: by blueprint. */
        public Owned(Map<CampBlueprint.Spot, Set<CampBlueprint.Material>> materials, Family family) {
            this.materials = materials;
            this.palette = FamilyPalette.of(family);
            this.placed = null;
        }

        /** Exact: a live view of what the pack has put down (offset {@code BlockPos.asLong} to block id). */
        public Owned(Map<Long, String> placed) {
            this.materials = Map.of();
            this.palette = null;
            this.placed = placed;
        }

        public boolean exact() {
            return placed != null;
        }

        boolean isSettlementBlock(CampBlueprint.Spot rel, BlockState state) {
            if (placed != null) {
                String mine = placed.get(BlockPos.asLong(rel.rx(), rel.ry(), rel.rz()));
                return mine != null && mine.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            }
            Set<CampBlueprint.Material> here = materials.get(rel);
            if (here == null) return false;
            for (CampBlueprint.Material m : here) {
                if (palette.blocks(m).contains(state.getBlock())) return true;
            }
            return false;
        }

        @Override
        public boolean contains(Object o) {
            if (placed != null) {
                return o instanceof CampBlueprint.Spot spot && placed.containsKey(BlockPos.asLong(spot.rx(), spot.ry(), spot.rz()));
            }
            return materials.containsKey(o);
        }

        @Override
        public java.util.Iterator<CampBlueprint.Spot> iterator() {
            if (placed == null) return materials.keySet().iterator();
            java.util.List<CampBlueprint.Spot> spots = new java.util.ArrayList<>(placed.size());
            for (long k : placed.keySet()) spots.add(new CampBlueprint.Spot(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k)));
            return spots.iterator();
        }

        @Override
        public int size() {
            return placed != null ? placed.size() : materials.size();
        }
    }

    /** Strict allowlist of natural terrain/foliage and replaceable blocks safe for automatic builds to clear. */
    public static boolean isNaturalOrReplaceable(BlockState state) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.canBeReplaced()) {
            return true;
        }
        if (state.is(BlockTags.DIRT)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(BlockTags.SNOW)
                || state.is(BlockTags.ICE)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.TALL_FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.MUSHROOM_GROW_BLOCK)
                || state.is(BlockTags.WART_BLOCKS)) {
            return true;
        }
        Block b = state.getBlock();
        return b == Blocks.GRAVEL
                || b == Blocks.CLAY
                || b == Blocks.MUD
                || b == Blocks.PACKED_MUD
                || b == Blocks.SANDSTONE
                || b == Blocks.RED_SANDSTONE
                || b == Blocks.CALCITE
                || b == Blocks.TUFF
                || b == Blocks.POINTED_DRIPSTONE
                || b == Blocks.DRIPSTONE_BLOCK
                || b == Blocks.NETHERRACK
                || b == Blocks.SOUL_SAND
                || b == Blocks.SOUL_SOIL
                || b == Blocks.BASALT
                || b == Blocks.SMOOTH_BASALT
                || b == Blocks.BLACKSTONE
                || b == Blocks.END_STONE
                || b == Blocks.MYCELIUM
                || b == Blocks.PODZOL
                || b == Blocks.ROOTED_DIRT
                || b == Blocks.MOSS_BLOCK
                || b == Blocks.MOSS_CARPET
                || b == Blocks.CACTUS
                || b == Blocks.SUGAR_CANE
                || b == Blocks.VINE
                || b == Blocks.GLOW_LICHEN
                || b == Blocks.SCULK
                || b == Blocks.SCULK_VEIN
                || b == Blocks.DEAD_BUSH
                || b == Blocks.FERN
                || b == Blocks.LARGE_FERN
                || b == Blocks.SHORT_GRASS
                || b == Blocks.TALL_GRASS
                || b == Blocks.SEAGRASS
                || b == Blocks.TALL_SEAGRASS
                || b == Blocks.KELP
                || b == Blocks.KELP_PLANT
                || b == Blocks.BROWN_MUSHROOM
                || b == Blocks.RED_MUSHROOM
                || b == Blocks.BROWN_MUSHROOM_BLOCK
                || b == Blocks.RED_MUSHROOM_BLOCK
                || b == Blocks.MUSHROOM_STEM
                || b == Blocks.CRIMSON_FUNGUS
                || b == Blocks.WARPED_FUNGUS
                || b == Blocks.CRIMSON_NYLIUM
                || b == Blocks.WARPED_NYLIUM
                || b == Blocks.CRIMSON_ROOTS
                || b == Blocks.WARPED_ROOTS
                || b == Blocks.NETHER_SPROUTS
                || b == Blocks.WEEPING_VINES
                || b == Blocks.WEEPING_VINES_PLANT
                || b == Blocks.TWISTING_VINES
                || b == Blocks.TWISTING_VINES_PLANT
                || b == Blocks.BAMBOO
                || b == Blocks.BAMBOO_SAPLING
                || b == Blocks.LILY_PAD
                || b == Blocks.BIG_DRIPLEAF
                || b == Blocks.BIG_DRIPLEAF_STEM
                || b == Blocks.SMALL_DRIPLEAF
                || b == Blocks.HANGING_ROOTS
                || b == Blocks.SPORE_BLOSSOM
                || b == Blocks.AZALEA
                || b == Blocks.FLOWERING_AZALEA
                || b == Blocks.PINK_PETALS
                || b == Blocks.SWEET_BERRY_BUSH
                || b == Blocks.MAGMA_BLOCK
                || b == Blocks.MANGROVE_ROOTS
                || b == Blocks.MUDDY_MANGROVE_ROOTS;
    }

    /** True for any block that stores items or is a container BlockEntity. */
    public static boolean isContainerBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.hasBlockEntity()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container) return true;
        }
        Block b = state.getBlock();
        return b instanceof AbstractChestBlock<?>
                || b instanceof BarrelBlock
                || b instanceof ShulkerBoxBlock
                || b instanceof AbstractFurnaceBlock
                || b instanceof HopperBlock
                || b instanceof DispenserBlock
                || b instanceof BrewingStandBlock
                || b instanceof EnderChestBlock
                || b instanceof DecoratedPotBlock
                || b instanceof ChiseledBookShelfBlock
                || b instanceof CrafterBlock
                || b instanceof LecternBlock
                || b instanceof JukeboxBlock;
    }

    /** True if the block at {@code pos} is a container BlockEntity currently holding any items. */
    public static boolean isNonEmptyContainer(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.hasBlockEntity()) return false;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof Container container && !container.isEmpty();
    }

    /** Never overwrite indestructible world/technical blocks even during manual operator builds. */
    public static boolean isIndestructibleBlock(BlockState state) {
        if (state.isAir()) return false;
        Block b = state.getBlock();
        return b == Blocks.BEDROCK
                || b == Blocks.END_PORTAL
                || b == Blocks.END_PORTAL_FRAME
                || b == Blocks.END_GATEWAY
                || b == Blocks.NETHER_PORTAL
                || b == Blocks.COMMAND_BLOCK
                || b == Blocks.REPEATING_COMMAND_BLOCK
                || b == Blocks.CHAIN_COMMAND_BLOCK
                || b == Blocks.BARRIER
                || b == Blocks.STRUCTURE_BLOCK
                || b == Blocks.JIGSAW;
    }
}
