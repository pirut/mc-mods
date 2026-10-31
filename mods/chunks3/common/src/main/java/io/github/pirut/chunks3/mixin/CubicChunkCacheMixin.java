package io.github.pirut.chunks3.mixin;

import io.github.pirut.chunks3.CubeWorld;
import io.github.pirut.chunks3.CubicLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Block broadcasts in a cubic level go to cube watchers; ChunkHolder can only index carrier sections. */
@Mixin(ServerChunkCache.class)
abstract class CubicChunkCacheMixin {
    @Shadow @Final ServerLevel level;

    @Inject(method = "blockChanged(Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), cancellable = true)
    private void chunks3$cubeChanged(BlockPos pos, CallbackInfo info) {
        CubeWorld cubes = CubicLevel.at(level, pos.getX(), pos.getZ());
        if (cubes == null) return;
        cubes.blockChanged(pos);
        info.cancel();
    }
}
