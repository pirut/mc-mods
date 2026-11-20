package io.github.pirut.ccagent.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Sending a payload to the server, for NeoForge 1.21.5 and later. */
final class ClientSend {
    private ClientSend() {
    }

    static void toServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
