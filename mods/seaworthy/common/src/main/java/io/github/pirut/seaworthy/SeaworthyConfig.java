package io.github.pirut.seaworthy;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * {@code config/seaworthy.properties}. Written with the defaults on first start; edit it and restart to change.
 *
 * @param flooding     water comes in through holes below the waterline
 * @param floodSpeed   multiplies how fast water comes in and drains out (1 = real-world speed for the hole size)
 * @param buoyancy     multiplies the lift of dry air in a hull (1 = the same water density Sable uses for blocks)
 * @param maxHoleWidth holes this many blocks across, or narrower, flood the compartment behind them over time;
 *                     wider ones let the sea straight in. 0 turns patching off.
 * @param maxHullCells ships whose bounding box holds more blocks than this are left alone
 * @param maxLumps     how finely buoyancy is spread over a big hull (more is smoother, and costs more)
 * @param pumpRate     blocks of water a second a Create mechanical pump empties at 256 RPM; slower pumps do less
 * @param drainShipWater water blocks a ship carries inside its hull when it's assembled (sea it was built in) are
 *                     removed once, so a new ship starts dry
 */
public record SeaworthyConfig(boolean flooding, double floodSpeed, double buoyancy, int maxHoleWidth, long maxHullCells,
                              int maxLumps, double pumpRate, boolean drainShipWater) {
    public static final SeaworthyConfig DEFAULT = new SeaworthyConfig(true, 1.0, 1.0, 2, 8_000_000L, 4096, 16.0, true);

    public static SeaworthyConfig load(Path file) {
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                p.load(r);
            } catch (IOException | IllegalArgumentException e) {
                Seaworthy.LOG.warn("Couldn't read {}, using the defaults: {}", file, e.toString());
            }
        }
        SeaworthyConfig d = DEFAULT;
        SeaworthyConfig c = new SeaworthyConfig(
                bool(p, "flooding", d.flooding),
                clamp(num(p, "floodSpeed", d.floodSpeed), 0, 100),
                clamp(num(p, "buoyancy", d.buoyancy), 0, 100),
                (int) clamp(num(p, "maxHoleWidth", d.maxHoleWidth), 0, 4),
                (long) clamp(num(p, "maxHullCells", d.maxHullCells), 1000, 64_000_000),
                (int) clamp(num(p, "maxLumps", d.maxLumps), 64, 65536),
                clamp(num(p, "pumpRate", d.pumpRate), 0, 10_000),
                bool(p, "drainShipWater", d.drainShipWater));
        if (!Files.exists(file)) c.save(file);
        return c;
    }

    private void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write("""
                        # Create: Seaworthy, watertight hulls for Sable / Create Aeronautics ships. Restart the game after editing.

                        # Water comes in through holes below the waterline, and fills the compartment behind them.
                        flooding=%s
                        # How fast water comes in and drains out. 1 is real-world speed for the size and depth of the hole.
                        floodSpeed=%s
                        # How strongly dry air in a hull lifts it. 1 matches the water density Sable floats blocks with.
                        buoyancy=%s
                        # Holes up to this many blocks across flood the compartment behind them over time. Wider gaps let
                        # the sea straight in. 0 turns this off, so any hole opens the hull.
                        maxHoleWidth=%d
                        # Ships with more blocks than this in their bounding box are left alone.
                        maxHullCells=%d
                        # How finely buoyancy is spread over a big hull. More is smoother and costs more.
                        maxLumps=%d
                        # Blocks of water a second a Create mechanical pump empties at 256 RPM. Slower pumps do less.
                        pumpRate=%s
                        # Water blocks a ship carries inside its hull when it's assembled, such as the sea it was built
                        # in, are removed once so a new ship starts dry.
                        drainShipWater=%s
                        """.formatted(flooding, floodSpeed, buoyancy, maxHoleWidth, maxHullCells, maxLumps, pumpRate,
                        drainShipWater));
            }
        } catch (IOException e) {
            Seaworthy.LOG.warn("Couldn't write {}: {}", file, e.toString());
        }
    }

    private static boolean bool(Properties p, String key, boolean fallback) {
        String v = p.getProperty(key);
        return v == null ? fallback : Boolean.parseBoolean(v.trim());
    }

    private static double num(Properties p, String key, double fallback) {
        String v = p.getProperty(key);
        if (v == null) return fallback;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            Seaworthy.LOG.warn("{} should be a number, not '{}'; using {}", key, v, fallback);
            return fallback;
        }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
