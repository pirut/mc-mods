package io.github.pirut.chunks3.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.pirut.chunks3.CubeWorld;
import io.github.pirut.chunks3.CubicLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemFrameItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Item frames hang at any cubic height, not only inside the 16-block carrier. */
@Mixin(ItemFrameItem.class)
abstract class CubicItemFrameMixin {
    @WrapOperation(method = "mayPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isOutsideBuildHeight(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean chunks3$cubicRange(Level level, BlockPos pos, Operation<Boolean> original) {
        return CubicLevel.of(level) != null ? pos.getY() < CubeWorld.MIN_Y || pos.getY() > CubeWorld.MAX_Y : original.call(level, pos);
    }
}
