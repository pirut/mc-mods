package io.github.pirut.chunks3.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.pirut.chunks3.CubeWorld;
import io.github.pirut.chunks3.CubicLevel;
import net.minecraft.client.multiplayer.ClientLevel;
//#if MC >= 1.21.11
//$$ import net.minecraft.client.renderer.fog.FogRenderer;
//#else
import net.minecraft.client.renderer.FogRenderer;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla fades fog to black as the camera nears the bottom of the world. The cubic world's
 * carrier bottom is Y 0, so every ocean deeper than about 30 m went black under water; the fade
 * now starts at the cubic world's true bottom instead, and the deep sea darkens with depth in
 * {@link io.github.pirut.chunks3.client.CubicClient#waterFogLight}. Applied after
 * {@link EarthFogColourMixin}, whose floor 1,000 m under the sea still blacked out the trenches.
 */
@Mixin(value = FogRenderer.class, priority = 500)
abstract class CubicVoidFogMixin {
    //#if MC >= 1.21.11
    //$$ @ModifyExpressionValue(method = "computeFogColor", require = 0,
    //$$         at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getMinBuildHeight()I"))
    //$$ private int chunks3$cubicBottom(
    //#else
    @ModifyExpressionValue(method = "setupColor", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getMinBuildHeight()I"))
    private static int chunks3$cubicBottom(
    //#endif
int floor, @Local(argsOnly = true) ClientLevel level) {
        return CubicLevel.of(level) != null ? CubeWorld.MIN_Y : floor;
    }
}
