package io.github.pirut.chunks3.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.pirut.chunks3.CubeWorld;
import io.github.pirut.chunks3.CubicLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A random teleport, as from chorus fruit, looks down for ground to land on. In the cubic level it
 * looks down {@value #REACH} blocks at most, as far as the overworld's floor usually lies below a
 * player, rather than to the carrier's floor at Y 0 or through thousands of blocks of rock.
 */
@Mixin(LivingEntity.class)
abstract class CubicTeleportMixin {
    private static final int REACH = 384;

    @WrapOperation(method = "randomTeleport", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getMinBuildHeight()I"))
    private int chunks3$cubicBottom(Level level, Operation<Integer> original, @Local(argsOnly = true, ordinal = 1) double y) {
        return CubicLevel.of(level) != null ? Math.max(CubeWorld.MIN_Y, (int) Math.floor(y) - REACH) : original.call(level);
    }
}
