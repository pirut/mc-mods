package io.github.pirut.seaworthy.neoforge;

import io.github.pirut.seaworthy.Seaworthy;
import io.github.pirut.seaworthy.sable.FloodRenderer;
import io.github.pirut.seaworthy.sable.GoggleHud;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The player's game only: draws the floodwater inside hulls and the goggles readout. */
final class SeaworthyNeoForgeClient {
    private SeaworthyNeoForgeClient() {
    }

    static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(SeaworthyNeoForgeClient::onRenderLevel);
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(Seaworthy.id("goggles"),
                (graphics, delta) -> GoggleHud.render(graphics)));
    }

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        FloodRenderer.render(event.getModelViewMatrix(), event.getCamera(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }
}
