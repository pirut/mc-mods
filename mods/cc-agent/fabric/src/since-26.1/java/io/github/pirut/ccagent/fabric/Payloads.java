package io.github.pirut.ccagent.fabric;

import io.github.pirut.ccagent.RelayPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Registering the relay payload both ways, for Fabric API on Minecraft 26.1 and later. */
final class Payloads {
    private Payloads() {
    }

    static void register() {
        PayloadTypeRegistry.serverboundPlay().register(RelayPayload.TYPE, RelayPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RelayPayload.TYPE, RelayPayload.CODEC);
    }
}
