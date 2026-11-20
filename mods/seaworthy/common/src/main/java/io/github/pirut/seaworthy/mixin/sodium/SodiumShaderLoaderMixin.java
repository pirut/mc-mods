package io.github.pirut.seaworthy.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.pirut.seaworthy.client.SodiumOcclusion;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Adds the occlusion test to Sodium's terrain fragment shader as it is read. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader", remap = false)
public abstract class SodiumShaderLoaderMixin {

    @ModifyReturnValue(method = "getShaderSource", at = @At("RETURN"), require = 0)
    private static String seaworthy$patchTerrainShader(String source, @Local(argsOnly = true) ResourceLocation name) {
        if (!"sodium".equals(name.getNamespace()) || !"blocks/block_layer_opaque.fsh".equals(name.getPath())) return source;
        return SodiumOcclusion.patchShader(source);
    }
}
