package io.github.pirut.ccagent.fabric;

import io.github.pirut.ccagent.RelayPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Registering the relay payload both ways, for Fabric API on Minecraft before 26.1. */
final class Payloads {
    private Payloads() {
    }

    static void register() {
        PayloadTypeRegistry.playC2S().register(RelayPayload.TYPE, RelayPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RelayPayload.TYPE, RelayPayload.CODEC);
    }
}
