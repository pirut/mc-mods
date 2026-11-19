package io.github.pirut.chunks3.client;

import io.github.pirut.chunks3.far.Far;

/**
 * How far terrain looks from high above the ground: it changes what is drawn, never what the
 * server sends or what is saved.
 */
final class FarAtmosphere {
    /** Haze over the ground far below, and textures fading to their average, with {@code -Dchunks3.far.atmosphere=true}. */
    static final boolean ENABLED = Boolean.getBoolean("chunks3.far.atmosphere");
    /** The first level smooth ground can reach: the levels that show a player's edits keep their blocks. */
    static final int FIRST_SMOOTH = Far.EDIT_LEVELS + 1;
    /**
     * Whether smoothing reaches down to {@link #FIRST_SMOOTH} while cruising high above the ground:
     * only with {@code far.smooth} on and {@code far.smoothFrom} past it, and not with
     * {@code -Dchunks3.far.cruiseSmooth=false}.
     */
    static boolean cruises() {
        int from = FarMesher.SMOOTH_FROM;
        return CRUISE_SMOOTH && from > FIRST_SMOOTH && from <= Far.MAX_LEVEL;
    }

    private static final boolean CRUISE_SMOOTH = !"false".equals(System.getProperty("chunks3.far.cruiseSmooth"));

    private FarAtmosphere() {}

    /** The first level drawn smooth: {@link #FIRST_SMOOTH} while cruising, else the one set. */
    static int smoothFrom(boolean cruise) { return cruise ? FIRST_SMOOTH : FarMesher.SMOOTH_FROM; }

    /** Whether a camera this high over the ground cruises: above 2,200 blocks, and until it comes back under 1,600. */
    static boolean cruise(double height, boolean cruising) { return cruise(height, cruising, false); }

    /** The same, and never under a shader pack, whose own look the cruise's smoothing would change. */
    static boolean cruise(double height, boolean cruising, boolean pack) {
        return !pack && Double.isFinite(height) && height > (cruising ? 1600 : 2200);
    }

    /** How much haze and texture fading a camera this high over the ground sees: none up to 2,000 blocks, all of it from 6,000. */
    static float blend(double height) {
        if (!Double.isFinite(height)) return 0;
        double t = Math.clamp((height - 2000) / 4000, 0, 1);
        return (float) (t * t * (3 - 2 * t));
    }
}
