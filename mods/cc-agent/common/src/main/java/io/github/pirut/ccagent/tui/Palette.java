package io.github.pirut.ccagent.tui;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * CC's 16 palette slots, given out to the colors a TUI actually draws with, so Claude Code's orange
 * stays orange instead of rounding to the nearest ANSI color. Once all 16 are taken, slots no cell
 * uses any more are given to new colors, and only then do colors round to the nearest slot.
 */
final class Palette {
    /** Colors closer than this (redmean distance squared) share a slot. */
    private static final long CLOSE = 600;

    private final int[] rgb = new int[16];
    private final boolean[] taken = new boolean[16];
    private final Map<Integer, Integer> slots = new HashMap<>();
    private final Supplier<Set<Integer>> inUse;

    Palette(Supplier<Set<Integer>> inUse) {
        this.inUse = inUse;
        // The default background and text colors are always there.
        assign(0, Vt.DEFAULT_BG);
        assign(1, Vt.DEFAULT_FG);
    }

    private void assign(int slot, int color) {
        rgb[slot] = Glyphs.rgb(color);
        taken[slot] = true;
        slots.put(color, slot);
    }

    int slot(int color, Map<Integer, Integer> changes) {
        var known = slots.get(color);
        if (known != null) return known;

        int want = Glyphs.rgb(color);
        int nearest = 0;
        long nearestDist = Long.MAX_VALUE;
        for (int i = 0; i < 16; i++) {
            if (!taken[i]) continue;
            long d = Glyphs.distance(want, rgb[i]);
            if (d < nearestDist) {
                nearestDist = d;
                nearest = i;
            }
        }
        if (nearestDist <= CLOSE) {
            slots.put(color, nearest);
            return nearest;
        }

        int free = -1;
        for (int i = 0; i < 16 && free < 0; i++) if (!taken[i]) free = i;
        if (free < 0) free = reclaim();
        if (free < 0) return nearest;
        assign(free, color);
        changes.put(free, rgb[free]);
        return free;
    }

    /** A slot that no cell on screen uses, freed for a new color, or -1. */
    private int reclaim() {
        var used = new boolean[16];
        used[0] = used[1] = true;
        for (int color : inUse.get()) {
            var slot = slots.get(color);
            if (slot != null) used[slot] = true;
        }
        for (int i = 2; i < 16; i++) {
            if (!used[i]) {
                int slot = i;
                slots.values().removeIf(s -> s == slot);
                taken[i] = false;
                return i;
            }
        }
        return -1;
    }

    Map<Integer, Integer> all() {
        var out = new HashMap<Integer, Integer>();
        for (int i = 0; i < 16; i++) if (taken[i]) out.put(i, rgb[i]);
        return out;
    }
}
