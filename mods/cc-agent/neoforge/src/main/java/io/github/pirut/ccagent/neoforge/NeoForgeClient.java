package io.github.pirut.ccagent.neoforge;

import io.github.pirut.ccagent.CCAgent;
import io.github.pirut.ccagent.Relay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only glue for the multiplayer relay. NeoForge only loads this class on the client. */
@Mod(value = CCAgent.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeClient {
    public NeoForgeClient() {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> Relay.setClientSender(ClientSend::toServer));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> Relay.clientDisconnected());
    }
}
