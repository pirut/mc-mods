package io.github.pirut.mcmods.lib;

/**
 * The internal library that mods in this repo can bundle (uses_common_lib=true in their gradle.properties).
 *
 * <p>Only plain Java goes here: no Minecraft or loader classes, so the same jar nests into NeoForge and Fabric
 * jars. Players never install it themselves. When several mods carry it, the loader keeps the newest copy, so
 * changes must stay backward compatible: add, don't rename or remove.
 */
public final class CommonLib {
    /** The bundled library's version, the same as common_lib_version in gradle.properties. */
    public static String version() {
        String v = CommonLib.class.getPackage().getImplementationVersion();
        return v != null ? v : "dev";
    }

    /**
     * Shortens a string to at most {@code max} characters, ending it with "..." when it was cut. Handy for chat
     * lines, titles and log messages.
     */
    public static String ellipsize(String text, int max) {
        if (max < 3) throw new IllegalArgumentException("max must be at least 3");
        if (text == null || text.length() <= max) return text;
        return text.substring(0, max - 3) + "...";
    }

    private CommonLib() {}
}
