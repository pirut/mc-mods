package io.github.pirut.chunks3.mixin;

import io.github.pirut.chunks3.client.FarLod;
import io.github.pirut.chunks3.client.FarLodIris;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * For a pack compiled to read Distant Horizons' terrain (see {@link FarLod#lodDefine}): gives it far
 * terrain's depth where Iris would give Distant Horizons' ({@code dhDepthTex0}, {@code dhDepthTex1}).
 * Skipped when Iris is not installed.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.compat.dh.DHCompat", remap = false)
abstract class CubicIrisDhDepthMixin {
    @Inject(method = "getDepthTex", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void chunks3$farDepth(CallbackInfoReturnable<Integer> result) {
        if (FarLod.dhDefined()) result.setReturnValue(FarLodIris.dhDepthTexture());
    }

    // Far water is drawn with the pack's own water, so the depth without translucents is the same one.
    @Inject(method = "getDepthTexNoTranslucent", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void chunks3$farDepthOpaque(CallbackInfoReturnable<Integer> result) {
        if (FarLod.dhDefined()) result.setReturnValue(FarLodIris.dhDepthTexture());
    }
}
