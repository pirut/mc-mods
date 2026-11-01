package io.github.pirut.seaworthy.hull;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FloodingTest {
    static final double G = 9.81, CD = 0.6, TICK = 0.05;

    static double[] outside(Compartment c, double level) {
        double[] out = new double[c.breaches().size()];
        Arrays.fill(out, level);
        return out;
    }

    @Test
    void aHoleFloodsUpToTheWaterlineAndStops() {
        HullGrid g = new HullGrid(7, 9, 7);
        HullAnalysisTest.box(g, 1, 1, 1, 5, 7, 5, true);
        HullAnalysisTest.clear(g, 3, 1, 3); // hole in the floor
        Compartment c = HullAnalysis.analyze(g, 2).compartments().get(0);
        double v = 0;
        double waterline = 4.0; // outside water surface, grid y
        double first = Flooding.step(g, c, 0, outside(c, waterline), G, CD, TICK);
        assertTrue(first > 0.1 && first < 1, "a 1x1 hole 2 deep lets in a fraction of a block per tick: " + first);
        for (int t = 0; t < 20 * 60 * 5; t++) v = Flooding.step(g, c, v, outside(c, waterline), G, CD, TICK);
        assertEquals(waterline, c.surface(v), 0.05);
        // Floor at y 2, so y 2 and 3 are under water: y 2 is full and y 3 is the live layer at the top.
        Flooding.Surface top = Flooding.surface(g, c, v);
        assertEquals(9, top.live().length);
        assertEquals(1.0, top.liveHeight(c, v), 0.05);
    }

    @Test
    void anOpenHoldOnlyFloodsWhenTheWaterTopsTheRim() {
        HullGrid g = new HullGrid(7, 9, 7);
        HullAnalysisTest.box(g, 1, 1, 1, 5, 6, 5, false);
        Compartment c = HullAnalysis.analyze(g, 2).compartments().get(0);
        assertEquals(0.0, Flooding.step(g, c, 0, outside(c, 6.5), G, CD, TICK)); // rim top is at 7
        assertTrue(Flooding.step(g, c, 0, outside(c, 7.2), G, CD, TICK) > 0);
    }

    @Test
    void aShipLiftedOutOfTheWaterDrains() {
        HullGrid g = new HullGrid(7, 9, 7);
        HullAnalysisTest.box(g, 1, 1, 1, 5, 7, 5, true);
        HullAnalysisTest.clear(g, 3, 1, 3);
        Compartment c = HullAnalysis.analyze(g, 2).compartments().get(0);
        double v = 20;
        for (int t = 0; t < 20 * 60; t++) v = Flooding.step(g, c, v, outside(c, Double.NaN), G, CD, TICK);
        assertEquals(0.0, v, 1e-9);
    }

    @Test
    void waterShowsALayerAtATime() {
        HullGrid g = new HullGrid(9, 9, 9);
        HullAnalysisTest.box(g, 1, 1, 1, 7, 7, 7, true);
        HullAnalysisTest.clear(g, 4, 1, 4);
        Compartment c = HullAnalysis.analyze(g, 2).compartments().get(0);
        float[] wet = new float[g.volume()];
        for (double v = 0; v <= c.size(); v += 0.37) {
            int full = Flooding.wetCells(c, v);
            assertTrue(Arrays.binarySearch(c.groupStart(), full) >= 0 || full == c.size(),
                    "only whole layers count as full: " + full + " at " + v);
            assertTrue(full <= v + 1e-9 && v - full < maxGroup(c), "the full layers are the water, less the live layer");
            Arrays.fill(wet, 0);
            Flooding.wetness(c, v, wet);
            double sum = 0;
            for (int i : c.cells()) sum += wet[i];
            assertEquals(v, sum, 1e-3, "buoyancy sees exactly the water that came in");
        }
        // The 5x5 floor is one layer: two blocks of water make the whole floor a thin, even layer.
        int floor = -1;
        for (int k = 0; k < c.groupStart().length; k++) if (c.groupEnd(k) - c.groupStart()[k] == 25) { floor = k; break; }
        assertTrue(floor >= 0);
        double v = c.groupStart()[floor] + 2;
        assertEquals(c.groupStart()[floor], Flooding.wetCells(c, v));
        Arrays.fill(wet, 0);
        Flooding.wetness(c, v, wet);
        for (int w = c.groupStart()[floor]; w < c.groupEnd(floor); w++) assertEquals(2f / 25, wet[c.cells()[w]], 1e-6);
        Flooding.Surface top = Flooding.surface(g, c, v);
        assertEquals(25, top.live().length, "the surface covers the whole floor");
        assertEquals(2.0 / 25, top.liveHeight(c, v), 1e-9);
        // Once the floor layer is full and the next has begun, the floor's top is under water, not the surface.
        double v2 = c.groupEnd(floor) + 5;
        Flooding.Surface top2 = Flooding.surface(g, c, v2);
        for (int i : top2.fixed()) assertTrue(g.y(i) != g.y(c.cells()[c.groupStart()[floor]]), "no surface under water");
        assertEquals(25, top2.live().length);
    }

    static int maxGroup(Compartment c) {
        int m = 0;
        for (int k = 0; k < c.groupStart().length; k++) m = Math.max(m, c.groupEnd(k) - c.groupStart()[k]);
        return m;
    }

    @Test
    void sealedCompartmentsNeverFlood() {
        HullGrid g = new HullGrid(7, 7, 7);
        HullAnalysisTest.box(g, 1, 1, 1, 5, 5, 5, true);
        Compartment c = HullAnalysis.analyze(g, 2).compartments().get(0);
        assertEquals(0.0, Flooding.step(g, c, 0, new double[0], G, CD, TICK));
    }

    @Test
    void lumpsKeepGiantHullsCheap() {
        assertEquals(1, Lumps.sizeFor(1000, 4096));
        assertEquals(4, Lumps.sizeFor(200 * 40 * 30, 4096));
        HullGrid g = new HullGrid(7, 7, 7);
        HullAnalysisTest.box(g, 1, 1, 1, 5, 5, 5, true);
        HullAnalysis.Result r = HullAnalysis.analyze(g, 2);
        Lumps l = Lumps.build(r, new float[g.volume()], 2);
        float air = 0;
        for (int i = 0; i < l.count; i++) air += l.air[i];
        assertEquals(27f, air, 1e-4);
    }

    @Test
    void waterLiesFlatInTheWorldWhenTheShipLeans() {
        HullGrid g = new HullGrid(13, 9, 9);
        HullAnalysisTest.box(g, 1, 1, 1, 11, 7, 7, true);
        Compartment c = HullAnalysis.analyze(g, 2).compartments().get(0);
        double v = c.size() * 0.3;
        float[] wet = new float[g.volume()];
        // Leaning 15 degrees: the world's up tips towards +x in the ship's frame, so -x is the low side.
        double ux = Math.sin(Math.toRadians(15)), uy = Math.cos(Math.toRadians(15));
        Flooding.wetness(g, c, v, ux, uy, 0, wet);
        double sum = 0, low = 0, high = 0;
        for (int i : c.cells()) {
            sum += wet[i];
            if (g.x(i) <= 3) low += wet[i];
            if (g.x(i) >= 9) high += wet[i];
        }
        assertEquals(v, sum, 0.01, "the same water, wherever it lies");
        assertTrue(low > high * 1.5, "more on the low side: " + low + " vs " + high);

        float[] level = new float[g.volume()];
        float[] flat = new float[g.volume()];
        Flooding.wetness(c, v, level);
        Flooding.wetness(g, c, v, 0, 1, 0, flat);
        for (int i : c.cells()) assertEquals(level[i], flat[i], 0.01, "upright, it's just the layers");
    }

    @Test
    void aPumpEmptiesWaterAndStopsAtNothing() {
        assertEquals(90, Flooding.pump(100, 20, 0.5), 1e-9);
        assertEquals(0, Flooding.pump(5, 20, 0.5), 1e-9);
        assertEquals(100, Flooding.pump(100, 0, 0.5), 1e-9);
    }
}
