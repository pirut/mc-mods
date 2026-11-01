package io.github.pirut.chunks3.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.pirut.chunks3.CubeWorld;
import io.github.pirut.chunks3.CubicLevel;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A mob below the carrier floor (Y 0) can still path in a cubic level; the cubes go far deeper. */
@Mixin(PathNavigation.class)
abstract class CubicNavigationMixin {
    @WrapOperation(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getMinBuildHeight()I"))
    private int chunks3$cubicFloor(Level level, Operation<Integer> original) {
        return CubicLevel.of(level) != null ? CubeWorld.MIN_Y : original.call(level);
    }
}
