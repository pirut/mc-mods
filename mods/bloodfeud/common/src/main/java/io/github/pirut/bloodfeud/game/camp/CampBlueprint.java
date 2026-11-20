package io.github.pirut.bloodfeud.game.camp;

import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.SettlementTier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Pure, deterministic, concentric layout planner for growing gang settlements:
 * Camp (radius 7) -> Outpost (radius 13) -> Stronghold (radius 20) -> Town (radius 29).
 * <p>
 * Keeps zero Minecraft imports so unit tests can verify every tier and family directly.
 */
public final class CampBlueprint {
    public static final int PASS_FOUNDATION = 0;
    public static final int PASS_STRUCTURE = 1;
    public static final int PASS_FURNITURE = 2;
    public static final int PASS_DECOR = 3;

    public enum Facing {
        NORTH(0, 0, -1),
        EAST(1, 1, 0),
        SOUTH(2, 0, 1),
        WEST(3, -1, 0);

        public final int index;
        public final int dx, dz;

        Facing(int index, int dx, int dz) {
            this.index = index;
            this.dx = dx;
            this.dz = dz;
        }

        public Facing rotate(int steps) {
            return values()[Math.floorMod(index + steps, 4)];
        }

        public Facing opposite() {
            return rotate(2);
        }

        public static Facing byIndex(int index) {
            return values()[Math.floorMod(index, 4)];
        }
    }

    public enum Material {
        AIR(PASS_FOUNDATION),
        FOUNDATION_COLUMN(PASS_FOUNDATION),
        GROUND(PASS_FOUNDATION),
        PATH(PASS_FOUNDATION),
        FLOOR(PASS_FOUNDATION),

        LOG(PASS_STRUCTURE),
        PILLAR(PASS_STRUCTURE),
        PLANKS(PASS_STRUCTURE),
        WALL(PASS_STRUCTURE),
        WALL_ACCENT(PASS_STRUCTURE),
        SLAB(PASS_STRUCTURE),
        STAIR(PASS_STRUCTURE),
        STAIR_TOP(PASS_STRUCTURE),
        FENCE(PASS_STRUCTURE),
        IRON_BARS(PASS_STRUCTURE),
        WOOL_PRIMARY(PASS_STRUCTURE),
        WOOL_ACCENT(PASS_STRUCTURE),
        FAMILY_PROP_A(PASS_STRUCTURE),

        BARREL(PASS_FURNITURE),
        CHEST_CAMP(PASS_FURNITURE),
        CHEST_OUTPOST(PASS_FURNITURE),
        CHEST_STRONGHOLD(PASS_FURNITURE),
        CHEST_TOWN(PASS_FURNITURE),
        CAMPFIRE(PASS_FURNITURE),
        THRONE(PASS_FURNITURE),
        FAMILY_PROP_B(PASS_FURNITURE),
        WORK_PROP_A(PASS_FURNITURE),
        WORK_PROP_B(PASS_FURNITURE),

        TORCH(PASS_DECOR),
        WALL_TORCH(PASS_DECOR),
        LANTERN(PASS_DECOR),
        HANGING_LANTERN(PASS_DECOR),
        FAMILY_LIGHT(PASS_DECOR),
        LADDER(PASS_DECOR),
        BANNER_STANDING(PASS_DECOR),
        BANNER_WALL(PASS_DECOR),
        VASSAL_BANNER(PASS_DECOR),
        CARPET(PASS_DECOR),
        FAMILY_CANOPY(PASS_DECOR),
        CHAIN(PASS_DECOR);

        public final int pass;

        Material(int pass) {
            this.pass = pass;
        }

        public boolean isChest() {
            return this == CHEST_CAMP || this == CHEST_OUTPOST || this == CHEST_STRONGHOLD || this == CHEST_TOWN;
        }

        public boolean isBanner() {
            return this == BANNER_STANDING || this == BANNER_WALL || this == VASSAL_BANNER;
        }
    }

    public record Placement(int rx, int ry, int rz, Material material, Facing facing, int meta, int pass, String zone) {
    }

    public record Spot(int rx, int ry, int rz) {
    }

    public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        Box expand(int x, int y, int z) {
            return new Box(Math.min(minX, x), Math.min(minY, y), Math.min(minZ, z),
                    Math.max(maxX, x), Math.max(maxY, y), Math.max(maxZ, z));
        }
    }

    public record Anchors(Spot bossSpot, List<Spot> guardSpots, List<Spot> patrolSpots) {
    }

    public record Plan(
            List<Placement> placements,
            Map<String, Box> zones,
            Spot bossSpot,
            List<Spot> guardSpots,
            List<Spot> patrolSpots,
            int fromTier,
            int toTier,
            int facing
    ) {
        public Anchors anchors() {
            return new Anchors(bossSpot, guardSpots, patrolSpots);
        }
    }

    private CampBlueprint() {
    }

    /**
     * Plans additive block placements to grow a pack settlement from {@code fromTier} to {@code toTier}.
     * Coordinates in the returned plan are already rotated by {@code facing} (0..3) around anchor (0, 0, 0).
     */
    public static Plan plan(UUID packId, Family family, int packColor, List<Integer> vassalColors,
                            int facing, int fromTier, int toTier) {
        int f = Math.floorMod(facing, 4);
        int from = Math.max(0, Math.min(SettlementTier.TOWN.level, fromTier));
        int to = Math.max(from, Math.min(SettlementTier.TOWN.level, toTier));
        long seed = packId == null ? 1L : (packId.getMostSignificantBits() ^ (packId.getLeastSignificantBits() * 31L));
        Builder b = new Builder(f, packColor, vassalColors == null ? List.of() : vassalColors, family, new Random(seed));
        for (int tier = from + 1; tier <= to; tier++) {
            switch (tier) {
                case 1 -> buildTier1Camp(b);
                case 2 -> buildTier2Outpost(b);
                case 3 -> buildTier3Stronghold(b);
                case 4 -> buildTier4Town(b);
            }
        }
        Anchors anchors = spawnAnchors(f, SettlementTier.byLevel(Math.max(1, to)));
        return b.finish(anchors, from, to);
    }

    /** Fast lookup of rotated garrison spawn spots for a settlement tier without generating all blocks. */
    public static Anchors spawnAnchors(int facing, SettlementTier tier) {
        int f = Math.floorMod(facing, 4);
        SettlementTier t = tier == null || tier == SettlementTier.NONE ? SettlementTier.CAMP : tier;
        return switch (t) {
            case CAMP -> new Anchors(
                    rotSpot(f, 0, 0, -2),
                    List.of(rotSpot(f, -3, 0, 3), rotSpot(f, 3, 0, 3), rotSpot(f, -4, 0, 0), rotSpot(f, 4, 0, 0)),
                    List.of(rotSpot(f, 0, 0, 5), rotSpot(f, -5, 0, 2), rotSpot(f, 5, 0, 2))
            );
            case OUTPOST -> new Anchors(
                    rotSpot(f, 0, 1, -8),
                    List.of(rotSpot(f, -3, 0, 11), rotSpot(f, 3, 0, 11), rotSpot(f, -9, 5, -9), rotSpot(f, 9, 0, -5)),
                    List.of(rotSpot(f, -6, 0, 0), rotSpot(f, 6, 0, 0), rotSpot(f, 0, 0, 6), rotSpot(f, 0, 0, 14))
            );
            case STRONGHOLD -> new Anchors(
                    rotSpot(f, 0, 1, -11),
                    List.of(rotSpot(f, -3, 0, 18), rotSpot(f, 3, 0, 18), rotSpot(f, -18, 6, 18), rotSpot(f, 18, 6, 18),
                            rotSpot(f, -11, 0, 0), rotSpot(f, 11, 0, 0)),
                    List.of(rotSpot(f, 0, 0, 14), rotSpot(f, -8, 0, 14), rotSpot(f, 8, 0, 14), rotSpot(f, 0, 0, 0),
                            rotSpot(f, 0, 0, 22))
            );
            case TOWN, NONE -> new Anchors(
                    rotSpot(f, 0, 2, -12),
                    List.of(rotSpot(f, -3, 0, 27), rotSpot(f, 3, 0, 27), rotSpot(f, 0, 0, -26), rotSpot(f, -26, 0, 0),
                            rotSpot(f, 26, 0, 0), rotSpot(f, 21, 0, -16), rotSpot(f, -21, 0, -16)),
                    List.of(rotSpot(f, 0, 0, 8), rotSpot(f, -14, 0, 0), rotSpot(f, 14, 0, 0), rotSpot(f, -18, 0, 20),
                            rotSpot(f, 18, 0, 10), rotSpot(f, 0, 0, 31))
            );
        };
    }

    /** Returns rotated banner placements for re-bannering an existing settlement (e.g. when swallowed by a King). */
    public static List<Placement> bannerPlacements(UUID packId, Family family, int packColor,
                                                   List<Integer> vassalColors, int facing, SettlementTier builtTier) {
        if (builtTier == null || builtTier == SettlementTier.NONE) return List.of();
        Plan full = plan(packId, family, packColor, vassalColors, facing, 0, builtTier.level);
        List<Placement> banners = new ArrayList<>();
        for (Placement p : full.placements) {
            if (p.material.isBanner()) banners.add(p);
        }
        return banners;
    }

    private static Spot rotSpot(int facing, int x, int y, int z) {
        return switch (facing) {
            case 1 -> new Spot(-z, y, x);
            case 2 -> new Spot(-x, y, -z);
            case 3 -> new Spot(z, y, -x);
            default -> new Spot(x, y, z);
        };
    }

    // =========================================================================
    // Tier 1: Camp (radius 7, ~15x15)
    // =========================================================================

    private static void buildTier1Camp(Builder b) {
        b.zone("camp_core");
        // Clear & prepare ground pad
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                double dist = Math.hypot(x, z);
                if (dist > 7.5) continue;
                b.clearColumn(x, z, 0, 6);
                b.foundation(x, z);
                Material surface = dist <= 3.6 ? Material.PATH : (b.rng.nextInt(4) == 0 ? Material.PATH : Material.GROUND);
                b.set(x, -1, z, surface);
            }
        }

        // Central campfire hearth & curb
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                b.set(x, -1, z, Material.WALL_ACCENT);
            }
        }
        b.set(0, 0, 0, Material.CAMPFIRE);
        // Log seats around campfire
        b.set(-2, 0, 0, Material.LOG, Facing.NORTH);
        b.set(2, 0, 0, Material.LOG, Facing.NORTH);
        b.set(0, 0, 2, Material.LOG, Facing.EAST);
        b.set(-2, 0, 1, Material.SLAB);
        b.set(2, 0, 1, Material.SLAB);

        // Tall Pack Banner Totem north of hearth
        b.set(0, -1, -3, Material.WALL_ACCENT);
        b.set(0, 0, -3, Material.PILLAR);
        b.set(0, 1, -3, Material.PILLAR);
        b.set(0, 2, -3, Material.FENCE);
        b.set(0, 3, -3, Material.BANNER_STANDING, Facing.SOUTH, b.packColor);
        b.set(-1, 1, -3, Material.WALL_TORCH, Facing.WEST);
        b.set(1, 1, -3, Material.WALL_TORCH, Facing.EAST);

        // Two pitched pack-colored wool tents (west has Camp Stash chest)
        buildTent(b, -5, -2, Facing.EAST, true);
        buildTent(b, 5, -2, Facing.WEST, false);

        // Family scatter props south of hearth
        b.set(-3, 0, 4, Material.FAMILY_PROP_A);
        b.set(-2, 0, 4, Material.FAMILY_PROP_B);
        b.set(-3, 1, 4, Material.FAMILY_CANOPY);
        b.set(3, 0, 4, Material.FAMILY_PROP_A);
        b.set(2, 0, 4, Material.BARREL);
        b.set(3, 1, 4, Material.FAMILY_LIGHT);

        // Perimeter corner light posts
        int[][] corners = {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}};
        for (int[] c : corners) {
            b.foundation(c[0], c[1]);
            b.set(c[0], -1, c[1], Material.GROUND);
            b.set(c[0], 0, c[1], Material.FENCE);
            b.set(c[0], 1, c[1], Material.FENCE);
            b.set(c[0], 2, c[1], Material.LANTERN);
        }
    }

    /** 3x4 pitched wool tent centered on (cx, cz), entrance facing East or West. */
    private static void buildTent(Builder b, int cx, int cz, Facing openFace, boolean withCampChest) {
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                b.clearColumn(x, z, 0, 4);
                b.foundation(x, z);
                b.set(x, -1, z, Material.FLOOR);
            }
        }
        // Corner timber posts
        b.set(cx - 1, 0, cz - 2, Material.LOG);
        b.set(cx - 1, 1, cz - 2, Material.LOG);
        b.set(cx + 1, 0, cz - 2, Material.LOG);
        b.set(cx + 1, 1, cz - 2, Material.LOG);
        b.set(cx - 1, 0, cz + 2, Material.LOG);
        b.set(cx - 1, 1, cz + 2, Material.LOG);
        b.set(cx + 1, 0, cz + 2, Material.LOG);
        b.set(cx + 1, 1, cz + 2, Material.LOG);
        // Back wall opposite entrance
        int backX = openFace == Facing.EAST ? cx - 1 : cx + 1;
        for (int z = cz - 1; z <= cz + 1; z++) {
            b.set(backX, 0, z, Material.WOOL_PRIMARY, Facing.NORTH, b.packColor);
            b.set(backX, 1, z, Material.WOOL_ACCENT, Facing.NORTH, b.packColor);
        }
        // Side walls (north & south)
        for (int y = 0; y <= 1; y++) {
            b.set(cx, y, cz - 2, Material.WOOL_PRIMARY, Facing.NORTH, b.packColor);
            b.set(cx, y, cz + 2, Material.WOOL_PRIMARY, Facing.NORTH, b.packColor);
        }
        // Pitched canopy roof
        for (int z = cz - 2; z <= cz + 2; z++) {
            Material stripe = (z & 1) == 0 ? Material.WOOL_PRIMARY : Material.WOOL_ACCENT;
            b.set(cx - 1, 2, z, stripe, Facing.NORTH, b.packColor);
            b.set(cx + 1, 2, z, stripe, Facing.NORTH, b.packColor);
            b.set(cx, 3, z, Material.WOOL_PRIMARY, Facing.NORTH, b.packColor);
        }
        // Interior furnishings
        b.set(cx, 0, cz, Material.CARPET, Facing.NORTH, b.packColor);
        b.set(cx, 0, cz + 1, Material.BARREL);
        if (withCampChest) {
            b.set(cx, 0, cz - 1, Material.CHEST_CAMP, openFace);
        } else {
            b.set(cx, 0, cz - 1, Material.CARPET, Facing.NORTH, b.packColor);
        }
        b.set(cx, 2, cz, Material.HANGING_LANTERN);
    }

    // =========================================================================
    // Tier 2: Outpost (radius 13, ~27x27)
    // =========================================================================

    private static void buildTier2Outpost(Builder b) {
        // 1. Courtyard paths & clearing ring (r = 7..13)
        b.zone("outpost_paths");
        for (int x = -13; x <= 13; x++) {
            for (int z = -13; z <= 13; z++) {
                if (Math.abs(x) <= 7 && Math.abs(z) <= 7) continue;
                b.clearColumn(x, z, 0, 7);
                b.foundation(x, z);
                boolean road = Math.abs(x) <= 1 || Math.abs(z) <= 1;
                b.set(x, -1, z, road ? Material.PATH : Material.GROUND);
            }
        }
        // Extend inner cross paths cleanly through outer edge of camp
        for (int i = -12; i <= 12; i++) {
            if (Math.abs(i) >= 4) {
                b.set(i, -1, 0, Material.PATH);
                b.set(0, -1, i, Material.PATH);
            }
        }

        // 2. Timber/stone palisade wall at max(|x|,|z|) == 12 with South arched gateway
        b.zone("outpost_palisade");
        for (int i = -12; i <= 12; i++) {
            placePalisadeColumn(b, i, -12, i);
            placePalisadeColumn(b, i, 12, i);
            placePalisadeColumn(b, -12, i, i);
            placePalisadeColumn(b, 12, i, i);
        }
        // Corner banner poles
        int[][] corners = {{-12, -12}, {12, -12}, {-12, 12}, {12, 12}};
        for (int[] c : corners) {
            b.set(c[0], 0, c[1], Material.PILLAR);
            b.set(c[0], 1, c[1], Material.PILLAR);
            b.set(c[0], 2, c[1], Material.PILLAR);
            b.set(c[0], 3, c[1], Material.PILLAR);
            b.set(c[0], 4, c[1], Material.FENCE);
            b.set(c[0], 5, c[1], Material.BANNER_STANDING, Facing.SOUTH, b.packColor);
        }
        // Arched South Gateway (x in [-2, 2], z == 12)
        for (int x = -1; x <= 1; x++) {
            b.clearColumn(x, 12, 0, 3);
            b.set(x, -1, 12, Material.PATH);
            b.set(x, 4, 12, Material.LOG, Facing.EAST);
        }
        for (int sx : new int[]{-2, 2}) {
            for (int y = 0; y <= 4; y++) b.set(sx, y, 12, Material.PILLAR);
            b.set(sx, 5, 12, Material.LANTERN);
            b.set(sx, 3, 13, Material.BANNER_WALL, Facing.SOUTH, b.packColor);
        }
        b.set(0, 3, 12, Material.HANGING_LANTERN);

        // 3. 2-Story Watchtower at NW corner (x in [-11,-7], z in [-11,-7])
        b.zone("outpost_watchtower");
        buildWatchtower(b, -9, -9);

        // 4. Boss Pavilion at North courtyard (x in [-3,3], z in [-11,-6])
        b.zone("outpost_pavilion");
        buildBossPavilion(b);

        // 5. Family Work Area at NE corner (x in [7,11], z in [-11,-7])
        b.zone("outpost_work");
        buildFamilyWorkArea(b, 9, -9);

        // 6. Two extra tents in SW & SE corners
        b.zone("outpost_tents");
        buildTent(b, -9, 7, Facing.EAST, false);
        buildTent(b, 9, 7, Facing.WEST, false);
    }

    private static void placePalisadeColumn(Builder b, int x, int z, int seq) {
        b.foundation(x, z);
        b.set(x, -1, z, Material.WALL_ACCENT);
        Material main = (seq & 1) == 0 ? Material.LOG : Material.WALL;
        b.set(x, 0, z, main);
        b.set(x, 1, z, main);
        b.set(x, 2, z, main);
        if ((seq & 1) == 0) {
            b.set(x, 3, z, Material.FENCE);
        } else {
            b.set(x, 3, z, Material.SLAB);
        }
    }

    private static void buildWatchtower(Builder b, int cx, int cz) {
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                b.clearColumn(x, z, 0, 9);
                b.foundation(x, z);
                b.set(x, -1, z, Material.FLOOR);
                // Second story floor at y = 4
                b.set(x, 4, z, Material.PLANKS);
            }
        }
        // Walls & corner pillars y=0..6
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                boolean edgeX = x == cx - 2 || x == cx + 2;
                boolean edgeZ = z == cz - 2 || z == cz + 2;
                if (!edgeX && !edgeZ) continue;
                boolean corner = edgeX && edgeZ;
                for (int y = 0; y <= 5; y++) {
                    if (corner) {
                        b.set(x, y, z, Material.PILLAR);
                    } else if (z == cz + 2 && x == cx && y <= 1) {
                        // South entrance doorway
                        b.set(x, y, z, Material.AIR);
                    } else if ((y == 2 && (x == cx || z == cz)) || (y == 5 && (x == cx || z == cz))) {
                        // Arrow slits
                        b.set(x, y, z, Material.AIR);
                    } else {
                        b.set(x, y, z, y == 5 ? Material.WALL_ACCENT : Material.WALL);
                    }
                }
                if (corner) {
                    b.set(x, 6, z, Material.WALL_ACCENT);
                    b.set(x, 7, z, Material.LANTERN);
                } else if (((x + z) & 1) == 0) {
                    b.set(x, 6, z, Material.SLAB);
                }
            }
        }
        // Ladder on north inner wall up to deck (hatch open at y=4)
        b.set(cx, 4, cz - 1, Material.AIR);
        for (int y = 0; y <= 4; y++) {
            b.set(cx, y, cz - 1, Material.LADDER, Facing.SOUTH);
        }
        b.set(cx - 1, 0, cz - 1, Material.BARREL);
        b.set(cx, 3, cz, Material.HANGING_LANTERN);
        b.set(cx, 5, cz, Material.BANNER_STANDING, Facing.SOUTH, b.packColor);
        b.set(cx, 3, cz + 3, Material.BANNER_WALL, Facing.SOUTH, b.packColor);
    }

    private static void buildBossPavilion(Builder b) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -11; z <= -6; z++) {
                b.clearColumn(x, z, 0, 7);
                b.foundation(x, z);
                b.set(x, -1, z, Material.WALL_ACCENT);
                if (z <= -7) b.set(x, 0, z, Material.FLOOR);
                else b.set(x, 0, z, Material.STAIR, Facing.NORTH);
            }
        }
        // Four tall corner pillars
        for (int px : new int[]{-3, 3}) {
            for (int pz : new int[]{-11, -7}) {
                for (int y = 1; y <= 4; y++) b.set(px, y, pz, Material.PILLAR);
            }
        }
        // Back wall screen
        for (int x = -2; x <= 2; x++) {
            b.set(x, 1, -11, Material.WALL);
            b.set(x, 2, -11, Material.WALL_ACCENT);
            b.set(x, 3, -11, Material.FENCE);
        }
        // Pitched wool & timber canopy
        for (int z = -11; z <= -7; z++) {
            for (int x = -3; x <= 3; x++) {
                int ax = Math.abs(x);
                int ry = ax == 3 ? 4 : ax >= 1 ? 5 : 6;
                Material m = ax == 3 ? Material.SLAB : ((z & 1) == 0 ? Material.WOOL_PRIMARY : Material.WOOL_ACCENT);
                b.set(x, ry, z, m, Facing.NORTH, b.packColor);
            }
        }
        // Throne, stash chest, carpet runner, banners
        b.set(0, 1, -10, Material.THRONE, Facing.SOUTH);
        b.set(-2, 1, -10, Material.BANNER_STANDING, Facing.SOUTH, b.packColor);
        b.set(2, 1, -10, Material.BANNER_STANDING, Facing.SOUTH, b.packColor);
        b.set(2, 1, -9, Material.CHEST_OUTPOST, Facing.WEST);
        b.set(-2, 1, -9, Material.BARREL);
        for (int z = -9; z <= -7; z++) b.set(0, 1, z, Material.CARPET, Facing.NORTH, b.packColor);
        b.set(0, 5, -8, Material.CHAIN);
        b.set(0, 4, -8, Material.HANGING_LANTERN);
    }

    private static void buildFamilyWorkArea(Builder b, int cx, int cz) {
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                b.clearColumn(x, z, 0, 6);
                b.foundation(x, z);
                b.set(x, -1, z, Material.FLOOR);
            }
        }
        switch (b.family) {
            case ILLAGERS -> {
                // Iron-bar prisoner cage + guard barrel
                for (int x = cx - 1; x <= cx + 1; x++) {
                    for (int z = cz - 1; z <= cz + 1; z++) {
                        boolean edge = x == cx - 1 || x == cx + 1 || z == cz - 1 || z == cz + 1;
                        if (edge) {
                            b.set(x, 0, z, Material.IRON_BARS);
                            b.set(x, 1, z, Material.IRON_BARS);
                            b.set(x, 2, z, Material.IRON_BARS);
                        }
                        b.set(x, 3, z, Material.SLAB);
                    }
                }
                b.set(cx, 0, cz + 1, Material.AIR); // cell opening
                b.set(cx, 1, cz + 1, Material.AIR);
                b.set(cx, 2, cz, Material.HANGING_LANTERN);
                b.set(cx - 2, 0, cz + 2, Material.BARREL);
                b.set(cx + 2, 0, cz + 2, Material.FENCE);
                b.set(cx + 2, 1, cz + 2, Material.LANTERN);
            }
            case BONES -> {
                // Archery range: bone posts, target blocks, fletching table
                for (int x = cx - 2; x <= cx + 2; x++) {
                    b.set(x, 0, cz - 2, Material.WALL_ACCENT);
                    b.set(x, 1, cz - 2, (x == cx - 1 || x == cx + 1) ? Material.WORK_PROP_A : Material.WALL);
                    b.set(x, 2, cz - 2, Material.SLAB);
                }
                b.set(cx, 0, cz + 2, Material.WORK_PROP_B);
                b.set(cx - 2, 0, cz + 2, Material.FENCE);
                b.set(cx - 2, 1, cz + 2, Material.FAMILY_LIGHT);
                b.set(cx + 2, 0, cz + 2, Material.FENCE);
                b.set(cx + 2, 1, cz + 2, Material.FAMILY_LIGHT);
                b.set(cx, 0, cz - 1, Material.FAMILY_PROP_A);
            }
            case WITCHES -> {
                // Covered cauldron & brewing shack
                for (int px : new int[]{cx - 2, cx + 2}) {
                    for (int pz : new int[]{cz - 2, cz + 2}) {
                        b.set(px, 0, pz, Material.PILLAR);
                        b.set(px, 1, pz, Material.PILLAR);
                        b.set(px, 2, pz, Material.PILLAR);
                    }
                }
                for (int x = cx - 2; x <= cx + 2; x++) {
                    for (int z = cz - 2; z <= cz + 2; z++) {
                        b.set(x, 3, z, (x == cx || z == cz) ? Material.WOOL_PRIMARY : Material.PLANKS, Facing.NORTH, b.packColor);
                    }
                }
                b.set(cx - 1, 0, cz - 1, Material.WORK_PROP_A); // cauldron
                b.set(cx + 1, 0, cz - 1, Material.WORK_PROP_A); // cauldron
                b.set(cx, 0, cz - 1, Material.PLANKS);
                b.set(cx, 1, cz - 1, Material.WORK_PROP_B); // brewing stand
                b.set(cx - 1, 0, cz + 1, Material.FAMILY_PROP_B); // mushroom
                b.set(cx + 1, 0, cz + 1, Material.BARREL);
                b.set(cx, 2, cz, Material.HANGING_LANTERN);
            }
            case SPIDERS -> {
                // Webbed brood den with cobweb drapes & silk cocoons
                for (int px : new int[]{cx - 2, cx + 2}) {
                    for (int pz : new int[]{cz - 2, cz + 2}) {
                        b.set(px, 0, pz, Material.LOG);
                        b.set(px, 1, pz, Material.LOG);
                        b.set(px, 2, pz, Material.LOG);
                        b.set(px, 3, pz, Material.FAMILY_CANOPY);
                    }
                }
                for (int x = cx - 1; x <= cx + 1; x++) {
                    for (int z = cz - 1; z <= cz + 1; z++) {
                        b.set(x, 3, z, Material.FAMILY_CANOPY);
                    }
                }
                b.set(cx, 0, cz - 1, Material.WOOL_PRIMARY, Facing.NORTH, b.packColor);
                b.set(cx, 1, cz - 1, Material.FAMILY_CANOPY);
                b.set(cx - 1, 0, cz + 1, Material.FAMILY_LIGHT);
                b.set(cx + 1, 0, cz + 1, Material.FAMILY_LIGHT);
                b.set(cx, 0, cz + 1, Material.BARREL);
            }
            case NETHER -> {
                // Soul brazier altar with basalt pillars & chains
                for (int px : new int[]{cx - 2, cx + 2}) {
                    for (int pz : new int[]{cz - 2, cz + 2}) {
                        b.set(px, 0, pz, Material.PILLAR);
                        b.set(px, 1, pz, Material.PILLAR);
                        b.set(px, 2, pz, Material.WALL_ACCENT);
                        b.set(px, 3, pz, Material.FAMILY_LIGHT);
                    }
                }
                for (int x = cx - 1; x <= cx + 1; x++) {
                    for (int z = cz - 1; z <= cz + 1; z++) {
                        b.set(x, 0, z, Material.WALL_ACCENT);
                    }
                }
                b.set(cx, 1, cz, Material.CAMPFIRE);
                b.set(cx - 1, 1, cz - 1, Material.FAMILY_PROP_A);
                b.set(cx + 1, 1, cz - 1, Material.FAMILY_PROP_A);
            }
            default -> {
                // Rotten bone & spoils pit
                for (int x = cx - 2; x <= cx + 2; x++) {
                    for (int z = cz - 2; z <= cz + 2; z++) {
                        boolean edge = x == cx - 2 || x == cx + 2 || z == cz - 2 || z == cz + 2;
                        if (edge && !(z == cz + 2 && x == cx)) {
                            b.set(x, 0, z, Material.WALL_ACCENT);
                            if ((x + z) % 2 == 0) b.set(x, 1, z, Material.FENCE);
                        }
                    }
                }
                b.set(cx, 0, cz, Material.CAMPFIRE);
                b.set(cx - 1, 0, cz - 1, Material.FAMILY_PROP_A);
                b.set(cx + 1, 0, cz - 1, Material.BARREL);
                b.set(cx - 2, 2, cz - 2, Material.LANTERN);
                b.set(cx + 2, 2, cz + 2, Material.LANTERN);
            }
        }
    }

    // =========================================================================
    // Tier 3: Stronghold (radius 20, ~41x41)
    // =========================================================================

    private static void buildTier3Stronghold(Builder b) {
        // 1. Paved cross-streets & clearing ring (r = 13..20)
        b.zone("stronghold_streets");
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                if (Math.abs(x) <= 13 && Math.abs(z) <= 13) continue;
                b.clearColumn(x, z, 0, 8);
                b.foundation(x, z);
                boolean road = Math.abs(x) <= 1 || Math.abs(z) <= 1;
                b.set(x, -1, z, road ? Material.PATH : Material.GROUND);
            }
        }
        // Punch clean archways through Tier 2's palisade on E/W/N so streets connect
        for (int s : new int[]{-12, 12}) {
            for (int d = -1; d <= 1; d++) {
                b.clearColumn(s, d, 0, 3);
                b.set(s, -1, d, Material.PATH);
                b.set(s, 4, d, Material.LOG, Facing.NORTH);
            }
        }
        for (int d = -1; d <= 1; d++) {
            b.clearColumn(d, 12, 0, 3);
            b.set(d, -1, 12, Material.PATH);
        }

        // 2. Fortified stone/brick outer wall at max(|x|,|z|) == 19 + 4 corner guard towers + South Gatehouse
        b.zone("stronghold_wall");
        for (int i = -19; i <= 19; i++) {
            placeFortWall(b, i, -19, i, false);
            placeFortWall(b, i, 19, i, false);
            placeFortWall(b, -19, i, i, true);
            placeFortWall(b, 19, i, i, true);
        }
        // Inner walkway ledge at max == 18
        for (int i = -18; i <= 18; i++) {
            b.set(i, 3, -18, Material.PLANKS);
            b.set(i, 3, 18, Material.PLANKS);
            b.set(-18, 3, i, Material.PLANKS);
            b.set(18, 3, i, Material.PLANKS);
        }
        // 4 Corner Guard Towers
        buildGuardTower(b, -18, -18);
        buildGuardTower(b, 18, -18);
        buildGuardTower(b, -18, 18);
        buildGuardTower(b, 18, 18);
        // Fortified Main Gatehouse on South Wall
        buildGatehouse(b, 0, 19, Facing.SOUTH, 7);

        // 3. Upgrade Boss Pavilion into enclosed gabled Boss Hall (x in [-4,4], z in [-14,-6])
        b.zone("stronghold_boss_hall");
        buildBossHall(b);

        // 4. West Barracks (x in [-17,-13], z in [-5,5])
        b.zone("stronghold_barracks");
        buildBarracks(b);

        // 5. East War Forge / Armory (x in [13,17], z in [-5,5])
        b.zone("stronghold_forge");
        buildStrongholdForge(b);

        // 6. Training Yard & Pillory in SW & SE inner courts
        b.zone("stronghold_training");
        buildTrainingYard(b, -14, 12);
        buildTrainingYard(b, 14, 12);
    }

    private static void placeFortWall(Builder b, int x, int z, int seq, boolean ns) {
        b.foundation(x, z);
        b.set(x, -1, z, Material.WALL_ACCENT);
        Material wall = (seq % 4 == 0) ? Material.WALL_ACCENT : Material.WALL;
        for (int y = 0; y <= 4; y++) b.set(x, y, z, wall);
        if ((seq & 1) == 0) {
            b.set(x, 5, z, Material.WALL_ACCENT);
        }
        if (Math.floorMod(seq, 6) == 0) {
            b.set(x, 6, z, Material.LANTERN);
        }
    }

    private static void buildGuardTower(Builder b, int cx, int cz) {
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                b.clearColumn(x, z, 0, 10);
                b.foundation(x, z);
                b.set(x, -1, z, Material.WALL_ACCENT);
                b.set(x, 5, z, Material.PLANKS);
            }
        }
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                boolean ex = x == cx - 2 || x == cx + 2;
                boolean ez = z == cz - 2 || z == cz + 2;
                if (!ex && !ez) continue;
                boolean corner = ex && ez;
                for (int y = 0; y <= 6; y++) {
                    if (corner) b.set(x, y, z, Material.PILLAR);
                    else if (y == 6 && (x == cx || z == cz)) b.set(x, y, z, Material.AIR);
                    else b.set(x, y, z, y <= 1 ? Material.WALL_ACCENT : Material.WALL);
                }
                if (corner) {
                    b.set(x, 7, z, Material.WALL_ACCENT);
                    b.set(x, 8, z, Material.LANTERN);
                }
            }
        }
        // Inward doorway on y=0..1 and ladder attached to opposite solid outer wall
        int inX = cx < 0 ? cx + 2 : cx - 2;
        int ladX = cx < 0 ? cx - 1 : cx + 1;
        b.set(inX, 0, cz, Material.AIR);
        b.set(inX, 1, cz, Material.AIR);
        b.set(ladX, 5, cz, Material.AIR);
        Facing ladFace = cx < 0 ? Facing.EAST : Facing.WEST;
        for (int y = 0; y <= 5; y++) b.set(ladX, y, cz, Material.LADDER, ladFace);
        b.set(cx, 7, cz + (cz > 0 ? 2 : -2), Material.BANNER_STANDING, cz > 0 ? Facing.SOUTH : Facing.NORTH, b.packColor);
    }

    private static void buildGatehouse(Builder b, int cx, int cz, Facing outFace, int height) {
        boolean ns = outFace == Facing.NORTH || outFace == Facing.SOUTH;
        for (int along = -4; along <= 4; along++) {
            for (int depth = -1; depth <= 1; depth++) {
                int x = ns ? cx + along : cx + depth;
                int z = ns ? cz + depth : cz + along;
                b.clearColumn(x, z, 0, height + 3);
                b.foundation(x, z);
                b.set(x, -1, z, Math.abs(along) <= 1 ? Material.PATH : Material.WALL_ACCENT);
            }
        }
        // Twin flanking towers (along in [-4,-2] and [2,4])
        for (int side : new int[]{-3, 3}) {
            for (int a = side - 1; a <= side + 1; a++) {
                for (int d = -1; d <= 1; d++) {
                    int x = ns ? cx + a : cx + d;
                    int z = ns ? cz + d : cz + a;
                    boolean corner = (a == side - 1 || a == side + 1) && (d == -1 || d == 1);
                    for (int y = 0; y <= height; y++) {
                        if (Math.abs(a - side) == 0 && d == 0 && y > 0 && y < height - 1) continue;
                        b.set(x, y, z, corner ? Material.PILLAR : Material.WALL);
                    }
                    if (corner) b.set(x, height + 1, z, Material.WALL_ACCENT);
                }
            }
            int bx = ns ? cx + side : cx;
            int bz = ns ? cz : cz + side;
            b.set(bx, height + 1, bz, Material.FENCE);
            b.set(bx, height + 2, bz, Material.BANNER_STANDING, outFace, b.packColor);
            // Outer wall banner
            int wx = ns ? cx + side : cx + outFace.dx * 2;
            int wz = ns ? cz + outFace.dz * 2 : cz + side;
            b.set(wx, 3, wz, Material.BANNER_WALL, outFace, b.packColor);
        }
        // Arch bridge over central opening (along in [-1, 1])
        for (int a = -1; a <= 1; a++) {
            for (int d = -1; d <= 1; d++) {
                int x = ns ? cx + a : cx + d;
                int z = ns ? cz + d : cz + a;
                b.set(x, 4, z, Material.WALL_ACCENT);
                if (d == 0) b.set(x, 3, z, Material.IRON_BARS); // portcullis teeth
                if (d != 0 && (a == -1 || a == 1)) b.set(x, 5, z, Material.WALL);
            }
        }
        int lx = ns ? cx : cx + outFace.dx;
        int lz = ns ? cz + outFace.dz : cz;
        b.set(lx, 3, lz, Material.HANGING_LANTERN);
    }

    private static void buildBossHall(Builder b) {
        int x0 = -4, x1 = 4, z0 = -14, z1 = -6;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                b.clearColumn(x, z, 0, 10);
                b.foundation(x, z);
                b.set(x, -1, z, Material.WALL_ACCENT);
                b.set(x, 0, z, Material.FLOOR);
            }
        }
        // Front entry steps
        for (int x = -1; x <= 1; x++) {
            b.foundation(x, z1 + 1);
            b.set(x, -1, z1 + 1, Material.PATH);
            b.set(x, 0, z1 + 1, Material.STAIR, Facing.NORTH);
        }
        // Walls y = 1..4
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean ex = x == x0 || x == x1;
                boolean ez = z == z0 || z == z1;
                if (!ex && !ez) continue;
                boolean corner = ex && ez;
                for (int y = 1; y <= 4; y++) {
                    if (z == z1 && Math.abs(x) <= 1 && y <= 3) {
                        b.set(x, y, z, Material.AIR); // wide doorway
                    } else if (ex && (z == -12 || z == -8) && y == 2) {
                        b.set(x, y, z, Material.IRON_BARS); // window
                    } else {
                        b.set(x, y, z, corner ? Material.PILLAR : (y == 1 ? Material.WALL_ACCENT : Material.WALL));
                    }
                }
            }
        }
        // Pitched gabled roof (ridge along x = 0, running z0..z1)
        for (int z = z0 - 1; z <= z1 + 1; z++) {
            for (int step = 0; step <= 4; step++) {
                int ry = 4 + step;
                if (step < 4) {
                    b.set(x0 + step, ry, z, Material.STAIR, Facing.EAST);
                    b.set(x1 - step, ry, z, Material.STAIR, Facing.WEST);
                } else {
                    b.set(0, ry, z, Material.SLAB);
                }
            }
        }
        // Gable infill triangles on North & South faces
        for (int gz : new int[]{z0, z1}) {
            for (int step = 1; step <= 3; step++) {
                for (int x = x0 + step; x <= x1 - step; x++) {
                    b.set(x, 4 + step, gz, Material.PLANKS);
                }
            }
        }
        // Interior throne dais, carpet, banners, Stronghold Stash
        for (int x = -2; x <= 2; x++) b.set(x, 1, -13, Material.SLAB);
        b.set(0, 1, -13, Material.THRONE, Facing.SOUTH);
        b.set(-2, 1, -12, Material.BANNER_STANDING, Facing.SOUTH, b.packColor);
        b.set(2, 1, -12, Material.BANNER_STANDING, Facing.SOUTH, b.packColor);
        b.set(3, 1, -12, Material.CHEST_STRONGHOLD, Facing.WEST);
        b.set(-3, 1, -12, Material.BARREL);
        for (int z = -12; z <= -7; z++) b.set(0, 1, z, Material.CARPET, Facing.NORTH, b.packColor);
        b.set(0, 7, -10, Material.PLANKS);
        b.set(0, 6, -10, Material.CHAIN);
        b.set(0, 5, -10, Material.HANGING_LANTERN);
        b.set(-2, 3, z1 + 1, Material.BANNER_WALL, Facing.SOUTH, b.packColor);
        b.set(2, 3, z1 + 1, Material.BANNER_WALL, Facing.SOUTH, b.packColor);
    }

    private static void buildBarracks(Builder b) {
        int x0 = -17, x1 = -13, z0 = -5, z1 = 5;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                b.clearColumn(x, z, 0, 7);
                b.foundation(x, z);
                b.set(x, -1, z, Material.FLOOR);
            }
        }
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean ex = x == x0 || x == x1;
                boolean ez = z == z0 || z == z1;
                if (!ex && !ez) continue;
                boolean corner = ex && ez;
                for (int y = 0; y <= 3; y++) {
                    if (x == x1 && Math.abs(z) <= 1 && y <= 2) {
                        b.set(x, y, z, Material.AIR); // East doorway
                    } else if (x == x1 && (z == -3 || z == 3) && y == 1) {
                        b.set(x, y, z, Material.FENCE);
                    } else {
                        b.set(x, y, z, corner ? Material.PILLAR : Material.PLANKS);
                    }
                }
            }
        }
        // Pitched roof running N-S
        for (int z = z0; z <= z1; z++) {
            b.set(x0, 3, z, Material.STAIR, Facing.EAST);
            b.set(x1, 3, z, Material.STAIR, Facing.WEST);
            b.set(x0 + 1, 4, z, Material.STAIR, Facing.EAST);
            b.set(x1 - 1, 4, z, Material.STAIR, Facing.WEST);
            b.set(-15, 5, z, Material.SLAB);
        }
        // Interior bunk pallets (wool + carpet, no beds!) & barrels
        for (int z : new int[]{-4, -2, 2, 4}) {
            b.set(-16, 0, z, Material.WOOL_PRIMARY, Facing.NORTH, b.packColor);
            b.set(-15, 0, z, Material.CARPET, Facing.NORTH, b.packColor);
        }
        b.set(-16, 0, 0, Material.BARREL);
        b.set(-15, 4, 0, Material.PLANKS);
        b.set(-15, 3, 0, Material.HANGING_LANTERN);
        b.set(x1 + 1, 2, -2, Material.BANNER_WALL, Facing.EAST, b.packColor);
    }

    private static void buildStrongholdForge(Builder b) {
        int x0 = 13, x1 = 17, z0 = -5, z1 = 5;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                b.clearColumn(x, z, 0, 8);
                b.foundation(x, z);
                b.set(x, -1, z, Material.WALL_ACCENT);
            }
        }
        // Back wall + corner pillars (open front on West facing street)
        for (int z = z0; z <= z1; z++) {
            for (int y = 0; y <= 3; y++) {
                b.set(x1, y, z, (z == z0 || z == z1) ? Material.PILLAR : Material.WALL);
            }
        }
        for (int z : new int[]{z0, z1}) {
            for (int y = 0; y <= 3; y++) b.set(x0, y, z, Material.PILLAR);
            for (int x = x0 + 1; x < x1; x++) {
                b.set(x, 0, z, Material.WALL);
                b.set(x, 1, z, Material.FENCE);
            }
        }
        // Sloped smithy roof
        for (int z = z0; z <= z1; z++) {
            b.set(x0, 3, z, Material.SLAB);
            b.set(x0 + 1, 4, z, Material.PLANKS);
            b.set(x0 + 2, 4, z, Material.PLANKS);
            b.set(x0 + 3, 4, z, Material.PLANKS);
            b.set(x1, 4, z, Material.WALL_ACCENT);
        }
        // Stone hearth & chimney at (16, 0, -3)
        b.set(16, 0, -3, Material.CAMPFIRE);
        b.set(15, 1, -3, Material.IRON_BARS);
        for (int y = 2; y <= 6; y++) b.set(16, y, -3, Material.WALL_ACCENT);
        b.set(16, 0, 0, Material.WORK_PROP_A);
        b.set(16, 0, 2, Material.BARREL);
        b.set(16, 0, 3, Material.BARREL);
        b.set(15, 3, 1, Material.HANGING_LANTERN);
        b.set(x0 - 1, 2, z0, Material.BANNER_WALL, Facing.WEST, b.packColor);
    }

    private static void buildTrainingYard(Builder b, int cx, int cz) {
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                b.clearColumn(x, z, 0, 5);
                b.foundation(x, z);
                b.set(x, -1, z, Material.PATH);
            }
        }
        b.set(cx, 0, cz, Material.FENCE);
        b.set(cx, 1, cz, Material.FENCE);
        b.set(cx, 2, cz, Material.WORK_PROP_A);
        b.set(cx - 2, 0, cz - 2, Material.FENCE);
        b.set(cx - 2, 1, cz - 2, Material.LANTERN);
        b.set(cx + 2, 0, cz + 2, Material.FENCE);
        b.set(cx + 2, 1, cz + 2, Material.LANTERN);
        b.set(cx + 1, 0, cz - 2, Material.BARREL);
    }

    // =========================================================================
    // Tier 4: Town (radius 29, ~59x59)
    // =========================================================================

    private static void buildTier4Town(Builder b) {
        // 1. Wide Town Avenues & outer ground ring (r = 20..29)
        b.zone("town_streets");
        for (int x = -29; x <= 29; x++) {
            for (int z = -29; z <= 29; z++) {
                if (Math.abs(x) <= 20 && Math.abs(z) <= 20) continue;
                b.clearColumn(x, z, 0, 9);
                b.foundation(x, z);
                boolean avenue = Math.abs(x) <= 2 || Math.abs(z) <= 2;
                boolean ring = Math.abs(Math.abs(x) - 20) <= 1 || Math.abs(Math.abs(z) - 20) <= 1;
                b.set(x, -1, z, (avenue || ring) ? Material.PATH : Material.GROUND);
            }
        }
        // Punch arched avenues through Tier 3's walls at N, E, W (South already has gatehouse)
        for (int d = -1; d <= 1; d++) {
            // North wall at z = -19
            b.clearColumn(d, -19, 0, 3);
            b.clearColumn(d, -18, 0, 2);
            b.set(d, -1, -19, Material.PATH);
            b.set(d, 4, -19, Material.WALL_ACCENT);
            // West wall at x = -19
            b.clearColumn(-19, d, 0, 3);
            b.clearColumn(-18, d, 0, 2);
            b.set(-19, -1, d, Material.PATH);
            b.set(-19, 4, d, Material.WALL_ACCENT);
            // East wall at x = 19
            b.clearColumn(19, d, 0, 3);
            b.clearColumn(18, d, 0, 2);
            b.set(19, -1, d, Material.PATH);
            b.set(19, 4, d, Material.WALL_ACCENT);
        }
        // Avenue lamp posts & pack banners
        for (int p : new int[]{-24, -15, 15, 24}) {
            placeLampPost(b, -3, p, Facing.EAST);
            placeLampPost(b, 3, p, Facing.WEST);
            placeLampPost(b, p, -3, Facing.SOUTH);
            placeLampPost(b, p, 3, Facing.NORTH);
        }

        // 2. Outer Town Wall at max(|x|,|z|) == 28 + 4 corner bastions + 4 Town Gatehouses
        b.zone("town_wall");
        for (int i = -28; i <= 28; i++) {
            placeFortWall(b, i, -28, i, false);
            placeFortWall(b, i, 28, i, false);
            placeFortWall(b, -28, i, i, true);
            placeFortWall(b, 28, i, i, true);
        }
        buildGuardTower(b, -27, -27);
        buildGuardTower(b, 27, -27);
        buildGuardTower(b, -27, 27);
        buildGuardTower(b, 27, 27);
        buildGatehouse(b, 0, 28, Facing.SOUTH, 7);
        buildGatehouse(b, 0, -28, Facing.NORTH, 7);
        buildGatehouse(b, -28, 0, Facing.WEST, 7);
        buildGatehouse(b, 28, 0, Facing.EAST, 7);

        // 3. Upgrade Boss Hall into grand 2-story King's Hall / Citadel (x in [-5,5], z in [-16,-5])
        b.zone("town_kings_hall");
        buildKingsHall(b);

        // 4. Town Square / War Monument at (0, 0, 5)
        b.zone("town_square");
        buildTownMonument(b, 0, 5);

        // 5. Outer Town Districts (each its own zone for per-building player-build protection)
        b.zone("town_tavern");
        buildTavern(b, -26, -17, -26, -18);

        b.zone("town_jail");
        buildTownJail(b, 17, 26, -26, -18);

        b.zone("town_forge");
        buildTownForge(b, 21, 26, 6, 15);

        b.zone("town_market");
        buildMarketStalls(b);

        b.zone("town_house_1");
        buildGangHouse(b, -26, -21, -14, -7, Facing.EAST);

        b.zone("town_house_2");
        buildGangHouse(b, -26, -21, 7, 14, Facing.EAST);

        b.zone("town_house_3");
        buildGangHouse(b, 21, 26, -14, -7, Facing.WEST);

        b.zone("town_house_4");
        buildGangHouse(b, 12, 19, 21, 26, Facing.NORTH);
    }

    private static void placeLampPost(Builder b, int x, int z, Facing arm) {
        b.foundation(x, z);
        b.set(x, -1, z, Material.WALL_ACCENT);
        b.set(x, 0, z, Material.FENCE);
        b.set(x, 1, z, Material.FENCE);
        b.set(x, 2, z, Material.FENCE);
        b.set(x, 3, z, Material.FENCE);
        b.set(x + arm.dx, 3, z + arm.dz, Material.FENCE);
        b.set(x + arm.dx, 2, z + arm.dz, Material.HANGING_LANTERN);
    }

    private static void buildKingsHall(Builder b) {
        int x0 = -5, x1 = 5, z0 = -16, z1 = -5;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                b.clearColumn(x, z, 0, 14);
                b.foundation(x, z);
                b.set(x, -1, z, Material.WALL_ACCENT);
                b.set(x, 0, z, Material.FLOOR);
            }
        }
        // Grand entry stairs at z = -4
        for (int x = -2; x <= 2; x++) {
            b.foundation(x, z1 + 1);
            b.set(x, -1, z1 + 1, Material.PATH);
            b.set(x, 0, z1 + 1, Material.STAIR, Facing.NORTH);
        }
        // Two-story fortified walls y = 1..6
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean ex = x == x0 || x == x1;
                boolean ez = z == z0 || z == z1;
                if (!ex && !ez) continue;
                boolean corner = ex && ez;
                for (int y = 1; y <= 6; y++) {
                    if (z == z1 && Math.abs(x) <= 1 && y <= 4) {
                        b.set(x, y, z, Material.AIR); // grand arch doorway
                    } else if (ex && (z == -13 || z == -10 || z == -7) && (y == 2 || y == 5)) {
                        b.set(x, y, z, Material.IRON_BARS); // tall arched windows
                    } else if (z == z0 && Math.abs(x) == 2 && y == 5) {
                        b.set(x, y, z, Material.IRON_BARS);
                    } else {
                        b.set(x, y, z, corner ? Material.PILLAR : ((y == 1 || y == 4) ? Material.WALL_ACCENT : Material.WALL));
                    }
                }
            }
        }
        // Second-floor interior gallery ledges along East & West walls
        for (int z = z0 + 1; z <= z1 - 1; z++) {
            b.set(x0 + 1, 4, z, Material.PLANKS);
            b.set(x1 - 1, 4, z, Material.PLANKS);
            if ((z & 1) == 0) {
                b.set(x0 + 1, 5, z, Material.FENCE);
                b.set(x1 - 1, 5, z, Material.FENCE);
            }
        }
        // Pitched grand roof y = 6..11
        for (int z = z0 - 1; z <= z1 + 1; z++) {
            for (int step = 0; step <= 5; step++) {
                int ry = 6 + step;
                if (step < 5) {
                    b.set(x0 + step, ry, z, Material.STAIR, Facing.EAST);
                    b.set(x1 - step, ry, z, Material.STAIR, Facing.WEST);
                } else {
                    b.set(0, ry, z, Material.SLAB);
                }
            }
        }
        // Gable triangles
        for (int gz : new int[]{z0, z1}) {
            for (int step = 1; step <= 4; step++) {
                for (int x = x0 + step; x <= x1 - step; x++) {
                    b.set(x, 6 + step, gz, Material.WALL_ACCENT);
                }
            }
        }
        // Central King's Banner Spire rising to y = 14
        for (int x = -1; x <= 1; x++) {
            for (int z = -11; z <= -9; z++) {
                boolean corner = Math.abs(x) == 1 && Math.abs(z + 10) == 1;
                b.set(x, 10, z, Material.WALL_ACCENT);
                if (corner) b.set(x, 11, z, Material.LANTERN);
            }
        }
        b.set(0, 11, -10, Material.PILLAR);
        b.set(0, 12, -10, Material.FENCE);
        b.set(0, 13, -10, Material.FENCE);
        b.set(0, 14, -10, Material.BANNER_STANDING, Facing.SOUTH, b.packColor);

        // Royal Throne Room dais & King's Treasury
        for (int x = -3; x <= 3; x++) {
            for (int z = -15; z <= -13; z++) {
                b.set(x, 1, z, Material.WALL_ACCENT);
            }
        }
        b.set(0, 2, -14, Material.THRONE, Facing.SOUTH);
        b.set(-2, 2, -14, Material.BANNER_STANDING, Facing.SOUTH, b.packColor);
        b.set(2, 2, -14, Material.BANNER_STANDING, Facing.SOUTH, b.packColor);
        b.set(-3, 2, -14, Material.CHEST_TOWN, Facing.EAST);
        b.set(3, 2, -14, Material.CHEST_TOWN, Facing.WEST);
        for (int z = -12; z <= -6; z++) {
            b.set(0, 1, z, Material.CARPET, Facing.NORTH, b.packColor);
        }
        // Chandeliers
        for (int z : new int[]{-12, -8}) {
            b.set(0, 10, z, Material.WALL_ACCENT);
            b.set(0, 9, z, Material.CHAIN);
            b.set(0, 8, z, Material.CHAIN);
            b.set(0, 7, z, Material.HANGING_LANTERN);
        }
        // Front facade banners
        b.set(-3, 4, z1 + 1, Material.BANNER_WALL, Facing.SOUTH, b.packColor);
        b.set(3, 4, z1 + 1, Material.BANNER_WALL, Facing.SOUTH, b.packColor);

        // Vassal Pack Banners flown in the forecourt alongside the King's banner
        int[][] vassalSpots = {{-4, -3}, {4, -3}, {-4, -1}, {4, -1}, {-6, -4}, {6, -4}};
        for (int i = 0; i < Math.min(b.vassalColors.size(), vassalSpots.length); i++) {
            int vx = vassalSpots[i][0];
            int vz = vassalSpots[i][1];
            int vColor = b.vassalColors.get(i);
            b.foundation(vx, vz);
            b.set(vx, -1, vz, Material.WALL_ACCENT);
            b.set(vx, 0, vz, Material.PILLAR);
            b.set(vx, 1, vz, Material.FENCE);
            b.set(vx, 2, vz, Material.VASSAL_BANNER, Facing.SOUTH, vColor);
        }
    }

    private static void buildTownMonument(Builder b, int cx, int cz) {
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                b.clearColumn(x, z, 0, 7);
                b.foundation(x, z);
                b.set(x, -1, z, Material.PATH);
            }
        }
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                b.set(x, 0, z, Material.WALL_ACCENT);
            }
        }
        b.set(cx, 1, cz, Material.PILLAR);
        b.set(cx, 2, cz, Material.CAMPFIRE);
        b.set(cx, 1, cz - 1, Material.BANNER_WALL, Facing.NORTH, b.packColor);
        b.set(cx, 1, cz + 1, Material.BANNER_WALL, Facing.SOUTH, b.packColor);
        b.set(cx - 1, 1, cz, Material.BANNER_WALL, Facing.WEST, b.packColor);
        b.set(cx + 1, 1, cz, Material.BANNER_WALL, Facing.EAST, b.packColor);
        for (int dx : new int[]{-2, 2}) {
            for (int dz : new int[]{-2, 2}) {
                b.set(cx + dx, 0, cz + dz, Material.FENCE);
                b.set(cx + dx, 1, cz + dz, Material.LANTERN);
            }
        }
    }

    private static void buildTavern(Builder b, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                b.clearColumn(x, z, 0, 9);
                b.foundation(x, z);
                b.set(x, -1, z, Material.FLOOR);
            }
        }
        int midX = (x0 + x1) / 2;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean ex = x == x0 || x == x1;
                boolean ez = z == z0 || z == z1;
                if (!ex && !ez) continue;
                boolean corner = ex && ez;
                for (int y = 0; y <= 3; y++) {
                    if (z == z1 && Math.abs(x - midX) <= 1 && y <= 2) {
                        b.set(x, y, z, Material.AIR); // wide tavern entry
                    } else if ((ex && (z == z0 + 2 || z == z1 - 2) && y == 1) || (ez && Math.abs(x - midX) == 3 && y == 1)) {
                        b.set(x, y, z, Material.FENCE); // windows
                    } else {
                        b.set(x, y, z, corner ? Material.PILLAR : (y == 0 ? Material.WALL : Material.PLANKS));
                    }
                }
            }
        }
        // Pitched gabled roof along X axis
        int halfZ = (z1 - z0) / 2;
        for (int x = x0; x <= x1; x++) {
            for (int step = 0; step <= halfZ; step++) {
                int ry = 3 + step;
                if (z0 + step < z1 - step) {
                    b.set(x, ry, z0 + step, Material.STAIR, Facing.SOUTH);
                    b.set(x, ry, z1 - step, Material.STAIR, Facing.NORTH);
                } else {
                    b.set(x, ry, z0 + step, Material.SLAB);
                }
            }
        }
        // Bar counter & stacked barrels at back
        for (int x = x0 + 2; x <= x1 - 2; x++) {
            b.set(x, 0, z0 + 2, Material.PLANKS);
            b.set(x, 0, z0 + 1, Material.BARREL);
            if ((x & 1) == 0) b.set(x, 1, z0 + 1, Material.BARREL);
        }
        // Long mess tables & benches
        for (int tx : new int[]{x0 + 2, x1 - 2}) {
            b.set(tx, 0, z1 - 3, Material.FENCE);
            b.set(tx, 1, z1 - 3, Material.SLAB);
            b.set(tx, 0, z1 - 2, Material.STAIR, Facing.NORTH);
        }
        b.set(x0 + 2, 1, z0 + 2, Material.LANTERN);
        b.set(x1 - 2, 1, z0 + 2, Material.LANTERN);
        b.set(midX - 2, 2, z1 + 1, Material.BANNER_WALL, Facing.SOUTH, b.packColor);
        b.set(midX + 2, 2, z1 + 1, Material.WALL_TORCH, Facing.SOUTH);
    }

    private static void buildTownJail(Builder b, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                b.clearColumn(x, z, 0, 7);
                b.foundation(x, z);
                b.set(x, -1, z, Material.WALL_ACCENT);
            }
        }
        // Fortified stone cell block along North half (z0 .. z0+4)
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z0 + 4; z++) {
                boolean back = z == z0;
                boolean side = x == x0 || x == x1;
                boolean divider = x == (x0 + x1) / 2;
                boolean front = z == z0 + 4;
                for (int y = 0; y <= 3; y++) {
                    if (back || side || divider) {
                        b.set(x, y, z, (x == x0 || x == x1) && (z == z0 || z == z0 + 4) ? Material.PILLAR : Material.WALL);
                    } else if (front) {
                        boolean door = (x == x0 + 2 || x == x1 - 2) && y <= 1;
                        b.set(x, y, z, door ? Material.AIR : Material.IRON_BARS);
                    }
                }
                b.set(x, 4, z, Material.WALL_ACCENT);
            }
        }
        // Guard desk & outdoor pillory in South yard
        b.set(x0 + 1, 0, z1 - 1, Material.PLANKS);
        b.set(x0 + 2, 0, z1 - 1, Material.BARREL);
        int px = x1 - 2, pz = z1 - 1;
        b.set(px, 0, pz, Material.FENCE);
        b.set(px, 1, pz, Material.FENCE);
        b.set(px - 1, 1, pz, Material.IRON_BARS);
        b.set(px + 1, 1, pz, Material.IRON_BARS);
        b.set(x0 + 2, 3, z0 + 2, Material.HANGING_LANTERN);
        b.set(x1 - 2, 3, z0 + 2, Material.HANGING_LANTERN);
        b.set((x0 + x1) / 2, 2, z0 + 5, Material.BANNER_WALL, Facing.SOUTH, b.packColor);
    }

    private static void buildTownForge(Builder b, int x0, int x1, int z0, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                b.clearColumn(x, z, 0, 8);
                b.foundation(x, z);
                b.set(x, -1, z, Material.WALL_ACCENT);
            }
        }
        for (int z = z0; z <= z1; z++) {
            for (int y = 0; y <= 3; y++) {
                b.set(x1, y, z, (z == z0 || z == z1) ? Material.PILLAR : Material.WALL);
            }
        }
        for (int z : new int[]{z0, z1}) {
            for (int y = 0; y <= 3; y++) b.set(x0, y, z, Material.PILLAR);
            for (int x = x0 + 1; x < x1; x++) {
                b.set(x, 0, z, Material.WALL);
                b.set(x, 1, z, Material.IRON_BARS);
            }
        }
        for (int z = z0; z <= z1; z++) {
            for (int x = x0; x <= x1; x++) {
                b.set(x, 4, z, x == x0 ? Material.SLAB : Material.PLANKS);
            }
        }
        // Hearth & chimney
        int cz = (z0 + z1) / 2;
        b.set(x1 - 1, 0, cz - 1, Material.CAMPFIRE);
        b.set(x1 - 2, 1, cz - 1, Material.IRON_BARS);
        for (int y = 2; y <= 7; y++) b.set(x1 - 1, y, cz - 1, Material.WALL_ACCENT);
        b.set(x1 - 1, 0, cz + 2, Material.WORK_PROP_A);
        b.set(x1 - 1, 0, cz + 3, Material.WORK_PROP_B);
        b.set(x1 - 1, 0, z0 + 1, Material.CHEST_OUTPOST, Facing.WEST);
        b.set(x1 - 1, 0, z1 - 1, Material.BARREL);
        b.set(x0 + 2, 3, cz, Material.HANGING_LANTERN);
        b.set(x0 - 1, 2, z0, Material.BANNER_WALL, Facing.WEST, b.packColor);
    }

    private static void buildMarketStalls(Builder b) {
        int[] starts = {-25, -20, -15};
        for (int s = 0; s < starts.length; s++) {
            int x0 = starts[s], x1 = x0 + 3, z0 = 21, z1 = 25;
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    b.clearColumn(x, z, 0, 5);
                    b.foundation(x, z);
                    b.set(x, -1, z, Material.PATH);
                }
            }
            for (int px : new int[]{x0, x1}) {
                for (int pz : new int[]{z0, z1}) {
                    b.set(px, 0, pz, Material.FENCE);
                    b.set(px, 1, pz, Material.FENCE);
                    b.set(px, 2, pz, Material.FENCE);
                }
            }
            // Striped wool awning
            for (int x = x0; x <= x1; x++) {
                Material stripe = ((x + s) & 1) == 0 ? Material.WOOL_PRIMARY : Material.WOOL_ACCENT;
                for (int z = z0; z <= z1; z++) {
                    b.set(x, 3, z, stripe, Facing.NORTH, b.packColor);
                }
            }
            // Stall counter, crates, lantern
            for (int x = x0 + 1; x <= x1 - 1; x++) {
                b.set(x, 0, z0 + 1, Material.PLANKS);
                b.set(x, 1, z0 + 1, Material.CARPET, Facing.NORTH, b.packColor);
            }
            b.set(x0 + 1, 0, z1 - 1, Material.BARREL);
            b.set(x1 - 1, 0, z1 - 1, s == 1 ? Material.FAMILY_PROP_A : Material.BARREL);
            b.set(x0 + 1, 2, z0 + 2, Material.HANGING_LANTERN);
        }
    }

    private static void buildGangHouse(Builder b, int x0, int x1, int z0, int z1, Facing doorFace) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                b.clearColumn(x, z, 0, 7);
                b.foundation(x, z);
                b.set(x, -1, z, Material.FLOOR);
            }
        }
        int midX = (x0 + x1) / 2;
        int midZ = (z0 + z1) / 2;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean ex = x == x0 || x == x1;
                boolean ez = z == z0 || z == z1;
                if (!ex && !ez) continue;
                boolean corner = ex && ez;
                boolean isDoor = switch (doorFace) {
                    case EAST -> x == x1 && z == midZ;
                    case WEST -> x == x0 && z == midZ;
                    case NORTH -> z == z0 && x == midX;
                    case SOUTH -> z == z1 && x == midX;
                };
                boolean isWindow = !corner && !isDoor && ((ex && z == midZ) || (ez && x == midX));
                for (int y = 0; y <= 3; y++) {
                    if (isDoor && y <= 1) b.set(x, y, z, Material.AIR);
                    else if (isWindow && y == 1) b.set(x, y, z, Material.FENCE);
                    else b.set(x, y, z, corner ? Material.PILLAR : (y == 0 ? Material.WALL : Material.PLANKS));
                }
            }
        }
        // Pitched gabled roof along longer axis
        if (x1 - x0 <= z1 - z0) {
            int half = (x1 - x0) / 2;
            for (int z = z0; z <= z1; z++) {
                for (int s = 0; s <= half; s++) {
                    if (x0 + s < x1 - s) {
                        b.set(x0 + s, 3 + s, z, Material.STAIR, Facing.EAST);
                        b.set(x1 - s, 3 + s, z, Material.STAIR, Facing.WEST);
                    } else {
                        b.set(x0 + s, 3 + s, z, Material.SLAB);
                    }
                }
            }
        } else {
            int half = (z1 - z0) / 2;
            for (int x = x0; x <= x1; x++) {
                for (int s = 0; s <= half; s++) {
                    if (z0 + s < z1 - s) {
                        b.set(x, 3 + s, z0 + s, Material.STAIR, Facing.SOUTH);
                        b.set(x, 3 + s, z1 - s, Material.STAIR, Facing.NORTH);
                    } else {
                        b.set(x, 3 + s, z0 + s, Material.SLAB);
                    }
                }
            }
        }
        // Interior carpet, barrel, lantern + exterior pack banner
        b.set(midX, 0, midZ, Material.CARPET, Facing.NORTH, b.packColor);
        b.set(x0 + 1, 0, z0 + 1, Material.BARREL);
        b.set(x0 + 1, 1, z0 + 1, Material.LANTERN);
        int bx = switch (doorFace) {
            case EAST -> x1 + 1;
            case WEST -> x0 - 1;
            default -> midX - 1;
        };
        int bz = switch (doorFace) {
            case NORTH -> z0 - 1;
            case SOUTH -> z1 + 1;
            default -> midZ - 1;
        };
        b.set(bx, 2, bz, Material.BANNER_WALL, doorFace, b.packColor);
    }

    // =========================================================================
    // Builder (rotation, zone tracking, deduplication, pass ordering)
    // =========================================================================

    private static final class Builder {
        final int facing;
        final int packColor;
        final List<Integer> vassalColors;
        final Family family;
        final Random rng;
        String currentZone = "core";

        private final Map<Long, Placement> byPos = new LinkedHashMap<>();
        private final Map<Long, Placement> foundations = new LinkedHashMap<>();
        private final Map<String, Box> zones = new LinkedHashMap<>();

        Builder(int facing, int packColor, List<Integer> vassalColors, Family family, Random rng) {
            this.facing = facing;
            this.packColor = packColor;
            this.vassalColors = vassalColors;
            this.family = family;
            this.rng = rng;
        }

        void zone(String name) {
            this.currentZone = name;
        }

        void clearColumn(int x, int z, int yFrom, int yTo) {
            for (int y = yFrom; y <= yTo; y++) {
                set(x, y, z, Material.AIR);
            }
        }

        void foundation(int x, int z) {
            int rx = rotX(x, z), rz = rotZ(x, z);
            long k = key(rx, -2, rz);
            foundations.put(k, new Placement(rx, -2, rz, Material.FOUNDATION_COLUMN, Facing.NORTH, 0, PASS_FOUNDATION, currentZone));
            recordZone(rx, -2, rz);
        }

        void set(int x, int y, int z, Material mat) {
            set(x, y, z, mat, Facing.NORTH, 0);
        }

        void set(int x, int y, int z, Material mat, Facing dir) {
            set(x, y, z, mat, dir, 0);
        }

        void set(int x, int y, int z, Material mat, Facing dir, int meta) {
            int rx = rotX(x, z);
            int rz = rotZ(x, z);
            Facing rdir = dir.rotate(facing);
            byPos.put(key(rx, y, rz), new Placement(rx, y, rz, mat, rdir, meta, mat.pass, currentZone));
            recordZone(rx, y, rz);
        }

        private void recordZone(int rx, int ry, int rz) {
            Box box = zones.get(currentZone);
            zones.put(currentZone, box == null ? new Box(rx, ry, rz, rx, ry, rz) : box.expand(rx, ry, rz));
        }

        private int rotX(int x, int z) {
            return switch (facing) {
                case 1 -> -z;
                case 2 -> -x;
                case 3 -> z;
                default -> x;
            };
        }

        private int rotZ(int x, int z) {
            return switch (facing) {
                case 1 -> x;
                case 2 -> -z;
                case 3 -> -x;
                default -> z;
            };
        }

        private static long key(int x, int y, int z) {
            return (((long) (x + 512) & 0xFFFFFL) << 40)
                    | (((long) (y + 512) & 0xFFFFFL) << 20)
                    | ((long) (z + 512) & 0xFFFFFL);
        }

        Plan finish(Anchors anchors, int fromTier, int toTier) {
            List<Placement> all = new ArrayList<>(foundations.size() + byPos.size());
            all.addAll(foundations.values());
            all.addAll(byPos.values());
            // Strict ordering: pass (foundation -> structure -> furniture -> decor), then bottom-up by ry
            all.sort(Comparator.comparingInt(Placement::pass)
                    .thenComparingInt(Placement::ry)
                    .thenComparingInt(Placement::rx)
                    .thenComparingInt(Placement::rz));
            return new Plan(List.copyOf(all), Map.copyOf(zones), anchors.bossSpot, anchors.guardSpots,
                    anchors.patrolSpots, fromTier, toTier, facing);
        }
    }
}
