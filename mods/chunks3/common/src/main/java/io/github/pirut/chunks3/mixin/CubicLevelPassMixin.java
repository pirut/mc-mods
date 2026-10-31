package io.github.pirut.chunks3.mixin;

import io.github.pirut.chunks3.client.LevelPasses;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks each time the level is drawn, which a VR mod does once per eye; see {@link LevelPasses}. */
@Mixin(GameRenderer.class)
abstract class CubicLevelPassMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void chunks3$levelPass(DeltaTracker deltaTracker, CallbackInfo info) {
        io.github.pirut.chunks3.client.CubicClient.levelPass();
        LevelPasses.begin();
    }
}
