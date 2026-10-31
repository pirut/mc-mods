package io.github.pirut.seaworthy.sable;

import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import io.github.pirut.seaworthy.Seaworthy;
import io.github.pirut.seaworthy.hull.HullGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.concurrent.ConcurrentHashMap;

/**
 * What a block means to water: whether it fills its cell, which of its faces are watertight, and how much of its
 * cell is still air. Uses the block's collision shape, which is what vanilla water uses to decide whether it can
 * flow past a block, so a slab or a stair keeps water out on its full sides and lets it in on its open ones.
 */
public final class BlockFlags {
    /** Blocks that keep water out as if they were full blocks, such as glass panes used as portholes. */
    public static final TagKey<Block> WATERTIGHT = TagKey.create(Registries.BLOCK, Seaworthy.id("watertight"));
    /** Blocks water passes straight through, whatever their shape. */
    public static final TagKey<Block> LEAKY = TagKey.create(Registries.BLOCK, Seaworthy.id("leaky"));

    private static final ConcurrentHashMap<BlockState, Integer> CACHE = new ConcurrentHashMap<>();

    private BlockFlags() {
    }

    /** Flags in the low byte (see {@link HullGrid}), air fraction 0..255 in the next byte. */
    public static int of(BlockState state) {
        Integer cached = CACHE.get(state);
        if (cached != null) return cached;
        int packed = compute(state);
        CACHE.put(state, packed);
        return packed;
    }

    public static int flags(int packed) {
        return packed & 0xFF;
    }

    public static float air(int packed) {
        return ((packed >>> 8) & 0xFF) / 255.0f;
    }

    /** Forget everything, for when tags or block properties are reloaded. */
    public static void clear() {
        CACHE.clear();
    }

    private static int compute(BlockState state) {
        if (state.isAir()) return pack(0, 1.0f);
        if (state.getBlock() instanceof LiquidBlock) return pack(HullGrid.FLUID, 0.0f); // the ship's own water
        int fluid = state.getFluidState().isEmpty() ? 0 : HullGrid.FLUID;
        if (state.is(LEAKY)) return pack(fluid, fluid != 0 ? 0.0f : 1.0f);
        if (state.is(WATERTIGHT)) return pack(HullGrid.SOLID | HullGrid.FACE_MASK, 0.0f);

        VoxelShape shape;
        try {
            shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (RuntimeException e) {
            return pack(HullGrid.SOLID | HullGrid.FACE_MASK, 0.0f); // a block that needs a real world: assume it's solid
        }
        if (Block.isShapeFullBlock(shape)) return pack(HullGrid.SOLID | HullGrid.FACE_MASK, 0.0f);

        int faces = 0;
        for (Direction d : Direction.values()) {
            if (Block.isFaceFull(shape, d)) faces |= 1 << d.ordinal();
        }
        float air;
        if (fluid != 0) {
            air = 0.0f; // waterlogged: the rest of the cell is water
        } else if (shape.isEmpty()) {
            air = 1.0f;
        } else {
            // Sable already floats the block itself by its volume, so only the rest of the cell is ours.
            double volume;
            try {
                volume = PhysicsBlockPropertyHelper.getVolume(state);
            } catch (RuntimeException e) {
                volume = 1.0;
            }
            air = (float) Math.max(0.0, Math.min(1.0, 1.0 - volume));
        }
        return pack(faces | fluid, air);
    }

    private static int pack(int flags, float air) {
        return (flags & 0xFF) | (Math.round(Math.max(0, Math.min(1, air)) * 255) << 8);
    }
}
