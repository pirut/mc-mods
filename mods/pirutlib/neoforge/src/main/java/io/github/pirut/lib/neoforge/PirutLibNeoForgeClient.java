package io.github.pirut.lib.neoforge;

import io.github.pirut.lib.ClientHooks;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** The client half of the NeoForge glue: keys and render layers. Only loaded in a game client. */
final class PirutLibNeoForgeClient {
    private PirutLibNeoForgeClient() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            for (ClientHooks.Key k : ClientHooks.keys()) e.register(k.mapping());
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> ClientHooks.tick());
        modBus.addListener(PirutLibNeoForgeClient::layers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void layers(EntityRenderersEvent.AddLayers e) {
        if (ClientHooks.layers().isEmpty()) return;
        for (EntityType<?> type : e.getEntityTypes()) {
            EntityRenderer<?> r = e.getRenderer(type);
            if (!(r instanceof LivingEntityRenderer living)) continue;
            for (ClientHooks.LayerFactory f : ClientHooks.layers()) {
                RenderLayer layer = f.create(living);
                if (layer != null) living.addLayer(layer);
            }
        }
    }
}
