package io.github.pirut.chunks3.mixin;

import io.github.pirut.chunks3.client.FarLod;
import io.github.pirut.chunks3.client.FarLodIris;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * For a pack compiled to read Distant Horizons' terrain (see {@link FarLod#lodDefine}): gives it far
 * terrain's projection, depth range and reach where Iris would give Distant Horizons'. Iris asks
 * these whether or not Distant Horizons is installed. Skipped when Iris is not installed.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.compat.dh.DHCompat", remap = false)
abstract class CubicIrisDhMixin {
    @Inject(method = "getProjection", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void chunks3$farProjection(CallbackInfoReturnable<Matrix4f> result) {
        if (FarLod.dhDefined()) result.setReturnValue(FarLodIris.dhProjection());
    }

    @Inject(method = "getFarPlane", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void chunks3$farPlane(CallbackInfoReturnable<Float> result) {
        if (FarLod.dhDefined()) result.setReturnValue(FarLod.dhFarPlane());
    }

    @Inject(method = "getNearPlane", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void chunks3$nearPlane(CallbackInfoReturnable<Float> result) {
        if (FarLod.dhDefined()) result.setReturnValue(FarLod.dhNearPlane());
    }

    @Inject(method = "getRenderDistance", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void chunks3$farReach(CallbackInfoReturnable<Integer> result) {
        if (FarLod.dhDefined()) result.setReturnValue(FarLod.dhRenderDistance());
    }
}
