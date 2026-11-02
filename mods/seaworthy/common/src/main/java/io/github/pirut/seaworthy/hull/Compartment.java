package io.github.pirut.seaworthy.hull;

import java.util.List;

/**
 * One watertight space inside a hull: cells that water can move between without leaving the ship.
 *
 * <p>{@link #cells} is in the order water fills them once it gets in through a {@link Breach}: each cell's key is
 * the highest point water has to climb over to reach it from a breach, and cells with the same key fill from the
 * bottom up. So water that comes over a bulkhead fills the next compartment's floor first, and water that pours
 * in over the gunwale falls to the bottom of the hold.
 *
 * @param anchor  the lowest grid index in the compartment; stays the same as long as the hull does
 * @param cells   grid indices in fill order
 * @param groupStart for each run of cells at the same key and height, where it starts in {@link #cells}
 * @param groupY  for each run, the height (grid y) of its cells
 * @param breaches the open faces between this compartment and the water outside; empty if it is sealed
 */
public record Compartment(int anchor, int[] cells, int[] groupStart, int[] groupY, List<Breach> breaches) {

    public boolean sealed() {
        return breaches.isEmpty();
    }

    public int size() {
        return cells.length;
    }

    /**
     * The height of the water surface inside, in grid y (the bottom of a cell at y is y), when {@code volume}
     * cells' worth of water have come in.
     */
    public double surface(double volume) {
        if (cells.length == 0) return 0;
        if (volume <= 0) return groupY[0];
        if (volume >= cells.length) return groupY[groupY.length - 1] + 1.0;
        int g = groupAt((int) volume);
        int start = groupStart[g];
        return groupY[g] + (volume - start) / (groupEnd(g) - start);
    }

    /** Where run {@code g} ends in {@link #cells} (exclusive). */
    public int groupEnd(int g) {
        return g + 1 < groupStart.length ? groupStart[g + 1] : cells.length;
    }

    /** The run that the cell at {@code index} in {@link #cells} belongs to. */
    public int groupAt(int index) {
        int lo = 0, hi = groupStart.length - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (groupStart[mid] <= index) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }
}
