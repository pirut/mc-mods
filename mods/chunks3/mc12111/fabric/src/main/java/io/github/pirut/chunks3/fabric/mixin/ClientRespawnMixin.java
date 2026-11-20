package io.github.pirut.chunks3.fabric.mixin;

import io.github.pirut.chunks3.client.CubicClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The local player was replaced by a respawn, as NeoForge's ClientPlayerNetworkEvent.Clone. */
@Mixin(ClientPacketListener.class)
abstract class ClientRespawnMixin {
    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void chunks3$respawned(ClientboundRespawnPacket packet, CallbackInfo info) { CubicClient.respawn(); }
}
