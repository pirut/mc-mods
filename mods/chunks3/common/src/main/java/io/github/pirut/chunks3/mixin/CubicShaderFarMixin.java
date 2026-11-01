package io.github.pirut.chunks3.mixin;

import io.github.pirut.chunks3.client.ClientCubeWorld;
import io.github.pirut.chunks3.client.FarLod;
import io.github.pirut.chunks3.client.FarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shader packs fade terrain out at the {@code far} uniform, which Iris takes from the render
 * distance. In the cubic world terrain carries on to the far terrain's fog, so packs get that
 * reach instead, unless the pack fades far terrain by its Voxy reach (see {@link FarLod}), when
 * {@code far} stays the near terrain's. Skipped when Iris is not installed.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CameraUniforms", remap = false)
abstract class CubicShaderFarMixin {
    @Inject(method = "getRenderDistanceInBlocks", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void chunks3$farTerrainReach(CallbackInfoReturnable<Integer> result) {
        if (ClientCubeWorld.current() != null && !FarLod.replacesFar()) result.setReturnValue(Math.max(result.getReturnValueI(), (int) FarRenderer.FOG_END));
    }
}
