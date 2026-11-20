package io.github.pirut.seaworthy.mixin.iris;

import io.github.pirut.seaworthy.client.SodiumOcclusion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** With a shader pack, Iris sets up its own terrain program in Sodium's place: set up the occlusion test in it. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.programs.SodiumShader", remap = false)
public abstract class IrisSodiumShaderMixin {

    @Inject(method = "setupState", at = @At("TAIL"), require = 0)
    private void seaworthy$setupState(CallbackInfo ci) {
        SodiumOcclusion.setupShader();
    }
}
