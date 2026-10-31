package io.github.pirut.chunks3.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.pirut.chunks3.CubeWorld;
import io.github.pirut.chunks3.CubicLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A sapling grows into a tree at any cubic height; the tree found no room outside the 16-block carrier. */
@Mixin(TreeFeature.class)
abstract class CubicTreeGrowthMixin {
    @WrapOperation(method = "doPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;getMinBuildHeight()I"))
    private int chunks3$cubicBottom(WorldGenLevel level, Operation<Integer> original) {
        return level instanceof Level live && CubicLevel.of(live) != null ? CubeWorld.MIN_Y : original.call(level);
    }

    @WrapOperation(method = "doPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;getMaxBuildHeight()I"))
    private int chunks3$cubicTop(WorldGenLevel level, Operation<Integer> original) {
        return level instanceof Level live && CubicLevel.of(live) != null ? CubeWorld.MAX_Y + 1 : original.call(level);
    }
}
