package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.game.camp.SiteFinder;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Picks the ground a pack that just formed makes its home on. It withdrew from the fight on paper (see
 * {@link Feud#settleAway}); this reads the generated ground at each of its {@link Feud#homeCandidates} off the server
 * thread, without loading anything, and takes the first that is dry and level and that no other pack holds. The
 * camp's own site search then works there as everywhere else, with the same reservation and player-block rules.
 */
final class Settler {
    /** Places tried round where the pack formed. */
    static final int CANDIDATES = 12;
    /** How far from a candidate's middle its ground is sampled. */
    static final int SPREAD = 10;
    /** Most height difference across a candidate's samples. */
    static final int MAX_SLOPE = 8;
    /** A pack whose ground can't be read in this long settles where it first picked. */
    static final long TIMEOUT_TICKS = 1200;

    private record Survey(List<int[]> candidates, CompletableFuture<int[][]> ground, long startedAt) {
    }

    private final Map<UUID, Survey> surveys = new HashMap<>();

    /** Sample offsets round a candidate's middle, the middle first. */
    static final int[][] SAMPLES = {{0, 0}, {SPREAD, 0}, {-SPREAD, 0}, {0, SPREAD}, {0, -SPREAD},
            {SPREAD, SPREAD}, {-SPREAD, SPREAD}, {SPREAD, -SPREAD}, {-SPREAD, -SPREAD}};

    /** One second's work. Returns whether a pack settled. */
    boolean tick(MinecraftServer server, Feud feud, long gameTime) {
        surveys.keySet().removeIf(id -> {
            Pack p = feud.pack(id);
            return p == null || !p.settling;
        });
        boolean changed = false;
        for (Pack p : new ArrayList<>(feud.packs.values())) {
            if (!p.settling) continue;
            if (p.siteSet) { // can't happen for a new pack; never move a pack off a site it chose
                p.settling = false;
                changed = true;
                continue;
            }
            ServerLevel level = Live.current() == null ? null : Live.current().level(p.dimension);
            Survey s = surveys.get(p.id);
            if (s == null) {
                List<int[]> candidates = feud.homeCandidates(p, CANDIDATES);
                CompletableFuture<int[][]> ground = level == null ? null : survey(level, candidates);
                if (ground == null) { // nothing to read the ground with: it stays where it first picked
                    changed |= settle(feud, p, p.homeX, p.homeZ, "it has nothing to read the ground with");
                    continue;
                }
                surveys.put(p.id, new Survey(candidates, ground, gameTime));
                continue;
            }
            if (!s.ground.isDone()) {
                if (gameTime - s.startedAt > TIMEOUT_TICKS) {
                    surveys.remove(p.id);
                    changed |= settle(feud, p, p.homeX, p.homeZ, "its ground couldn't be read in time");
                }
                continue;
            }
            surveys.remove(p.id);
            int[][] ground = s.ground.isCompletedExceptionally() ? null : s.ground.join();
            int[] pick = ground == null ? null : choose(level, feud, p, s.candidates, ground);
            if (pick == null) {
                changed |= settle(feud, p, p.homeX, p.homeZ, ground == null ? "its ground couldn't be read"
                        : "no dry, level ground no one holds was found");
            } else {
                changed |= settle(feud, p, pick[0], pick[1], null);
            }
        }
        return changed;
    }

    void clear() {
        surveys.clear();
    }

    private static boolean settle(Feud feud, Pack p, int x, int z, String why) {
        feud.settle(p, x, z);
        BloodFeud.LOG.info("Blood Feud: {} withdrew {} blocks from where it formed ({}, {}) and made its home at ({}, {}){}",
                p.title(), Math.round(Math.hypot(x - p.settleFromX, z - p.settleFromZ)), p.settleFromX, p.settleFromZ, x, z,
                why == null ? "" : " (" + why + ")");
        return true;
    }

    /** The first candidate with dry, level ground that keeps off other packs' sites and the spawn, or null. */
    static int[] choose(ServerLevel level, Feud feud, Pack p, List<int[]> candidates, int[][] ground) {
        for (int i = 0; i < candidates.size(); i++) {
            int[] c = candidates.get(i);
            if (!dryAndLevel(ground, i * SAMPLES.length)) continue;
            if (SiteFinder.claimedByAnother(feud, p, c[0], c[1], SettlementTier.CAMP.radius)) continue;
            if (level != null && nearSpawn(level, c[0], c[1])) continue;
            return c;
        }
        return null;
    }

    /**
     * Too close to where players arrive for a camp. A spawn the Earth hasn't worked out yet doesn't rule a place out
     * here: the camp's own site search waits for it and keeps clear of it.
     */
    private static boolean nearSpawn(ServerLevel level, int x, int z) {
        if (Earth.is(level) && Earth.arrival(level) == null) return false;
        return SiteFinder.tooCloseToWorldSpawn(level, x, z);
    }

    /** Ground is {@code {top, wet}} per column: {@code wet} is 1 where water stands over it. */
    static boolean dryAndLevel(int[][] ground, int first) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < SAMPLES.length; i++) {
            int[] g = ground[first + i];
            if (g == null || g[1] != 0) return false;
            min = Math.min(min, g[0]);
            max = Math.max(max, g[0]);
        }
        return max - min <= MAX_SLOPE;
    }

    /**
     * Reads the generated ground under every sample of every candidate, off the server thread, as {@code {top, wet}}.
     * On the Earth from its map data; elsewhere from the world generator, so nothing is loaded or generated. Null
     * where it can't be read (a dimension with a ceiling, like the Nether, has no "ground" to read this way).
     */
    static CompletableFuture<int[][]> survey(ServerLevel level, List<int[]> candidates) {
        List<int[]> columns = new ArrayList<>();
        for (int[] c : candidates) for (int[] s : SAMPLES) columns.add(new int[]{c[0] + s[0], c[1] + s[1]});
        if (Earth.is(level)) {
            CompletableFuture<int[][]> heights = Earth.survey(level, columns);
            if (heights == null) return null;
            return heights.thenApply(h -> {
                int[][] out = new int[h.length][];
                for (int i = 0; i < h.length; i++) out[i] = new int[]{h[i][0], h[i][1] >= h[i][0] ? 1 : 0};
                return out;
            });
        }
        if (level.dimensionType().hasCeiling()) return null;
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        return CompletableFuture.supplyAsync(() -> {
            int[][] out = new int[columns.size()][];
            for (int i = 0; i < out.length; i++) {
                int x = columns.get(i)[0], z = columns.get(i)[1];
                int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
                out[i] = new int[]{floor, surface > floor ? 1 : 0};
            }
            return out;
        }, Util.backgroundExecutor());
    }

    /** Test hook: how many surveys are running. */
    int running() {
        return surveys.size();
    }
}
