package io.github.pirut.seaworthy.mixin.iris;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.pirut.seaworthy.client.SodiumOcclusion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

/** Adds the occlusion test to the terrain programs Iris builds from a shader pack for Sodium. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.transform.TransformPatcher", remap = false)
public abstract class IrisTransformPatcherMixin {

    @ModifyReturnValue(method = "patchSodium", at = @At("RETURN"), require = 0)
    private static <K> Map<K, String> seaworthy$patchTerrainShaders(Map<K, String> sources) {
        return SodiumOcclusion.patchIris(sources);
    }
}
