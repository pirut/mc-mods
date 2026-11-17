package io.github.pirut.lib.fabric;

import io.github.pirut.lib.ClientHooks;
import io.github.pirut.lib.Net;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** The client half of the Fabric glue: message receivers and keys. */
public final class PirutLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Every mod's main entry point has run by now, so every channel is known.
        for (Net.Channel c : Net.channels()) {
            if (c.toClient()) ClientPlayNetworking.registerGlobalReceiver(c.type(), (m, context) -> c.client().accept(m.tag()));
        }
        ClientHooks.bridge(k -> KeyBindingHelper.registerKeyBinding(k.mapping()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientHooks.tick());
    }
}
