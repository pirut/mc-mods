package io.github.pirut.seaworthy.sable;

import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import io.github.pirut.seaworthy.Seaworthy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import org.joml.Vector3d;

/**
 * Tells the sea to fill the hole a new ship left in it.
 *
 * <p>When a ship is put together on the water, its blocks leave the world, and the hole they leave in the sea can stay
 * dry: water only fills a gap when something nudges it. A ship floating over a dry hole has nothing to displace, and
 * sits blocks too low. So when a ship is first seen, every bit of sea beside an air gap under it is told to flow, and
 * the hole fills as it would if you dug it out by hand. It only happens once for each ship, so a dry dock the ship
 * later comes back to is left alone. A big ship is done over several ticks.
 */
final class SeaWake {
    /** Positions looked at per tick. */
    private static final int BUDGET = 100_000;
    private static final Direction[] SIDES = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final int x0, y0, z0, x1, y1, z1;
    private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
    private int x, z;
    private int woken;

    private SeaWake(int x0, int y0, int z0, int x1, int y1, int z1) {
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x = x0;
        this.z = z0;
    }

    /** For the world box around the ship where it is now. */
    static SeaWake under(SubLevel subLevel) {
        BoundingBox3ic b = subLevel.getPlot().getBoundingBox();
        Pose3dc pose = subLevel.logicalPose();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        Vector3d p = new Vector3d();
        for (int c = 0; c < 8; c++) {
            p.set((c & 1) == 0 ? b.minX() : b.maxX() + 1, (c & 2) == 0 ? b.minY() : b.maxY() + 1, (c & 4) == 0 ? b.minZ() : b.maxZ() + 1);
            p = pose.transformPosition(p);
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
            maxZ = Math.max(maxZ, p.z);
        }
        // Not clamped to the level's build height: some dimensions (Earth Platform's) report -64..320 and reach far beyond.
        return new SeaWake((int) Math.floor(minX) - 1, (int) Math.floor(minY) - 1, (int) Math.floor(minZ) - 1,
                (int) Math.ceil(maxX) + 1, (int) Math.ceil(maxY) + 1, (int) Math.ceil(maxZ) + 1);
    }

    /** Carries on for a tick. True when it's done. */
    boolean run(ServerLevel server) {
        int budget = BUDGET;
        while (x <= x1) {
            if (budget <= 0) return false;
            if (server.hasChunk(x >> 4, z >> 4)) {
                for (int y = y0; y <= y1; y++) wake(server, y);
                budget -= y1 - y0 + 1;
            }
            budget--;
            if (++z > z1) {
                z = z0;
                x++;
            }
        }
        Seaworthy.LOG.debug("Told {} blocks of sea to flow in x {}..{} y {}..{} z {}..{}", woken, x0, x1, y0, y1, z0, z1);
        return true;
    }

    private void wake(ServerLevel server, int y) {
        at.set(x, y, z);
        FluidState fluid = server.getFluidState(at);
        if (!fluid.is(FluidTags.WATER)) return;
        for (Direction side : SIDES) {
            if (server.getBlockState(next.setWithOffset(at, side)).isAir()) {
                server.scheduleTick(at.immutable(), fluid.getType(), fluid.getType().getTickDelay(server));
                woken++;
                return;
            }
        }
    }
}
