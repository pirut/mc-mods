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
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * Finds every installed mod that takes bug reports. In order: what the mod registered in code, its
 * {@code [modproperties.<modid>]} keys, then the {@code issueTrackerURL} NeoForge mods already declare.
 */
public final class ReportTargets {
    public static final String PROP_TRACKER = "bugcourier_tracker";
    public static final String PROP_LABELS = "bugcourier_labels";
    public static final String PROP_OFF = "bugcourier_disabled";
    // Bugs in the game or the loader itself belong upstream, not in a player report from a modpack.
    private static final Set<String> SKIP = Set.of("minecraft", "neoforge");

    private static volatile List<ReportTarget> cache;

    private ReportTargets() {
    }

    public static List<ReportTarget> all() {
        List<ReportTarget> found = cache;
        if (found != null) return found;
        found = new ArrayList<>();
        for (IModInfo mod : ModList.get().getMods()) {
            if (SKIP.contains(mod.getModId())) continue;
            target(mod).ifPresent(found::add);
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
        Minecraft mc = Minecraft.getInstance();
        mc.tell(() -> {
            Screen parent = mc.screen;
            mc.setScreen(new ReportScreen(parent, modId, crashText));
        });
    }

    private static Optional<ReportTarget> target(IModInfo mod) {
        Map<String, Object> props = mod.getModProperties();
        if (truthy(props.get(PROP_OFF))) return Optional.empty();
        ReportConfig config = BugReports.registered(mod.getModId()).orElse(null);
        Tracker tracker = config == null ? null : config.tracker();
        List<String> labels = new ArrayList<>(config == null ? List.of() : config.labels());
        if (tracker == null && props.get(PROP_TRACKER) instanceof String spec) {
            tracker = Trackers.parse(spec).orElse(null);
            if (tracker == null) BugCourier.LOG.warn("{} declares an issue tracker Bug Courier can't read: {}", mod.getModId(), spec);
        }
        if (labels.isEmpty() && props.get(PROP_LABELS) instanceof String list) {
            Arrays.stream(list.split(",")).map(String::strip).filter(s -> !s.isEmpty()).forEach(labels::add);
        }
        if (tracker == null) {
            Optional<Object> issues = mod.getOwningFile().getConfig().getConfigElement("issueTrackerURL");
            if (issues.isPresent()) tracker = Trackers.parse(issues.get().toString()).orElse(null);
        }
        if (tracker == null) return Optional.empty();
        return Optional.of(new ReportTarget(mod.getModId(), mod.getDisplayName(), mod.getVersion().toString(), tracker, List.copyOf(labels),
                config));
    }

    private static boolean truthy(Object o) {
        return o instanceof Boolean b ? b : o instanceof String s && s.equalsIgnoreCase("true");
    }
}
