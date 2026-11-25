package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.game.camp.CampBlueprint;
import io.github.pirut.bloodfeud.game.camp.SiteFinder;
import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;

/**
 * Picks the ground a pack that just formed makes its home on. It withdrew from the fight on paper (see
 * {@link Feud#settleAway}); this reads the generated ground at each of its {@link Feud#homeCandidates} off the server
 * thread, without loading anything, and takes the first that is dry, level, real ground (not void), inside the world
 * border, and clear of every other pack's held ground. If none is, or the ground can't be read (the Nether, a
 * survey that fails or takes too long, the dimension gone), it settles where it formed, as before 0.1.0-beta.3.
 * A pick is never committed unchecked. The camp's own site search then works at the home with the usual rules.
 */
final class Settler {
    /** Places tried round where the pack formed. */
    static final int CANDIDATES = 12;
    /** How far from a candidate's middle its ground is sampled. */
    static final int SPREAD = 10;
    /** Most height difference across a candidate's samples. */
    static final int MAX_SLOPE = 8;
    /** A pack whose ground can't be read in this long settles where it formed. */
    static final long TIMEOUT_TICKS = 1200;
    /**
     * The ground a home holds until its camp has a site: everything its site search could reserve, a Stronghold's
     * reservation round any spiral candidate. Two homes whose holds overlap could end up with overlapping camps.
     */
    static final int HOLD = SiteFinder.HOME_HOLD;

    /** A survey's ground value: {@code {top, kind}}, kind 0 for dry ground. */
    static final int DRY = 0, WET = 1, VOID = 2;

    private record Survey(List<int[]> candidates, CompletableFuture<int[][]> ground, AtomicBoolean cancelled, long startedAt) {
        void cancel() {
            cancelled.set(true);
            ground.cancel(false);
        }
    }

    private final Map<UUID, Survey> surveys = new HashMap<>();

    /** Sample offsets round a candidate's middle, the middle first. */
    static final int[][] SAMPLES = {{0, 0}, {SPREAD, 0}, {-SPREAD, 0}, {0, SPREAD}, {0, -SPREAD},
            {SPREAD, SPREAD}, {-SPREAD, SPREAD}, {SPREAD, -SPREAD}, {-SPREAD, -SPREAD}};

    /** One second's work. Returns whether a pack settled. */
    boolean tick(Function<String, ServerLevel> levels, Feud feud, long gameTime) {
        for (Iterator<Map.Entry<UUID, Survey>> it = surveys.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Survey> e = it.next();
            Pack p = feud.pack(e.getKey());
            if (p == null || !p.settling) { // it died, or settled some other way: its survey is no use
                e.getValue().cancel();
                it.remove();
            }
        }
        boolean changed = false;
        for (Pack p : new ArrayList<>(feud.packs.values())) {
            if (!p.settling) continue;
            ServerLevel level = levels.apply(p.dimension);
            Survey s = surveys.get(p.id);
            if (s == null) {
                List<int[]> candidates = feud.homeCandidates(p, CANDIDATES);
                AtomicBoolean cancelled = new AtomicBoolean();
                CompletableFuture<int[][]> ground = level == null ? null : survey(level, candidates, cancelled);
                if (ground == null) {
                    changed |= fallBack(feud, p, level == null ? "its dimension is gone" : "this dimension's ground can't be read");
                    continue;
                }
                surveys.put(p.id, new Survey(candidates, ground, cancelled, gameTime));
                continue;
            }
            if (!s.ground.isDone()) {
                if (gameTime - s.startedAt > TIMEOUT_TICKS) {
                    s.cancel();
                    surveys.remove(p.id);
                    changed |= fallBack(feud, p, "its ground couldn't be read in time");
                }
                continue;
            }
            surveys.remove(p.id);
            int[][] ground = s.ground.isCompletedExceptionally() || s.ground.isCancelled() ? null : s.ground.join();
            if (ground == null || level == null) {
                changed |= fallBack(feud, p, "its ground couldn't be read");
                continue;
            }
            Choice c = choose(feud, p, s.candidates, ground, inside(level), (x, z) -> nearSpawn(level, x, z) ? 1 : 0);
            if (c.pick() == null) changed |= fallBack(feud, p, c.why());
            else changed |= settle(feud, p, c.pick()[0], c.pick()[1], null);
        }
        return changed;
    }

    void clear() {
        for (Survey s : surveys.values()) s.cancel();
        surveys.clear();
    }

    /** No checked home to be had: it settles where it formed, as packs always did. */
    static boolean fallBack(Feud feud, Pack p, String why) {
        return settle(feud, p, p.settleFromX, p.settleFromZ, why + "; it settles where it formed");
    }

    private static boolean settle(Feud feud, Pack p, int x, int z, String why) {
        feud.settle(p, x, z);
        BloodFeud.LOG.info("Blood Feud: {} withdrew {} blocks from where it formed ({}, {}) and made its home at ({}, {}){}",
                p.title(), Math.round(Math.hypot(x - p.settleFromX, z - p.settleFromZ)), p.settleFromX, p.settleFromZ, x, z,
                why == null ? "" : " (" + why + ")");
        return true;
    }

    /** Whether a home's whole hold lies inside the level's world border. */
    static IntBinaryOperator inside(ServerLevel level) {
        return (x, z) -> {
            var border = level.getWorldBorder();
            return border.isWithinBounds(x - HOLD, z - HOLD) && border.isWithinBounds(x + HOLD, z + HOLD) ? 1 : 0;
        };
    }

    /**
     * The first candidate with dry, level, real ground, inside the border ({@code inside} gives 1), not near spawn
     * ({@code nearSpawn} gives 0), and holding no ground another pack holds; or null.
     */
    static Choice choose(Feud feud, Pack p, List<int[]> candidates, int[][] ground, IntBinaryOperator inside,
                         IntBinaryOperator nearSpawn) {
        int wet = 0, voids = 0, steep = 0, border = 0, spawn = 0, held = 0;
        for (int i = 0; i < candidates.size(); i++) {
            int[] c = candidates.get(i);
            int first = i * SAMPLES.length;
            if (!dryAndLevel(ground, first)) {
                if (anyKind(ground, first, VOID)) voids++;
                else if (anyKind(ground, first, WET)) wet++;
                else steep++;
                continue;
            }
            if (inside.applyAsInt(c[0], c[1]) == 0) {
                border++;
                continue;
            }
            if (nearSpawn.applyAsInt(c[0], c[1]) != 0) {
                spawn++;
                continue;
            }
            if (heldByAnother(feud, p, c[0], c[1])) {
                held++;
                continue;
            }
            return new Choice(c, null);
        }
        List<String> why = new ArrayList<>();
        if (border > 0) why.add(border + " outside the world border");
        if (wet > 0) why.add(wet + " under water");
        if (voids > 0) why.add(voids + " over the void");
        if (steep > 0) why.add(steep + " too steep");
        if (spawn > 0) why.add(spawn + " too near spawn");
        if (held > 0) why.add(held + " on ground another pack holds");
        return new Choice(null, "none of its " + candidates.size() + " places would do: " + String.join(", ", why));
    }

    /** The home picked, or why none was. */
    record Choice(int[] pick, String why) {
    }

    private static boolean anyKind(int[][] ground, int first, int kind) {
        for (int i = 0; i < SAMPLES.length; i++) if (ground[first + i] != null && ground[first + i][1] == kind) return true;
        return false;
    }

    /**
     * Whether a home at {@code (x, z)} would hold ground another pack holds. A home holds {@link #HOLD} round it, from
     * the moment it is picked (while its pack is still settling, too) until its camp has a site; a camp with a site
     * holds its reservation (see {@link SiteFinder#heldByOthers}).
     */
    static boolean heldByAnother(Feud feud, Pack p, int x, int z) {
        return SiteFinder.overlapsAny(SiteFinder.heldByOthers(feud, p), x - HOLD, z - HOLD, x + HOLD, z + HOLD);
    }

    /**
     * Too close to where players arrive for a camp. A spawn the Earth hasn't worked out yet doesn't rule a place out
     * here: the camp's own site search waits for it and keeps clear of it.
     */
    private static boolean nearSpawn(ServerLevel level, int x, int z) {
        if (Earth.is(level) && Earth.arrival(level) == null) return false;
        return SiteFinder.tooCloseToWorldSpawn(level, x, z);
    }

    /** Ground is {@code {top, kind}} per column. A candidate is usable when every sample is dry and they're level. */
    static boolean dryAndLevel(int[][] ground, int first) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < SAMPLES.length; i++) {
            int[] g = ground[first + i];
            if (g == null || g[1] != DRY) return false;
            min = Math.min(min, g[0]);
            max = Math.max(max, g[0]);
        }
        return max - min <= MAX_SLOPE;
    }

    /**
     * Reads the generated ground under every sample of every candidate, off the server thread, as {@code {top, kind}}.
     * On the Earth from its map data; elsewhere from the world generator, so nothing is loaded or generated. A
     * column with nothing in it (both heights at the world's floor) is void. Null where it can't be read: a
     * dimension with a ceiling, like the Nether, has no "ground" to read this way. Stops early once cancelled.
     */
    static CompletableFuture<int[][]> survey(ServerLevel level, List<int[]> candidates, AtomicBoolean cancelled) {
        List<int[]> columns = new ArrayList<>();
        for (int[] c : candidates) for (int[] s : SAMPLES) columns.add(new int[]{c[0] + s[0], c[1] + s[1]});
        if (Earth.is(level)) {
            CompletableFuture<int[][]> heights = Earth.survey(level, columns, cancelled::get);
            if (heights == null) return null;
            int floor = Earth.bottom(level);
            return heights.thenApply(h -> {
                int[][] out = new int[h.length][];
                for (int i = 0; i < h.length; i++) out[i] = new int[]{h[i][0], kind(h[i][0], h[i][1], floor)};
                return out;
            });
        }
        if (level.dimensionType().hasCeiling()) return null;
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        int floor = level.getMinBuildHeight();
        return CompletableFuture.supplyAsync(() -> {
            int[][] out = new int[columns.size()][];
            for (int i = 0; i < out.length; i++) {
                if (cancelled.get()) throw new java.util.concurrent.CancellationException();
                int x = columns.get(i)[0], z = columns.get(i)[1];
                int ground = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
                out[i] = new int[]{ground, ground <= floor && surface <= floor ? VOID : surface > ground ? WET : DRY};
            }
            return out;
        }, Util.backgroundExecutor());
    }

    /** On the Earth: water at or over the ground is wet, ground at the world's floor is void. */
    static int kind(int ground, int water, int floor) {
        if (ground <= floor) return VOID;
        return water >= ground ? WET : DRY;
    }

    /** Test hook: how many surveys are running. */
    int running() {
        return surveys.size();
    }
}
