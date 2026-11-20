package io.github.pirut.seaworthy.mixin.sodium;

import io.github.pirut.seaworthy.client.SodiumOcclusion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sodium has bound a terrain shader: set up the occlusion test in it. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.shader.DefaultShaderInterface", remap = false)
public abstract class SodiumShaderInterfaceMixin {

    @Inject(method = "setupState", at = @At("TAIL"), require = 0)
    private void seaworthy$setupState(CallbackInfo ci) {
        SodiumOcclusion.setupShader();
    }
}
