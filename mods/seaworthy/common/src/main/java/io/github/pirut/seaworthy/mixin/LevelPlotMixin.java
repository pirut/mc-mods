package io.github.pirut.seaworthy.mixin;

import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import io.github.pirut.seaworthy.sable.ShipTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sable tells a ship's plot about every block change in it, on the server and on players' games: so do we. */
@Mixin(value = LevelPlot.class, remap = false)
public abstract class LevelPlotMixin {

    @Inject(method = "onBlockChange", at = @At("TAIL"))
    private void seaworthy$onBlockChange(BlockPos pos, BlockState state, CallbackInfo ci) {
        ShipTracker.blockChanged(((LevelPlot) (Object) this).getSubLevel());
    }
}
