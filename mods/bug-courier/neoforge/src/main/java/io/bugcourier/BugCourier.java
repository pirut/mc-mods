package io.bugcourier;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BugCourier.MOD_ID)
public final class BugCourier {
    public static final String MOD_ID = "bugcourier";
    public static final Logger LOG = LoggerFactory.getLogger("Bug Courier");

    public BugCourier(IEventBus modBus) {
        // Reports are made on the player's computer; a dedicated server only needs the jar to load.
        if (FMLEnvironment.dist == Dist.CLIENT) io.bugcourier.client.ClientSetup.init(modBus);
    }
}
