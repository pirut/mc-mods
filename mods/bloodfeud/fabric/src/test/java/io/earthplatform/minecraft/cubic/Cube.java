package io.earthplatform.minecraft.cubic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public final class Cube {
    private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();

    public Map<BlockPos, BlockEntity> blockEntities() {
        return blockEntities;
    }
}
