package io.bugcourier.client;

import io.bugcourier.BugCourier;
import io.bugcourier.api.BugReports;
import io.bugcourier.api.ReportConfig;
import io.bugcourier.core.Tracker;
import io.bugcourier.core.Trackers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.screens.Screen;
import io.bugcourier.platform.Platform;
import io.bugcourier.client.version.Mc;
import io.bugcourier.platform.Platform.ModEntry;

/**
 * Finds every installed mod that takes bug reports. In order: what the mod registered in code, its bugcourier_*
 * keys ({@code [modproperties.<modid>]} on NeoForge, {@code "custom"} in fabric.mod.json on Fabric), then the issue
 * link the mod already declares ({@code issueTrackerURL} / {@code contact.issues}).
 */
public final class ReportTargets {
    public static final String PROP_TRACKER = "bugcourier_tracker";
    public static final String PROP_LABELS = "bugcourier_labels";
    public static final String PROP_OFF = "bugcourier_disabled";

    private static volatile List<ReportTarget> cache;

    private ReportTargets() {
    }

    public static List<ReportTarget> all() {
        List<ReportTarget> found = cache;
        if (found != null) return found;
        found = new ArrayList<>();
        for (ModEntry mod : Platform.get().allMods()) {
            if (mod.reportable()) target(mod).ifPresent(found::add);
        }
        found.sort(Comparator.comparing(t -> t.name().toLowerCase(java.util.Locale.ROOT)));
        cache = found = List.copyOf(found);
        return found;
    }

    public static Optional<ReportTarget> find(String modId) {
        return all().stream().filter(t -> t.modId().equals(modId)).findFirst();
    }

    /** Opens the report screen on the next tick, so a closing chat or menu can't replace it. */
    public static void openScreen(String modId, String crashText) {
        Mc.later(() -> {
            Screen parent = Mc.screen();
            Mc.setScreen(new ReportScreen(parent, modId, crashText));
        });
    }

    private static Optional<ReportTarget> target(ModEntry mod) {
        Map<String, Object> props = mod.properties();
        if (truthy(props.get(PROP_OFF))) return Optional.empty();
        ReportConfig config = BugReports.registered(mod.id()).orElse(null);
        Tracker tracker = config == null ? null : config.tracker();
        List<String> labels = new ArrayList<>(config == null ? List.of() : config.labels());
        if (tracker == null && props.get(PROP_TRACKER) instanceof String spec) {
            tracker = Trackers.parse(spec).orElse(null);
            if (tracker == null) BugCourier.LOG.warn("{} declares an issue tracker Bug Courier can't read: {}", mod.id(), spec);
        }
        if (labels.isEmpty() && props.get(PROP_LABELS) instanceof String list) {
            Arrays.stream(list.split(",")).map(String::strip).filter(s -> !s.isEmpty()).forEach(labels::add);
        }
        if (tracker == null && mod.issueTracker() != null && !mod.issueTracker().isBlank()) {
            tracker = Trackers.parse(mod.issueTracker()).orElse(null);
        }
        if (tracker == null) return Optional.empty();
        return Optional.of(new ReportTarget(mod.id(), mod.name(), mod.version(), tracker, List.copyOf(labels),
                config));
    }

    private static boolean truthy(Object o) {
        return o instanceof Boolean b ? b : o instanceof String s && s.equalsIgnoreCase("true");
    }
}
