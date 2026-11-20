package io.github.pirut.chunks3.mixin.compat;

import io.github.pirut.chunks3.compat.ChunkyCompat;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chunky in the Earth world. Its chunks there would be the empty carriers; instead each chunk it
 * asks for has its Earth cubes generated and saved. See {@link ChunkyCompat}.
 */
@Pseudo
@Mixin(targets = "org.popcraft.chunky.platform.NeoForgeWorld", remap = false)
abstract class ChunkyWorldMixin {
    @Shadow @Final private ServerLevel world;

    @Inject(method = "getChunkAtAsync", at = @At("HEAD"), cancellable = true, require = 0)
    private void chunks3$generate(int x, int z, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        CompletableFuture<Void> earth = ChunkyCompat.generate(world, x, z);
        if (earth != null) cir.setReturnValue(earth);
    }

    @Inject(method = "isChunkGenerated", at = @At("HEAD"), cancellable = true, require = 0)
    private void chunks3$generated(int x, int z, CallbackInfoReturnable<CompletableFuture<Boolean>> cir) {
        CompletableFuture<Boolean> earth = ChunkyCompat.generated(world);
        if (earth != null) cir.setReturnValue(earth);
    }
}
