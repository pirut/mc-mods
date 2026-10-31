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
     * How many of the compartment's cells show as water at this volume (they fill in {@link Compartment#cells} order).
     *
     * <p>Minecraft water is a whole block or nothing, so the water is shown a layer at a time: a layer appears all
     * at once when it is half full, instead of block by block across the floor. That keeps the surface level.
     */
    public static int wetCells(Compartment c, double volume) {
        int size = c.size();
        if (volume <= 0 || size == 0) return 0;
        if (volume >= size) return size;
        int g = c.groupAt((int) volume);
        int start = c.groupStart()[g], end = c.groupEnd(g);
        return (volume - start) * 2 >= end - start ? end : start;
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
}
