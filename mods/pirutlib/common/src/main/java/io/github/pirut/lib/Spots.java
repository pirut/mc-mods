package io.github.pirut.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

/** Finding places in the world where a mob (or a player) can stand. Only looks at loaded chunks. */
public final class Spots {
    private Spots() {
    }

    /**
     * Whether a two-block-tall mob can stand at {@code p}: something sturdy below, room for its body.
     *
     * @param wet allow standing in shallow water (never lava)
     */
    public static boolean standable(ServerLevel level, BlockPos p, boolean wet) {
        BlockPos below = p.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                || !level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                || !level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) return false;
        if (wet) return !level.getFluidState(p).is(FluidTags.LAVA) && level.getFluidState(p.above(2)).isEmpty();
        return level.getFluidState(p).isEmpty() && level.getFluidState(p.above()).isEmpty();
    }

    /**
     * A spot between {@code min} and {@code max} blocks (sideways) from {@code near}: on the surface, or near
     * {@code near}'s height when that is underground. Dry spots first, then shallow water. Null if none is found.
     */
    public static BlockPos near(ServerLevel level, BlockPos near, int min, int max, RandomSource random) {
        BlockPos dry = search(level, near, min, max, random, false);
        return dry != null ? dry : search(level, near, min, max, random, true);
    }

    private static BlockPos search(ServerLevel level, BlockPos near, int min, int max, RandomSource random, boolean wet) {
        for (int i = 0; i < 24; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = min + random.nextDouble() * Math.max(1, max - min);
            int x = near.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = near.getZ() + (int) Math.round(Math.sin(angle) * dist);
            if (!level.isLoaded(new BlockPos(x, near.getY(), z))) continue;
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            int startY = near.getY() < top - 8 ? near.getY() : top;
            for (int dy = 0; dy <= 10; dy++) {
                for (int sign : new int[]{1, -1}) {
                    BlockPos p = new BlockPos(x, startY + dy * sign, z);
                    if (standable(level, p, wet)) return p;
                }
            }
        }
        return null;
    }
}
