package io.bugcourier.neoforge;

import io.bugcourier.BugCourier;
import io.bugcourier.client.ClientSetup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(BugCourier.MOD_ID)
public final class BugCourierNeoForge {
    // NeoForge passes the side in: FMLEnvironment.dist became FMLEnvironment.getDist() in newer versions, so asking for it
    // keeps one entry point for every version.
    public BugCourierNeoForge(IEventBus modBus, Dist dist) {
        NeoForgePlatform.dist = dist;
        // Reports are made on the player's computer; a dedicated server only needs the jar to load.
        if (dist == Dist.CLIENT) Client.init(modBus);
    }

    private static final class Client {
        static void init(IEventBus modBus) {
            modBus.addListener(RegisterKeyMappingsEvent.class, e -> e.register(ClientSetup.OPEN));
            NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> ClientSetup.onClientTick());
            NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class,
                    e -> ClientSetup.onScreenInit(e.getScreen(), e.getListenersList(), e::addListener));
            NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, e -> e.getDispatcher().register(ClientSetup.command()));
        }
    }
}
