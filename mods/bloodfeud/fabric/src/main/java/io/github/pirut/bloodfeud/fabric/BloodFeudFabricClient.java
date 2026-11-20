package io.github.pirut.bloodfeud.fabric;

import io.github.pirut.bloodfeud.client.FeudClient;
import net.fabricmc.api.ClientModInitializer;

/** The Ledger and Map screens, for players who install Blood Feud on their client. */
public final class BloodFeudFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FeudClient.init();
    }
}
