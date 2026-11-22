package io.earthplatform.minecraft.cubic;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public abstract class CubeWorld {
    public static final int MIN_Y = -16384, MAX_Y = 16383;

    public final Map<BlockPos, Cube> loaded = new HashMap<>();

    static BlockPos key(int x, int y, int z) {
        return new BlockPos(x >> 4, y >> 4, z >> 4);
    }

    public Cube load(int x, int y, int z) {
        return loaded.computeIfAbsent(key(x, y, z), k -> new Cube());
    }

    public boolean isLoaded(BlockPos pos) {
        return cubeAtBlock(pos.getX(), pos.getY(), pos.getZ()) != null;
    }

    public Cube cubeAtBlock(int x, int y, int z) {
        return loaded.get(key(x, y, z));
    }

    public int[] columnTops(int cubeX, int cubeZ) {
        for (BlockPos cube : loaded.keySet()) {
            if (cube.getX() == cubeX && cube.getZ() == cubeZ) return new int[256];
        }
        return null;
    }
}
