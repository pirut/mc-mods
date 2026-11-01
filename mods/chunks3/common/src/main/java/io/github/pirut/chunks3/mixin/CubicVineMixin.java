package io.github.pirut.chunks3.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.pirut.chunks3.CubeWorld;
import io.github.pirut.chunks3.CubicLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.VineBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Vines spread up and down at any cubic height. */
@Mixin(VineBlock.class)
abstract class CubicVineMixin {
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getMinBuildHeight()I"))
    private int chunks3$cubicBottom(ServerLevel level, Operation<Integer> original) {
        return CubicLevel.of(level) != null ? CubeWorld.MIN_Y : original.call(level);
    }

    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getMaxBuildHeight()I"))
    private int chunks3$cubicTop(ServerLevel level, Operation<Integer> original) {
        return CubicLevel.of(level) != null ? CubeWorld.MAX_Y + 1 : original.call(level);
    }
}
