package io.github.pirut.chunks3.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.pirut.chunks3.CubeWorld;
import io.github.pirut.chunks3.CubicLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Mining and using items are allowed at any cubic height, not just below the carrier's top. */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class CubicBuildHeightMixin {
    @WrapOperation(method = {"handlePlayerAction", "handleUseItemOn"},
    //#if MC >= 1.21.2
    //$$         // A server player's level() is a ServerLevel from 1.21.2 (it was serverLevel() before), so the call names it.
    //$$         at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getMaxBuildHeight()I"))
    //$$ private int chunks3$cubicTop(net.minecraft.server.level.ServerLevel level, Operation<Integer> original) {
    //#else
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getMaxBuildHeight()I"))
    private int chunks3$cubicTop(Level level, Operation<Integer> original) {
    //#endif
        return CubicLevel.of(level) != null ? CubeWorld.VANILLA_TOP : original.call(level);
    }
}
