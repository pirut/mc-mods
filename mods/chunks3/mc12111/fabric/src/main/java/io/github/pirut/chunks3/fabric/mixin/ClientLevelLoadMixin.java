package io.github.pirut.chunks3.fabric.mixin;

import io.github.pirut.chunks3.client.CubicClient;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A client level is made, as NeoForge's LevelEvent.Load at the end of its constructor. */
@Mixin(ClientLevel.class)
abstract class ClientLevelLoadMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void chunks3$load(CallbackInfo info) { CubicClient.load((ClientLevel) (Object) this); }
}
