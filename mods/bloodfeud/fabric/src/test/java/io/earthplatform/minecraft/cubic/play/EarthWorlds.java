package io.earthplatform.minecraft.cubic.play;

import net.minecraft.server.MinecraftServer;

public final class EarthWorlds {
    private static Session session;

    private static final class Session {
        MinecraftServer server;
        volatile int[] landing;
    }
}
