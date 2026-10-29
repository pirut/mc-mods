package io.github.pirut.ccagent.fabric;

import io.github.pirut.ccagent.Relay;
import io.github.pirut.ccagent.RelayPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client-only glue for the multiplayer relay. */
public final class CCAgentFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RelayPayload.TYPE, (payload, context) -> Relay.clientReceive(payload));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> Relay.setClientSender(ClientPlayNetworking::send));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> Relay.clientDisconnected());
    }
}
