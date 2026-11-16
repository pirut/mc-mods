package io.github.pirut.lib;

import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;

/** What differs between NeoForge and Fabric for a mod's shared code. {@link #get()} returns the running one. */
public interface Loader {
    /** "neoforge" or "fabric". */
    String name();

    Path configDir();

    Path gameDir();

    /** Whether this is a player's game (true) or a dedicated server (false). */
    boolean isClient();

    /** An installed mod's version, if it is installed. */
    Optional<String> modVersion(String modId);

    default boolean isLoaded(String modId) {
        return modVersion(modId).isPresent();
    }

    static Loader get() {
        return Holder.INSTANCE;
    }

    final class Holder {
        static final Loader INSTANCE = ServiceLoader.load(Loader.class, Loader.class.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("Pirut Lib has no loader glue for this loader"));

        private Holder() {
        }
    }
}
