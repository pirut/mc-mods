package io.github.pirut.chunks3.mixin;

import io.github.pirut.chunks3.CubicLevel;
import io.github.pirut.chunks3.ServerCubeWorld;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A bed far from any player sits in an unloaded cube; load it so the respawn check can see it. */
@Mixin(ServerPlayer.class)
abstract class CubicRespawnMixin {
    @Inject(method = "findRespawnAndUseSpawnBlock", at = @At("HEAD"))
    private static void chunks3$loadBed(ServerLevel level, BlockPos pos, float angle, boolean forced, boolean keepInventory,
                                              CallbackInfoReturnable<Optional<?>> info) {
        if (CubicLevel.of(level) instanceof ServerCubeWorld cubes) cubes.loadAround(pos);
    }
}
