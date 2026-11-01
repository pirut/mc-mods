package io.github.pirut.chunks3.mixin.compat;

import io.github.pirut.chunks3.compat.SableCarrier;
import io.github.pirut.chunks3.compat.SableCompat;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The build range a level reports, widened for the cubic carrier when Sable needs room for ships. */
@Mixin(LevelReader.class)
interface SableCarrierHeightMixin {
    @Inject(method = "getMinBuildHeight()I", at = @At("HEAD"), cancellable = true)
    private void chunks3$carrierBottom(CallbackInfoReturnable<Integer> result) {
        if (this instanceof SableCarrier carrier && carrier.chunks3$tallCarrier()) result.setReturnValue(SableCompat.CARRIER_MIN_Y);
    }

    @Inject(method = "getHeight()I", at = @At("HEAD"), cancellable = true)
    private void chunks3$carrierHeight(CallbackInfoReturnable<Integer> result) {
        if (this instanceof SableCarrier carrier && carrier.chunks3$tallCarrier()) result.setReturnValue(SableCompat.CARRIER_HEIGHT);
    }
}
