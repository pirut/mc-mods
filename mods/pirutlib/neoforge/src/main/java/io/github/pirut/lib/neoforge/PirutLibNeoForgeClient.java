package io.github.pirut.lib.neoforge;

import io.github.pirut.lib.ClientHooks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The client half of the NeoForge glue: keys. Only loaded in a game client. */
final class PirutLibNeoForgeClient {
    private PirutLibNeoForgeClient() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            for (ClientHooks.Key k : ClientHooks.keys()) e.register(k.mapping());
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ClientHooks.tick());
    }
}
