package io.github.pirut.seaworthy.hull;

import java.util.List;

/**
 * Water coming in through breaches, and going back out.
 *
 * <p>Each breach is a one-square-metre hole. Water flows through it at Torricelli's speed, {@code sqrt(2 g h)},
 * times a discharge coefficient, where {@code h} is the difference between the water level outside and inside
 * (measured from the hole when the water on one side is below it). So a hole deep under the waterline floods fast,
 * a hole just under it trickles, and the flow slows down as the compartment fills up to the level outside.
 */
public final class Flooding {
    private static final int SUB_STEPS = 4;

    private Flooding() {
    }

    /**
     * The compartment's water volume, in blocks, after {@code seconds}.
     *
     * @param outside for each of the compartment's breaches (same order), the height of the water surface outside it
     *                in grid y, or {@link Double#NaN} if there is no water there
     * @param gravity  m/s^2
     * @param discharge the discharge coefficient (about 0.6 for a sharp-edged hole), times the server's flood speed
     */
    public static double step(HullGrid grid, Compartment c, double volume, double[] outside, double gravity,
                              double discharge, double seconds) {
        List<Breach> breaches = c.breaches();
        if (breaches.isEmpty() || discharge <= 0) return volume;
        double dt = seconds / SUB_STEPS;
        for (int s = 0; s < SUB_STEPS; s++) {
            double inside = c.surface(volume);
            double change = 0;
            for (int i = 0; i < breaches.size(); i++) {
                double hole = breaches.get(i).height(grid);
                double out = Double.isNaN(outside[i]) ? Double.NEGATIVE_INFINITY : outside[i];
                if (out > inside && out > hole) {
                    change += discharge * Math.sqrt(2 * gravity * (out - Math.max(hole, inside))) * dt;
                } else if (inside > out && inside > hole) {
                    change -= discharge * Math.sqrt(2 * gravity * (inside - Math.max(hole, out))) * dt;
                }
            }
            volume = Math.max(0, Math.min(c.size(), volume + change));
        }
        return volume;
    }

    /**
     * How many of the compartment's cells are completely under water at this volume: the layers that are full (cells
     * fill in {@link Compartment#cells} order, a layer at a time). The layer the surface is in is drawn by
     * {@link Surface}, at its real height.
     */
    public static int wetCells(Compartment c, double volume) {
        int size = c.size();
        if (volume <= 0 || size == 0) return 0;
        if (volume >= size) return size;
        int g = c.groupAt((int) volume);
        int start = c.groupStart()[g];
        return volume >= c.groupEnd(g) ? c.groupEnd(g) : start;
    }

    /**
     * Where the top of the water in a compartment is, for drawing it. Minecraft can't show the water inside a hull:
     * the only water there is the sea, whose surface is at sea level. So the surface is drawn on its own.
     *
     * @param fixed cells of full layers with nothing wet above them: their top is the surface
     * @param live  cells of the layer the surface is in, whose height follows the volume ({@link #liveHeight})
     * @param group the run of {@link Compartment#cells} that {@code live} is, or -1
     */
    public record Surface(int[] fixed, int[] live, int group) {
        public static final Surface NONE = new Surface(new int[0], new int[0], -1);

        /** How far up its cells the live layer's water is, 0 to 1. */
        public double liveHeight(Compartment c, double volume) {
            if (group < 0) return 0;
            int start = c.groupStart()[group], end = c.groupEnd(group);
            return Math.max(0, Math.min(1, (volume - start) / (end - start)));
        }
    }

    /** The surface at this volume. Changes only when a layer fills or empties ({@link #wetCells} changes). */
    public static Surface surface(HullGrid g, Compartment c, double volume) {
        int full = wetCells(c, volume);
        if (volume <= 0 || c.size() == 0) return Surface.NONE;
        int[] cells = c.cells();
        int liveGroup = full < cells.length ? c.groupAt(full) : -1;
        int liveEnd = liveGroup >= 0 ? c.groupEnd(liveGroup) : full;
        java.util.BitSet wet = new java.util.BitSet(g.volume());
        for (int w = 0; w < liveEnd; w++) wet.set(cells[w]);
        int[] fixed = new int[full];
        int n = 0;
        for (int w = 0; w < full; w++) {
            int i = cells[w];
            int y = g.y(i) + 1;
            if (y >= g.sy || !wet.get(g.index(g.x(i), y, g.z(i)))) fixed[n++] = i;
        }
        int[] live = liveGroup >= 0 ? java.util.Arrays.copyOfRange(cells, full, liveEnd) : new int[0];
        return new Surface(java.util.Arrays.copyOf(fixed, n), live, liveGroup);
    }

    /**
     * How much of each cell is under water, from 0 to 1, at this volume: full layers are 1, the layer the surface is
     * in is evenly part full, and the rest are 0. Buoyancy uses this, so a filling layer weighs the same all across.
     *
     * @param out indexed by grid cell; only the compartment's own cells are written
     */
    public static void wetness(Compartment c, double volume, float[] out) {
        int[] cells = c.cells();
        int[] groupStart = c.groupStart();
        for (int g = 0; g < groupStart.length; g++) {
            int start = groupStart[g], end = c.groupEnd(g);
            float f = (float) Math.max(0, Math.min(1, (volume - start) / (end - start)));
            for (int w = start; w < end; w++) out[cells[w]] = f;
        }
    }

    /**
     * Like {@link #wetness(Compartment, double, float[])} for a ship that isn't level: the water lies flat in the world
     * rather than in the ship, so more of it is on the low side. The plane is found so the volume stays the same.
     *
     * @param ux the world's up in the ship's frame, x (also {@code uy}, {@code uz}); a ship near or past its side just
     *           gets the level answer, since the water has no simple shape then
     */
    public static void wetness(HullGrid g, Compartment c, double volume, double ux, double uy, double uz, float[] out) {
        int[] cells = c.cells();
        if (uy < 0.25 || volume <= 0 || volume >= cells.length) {
            wetness(c, volume, out);
            return;
        }
        double sx = -ux / uy, sz = -uz / uy;
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        double[] base = new double[cells.length];
        for (int w = 0; w < cells.length; w++) {
            int i = cells[w];
            base[w] = g.y(i) - sx * (g.x(i) + 0.5) - sz * (g.z(i) + 0.5);
            lo = Math.min(lo, base[w]);
            hi = Math.max(hi, base[w] + 1);
        }
        for (int step = 0; step < 24; step++) {
            double h = (lo + hi) / 2, sum = 0;
            for (double b : base) sum += Math.max(0, Math.min(1, h - b));
            if (sum < volume) lo = h;
            else hi = h;
        }
        double h = (lo + hi) / 2;
        for (int w = 0; w < cells.length; w++) out[cells[w]] = (float) Math.max(0, Math.min(1, h - base[w]));
    }

    /** The compartment's water volume after a pump has moved {@code rate} blocks a second out of it for {@code seconds}. */
    public static double pump(double volume, double rate, double seconds) {
        return Math.max(0, volume - rate * seconds);
    }
}
