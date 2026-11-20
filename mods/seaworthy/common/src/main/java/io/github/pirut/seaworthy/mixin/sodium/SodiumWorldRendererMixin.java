package io.github.pirut.seaworthy.mixin.sodium;

import io.github.pirut.seaworthy.client.SodiumOcclusion;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells {@link SodiumOcclusion} when Sodium draws translucent terrain, where the sea's water is. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer", remap = false)
public abstract class SodiumWorldRendererMixin {

    @Inject(method = "drawChunkLayer", at = @At("HEAD"), require = 0)
    private void seaworthy$beginLayer(RenderType layer, @Coerce Object matrices, double x, double y, double z, CallbackInfo ci) {
        // The matrices are Sodium's ChunkRenderMatrices: the camera's view, which terrain is drawn with.
        SodiumOcclusion.beginLayer(layer == RenderType.translucent(), matrices);
    }

    @Inject(method = "drawChunkLayer", at = @At("RETURN"), require = 0)
    private void seaworthy$endLayer(CallbackInfo ci) {
        SodiumOcclusion.endLayer();
    }
}
