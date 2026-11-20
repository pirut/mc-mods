package io.github.pirut.chunks3.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.pirut.chunks3.CubeWorld;
import io.github.pirut.chunks3.CubicLevel;
import net.minecraft.server.level.ServerLevel;
//#if MC >= 1.21.2
//$$ import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
//#else
import net.minecraft.world.item.ChorusFruitItem;
//#endif
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Chorus fruit teleports within eight blocks of the eater's height. It clamped the height to the
 * 16-block carrier, so eating one anywhere else sent the player into the ground near Y 0.
 */
//#if MC >= 1.21.2
//$$ @Mixin(TeleportRandomlyConsumeEffect.class)
//#else
@Mixin(ChorusFruitItem.class)
//#endif
abstract class CubicChorusFruitMixin {
    //#if MC >= 1.21.2
    //$$ private static final String CHORUS_METHOD = "apply";
    //#else
    private static final String CHORUS_METHOD = "finishUsingItem";
    //#endif

    @WrapOperation(method = CHORUS_METHOD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getMinBuildHeight()I"))
    private int chunks3$cubicBottom(Level level, Operation<Integer> original) {
        return CubicLevel.of(level) != null ? CubeWorld.MIN_Y : original.call(level);
    }

    @WrapOperation(method = CHORUS_METHOD, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getLogicalHeight()I"))
    private int chunks3$cubicHeight(ServerLevel level, Operation<Integer> original) {
        return CubicLevel.of(level) != null ? CubeWorld.MAX_Y - CubeWorld.MIN_Y + 1 : original.call(level);
    }
}
