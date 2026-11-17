package io.github.pirut.lib.fabric;

import io.github.pirut.lib.ClientHooks;
import io.github.pirut.lib.Net;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** The client half of the Fabric glue: message receivers, keys and render layers. */
public final class PirutLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Every mod's main entry point has run by now, so every channel is known.
        for (Net.Channel c : Net.channels()) {
            if (c.toClient()) ClientPlayNetworking.registerGlobalReceiver(c.type(), (m, context) -> c.client().accept(m.tag()));
        }
        ClientHooks.bridge(k -> KeyBindingHelper.registerKeyBinding(k.mapping()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientHooks.tick());
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(PirutLibFabricClient::layers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void layers(net.minecraft.world.entity.EntityType<?> type, net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?> renderer,
                               LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper helper,
                               net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        if (type == net.minecraft.world.entity.EntityType.PLAYER) return;
        for (ClientHooks.LayerFactory f : ClientHooks.layers()) {
            RenderLayer layer = f.create(renderer);
            if (layer != null) helper.register(layer);
        }
    }
}
