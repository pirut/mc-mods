package io.github.pirut.chunks3.mixin;

import io.github.pirut.chunks3.CubicLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A cubic level has no void below its carrier floor; the cubes go down to the deepest trench. */
@Mixin(Entity.class)
abstract class CubicEntityMixin {
    @Shadow public abstract Level level();

    @Inject(method = "checkBelowWorld()V", at = @At("HEAD"), cancellable = true)
    private void chunks3$noCarrierVoid(CallbackInfo info) {
        if (CubicLevel.of(level()) != null && getY() > io.github.pirut.chunks3.CubeWorld.MIN_Y - 64) info.cancel();
    }

    @Shadow public abstract double getY();
}
