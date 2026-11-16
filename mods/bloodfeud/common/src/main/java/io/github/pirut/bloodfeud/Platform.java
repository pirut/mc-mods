package io.github.pirut.bloodfeud;

import java.nio.file.Path;

/** What the loader glue does for the shared code. */
public interface Platform {
    String loader();

    Path configDir();
}
