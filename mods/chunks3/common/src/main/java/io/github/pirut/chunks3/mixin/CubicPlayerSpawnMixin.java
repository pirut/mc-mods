package io.github.pirut.chunks3.mixin;

import io.github.pirut.chunks3.CubicEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where a player starts about the world spawn in a cubic level (joining for the first time, or respawning
 * with no bed): on the cubic ground within the spawn radius, as vanilla scatters players, rather than what
 * vanilla's search of the empty carrier chunks finds, which is the spawn block itself even when rock or a
 * build stands there.
 */
@Mixin(ServerPlayer.class)
abstract class CubicPlayerSpawnMixin {
    @Inject(method = "adjustSpawnLocation", at = @At("HEAD"), cancellable = true)
    private void chunks3$standOnCubes(ServerLevel level, BlockPos spawn, CallbackInfoReturnable<BlockPos> info) {
        BlockPos spot = CubicEvents.spawnNear(level, spawn);
        if (spot != null) info.setReturnValue(spot);
    }
}
