package io.github.pirut.chunks3.mixin;

import io.github.pirut.chunks3.CubicLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pathfinding reads blocks through a snapshot of carrier chunks, which in a cubic level are 16
 * blocks tall and empty, so every mob saw only air and never found a path. In a cubic level the
 * region reads the level itself (its cubes) and spans the Y of the box it was made for, with a
 * margin for falls, instead of the carrier's height.
 */
@Mixin(PathNavigationRegion.class)
abstract class CubicPathRegionMixin {
    /** Blocks below and above the requested box that a search may still read (falls, floating). */
    @Unique private static final int MARGIN = 64;

    @Shadow @Final protected Level level;
    @Unique private boolean chunks3$cubic;
    @Unique private int chunks3$minY, chunks3$height;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void chunks3$cubicSpan(Level level, BlockPos from, BlockPos to, CallbackInfo info) {
        if (CubicLevel.of(level) == null) return;
        chunks3$cubic = true;
        chunks3$minY = Math.min(from.getY(), to.getY()) - MARGIN;
        chunks3$height = Math.max(from.getY(), to.getY()) + MARGIN + 1 - chunks3$minY;
    }

    @Unique private boolean chunks3$outside(BlockPos pos) {
        int y = pos.getY() - chunks3$minY;
        return y < 0 || y >= chunks3$height;
    }

    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void chunks3$cubeState(BlockPos pos, CallbackInfoReturnable<BlockState> result) {
        if (chunks3$cubic) result.setReturnValue(chunks3$outside(pos) ? Blocks.AIR.defaultBlockState() : level.getBlockState(pos));
    }

    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    private void chunks3$cubeFluid(BlockPos pos, CallbackInfoReturnable<FluidState> result) {
        if (chunks3$cubic) result.setReturnValue(chunks3$outside(pos) ? Fluids.EMPTY.defaultFluidState() : level.getFluidState(pos));
    }

    @Inject(method = "getBlockEntity", at = @At("HEAD"), cancellable = true)
    private void chunks3$cubeEntity(BlockPos pos, CallbackInfoReturnable<BlockEntity> result) {
        if (chunks3$cubic) result.setReturnValue(level.getBlockEntity(pos));
    }

    /** Collision checks along a path (doors, slabs, fences) see the cubes too. */
    @Inject(method = "getChunkForCollisions", at = @At("HEAD"), cancellable = true)
    private void chunks3$cubeCollisions(int chunkX, int chunkZ, CallbackInfoReturnable<BlockGetter> result) {
        if (chunks3$cubic) result.setReturnValue(level);
    }

    @Inject(method = "getMinBuildHeight", at = @At("HEAD"), cancellable = true)
    private void chunks3$cubeMin(CallbackInfoReturnable<Integer> result) {
        if (chunks3$cubic) result.setReturnValue(chunks3$minY);
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    private void chunks3$cubeHeight(CallbackInfoReturnable<Integer> result) {
        if (chunks3$cubic) result.setReturnValue(chunks3$height);
    }
}
