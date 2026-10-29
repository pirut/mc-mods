package io.github.pirut.ccagent.neoforge;

import io.github.pirut.ccagent.Relay;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client-only glue for the multiplayer relay. */
final class NeoForgeClient {
    private NeoForgeClient() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> Relay.setClientSender(PacketDistributor::sendToServer));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> Relay.clientDisconnected());
    }
}
