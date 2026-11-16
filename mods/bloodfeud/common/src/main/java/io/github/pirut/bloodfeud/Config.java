package io.github.pirut.bloodfeud;

import io.github.pirut.mcmods.lib.PropertiesFile;

import java.nio.file.Path;
import java.util.Properties;

/** config/bloodfeud.properties. Read once at start. */
public final class Config {
    /** Seconds between Captain encounters for one player, at least. */
    public int encounterCooldownSeconds = 420;
    /** Chance (percent) of a Captain showing up at each 10-second check once the cooldown is over. */
    public int encounterChancePercent = 5;
    /** No Captain shows up this close to the world spawn. */
    public int spawnSafeRadius = 96;
    /** Most Captain mobs in the world at once, all players together. */
    public int maxActiveCaptains = 12;
    /** Tell every player what the Warband did when power struggles resolve. */
    public boolean announcePowerShifts = true;

    static Config load(Path file) {
        Config c = new Config();
        Properties p = PropertiesFile.read(file, (f, e) -> BloodFeud.LOG.warn("Couldn't read {}: {}", f, e.toString()));
        c.encounterCooldownSeconds = PropertiesFile.integer(p, "encounterCooldownSeconds", c.encounterCooldownSeconds);
        c.encounterChancePercent = PropertiesFile.integer(p, "encounterChancePercent", c.encounterChancePercent);
        c.spawnSafeRadius = PropertiesFile.integer(p, "spawnSafeRadius", c.spawnSafeRadius);
        c.maxActiveCaptains = PropertiesFile.integer(p, "maxActiveCaptains", c.maxActiveCaptains);
        c.announcePowerShifts = PropertiesFile.bool(p, "announcePowerShifts", c.announcePowerShifts);
        String text = """
                # Blood Feud settings. Delete a line to get its default back.

                # Seconds between Captain encounters for one player, at least.
                encounterCooldownSeconds=%d
                # Chance (percent) of a Captain showing up at each 10-second check once the cooldown is over.
                encounterChancePercent=%d
                # No Captain shows up this close (in blocks) to the world spawn.
                spawnSafeRadius=%d
                # Most Captain mobs in the world at once, all players together.
                maxActiveCaptains=%d
                # Tell every player what the Warband did when power struggles resolve.
                announcePowerShifts=%b
                """.formatted(c.encounterCooldownSeconds, c.encounterChancePercent, c.spawnSafeRadius,
                c.maxActiveCaptains, c.announcePowerShifts);
        PropertiesFile.writeText(file, text, (f, e) -> BloodFeud.LOG.warn("Couldn't write {}: {}", f, e.toString()));
        return c;
    }

}
