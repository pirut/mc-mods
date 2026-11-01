package io.github.pirut.chunks3.mixin;

import io.github.pirut.chunks3.client.PhotonCompat;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Hands Iris an adapted copy of an unmodified Photon pack on OpenGL drivers it doesn't fit, such as
 * Apple's; see {@link PhotonCompat}. Skipped when Iris is not installed.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.Iris", remap = false)
abstract class CubicIrisPhotonCompatMixin {
    @ModifyArg(method = "loadExternalShaderpack", at = @At(value = "INVOKE",
            target = "Lnet/irisshaders/iris/shaderpack/ShaderPack;<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;Z)V"),
            index = 0, require = 0, remap = false)
    private static Path chunks3$adaptPhoton(Path root) {
        return PhotonCompat.adapt(root);
    }
}
