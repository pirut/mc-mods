package io.github.pirut.seaworthy.net;

import io.github.pirut.seaworthy.sable.ShipTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

/** Applies the server's flood levels on the player's game. Only ever loaded on the client. */
public final class ClientFlood {
    private ClientFlood() {
    }

    public static void handle(FloodSyncPayload payload) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        ShipTracker tracker = ShipTracker.of(level);
        if (tracker != null) tracker.receive(payload);
    }
}
