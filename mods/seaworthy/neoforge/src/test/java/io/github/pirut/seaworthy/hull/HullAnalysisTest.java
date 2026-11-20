package io.github.pirut.seaworthy.hull;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullAnalysisTest {
    static final int S = HullGrid.SOLID | HullGrid.FACE_MASK;

    /** A box of solid walls from (x0,y0,z0) to (x1,y1,z1) inclusive, optionally without its top. */
    static void box(HullGrid g, int x0, int y0, int z0, int x1, int y1, int z1, boolean top) {
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++) {
                    boolean edge = x == x0 || x == x1 || z == z0 || z == z1 || y == y0 || (top && y == y1);
                    if (edge) g.set(x, y, z, S);
                }
    }

    static void clear(HullGrid g, int x, int y, int z) {
        g.set(x, y, z, 0);
    }

    @Test
    void sealedBoxIsOneSealedCompartment() {
        HullGrid g = new HullGrid(7, 7, 7);
        box(g, 1, 1, 1, 5, 5, 5, true);
        HullAnalysis.Result r = HullAnalysis.analyze(g, 2);
        assertEquals(1, r.compartments().size());
        Compartment c = r.compartments().get(0);
        assertEquals(27, c.size());
        assertTrue(c.sealed());
    }

    @Test
    void openTopHoldIsInsideAndBreachedOnlyAtTheRim() {
        HullGrid g = new HullGrid(7, 7, 7);
        box(g, 1, 1, 1, 5, 5, 5, false);
        HullAnalysis.Result r = HullAnalysis.analyze(g, 2);
        assertEquals(1, r.compartments().size());
        Compartment c = r.compartments().get(0);
        // The hold is 3x3 wide and 4 deep (y 2..5); the top layer is level with the rim.
        assertEquals(36, c.size());
        assertEquals(9, c.breaches().size());
        for (Breach b : c.breaches()) {
            assertEquals(1, b.direction());
            assertEquals(6.0, b.height(g));
        }
        // Water poured over the rim falls to the floor first.
        assertEquals(2, g.y(c.cells()[0]));
        assertEquals(5, g.y(c.cells()[c.size() - 1]));
    }

    @Test
    void aSmallHoleIsABreachNotOpenSea() {
        HullGrid g = new HullGrid(7, 7, 7);
        box(g, 1, 1, 1, 5, 5, 5, true);
        clear(g, 1, 3, 3); // one block missing from the west wall
        HullAnalysis.Result r = HullAnalysis.analyze(g, 2);
        assertTrue(r.outside()[g.index(1, 3, 3)], "the hole itself is open water");
        assertEquals(1, r.compartments().size());
        Compartment c = r.compartments().get(0);
        assertEquals(27, c.size());
        assertEquals(1, c.breaches().size());
        assertEquals(4, c.breaches().get(0).direction());
        assertEquals(3.5, c.breaches().get(0).height(g));
    }

    @Test
    void aTwoWideHoleIsStillABreach() {
        HullGrid g = new HullGrid(9, 7, 7);
        box(g, 1, 1, 1, 7, 5, 5, true);
        clear(g, 3, 2, 1);
        clear(g, 4, 2, 1); // two blocks out of the north wall
        HullAnalysis.Result r = HullAnalysis.analyze(g, 2);
        assertEquals(1, r.compartments().size());
        assertEquals(5 * 3 * 3, r.compartments().get(0).size());
        assertEquals(2, r.compartments().get(0).breaches().size());
    }

    @Test
    void aMissingWallOpensTheHullToTheSea() {
        HullGrid g = new HullGrid(9, 9, 9);
        box(g, 1, 1, 1, 7, 7, 7, true);
        for (int z = 2; z <= 6; z++) for (int y = 2; y <= 6; y++) clear(g, 1, y, z);
        HullAnalysis.Result r = HullAnalysis.analyze(g, 2);
        // Water comes in sideways and fills the whole hold from the gap's bottom (y 2) up.
        assertEquals(0, r.interiorCells());
    }

    @Test
    void cellsBelowAWideGashStayDryUntilTheyFlood() {
        HullGrid g = new HullGrid(9, 9, 9);
        box(g, 1, 1, 1, 7, 7, 7, true);
        for (int z = 2; z <= 6; z++) for (int y = 4; y <= 6; y++) clear(g, 1, y, z); // gash from y 4 up
        HullAnalysis.Result r = HullAnalysis.analyze(g, 2);
        assertEquals(1, r.compartments().size());
        Compartment c = r.compartments().get(0);
        assertEquals(5 * 5 * 2, c.size()); // y 2 and 3 under the gash
        assertFalse(c.sealed());
    }

    @Test
    void theGapBetweenTwoHullsIsSea() {
        for (int gap = 1; gap <= 4; gap++) {
            HullGrid g = new HullGrid(5 + 2 * 3 + gap, 5, 5);
            // Two solid pontoons 3 wide, `gap` apart.
            for (int y = 1; y <= 3; y++)
                for (int z = 1; z <= 3; z++) {
                    for (int x = 1; x <= 3; x++) g.set(x, y, z, S);
                    for (int x = 4 + gap; x <= 6 + gap; x++) g.set(x, y, z, S);
                }
            HullAnalysis.Result r = HullAnalysis.analyze(g, 2);
            assertEquals(0, r.interiorCells(), "gap " + gap);
        }
    }

    @Test
    void waterBesideASlopedHullIsSea() {
        // A hull whose side steps outwards as it rises (like a flared bow): the water under the overhang and
        // beside the lower steps is outside.
        HullGrid g = new HullGrid(11, 7, 5);
        for (int y = 1; y <= 5; y++) {
            int half = Math.min(4, y);
            for (int x = 5 - half; x <= 5 + half; x++) for (int z = 1; z <= 3; z++) g.set(x, y, z, S);
        }
        HullAnalysis.Result r = HullAnalysis.analyze(g, 2);
        assertEquals(0, r.interiorCells());
        assertTrue(r.outside()[g.index(1, 2, 2)]);
    }

    @Test
    void slabsLetWaterThroughTheirOpenSide() {
        HullGrid g = new HullGrid(7, 7, 7);
        box(g, 1, 1, 1, 5, 5, 5, true);
        // Replace one wall block with a bottom slab: its top half is open, so water can come in over it.
        int bottomSlab = 1; // full bottom face only
        g.set(1, 3, 3, bottomSlab);
        HullAnalysis.Result r = HullAnalysis.analyze(g, 2);
        Compartment c = r.compartments().get(0);
        assertFalse(c.sealed());
    }

    @Test
    void waterFillsTheBreachedSideOfABulkheadFirst() {
        // A 7-long, 3-wide hold split by a bulkhead 2 high (the hold is 4 deep). Hole in the west end only.
        HullGrid g = new HullGrid(11, 8, 7);
        box(g, 1, 1, 1, 9, 6, 5, true);
        for (int z = 2; z <= 4; z++) {
            g.set(5, 2, z, S);
            g.set(5, 3, z, S);
        }
        clear(g, 1, 2, 3);
        HullAnalysis.Result r = HullAnalysis.analyze(g, 2);
        assertEquals(1, r.compartments().size());
        Compartment c = r.compartments().get(0);
        // The first cells to fill are all west of the bulkhead, until it is topped.
        int westBelowTop = 3 * 3 * 2; // x 2..4, z 2..4, y 2..3
        for (int i = 0; i < westBelowTop; i++) assertTrue(g.x(c.cells()[i]) < 5, "cell " + i);
        // Then the east side's floor.
        assertTrue(g.x(c.cells()[westBelowTop]) > 5);
        assertEquals(2, g.y(c.cells()[westBelowTop]));
    }

    @Test
    void aHoleInANarrowBoatOnlyFloodsTheBoatOverTime() {
        // A rowboat: 2 wide inside, open on top. Water through a hole must not fill it instantly.
        HullGrid g = new HullGrid(10, 6, 6);
        box(g, 1, 1, 1, 8, 4, 4, false);
        clear(g, 4, 2, 1);
        HullAnalysis.Result r = HullAnalysis.analyze(g, 2);
        assertEquals(1, r.compartments().size());
        Compartment c = r.compartments().get(0);
        assertEquals(6 * 2 * 3, c.size());
        assertTrue(c.breaches().stream().anyMatch(b -> b.direction() == 2), "the hole");
    }

    @Test
    void aTunnelThroughTheShipIsSea() {
        HullGrid g = new HullGrid(9, 9, 9);
        for (int x = 1; x <= 7; x++) for (int y = 1; y <= 7; y++) for (int z = 1; z <= 7; z++) g.set(x, y, z, S);
        for (int x = 1; x <= 7; x++) for (int y = 3; y <= 4; y++) for (int z = 3; z <= 4; z++) clear(g, x, y, z);
        HullAnalysis.Result r = HullAnalysis.analyze(g, 2);
        assertEquals(0, r.interiorCells());
    }
}
