package io.github.pirut.chunks3.mixin.compat;

import io.github.pirut.chunks3.CubeWorld;
import io.github.pirut.chunks3.CubicLevel;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Immersive Engineering reads blocks through SafeChunkUtils, which goes straight to the loaded chunk.
 * In the cubic level that is an empty 16-block carrier, so a formed multiblock saw air where its own
 * blocks were and crashed the server on its first tick (the coke oven reading its "active" property).
 * These reads go to the cubes instead.
 */
@Pseudo
@Mixin(targets = "blusunrize.immersiveengineering.api.utils.SafeChunkUtils", remap = false)
abstract class ImmersiveEngineeringChunkMixin {
    @Nullable
    private static CubeWorld chunks3$cubes(LevelAccessor world) {
        return world instanceof Level level ? CubicLevel.of(level) : null;
    }

    @Inject(method = "isChunkSafe", at = @At("HEAD"), cancellable = true, require = 0)
    private static void chunks3$safe(LevelAccessor world, BlockPos pos, CallbackInfoReturnable<Boolean> result) {
        CubeWorld cubes = chunks3$cubes(world);
        if (cubes != null) result.setReturnValue(cubes.isLoaded(pos));
    }

    @Inject(method = "getSafeBE", at = @At("HEAD"), cancellable = true, require = 0)
    private static void chunks3$blockEntity(LevelAccessor world, BlockPos pos, CallbackInfoReturnable<BlockEntity> result) {
        CubeWorld cubes = chunks3$cubes(world);
        if (cubes != null) result.setReturnValue(cubes.isLoaded(pos) ? world.getBlockEntity(pos) : null);
    }

    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true, require = 0)
    private static void chunks3$state(LevelAccessor world, BlockPos pos, CallbackInfoReturnable<BlockState> result) {
        CubeWorld cubes = chunks3$cubes(world);
        if (cubes != null) result.setReturnValue(cubes.getBlockState(pos));
    }
}
