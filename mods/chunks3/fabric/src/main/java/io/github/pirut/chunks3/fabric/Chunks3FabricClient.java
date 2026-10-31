package io.github.pirut.chunks3.fabric;

import io.github.pirut.chunks3.client.CubicClient;
import io.github.pirut.chunks3.client.Flawless;
import io.github.pirut.chunks3.client.PerfExportCommand;
import io.github.pirut.chunks3.compat.CubicCompat;
import io.github.pirut.chunks3.fabric.axiom.AxiomCubes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The Fabric client entry point. Level loading, frames, the level's draw stages, fog and the
 * debug screen have no Fabric API events with NeoForge's timing, so mixins in
 * {@code io.github.pirut.chunks3.fabric.mixin} call the engine at those points.
 */
public final class Chunks3FabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricPayloads.registerClient();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> Flawless.offer());

        ClientTickEvents.START_CLIENT_TICK.register(client -> CubicClient.tickStart());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CubicClient.tick();
            CubicCompat.tick();
        });
        if (FabricLoader.getInstance().isModLoaded("axiom")) ClientTickEvents.END_CLIENT_TICK.register(client -> AxiomCubes.flush(true));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CubicCompat.logout());

        HudRenderCallback.EVENT.register((graphics, delta) -> CubicClient.waiting(graphics));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> PerfExportCommand.register(dispatcher));
    }
}
