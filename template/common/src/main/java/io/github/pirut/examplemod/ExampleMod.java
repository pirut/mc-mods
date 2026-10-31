package io.github.pirut.examplemod;

import io.github.pirut.mcmods.lib.CommonLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Code shared by both loaders. Each loader's entry point calls {@link #init()}. */
public final class ExampleMod {
    public static final String MOD_ID = "examplemod";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public static void init(String loader) {
        LOG.info("{} loaded on {} (common-lib {})", MOD_ID, loader, CommonLib.version());
    }

    private ExampleMod() {}
}
