package io.github.pirut.chunks3.config;

import io.github.pirut.chunks3.Cubic;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The engine's player-facing on/off switches, each also a {@code -Dchunks3...} system property,
 * as settings: the ones Earth Platform puts on Sodium's page, less those only Earth's features use.
 * The engine reads each property once, when the class that uses it initializes, so a change applies
 * at the next start; {@link Chunks3Config#load} sets the properties before those classes load.
 */
public final class EngineToggles {
    private EngineToggles() {}

    /** The heading a toggle is listed under, on the screens and in the settings file. */
    public enum Group {
        FAR_TERRAIN("farTerrain", "Far terrain"), NEARBY_BLOCKS("nearbyBlocks", "Nearby blocks"), COMPATIBILITY("compatibility", "Shader packs and culling");

        public final String key;
        public final String label;

        Group(String key, String label) {
            this.key = key;
            this.label = label;
        }
    }

    /**
     * One switch.
     *
     * @param property the system property without its {@code chunks3.} prefix
     * @param defaultValue what the engine does when the property is absent
     * @param experimental marked so on the screens, and off by default
     */
    public record Toggle(Group group, String property, boolean defaultValue, boolean experimental) {
        public String systemProperty() { return "chunks3." + property; }

        /** The name in the settings file and the end of the language keys: {@code far.blockTextures} is {@code farBlockTextures}. */
        public String key() {
            String[] parts = property.split("\\.");
            StringBuilder key = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) key.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i], 1, parts[i].length());
            return key.toString();
        }

        /** The id path on Sodium's page, lowercase as a {@code ResourceLocation} requires. */
        public String sodiumId() { return "toggle_" + property.replace('.', '_').toLowerCase(Locale.ROOT); }

        /** The language key of the name; the description is the same with {@code .tooltip}. */
        public String langKey() { return Cubic.MOD_ID + ".configuration." + key(); }
    }

    private static Toggle on(Group group, String property) { return new Toggle(group, property, true, false); }

    private static Toggle experimental(Group group, String property) { return new Toggle(group, property, false, true); }

    /** Every switch, in the order the screens list them. The defaults are the engine's own, as each class reads its property. */
    public static final List<Toggle> ALL = List.of(
            experimental(Group.FAR_TERRAIN, "far.smooth"),
            experimental(Group.FAR_TERRAIN, "far.blockTextures"),
            on(Group.FAR_TERRAIN, "far.fade"),
            on(Group.FAR_TERRAIN, "far.zoom"),
            on(Group.NEARBY_BLOCKS, "cubes.caveCull"),
            on(Group.NEARBY_BLOCKS, "cubes.shadowCull"),
            on(Group.NEARBY_BLOCKS, "cubes.shadowCaves"),
            experimental(Group.NEARBY_BLOCKS, "cubes.occlusion"),
            experimental(Group.NEARBY_BLOCKS, "cubes.skipInterior"),
            experimental(Group.NEARBY_BLOCKS, "cubes.reuseSnapshots"),
            on(Group.COMPATIBILITY, "far.lodDepth"),
            on(Group.COMPATIBILITY, "far.dhPacks"),
            on(Group.COMPATIBILITY, "far.hiddenCull"),
            experimental(Group.COMPATIBILITY, "far.twoPhase"),
            experimental(Group.COMPATIBILITY, "far.queryCull"));

    private static final Map<Toggle, Boolean> VALUES = new ConcurrentHashMap<>();

    /** The toggles of one group. */
    public static List<Toggle> of(Group group) { return ALL.stream().filter(toggle -> toggle.group() == group).toList(); }

    /** The saved value, which applies from the next start. */
    public static boolean get(Toggle toggle) { return VALUES.getOrDefault(toggle, toggle.defaultValue()); }

    static void set(Toggle toggle, boolean value) { VALUES.put(toggle, value); }
}
