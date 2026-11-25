package io.github.pirut.bloodfeud;

import io.github.pirut.mcmods.lib.PropertiesFile;

import java.nio.file.Path;
import java.util.Properties;

/** config/bloodfeud.properties. Read at start and updated when toggled in game. */
public final class Config {
    /** Kills of its kind a mob has to see (and live through) to become a Brute. */
    public int killsToRise = 3;
    /** Seconds after a mob rises because of a player before another can rise because of the same player. */
    public int riseCooldownSeconds = 90;
    /** Seconds between one player's encounters with Brutes, at least. */
    public int encounterCooldownSeconds = 150;
    /** Chance (percent) a lone Brute nearby shows up at each 10-second check once the cooldown is over. */
    public int encounterChancePercent = 25;
    /** No Brute shows up this close to the world spawn. */
    public int spawnSafeRadius = 48;
    /** Most Brute mobs in the world at once, all players together. */
    public int maxActiveBrutes = 12;
    /** Most packs in a world. When there are more, the oldest far-away pack no one has met fades. */
    public int maxPacks = 64;
    /** Tell players what the packs near them did each night. */
    public boolean announceNews = true;
    /** Hand each player a Feud Ledger the first time a Brute finds them. */
    public boolean giveLedger = true;
    /** A Brute that kills you takes your weapon and armor, fights with them, and drops them when it dies. */
    public boolean takeGear = true;
    /** Packs automatically build camps that grow into outposts, strongholds and towns as they grow stronger. */
    public boolean buildCamps = true;
    /** Blocks each builder places a minute. A crew of three at 60 puts up about three blocks a second. */
    public int campBlocksPerMinute = 60;

    private Path file;

    static Config load(Path file) {
        Config c = new Config();
        c.file = file;
        Properties p = PropertiesFile.read(file, (f, e) -> BloodFeud.LOG.warn("Couldn't read {}: {}", f, e.toString()));
        c.killsToRise = Math.max(1, PropertiesFile.integer(p, "killsToRise", c.killsToRise));
        c.riseCooldownSeconds = Math.max(0, PropertiesFile.integer(p, "riseCooldownSeconds", c.riseCooldownSeconds));
        c.encounterCooldownSeconds = PropertiesFile.integer(p, "encounterCooldownSeconds", c.encounterCooldownSeconds);
        c.encounterChancePercent = PropertiesFile.integer(p, "encounterChancePercent", c.encounterChancePercent);
        c.spawnSafeRadius = PropertiesFile.integer(p, "spawnSafeRadius", c.spawnSafeRadius);
        c.maxActiveBrutes = PropertiesFile.integer(p, "maxActiveBrutes", c.maxActiveBrutes);
        c.maxPacks = Math.max(1, PropertiesFile.integer(p, "maxPacks", c.maxPacks));
        c.announceNews = PropertiesFile.bool(p, "announceNews", c.announceNews);
        c.giveLedger = PropertiesFile.bool(p, "giveLedger", c.giveLedger);
        c.takeGear = PropertiesFile.bool(p, "takeGear", c.takeGear);
        c.buildCamps = PropertiesFile.bool(p, "buildCamps", c.buildCamps);
        c.campBlocksPerMinute = Math.max(1, Math.min(1200, PropertiesFile.integer(p, "campBlocksPerMinute", c.campBlocksPerMinute)));
        c.save();
        return c;
    }

    /** Writes current settings back to config/bloodfeud.properties. */
    public void save() {
        if (file == null) return;
        String text = """
                # Blood Feud settings. Delete a line to get its default back.

                # Kills of its kind a mob has to see (and live through) to become a Brute.
                killsToRise=%d
                # Seconds after a mob rises because of you before another can (keeps busy nights from flooding the world).
                riseCooldownSeconds=%d
                # Seconds between one player's encounters with Brutes, at least.
                encounterCooldownSeconds=%d
                # Chance (percent) a lone Brute nearby shows up at each 10-second check once the cooldown is over.
                encounterChancePercent=%d
                # No Brute shows up this close (in blocks) to the world spawn.
                spawnSafeRadius=%d
                # Most Brute mobs in the world at once, all players together.
                maxActiveBrutes=%d
                # Most packs in a world. When there are more, the oldest far-away pack no one has met fades.
                maxPacks=%d
                # Tell players what the packs near them did each night.
                announceNews=%b
                # Hand each player a Feud Ledger (a book about the packs they know) the first time a Brute finds them.
                giveLedger=%b
                # A Brute that kills you takes your best weapon and your armor, fights with them, and drops them when you kill it.
                # (Not with the keepInventory game rule.)
                takeGear=%b
                # Packs build camps that grow into outposts, strongholds and towns as they grow stronger.
                buildCamps=%b
                # Pack mobs build their settlements in front of you (out of sight, they're built off screen).
                # Blocks each builder places a minute (1 to 1200).
                campBlocksPerMinute=%d
                """.formatted(killsToRise, riseCooldownSeconds, encounterCooldownSeconds, encounterChancePercent, spawnSafeRadius,
                maxActiveBrutes, maxPacks, announceNews, giveLedger, takeGear, buildCamps, campBlocksPerMinute);
        PropertiesFile.writeText(file, text, (f, e) -> BloodFeud.LOG.warn("Couldn't write {}: {}", f, e.toString()));
    }
}
