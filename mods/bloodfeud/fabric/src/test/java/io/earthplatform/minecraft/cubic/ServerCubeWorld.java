package io.earthplatform.minecraft.cubic;

import io.earthplatform.minecraft.cubic.gen.CubeGenerator;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

public final class ServerCubeWorld extends CubeWorld {
    private final CubeGenerator generator;
    public final Set<BlockPos> requested = new HashSet<>();

    public ServerCubeWorld(CubeGenerator generator) {
        this.generator = generator;
    }

    public CubeGenerator generator() {
        return generator;
    }

    /** Only queues, as the real one does: the test decides when the workers deliver. */
    public boolean readyAround(BlockPos pos) {
        boolean ready = true;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos cube = new BlockPos((pos.getX() >> 4) + dx, (pos.getY() >> 4) + dy, (pos.getZ() >> 4) + dz);
                    if (loaded.containsKey(cube)) continue;
                    ready = false;
                    requested.add(cube);
                }
            }
        }
        return ready;
    }

    public void deliver() {
        for (BlockPos cube : requested) loaded.put(cube, new Cube());
        requested.clear();
    }
}
