package io.github.pirut.chunks3.neoforge;

import io.github.pirut.chunks3.Cubic;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * The NeoForge entry point. A world made with the Cubic world type is cubic through its dimension type;
 * other mods make their own dimensions cubic with {@link Cubic#register}. The engine is wired to NeoForge's events in
 * {@link NeoForgeEvents} and {@link NeoForgeClientEvents}, and its mixins are listed in
 * {@code chunks3.mixins.json} and {@code chunks3.neoforge.mixins.json}.
 */
@Mod(Cubic.MOD_ID)
public final class Chunks3NeoForge {
    public Chunks3NeoForge(ModContainer container, Dist dist) {
        // The settings first, before any engine class reads the properties they set.
        if (dist.isClient()) NeoForgeClientEvents.construct(container);
        Cubic.LOGGER.info("Chunks3 loaded");
    }
}
