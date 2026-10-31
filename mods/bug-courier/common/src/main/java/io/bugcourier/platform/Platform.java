package io.bugcourier.platform;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/** What Bug Courier needs from the mod loader. Each loader's jar provides one through META-INF/services. */
public interface Platform {
    /**
     * An installed mod. {@code issueTracker} is the link the mod declares itself (issueTrackerURL on NeoForge,
     * contact.issues on Fabric), {@code properties} its bugcourier_* keys ([modproperties.modid] on NeoForge,
     * "custom" in fabric.mod.json on Fabric), and {@code file} the jar it came in.
     */
    record ModEntry(String id, String name, String version, String issueTracker, Map<String, Object> properties, String file,
                    boolean reportable) {
    }

    /**
     * Every mod the loader lists, for the report's mod list. {@code reportable} is false for the game, the loader,
     * Java and mods nested inside other mods: bugs there belong upstream or to the mod that carries them.
     */
    List<ModEntry> allMods();

    default Optional<ModEntry> mod(String id) {
        return allMods().stream().filter(m -> m.id().equals(id)).findFirst();
    }

    Path gameDir();

    Path configDir();

    /** "NeoForge" or "Fabric". */
    String loaderName();

    String loaderVersion();

    boolean isClient();

    static Platform get() {
        return Holder.INSTANCE;
    }

    final class Holder {
        static final Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No Bug Courier platform for this loader"));

        private Holder() {
        }
    }
}
