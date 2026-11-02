package io.github.pirut.seaworthy.mixin.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.pirut.seaworthy.client.SodiumOcclusion;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells {@link SodiumOcclusion} when Sodium draws translucent terrain, where the sea's water is. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer", remap = false)
public abstract class SodiumWorldRendererMixin {

    @Inject(method = "drawChunkLayer", at = @At("HEAD"), require = 0)
    private void seaworthy$beginLayer(CallbackInfo ci, @Local(argsOnly = true) RenderType layer) {
        SodiumOcclusion.beginLayer(layer == RenderType.translucent());
    }

    @Inject(method = "drawChunkLayer", at = @At("RETURN"), require = 0)
    private void seaworthy$endLayer(CallbackInfo ci) {
        SodiumOcclusion.endLayer();
    }
}
