package io.github.pirut.ccagent;

import java.nio.file.Path;

/** What the loader glue tells the shared code. */
public interface Platform {
    Path configDir();

    String loader();
}
