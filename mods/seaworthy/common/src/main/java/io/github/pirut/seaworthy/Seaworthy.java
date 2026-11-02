package io.github.pirut.seaworthy;

import dev.ryanhcode.sable.platform.SableEventPlatform;
import io.github.pirut.seaworthy.sable.BlockFlags;
import io.github.pirut.seaworthy.sable.Buoyancy;
import io.github.pirut.seaworthy.sable.ShipTracker;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watertight hulls for Sable ships. Code shared by both loaders; each loader's entry point calls {@link #init}.
 *
 * <p>Sable can already hide water inside a ship, keep players from swimming there and drop the underwater fog, but
 * only in spaces someone registers with it, and nothing does. This mod works out every ship's dry spaces, keeps
 * them registered as the ship is built, damaged and flooded, and makes the dry air lift the ship like a real hull.
 */
public final class Seaworthy {
    public static final String MOD_ID = "seaworthy";
    public static final Logger LOG = LoggerFactory.getLogger("Seaworthy");

    private static Platform platform;
    private static SeaworthyConfig config = SeaworthyConfig.DEFAULT;

    private Seaworthy() {
    }

    public static void init(Platform platform) {
        Seaworthy.platform = platform;
        config = SeaworthyConfig.load(platform.configDir().resolve("seaworthy.properties"));
        SableEventPlatform.INSTANCE.onSubLevelContainerReady((level, container) -> {
            BlockFlags.clear();
            container.addObserver(ShipTracker.create(level));
        });
        SableEventPlatform.INSTANCE.onPhysicsTick(Buoyancy::onPhysicsTick);
        LOG.info("Seaworthy loaded on {}", platform.loader());
    }

    public static Platform platform() {
        return platform;
    }

    public static SeaworthyConfig config() {
        return config;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
