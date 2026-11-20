package io.github.pirut.seaworthy.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

/** Where the real world's water is around a ship. */
final class WaterProbe {
    /** How far up to look for the surface from a point under water. */
    private static final int MAX_DEPTH = 48;

    private final Level level;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    WaterProbe(Level level) {
        this.level = level;
    }

    /**
     * The height of the water surface above the world point (x, y, z), if that point is in water, else NaN. Flowing
     * water counts with its real height, so a ship in a river sits in it as deep as the river is.
     */
    double surfaceAbove(double x, double y, double z) {
        pos.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.is(FluidTags.WATER)) return Double.NaN;
        for (int i = 0; i < MAX_DEPTH; i++) {
            pos.move(0, 1, 0);
            FluidState above = level.getFluidState(pos);
            if (!above.is(FluidTags.WATER)) {
                pos.move(0, -1, 0);
                return pos.getY() + fluid.getHeight(level, pos);
            }
            fluid = above;
        }
        return pos.getY() + 1.0;
    }

    /**
     * How much of the vertical span from {@code bottom} to {@code top} at (x, z) is under water, 0..1. Looks at the
     * bottom first, so a lump in the air costs a single block lookup.
     */
    double submerged(double x, double bottom, double top, double z) {
        int bx = Mth.floor(x), bz = Mth.floor(z);
        pos.set(bx, Mth.floor(bottom), bz);
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.is(FluidTags.WATER)) return 0.0;
        if (top <= bottom) return 1.0;
        // Deep under water, the common case for a big hull: one more lookup.
        pos.set(bx, Mth.floor(top), bz);
        FluidState topFluid = level.getFluidState(pos);
        if (topFluid.is(FluidTags.WATER) && top - pos.getY() <= topFluid.getHeight(level, pos)) return 1.0;
        // The surface is somewhere in between.
        int y0 = Mth.floor(bottom), y1 = Mth.floor(top);
        double surface = y0 + fluid.getHeight(level, pos.set(bx, y0, bz));
        for (int y = y0 + 1; y <= y1; y++) {
            FluidState f = level.getFluidState(pos.set(bx, y, bz));
            if (!f.is(FluidTags.WATER)) break;
            surface = y + f.getHeight(level, pos);
        }
        return Mth.clamp((surface - bottom) / (top - bottom), 0.0, 1.0);
    }
}
