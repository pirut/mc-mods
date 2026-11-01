package io.github.pirut.chunks3.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.SortedSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Block cracks are keyed by BlockPos.asLong, whose 12-bit Y wraps above Y 2047, so the overlay
 * landed thousands of blocks away. Remember the real position for each key and draw it there.
 * Remembered at TAIL: each new stage first removes the old entry (which forgets the key) and
 * then adds it back, so a HEAD record was dropped after the first stage.
 */
@Mixin(LevelRenderer.class)
abstract class CubicDestroyProgressMixin {
    @Shadow @Final private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;
    @Unique private final Long2ObjectOpenHashMap<BlockPos> chunks3$crackPositions = new Long2ObjectOpenHashMap<>();

    @Inject(method = "destroyBlockProgress", at = @At("TAIL"))
    private void chunks3$remember(int breakerId, BlockPos pos, int progress, CallbackInfo info) {
        if (progress >= 0 && progress < 10) chunks3$crackPositions.put(pos.asLong(), pos.immutable());
    }

    @Inject(method = "removeProgress", at = @At("TAIL"))
    private void chunks3$forget(BlockDestructionProgress progress, CallbackInfo info) {
        long key = progress.getPos().asLong();
        if (!destructionProgress.containsKey(key)) chunks3$crackPositions.remove(key);
    }

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;of(J)Lnet/minecraft/core/BlockPos;"))
    private BlockPos chunks3$realPosition(long key, Operation<BlockPos> original) {
        BlockPos pos = chunks3$crackPositions.get(key);
        return pos != null ? pos : original.call(key);
    }
}
