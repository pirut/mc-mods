package io.github.pirut.chunks3.mixin.compat;

import io.github.pirut.chunks3.compat.SableCarrier;
import io.github.pirut.chunks3.compat.SableCompat;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * With Sable installed, the cubic level's carrier takes the Overworld's build range and leaves
 * Sable's ship plots to chunks. Decided once, as the level is built, before its chunk source and
 * light engine size themselves from the build range.
 */
@Mixin(Level.class)
abstract class SableCarrierMixin implements SableCarrier {
    @Unique private boolean chunks3$tallCarrier;

    @Override public boolean chunks3$tallCarrier() { return chunks3$tallCarrier; }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void chunks3$sizeCarrier(CallbackInfo info) {
        Level level = (Level) (Object) this;
        if (!SableCompat.tallCarrier(level)) return;
        chunks3$tallCarrier = true;
        SableCompat.keepPlots(level);
    }
}
