package io.github.pirut.ccagent.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sending a payload to the server, for NeoForge before 1.21.5. */
final class ClientSend {
    private ClientSend() {
    }

    static void toServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
