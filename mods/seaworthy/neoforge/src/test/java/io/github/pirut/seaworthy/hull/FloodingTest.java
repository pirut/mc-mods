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
        assertEquals(9 * 2, Flooding.wetCells(c, v), 1); // floor at y 2, so y 2 and 3 are under water
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
        Lumps l = Lumps.build(r, new boolean[g.volume()], 2);
        float air = 0;
        for (int i = 0; i < l.count; i++) air += l.air[i];
        assertEquals(27f, air, 1e-4);
    }
}
