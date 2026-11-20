package io.github.pirut.chunks3.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.pirut.chunks3.CubeWorld;
import io.github.pirut.chunks3.CubicLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Digital Miner clamps its Y range to the level's build height, which in the cubic level is the
 * 16-block carrier, so it could only mine between Y 0 and 15. It gets the cubic world's range instead.
 * Covers the miner itself and its config screen.
 */
@Pseudo
@Mixin(targets = {
        "mekanism.common.tile.machine.TileEntityDigitalMiner",
        "mekanism.client.gui.machine.GuiDigitalMinerConfig"}, remap = false)
abstract class MekanismDigitalMinerMixin {
    @WrapOperation(method = "*", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getMinBuildHeight()I"))
    private int chunks3$cubicBottom(Level level, Operation<Integer> original) {
        return CubicLevel.of(level) != null ? CubeWorld.MIN_Y : original.call(level);
    }

    @WrapOperation(method = "*", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getMaxBuildHeight()I"))
    private int chunks3$cubicTop(Level level, Operation<Integer> original) {
        return CubicLevel.of(level) != null ? CubeWorld.MAX_Y + 1 : original.call(level);
    }
}
