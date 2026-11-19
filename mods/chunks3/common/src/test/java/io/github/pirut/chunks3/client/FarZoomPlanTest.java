package io.github.pirut.chunks3.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.pirut.chunks3.far.Far;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What a zoomed view asks for (see {@link FarZoom#plan}): open, flat ground at Y 64 with every column held, the
 * camera 36 blocks over it looking at a spot straight ahead, on a 16:9 screen with the game's 70 degree view; the
 * default far detail (11) and sharpness (Fast), and a render distance of 12 chunks. Flat open ground seen low is the
 * most a view can hold: all of it out to the horizon lies within a degree of the spot.
 */
class FarZoomPlanTest {
    private static final double SPREAD = Math.tan(Math.toRadians(35)) * Math.hypot(16 / 9.0, 1), WIDE = 16 / 9.0;
    private static final double EYE = 36;

    private record Flat(double voxel) implements FarZoom.Box {
        @Override public double lowY() { return 64; }
        @Override public double highY() { return 65; }
    }

    private record Planned(FarZoom view, LongArrayList asked, LongOpenHashSet split) {}

    @BeforeEach void blocks() {
        FarZoom.finest = Far.FINEST_LEVEL;
        FarZoom.nearBlocks = 12 * 16;
    }

    @AfterEach void reset() { FarZoom.finest = Far.MIN_LEVEL; }

    private static Planned plan(double zoom, double distance) {
        float pitch = (float) Math.toDegrees(Math.atan(EYE / distance));
        FarZoom view = FarZoom.at(zoom, SPREAD, WIDE, false, 0, 0, 64 + EYE, 0, EYE, 0, pitch, null);
        LongOpenHashSet split = new LongOpenHashSet();
        int outer = Far.outer(), hole = Far.hole();
        LongArrayList asked = view.plan(null, 0, pitch, 0, 0, outer, (level, x, z) -> new Flat(Far.voxel(level)),
                key -> Far.kept(key, 0, 0, Far.MIN_LEVEL, hole, outer), FarZoom.MAX_COLUMNS, split);
        return new Planned(view, asked, split);
    }

    /** The level the zoomed view draws the ground at the spot with. */
    private static int reached(Planned planned, double distance) {
        for (int level = Far.MAX_LEVEL; level > Far.FINEST_LEVEL; level--) {
            int x = Far.column(0, level), z = Far.column((int) distance, level);
            if (!Far.refined(level, x, z, 0, 0) && !planned.split.contains(Far.key(level, x, z))) return level;
        }
        return Far.FINEST_LEVEL;
    }

    @Test void theSpotLookedAtComesDownToItsBlocks() {
        StringBuilder table = new StringBuilder();
        for (double zoom : new double[] {4, 10, 30, 80}) for (double distance : new double[] {1000, 8000, 50000}) {
            Planned planned = plan(zoom, distance);
            int level = reached(planned, distance);
            // How many pixels one of its voxels spans through the zoom on a 1080p screen.
            double pixels = Far.voxel(level) / distance * zoom * 540 / Math.tan(Math.toRadians(35));
            // How far down the requests the spot's own column comes, and how many of each level are asked before it.
            long spot = Far.key(level, Far.column(0, level), Far.column((int) distance, level));
            int at = planned.asked.indexOf(spot);
            int[] before = new int[Far.MAX_LEVEL + 1];
            for (int i = 0; i < Math.max(0, at); i++) before[Far.level(planned.asked.getLong(i))]++;
            table.append(String.format(Locale.ROOT, "ZOOM_REACH %3.0fx at %5.0f m: level %d, %3d-block voxels (%.1f px), %d columns; the spot's is request %d, after %s%n",
                    zoom, distance, level, Far.voxel(level), pixels, planned.asked.size(), at, java.util.Arrays.toString(before)));
        }
        System.out.print(table);
        // A spyglass (10 times): single blocks 1 km off, two-block voxels 8 km off, where a block is a pixel.
        assertEquals(Far.FINEST_LEVEL, reached(plan(10, 1000), 1000), table::toString);
        assertTrue(reached(plan(10, 8000), 8000) <= 1, table::toString);
        assertTrue(reached(plan(10, 50000), 50000) <= 4, table::toString);
        // A zoom mod at 80 times: single blocks 8 km off.
        assertEquals(Far.FINEST_LEVEL, reached(plan(80, 8000), 8000), table::toString);
    }

    @Test void itStaysWithinItsBudgetAndAsksForEachColumnAfterTheOneItSplits() {
        for (double zoom : new double[] {10, 80}) {
            Planned planned = plan(zoom, 8000);
            assertTrue(planned.asked.size() <= FarZoom.MAX_COLUMNS);
            LongOpenHashSet before = new LongOpenHashSet();
            int outer = Far.outer(), hole = Far.hole();
            for (long key : planned.asked) {
                long parent = Far.key(Far.level(key) + 1, Far.x(key) >> 1, Far.z(key) >> 1);
                assertTrue(planned.split.contains(parent), "asked only under a column the view splits");
                assertTrue(Far.kept(parent, 0, 0, Far.MIN_LEVEL, hole, outer) || before.contains(parent), "a column comes after the one it splits");
                before.add(key);
            }
        }
    }

    @Test void blockColumnsOnlyPastTheGamesOwnBlocks() {
        Planned planned = plan(10, 1000);
        for (long key : planned.asked) {
            if (Far.level(key) != Far.FINEST_LEVEL) continue;
            double cx = (Far.x(key) + 0.5) * Far.width(0), cz = (Far.z(key) + 0.5) * Far.width(0);
            assertTrue(Math.hypot(cx, cz) > FarZoom.nearBlocks, "no block-sized far column under the game's own blocks");
        }
    }

    @Test void withoutBlockColumnsItStopsAtLevelOne() {
        FarZoom.finest = Far.MIN_LEVEL;
        Planned planned = plan(10, 1000);
        for (long key : planned.asked) assertTrue(Far.level(key) >= Far.MIN_LEVEL);
        assertEquals(Far.MIN_LEVEL, reached(planned, 1000));
    }
}
