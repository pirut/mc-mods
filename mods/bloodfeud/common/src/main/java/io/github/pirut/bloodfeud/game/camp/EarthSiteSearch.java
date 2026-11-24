package io.github.pirut.bloodfeud.game.camp;

import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.game.Earth;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * An automatic site search on the Earth, where the ground's height is only known from map data or loaded cubes.
 * It reads the generated heights of every spiral candidate off the server thread, then loads the cubes of one
 * plausible candidate at a time and judges that with the real blocks, as everywhere else.
 */
final class EarthSiteSearch {
    /** {@code done} without a site means there is none to be had for now. */
    record Step(SiteFinder.Site site, boolean done) {
    }

    private static final Step WAIT = new Step(null, false);
    private static final Step NOTHING = new Step(null, true);

    private final int radius;
    private final int facing;
    private final int start;
    private final int homeX;
    private final int homeZ;
    private final List<int[]> samples;
    private final CompletableFuture<int[][]> heights;
    private int tried;

    EarthSiteSearch(ServerLevel level, Pack pack, int radius) {
        this.radius = radius;
        this.facing = Math.floorMod(pack.id.hashCode(), 4);
        this.start = Math.floorMod(pack.siteSearchOffset, SiteFinder.SPIRAL_OFFSETS.size());
        this.homeX = pack.homeX;
        this.homeZ = pack.homeZ;
        this.samples = new ArrayList<>();
        samples.add(new int[]{0, 0});
        int coreR = Math.min(radius, SiteFinder.CORE_RADIUS);
        samples.addAll(List.of(SiteFinder.coreSamples(coreR)));
        if (radius > coreR) samples.addAll(List.of(SiteFinder.outerSamples(radius)));
        List<int[]> columns = new ArrayList<>();
        for (int[] off : SiteFinder.SPIRAL_OFFSETS) {
            for (int[] s : samples) columns.add(new int[]{homeX + off[0] + s[0], homeZ + off[1] + s[1]});
        }
        this.heights = Earth.survey(level, columns);
    }

    /** One tick's work. */
    Step step(ServerLevel level, Pack pack) {
        return step(level, pack, null);
    }

    /** One tick's work, keeping off sites other packs of {@code feud} hold. */
    Step step(ServerLevel level, Pack pack, io.github.pirut.bloodfeud.feud.Feud feud) {
        if (heights == null) return NOTHING;
        if (!heights.isDone()) return WAIT;
        if (heights.isCompletedExceptionally()) return NOTHING;
        if (Earth.arrival(level) == null) return WAIT; // can't keep clear of a spawn we don't know yet
        int[][] ground = heights.join();
        int total = SiteFinder.SPIRAL_OFFSETS.size();
        for (; tried < total; tried++) {
            int index = (start + tried) % total;
            int[] span = plausibleSpan(ground, index * samples.size());
            if (span == null) continue;
            int cx = homeX + SiteFinder.SPIRAL_OFFSETS.get(index)[0];
            int cz = homeZ + SiteFinder.SPIRAL_OFFSETS.get(index)[1];
            if (SiteFinder.claimedByAnother(feud, pack, cx, cz, radius)) continue;
            if (!SiteFinder.isFootprintLoaded(level, cx, cz, radius)) return WAIT;
            int reach = radius + SiteFinder.SITE_SIDE_MARGIN;
            if (!Earth.ready(level, cx - reach, span[0] - SiteFinder.SITE_DOWN, cz - reach,
                    cx + reach, span[1] + 1 + SiteFinder.SITE_UP, cz + reach)) {
                return WAIT;
            }
            SiteFinder.Site site = SiteFinder.evaluateAutoCandidate(level, cx, cz, radius, facing);
            if (site != null) {
                pack.siteSearchOffset = 0;
                return new Step(site, true);
            }
            tried++;
            pack.siteSearchOffset = (index + 1) % total;
            return WAIT; // one real evaluation a tick
        }
        pack.siteSearchOffset = 0;
        return NOTHING;
    }

    /** The lowest and highest generated ground of a candidate, or null if it is wet or too steep to bother loading. */
    private int[] plausibleSpan(int[][] ground, int first) {
        int centre = ground[first][0];
        int min = centre, max = centre, coreMin = centre, coreMax = centre;
        int core = 1 + SiteFinder.coreSamples(0).length;
        for (int i = 0; i < samples.size(); i++) {
            int y = ground[first + i][0];
            if (ground[first + i][1] >= y) return null;
            if (i < core) {
                coreMin = Math.min(coreMin, y);
                coreMax = Math.max(coreMax, y);
                if (coreMax - coreMin > SiteFinder.MAX_CLIFF_SPREAD) return null;
            } else if (Math.abs(y - centre) > CampBuilder.MAX_FOUNDATION_DEPTH - 2) {
                return null;
            }
            min = Math.min(min, y);
            max = Math.max(max, y);
        }
        return new int[]{min, max};
    }
}
