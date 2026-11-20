package io.github.pirut.chunks3.mixin;

import io.github.pirut.chunks3.CubicLevel;
import io.github.pirut.chunks3.portal.CubicPortals;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cubic Nether portal blocks go to the cubic portal list instead of the point-of-interest store,
 * which only spans the carrier's height and rejects true-height positions.
 */
@Mixin(ServerLevel.class)
abstract class CubicPortalPoiMixin {
    //#if MC >= 1.21.11
    //$$ @Inject(method = "updatePOIOnBlockStateChange", at = @At("HEAD"), cancellable = true)
    //#else
    @Inject(method = "onBlockStateChange", at = @At("HEAD"), cancellable = true)
    //#endif
    private void chunks3$cubicPortal(BlockPos pos, BlockState old, BlockState now, CallbackInfo info) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (CubicLevel.of(self) == null || !(old.is(Blocks.NETHER_PORTAL) || now.is(Blocks.NETHER_PORTAL))) return;
        CubicPortals.blockChanged(self, pos, old, now);
        info.cancel();
    }
}
