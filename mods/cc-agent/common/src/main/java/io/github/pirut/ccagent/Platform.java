package io.github.pirut.ccagent;

import java.nio.file.Path;

/** What the loader glue tells the shared code. */
public interface Platform {
    Path configDir();

    Path gameDir();

    String loader();

    /** A loaded mod's name, version and links, or null if no such mod. */
    ModMeta mod(String modId);

    record ModMeta(String id, String name, String version, String homepage, String sources, String issues) {
    }
}
