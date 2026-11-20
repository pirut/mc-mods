package io.github.pirut.chunks3.mixin;

import io.github.pirut.chunks3.CubicLevel;
import io.github.pirut.chunks3.ServerCubeWorld;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla kicks a player who spends four seconds with no blocks around them, falling or not. Earth
 * has drops far longer than four seconds, and after a far teleport the client also holds the
 * player in mid-air until the cubes it stands in and on arrive (see CubicTravelMixin). In a cubic
 * level a player waiting on those cubes, or going down, is not floating.
 */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class CubicFloatingKickMixin {
    @Shadow public ServerPlayer player;
    @Shadow private boolean clientIsFloating;
    @Unique private double chunks3$lastY = Double.NaN;

    @Inject(method = "noBlocksAround", at = @At("HEAD"), cancellable = true)
    private void chunks3$waitingForCubes(Entity entity, CallbackInfoReturnable<Boolean> result) {
        if (!(CubicLevel.of(entity.level()) instanceof ServerCubeWorld cubes)) return;
        int x = entity.getBlockX(), y = entity.getBlockY(), z = entity.getBlockZ();
        if (!cubes.sentTo(player, x, y, z) || !cubes.sentTo(player, x, y - 2, z)) result.setReturnValue(false);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void chunks3$falling(CallbackInfo info) {
        double y = player.getY(), last = chunks3$lastY;
        chunks3$lastY = y;
        if (y < last - 0.05 && CubicLevel.of(player.level()) != null) clientIsFloating = false;
    }
}
