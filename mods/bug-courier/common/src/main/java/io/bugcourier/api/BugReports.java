package io.bugcourier.api;

import io.bugcourier.client.ReportTargets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import io.bugcourier.platform.Platform;

/**
 * The library's front door for other mods. Most mods need none of this: the issue link the mod already
 * declares (issueTrackerURL in neoforge.mods.toml, contact.issues in fabric.mod.json) is enough. Call it to add labels, extra context or files, or to open the report screen
 * from your own button. Guard calls with a check that the
 * bugcourier mod is loaded to keep it optional.
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
        if (Platform.get().isClient()) ReportTargets.openScreen(modId, null);
    }
}
