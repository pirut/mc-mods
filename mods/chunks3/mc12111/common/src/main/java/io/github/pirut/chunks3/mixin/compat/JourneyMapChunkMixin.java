package io.github.pirut.chunks3.mixin.compat;

import io.github.pirut.chunks3.compat.CubicMaps;
import io.github.pirut.chunks3.compat.JourneyMapCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** JourneyMap clamps its scans at the world's bottom; a cube snapshot's bottom is its lowest copied cube. */
@Pseudo
@Mixin(targets = "journeymap.client.model.chunk.ChunkMD", remap = false)
abstract class JourneyMapChunkMixin {
    @Inject(method = "getMinY", at = @At("HEAD"), cancellable = true, require = 0)
    private void chunks3$snapshotBottom(CallbackInfoReturnable<Integer> result) {
        if (!CubicMaps.inCubicWorld()) return;
        Integer bottom = JourneyMapCompat.minY(this);
        if (bottom != null) result.setReturnValue(bottom);
    }
}
