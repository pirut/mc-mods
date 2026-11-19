package io.github.pirut.chunks3.config;

import io.github.pirut.chunks3.Cubic;
import io.github.pirut.chunks3.far.Far;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import net.minecraft.network.chat.Component;

/**
 * Chunks3's player settings, in {@code config/chunks3-client.properties}: the far terrain's distance,
 * sharpness, frame rate to hold and textured levels, and the engine's player-facing on/off switches
 * (see {@link EngineToggles}). The screens (the config screen, Sodium's page, the pause menu) all
 * read and write these values. Distance and sharpness apply at once while playing; a switch read
 * when the game starts applies at the next start.
 *
 * <p>This is Earth Platform's {@code EarthConfig} for the engine's part, without Earth's own settings
 * and without a config library: the engine's code ported from Earth calls the same names here.
 * A {@code -Dchunks3.*} property given on the command line always wins over the file.
 */
public final class Chunks3Config {
    private Chunks3Config() {}

    /** An integer setting, kept in range. */
    public static final class IntValue {
        final String key;
        final int defaultValue, min, max;
        private volatile int value;

        IntValue(String key, int defaultValue, int min, int max) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.value = defaultValue;
        }

        public int get() { return value; }
        public void set(int value) { this.value = Math.clamp(value, min, max); }
        public int defaultValue() { return defaultValue; }
        public int min() { return min; }
        public int max() { return max; }
    }

    /** A choice among an enum's values. */
    public static final class EnumValue<E extends Enum<E>> {
        final String key;
        final E defaultValue;
        private final Class<E> type;
        private volatile E value;

        EnumValue(String key, E defaultValue, Class<E> type) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.type = type;
            this.value = defaultValue;
        }

        public E get() { return value; }
        public void set(E value) { this.value = value; }
        public E defaultValue() { return defaultValue; }
    }

    /**
     * Far terrain sharpness choices, as scales of the detail (see {@link Far#sharpness}): how close
     * coarser levels take over. Extreme and Ultra, past {@link Far#EXTREME_SCALE}, are for very
     * powerful computers.
     */
    public enum Sharpness {
        FAST(1.0), BALANCED(1.3), SHARP(1.6), SHARPEST(2.0), EXTREME(3.0), ULTRA(4.0);

        public final double scale;

        Sharpness(double scale) { this.scale = scale; }

        public Component getTranslatedName() { return Component.translatable(key("farSharpness." + name().toLowerCase(Locale.ROOT))); }
    }

    /** The far terrain's reach (see {@link Far#detail()}): 4 is about 37 km, 11 (the default) about 94 km, 48 about 397 km. */
    public static final IntValue FAR_DETAIL = new IntValue("farDetail", Far.DEFAULT_DETAIL, 4, Far.MAX_DETAIL);
    public static final EnumValue<Sharpness> FAR_SHARPNESS = new EnumValue<>("farSharpness", Sharpness.FAST, Sharpness.class);
    /** A frame rate to hold, or 0 for off: sharpness steps down while the game runs slower (see {@code FarGovernor}). */
    public static final IntValue FAR_TARGET_FPS = new IntValue("farTargetFps", 0, 0, 240);
    /** How many far levels are drawn with block textures; coarser ones use flat colours. Read at start. */
    public static final IntValue FAR_TEXTURED_LEVELS = new IntValue("farTexturedLevels", Far.MAX_LEVEL, 0, Far.MAX_LEVEL);

    private static final String FILE = "chunks3-client.properties";
    private static Path file;
    /** The command-line values, taken before this class sets any property. */
    private static final Properties COMMAND_LINE = new Properties();

    static {
        for (String key : List.of("chunks3.far.detail", "chunks3.far.textured")) {
            String value = System.getProperty(key);
            if (value != null) COMMAND_LINE.setProperty(key, value);
        }
        for (EngineToggles.Toggle toggle : EngineToggles.ALL) {
            String value = System.getProperty(toggle.systemProperty());
            if (value != null) COMMAND_LINE.setProperty(toggle.systemProperty(), value);
        }
    }

    /**
     * Reads the settings file and hands its values to the engine, before any class that reads them
     * initializes: each loader's entry point calls this first thing. A missing file leaves the defaults
     * and writes them out, so the file is there to edit.
     */
    public static synchronized void load(Path configDir) {
        file = configDir.resolve(FILE);
        Properties saved = new Properties();
        boolean found = Files.isRegularFile(file);
        if (found) {
            try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                saved.load(in);
            } catch (IOException | IllegalArgumentException failure) {
                Cubic.LOGGER.warn("Could not read {}; the defaults stand", file, failure);
            }
        }
        // The sharpness defaults to Fast, as the engine draws with no setting; the file's choice applies from the start.
        FAR_DETAIL.set(readInt(saved, FAR_DETAIL, COMMAND_LINE.getProperty("chunks3.far.detail")));
        FAR_TEXTURED_LEVELS.set(readInt(saved, FAR_TEXTURED_LEVELS, COMMAND_LINE.getProperty("chunks3.far.textured")));
        FAR_TARGET_FPS.set(readInt(saved, FAR_TARGET_FPS, null));
        FAR_SHARPNESS.set(readEnum(saved, FAR_SHARPNESS));
        for (EngineToggles.Toggle toggle : EngineToggles.ALL) {
            String value = saved.getProperty(toggle.key());
            EngineToggles.set(toggle, value == null ? toggle.defaultValue() : Boolean.parseBoolean(value.trim()));
        }
        apply();
        if (!found) save();
    }

    private static int readInt(Properties saved, IntValue setting, String commandLine) {
        String value = commandLine != null ? commandLine : saved.getProperty(setting.key);
        if (value == null) return setting.defaultValue;
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException wrong) {
            Cubic.LOGGER.warn("Chunks3 setting {} = '{}' is not a number; using {}", setting.key, value, setting.defaultValue);
            return setting.defaultValue;
        }
    }

    private static <E extends Enum<E>> E readEnum(Properties saved, EnumValue<E> setting) {
        String value = saved.getProperty(setting.key);
        if (value == null) return setting.defaultValue;
        try { return Enum.valueOf(setting.type, value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException wrong) { return setting.defaultValue; }
    }

    /** Hands the values to the engine through its properties and live setters, unless the command line set them. */
    private static void apply() {
        property("chunks3.far.detail", Integer.toString(FAR_DETAIL.get()));
        if (!COMMAND_LINE.containsKey("chunks3.far.detail")) Far.detail(FAR_DETAIL.get());
        property("chunks3.far.textured", Integer.toString(FAR_TEXTURED_LEVELS.get()));
        steps = Math.min(steps, mostSharpnessSteps());
        applySharpness();
        for (EngineToggles.Toggle toggle : EngineToggles.ALL) {
            String property = toggle.systemProperty();
            if (COMMAND_LINE.containsKey(property)) continue;
            boolean value = EngineToggles.get(toggle);
            // A toggle at its default leaves its property unset, so nothing differs for anyone who touches no setting.
            if (value == toggle.defaultValue()) System.clearProperty(property);
            else System.setProperty(property, Boolean.toString(value));
        }
    }

    private static void property(String key, String value) {
        if (!COMMAND_LINE.containsKey(key)) System.setProperty(key, value);
    }

    /** Writes the settings file. Safe to call from any screen; a failure is logged, never thrown. */
    public static synchronized void save() {
        if (file == null) return;
        List<String> lines = new ArrayList<>();
        lines.add("# Chunks3 client settings. Changed from Mods > Chunks3 > Config, Mod Menu, Sodium's Video Settings or the pause menu.");
        lines.add("# A -Dchunks3.* property given on the command line wins over this file.");
        lines.add("");
        lines.add("# How far the far terrain reaches, 4 to " + Far.MAX_DETAIL + ": 4 is about " + distanceKm(4) + " km, " + Far.DEFAULT_DETAIL
                + " (the default) about " + distanceKm(Far.DEFAULT_DETAIL) + " km, " + Far.NORMAL_DETAIL + " about " + distanceKm(Far.NORMAL_DETAIL)
                + " km, past that the extreme range. Applies at once.");
        lines.add(FAR_DETAIL.key + "=" + FAR_DETAIL.get());
        lines.add("# How close coarser far levels take over: fast, balanced, sharp, sharpest, extreme or ultra. Default fast. Applies at once.");
        lines.add(FAR_SHARPNESS.key + "=" + FAR_SHARPNESS.get().name().toLowerCase(Locale.ROOT));
        lines.add("# A frame rate to hold (0 for off): far sharpness steps down while the game runs slower than this.");
        lines.add(FAR_TARGET_FPS.key + "=" + FAR_TARGET_FPS.get());
        lines.add("# How many far levels are drawn with block textures (0 to " + Far.MAX_LEVEL + "); coarser ones use flat colours. Applies at the next start.");
        lines.add(FAR_TEXTURED_LEVELS.key + "=" + FAR_TEXTURED_LEVELS.get());
        for (EngineToggles.Group group : EngineToggles.Group.values()) {
            lines.add("");
            lines.add("# " + group.label + " (true or false; these apply at the next start)");
            for (EngineToggles.Toggle toggle : EngineToggles.of(group))
                lines.add(toggle.key() + "=" + EngineToggles.get(toggle));
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                for (String line : lines) out.write(line + "\n");
            }
        } catch (IOException failure) {
            Cubic.LOGGER.warn("Could not save {}", file, failure);
        }
    }

    /** How far the far terrain reaches with a detail setting, in kilometres of blocks. */
    public static int distanceKm(int detail) {
        return (int) Math.round((2 * detail + 1) * (double) Far.width(Far.MAX_LEVEL) / 1000.0);
    }

    /** Sets the far terrain's distance from a screen: saved and in effect at once (see {@link Far#detail(int)}). */
    public static void setDetail(int detail) {
        if (FAR_DETAIL.get() != detail) {
            FAR_DETAIL.set(detail);
            save();
        }
        Far.detail(FAR_DETAIL.get());
    }

    /** Sets the far terrain's sharpness from a screen: saved and in effect at once. */
    public static void setSharpness(Sharpness sharpness) {
        FAR_SHARPNESS.set(sharpness);
        save();
        steps = 0;
        applySharpness();
    }

    /** Sets the frame rate to hold from a screen, saved. */
    public static void setTargetFps(int fps) {
        FAR_TARGET_FPS.set(fps);
        save();
    }

    /** Sets the textured levels from a screen, saved; applies at the next start. */
    public static void setTexturedLevels(int levels) {
        FAR_TEXTURED_LEVELS.set(levels);
        save();
    }

    /** Sets a switch from a screen, saved; applies at the next start, as the engine reads it once. */
    public static void setToggle(EngineToggles.Toggle toggle, boolean value) {
        EngineToggles.set(toggle, value);
        save();
    }

    /**
     * How many choices below the one chosen the far terrain's sharpness is held for now, to keep
     * the frame rate asked for (see {@link #FAR_TARGET_FPS}); 0 when nothing holds it down.
     */
    private static volatile int steps;

    public static int sharpnessSteps() { return steps; }

    /** The most {@link #sharpnessSteps} the choice made leaves: down to Fast. */
    public static int mostSharpnessSteps() { return FAR_SHARPNESS.get().ordinal(); }

    /** Holds the sharpness that many choices below the one chosen, in effect at once; never saved. */
    public static void sharpnessSteps(int below) {
        int now = Math.clamp(below, 0, mostSharpnessSteps());
        if (now == steps) return;
        steps = now;
        applySharpness();
    }

    private static void applySharpness() {
        Sharpness[] all = Sharpness.values();
        Far.sharpness(all[Math.max(0, FAR_SHARPNESS.get().ordinal() - steps)].scale);
    }

    /** The language key of a setting's name; its description is the same with {@code .tooltip}. */
    public static String key(String name) { return Cubic.MOD_ID + ".configuration." + name; }
}
