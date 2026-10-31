package io.github.pirut.chunks3.mixin.compat;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.pirut.chunks3.compat.CubicMaps;
import io.github.pirut.chunks3.compat.JourneyMapCompat;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** JourneyMap asks its cache for each chunk it maps; in the cubic world the answer is a cube snapshot. */
@Pseudo
@Mixin(targets = "journeymap.client.data.DataCache", remap = false)
abstract class JourneyMapDataCacheMixin {
    @Inject(method = "getChunkMD(JLjourneymap/client/data/DataCache$ChunkQueryIntent;)Ljourneymap/client/model/chunk/ChunkMD;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void chunks3$cubicChunk(CallbackInfoReturnable<Object> result, @Local(argsOnly = true) long coord) {
        if (CubicMaps.inCubicWorld()) result.setReturnValue(JourneyMapCompat.chunk(ChunkPos.getX(coord), ChunkPos.getZ(coord)));
    }
}
