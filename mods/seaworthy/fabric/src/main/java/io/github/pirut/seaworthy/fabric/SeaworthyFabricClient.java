package io.github.pirut.seaworthy.fabric;

import io.github.pirut.seaworthy.net.ClientFlood;
import io.github.pirut.seaworthy.net.FloodSyncPayload;
import io.github.pirut.seaworthy.sable.FloodRenderer;
import io.github.pirut.seaworthy.sable.GoggleHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class SeaworthyFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FloodSyncPayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientFlood.handle(payload)));
        HudRenderCallback.EVENT.register((graphics, delta) -> GoggleHud.render(graphics));
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> FloodRenderer.render(context.positionMatrix(),
                context.camera(), context.tickCounter().getGameTimeDeltaPartialTick(false)));
    }
}
