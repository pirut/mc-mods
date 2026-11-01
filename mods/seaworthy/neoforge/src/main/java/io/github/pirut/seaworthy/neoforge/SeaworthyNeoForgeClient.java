package io.github.pirut.seaworthy.neoforge;

import io.github.pirut.seaworthy.sable.FloodRenderer;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The player's game only: draws the floodwater inside hulls. */
final class SeaworthyNeoForgeClient {
    private SeaworthyNeoForgeClient() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener(SeaworthyNeoForgeClient::onRenderLevel);
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        FloodRenderer.render(event.getModelViewMatrix(), event.getCamera(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }
}
