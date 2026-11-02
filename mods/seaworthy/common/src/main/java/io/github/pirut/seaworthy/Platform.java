package io.github.pirut.seaworthy;

import io.github.pirut.seaworthy.net.FloodSyncPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

/** What the loader glue does for the shared code. */
public interface Platform {
    String loader();

    Path configDir();

    /** Sends flood levels to a player, if their game has Seaworthy too. */
    void sendFlood(ServerPlayer player, FloodSyncPayload payload);
}
