package io.github.pirut.lib;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pirut Lib: the loader hooks and game helpers Pirut's mods share, so each mod doesn't build them again.
 *
 * <ul>
 *   <li>{@link Loader}: which loader is running, the config and game folders, installed mods.</li>
 *   <li>{@link ServerEvents}: server ticks, entities loading, deaths, damage, commands and shutdown, the same way
 *       on NeoForge and Fabric. A mod that only needs these has no loader code of its own.</li>
 *   <li>{@link Titles}: title cards, subtitles and action bar lines.</li>
 *   <li>{@link Stores}: world save data.</li>
 *   <li>{@link Spots}: places a mob can stand.</li>
 * </ul>
 *
 * Mods nest it in their jars, so players never install it by hand. Changes must stay backward compatible: add,
 * don't rename or remove, because the newest copy any mod carries is the one that loads.
 */
public final class PirutLib {
    public static final String MOD_ID = "pirutlib";
    public static final Logger LOG = LoggerFactory.getLogger("Pirut Lib");

    private PirutLib() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
