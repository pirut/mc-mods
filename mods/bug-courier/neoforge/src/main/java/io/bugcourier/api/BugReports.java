package io.bugcourier.api;

import io.bugcourier.client.ReportTargets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * The library's front door for other mods. Most mods need none of this: an {@code issueTrackerURL} in
 * neoforge.mods.toml is enough. Call it to add labels, extra context or files, or to open the report screen
 * from your own button. Guard calls with {@code ModList.get().isLoaded("bugcourier")} to keep it optional.
 */
public final class BugReports {
    private static final Map<String, ReportConfig> REGISTERED = new ConcurrentHashMap<>();

    private BugReports() {
    }

    /** Sets up how {@code modId} takes reports. Safe to call from your mod's constructor, on any side. */
    public static void register(String modId, Consumer<ReportConfig> setup) {
        ReportConfig config = REGISTERED.computeIfAbsent(modId, id -> new ReportConfig());
        setup.accept(config);
    }

    /** The config a mod registered in code, if any. */
    public static Optional<ReportConfig> registered(String modId) {
        return Optional.ofNullable(REGISTERED.get(modId));
    }

    /** Opens the report screen with {@code modId} picked. Does nothing on a dedicated server. */
    public static void open(String modId) {
        if (FMLEnvironment.dist == Dist.CLIENT) ReportTargets.openScreen(modId, null);
    }
}
