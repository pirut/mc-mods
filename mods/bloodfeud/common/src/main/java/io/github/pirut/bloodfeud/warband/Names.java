package io.github.pirut.bloodfeud.warband;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Names and epithets for Captains. All of it written for this mod. */
public final class Names {
    private Names() {
    }

    private static final String[] FIRST = {
            "Grak", "Mol", "Durz", "Skeg", "Bram", "Vosk", "Thul", "Kreb", "Ozz", "Rusk", "Gorm", "Nib", "Hask",
            "Ulg", "Zab", "Prug", "Feld", "Krot", "Mard", "Shug", "Brisk", "Dolt", "Fen", "Grub", "Hob", "Jask",
            "Klem", "Lurk", "Murt", "Nog", "Pell", "Quill", "Rattle", "Snag", "Tusk", "Vell", "Wort", "Yorl",
            "Clod", "Dreg", "Flint", "Grist", "Husk", "Knob", "Lump", "Moss", "Pock", "Scab", "Slag", "Thorn"};
    private static final String[] LAST = {"", "", "", "", "ok", "ug", "ash", "rik", "bo", "nar", "gul", "et", "ard",
            "wick", "en", "o", "le", "y", "um", "ith"};

    private static final String[] GENERAL = {
            "the Gravel-Eater", "Torchbane", "the Bedbreaker", "Cobble-Tooth", "the Unburied", "Moss-Belly",
            "the Pit-Digger", "Rot-Hand", "the Lantern-Snuffer", "Creeper-Kisser", "the Cave Lord", "Mud-Mouth",
            "the Fence-Climber", "Door-Breaker", "the Quiet", "the Loud", "Dirt-Nap", "the Hungry", "Ore-Thief",
            "the Grinning", "Two-Boots", "the Wheat-Trampler", "Bucket-Head", "the Last Light", "Iron-Jaw",
            "the Patient", "Flint-Eye", "the Sour", "Hollow-Chest", "the Collector"};

    private static final Map<DeathCause, String[]> BY_CAUSE = Map.of(
            DeathCause.ARROW, new String[]{"Long-Shot", "the Fletcher", "Two-Arrows", "Bowstring"},
            DeathCause.FALL, new String[]{"the Shover", "Cliff-Pusher", "the Ledge"},
            DeathCause.DROWNING, new String[]{"the Drowner", "Deep-Hand", "the Undertow"},
            DeathCause.FIRE, new String[]{"the Kindler", "Ember-Hand", "Hearth-Wrecker"},
            DeathCause.EXPLOSION, new String[]{"Boomhand", "the Blaster", "Crater-Maker"},
            DeathCause.MELEE, new String[]{"Bone-Knocker", "the Butcher-Bird", "Heavy-Hand", "the Gutter"},
            DeathCause.BEAST, new String[]{"the Kennel-Master", "Pack-Lord"});

    private static final Map<String, String[]> BY_TYPE = Map.ofEntries(
            Map.entry("minecraft:drowned", new String[]{"of the Deep", "Kelp-Beard", "the Waterlogged"}),
            Map.entry("minecraft:husk", new String[]{"Sand-Skin", "the Dry", "Dune-Walker"}),
            Map.entry("minecraft:stray", new String[]{"Frost-Rattle", "the Pale", "Snow-Bones"}),
            Map.entry("minecraft:wither_skeleton", new String[]{"Coal-Bones", "the Black", "Soot-Grin"}),
            Map.entry("minecraft:piglin_brute", new String[]{"Gold-Fist", "the Tusked", "Bastion-Born"}),
            Map.entry("minecraft:pillager", new String[]{"Bolt-Caller", "the Raider", "Banner-Bearer"}),
            Map.entry("minecraft:vindicator", new String[]{"Axe-Happy", "the Mansion-Born", "Door-Splitter"}),
            Map.entry("minecraft:bogged", new String[]{"Swamp-Rot", "the Mushroomed", "Bog-Bones"}),
            Map.entry("minecraft:zombie_villager", new String[]{"the Ex-Farmer", "No-Trade", "the Unemployed"}),
            Map.entry("minecraft:skeleton", new String[]{"Rib-Rattler", "the Bleached", "Dry-Bones"}),
            Map.entry("minecraft:zombie", new String[]{"the Shambler", "Rot-Gut", "Green-Teeth"}));

    public static String given(Random random, Set<String> taken) {
        for (int i = 0; i < 50; i++) {
            String n = FIRST[random.nextInt(FIRST.length)] + LAST[random.nextInt(LAST.length)];
            if (!taken.contains(n)) return n;
        }
        return FIRST[random.nextInt(FIRST.length)] + (taken.size() + 1);
    }

    /** An epithet, preferring one about how it won (if it killed someone) or what it is. */
    public static String epithet(Random random, String entityType, DeathCause cause) {
        if (cause != null && BY_CAUSE.containsKey(cause) && random.nextInt(3) < 2) return pick(random, BY_CAUSE.get(cause));
        String[] typed = BY_TYPE.get(entityType);
        if (typed != null && random.nextInt(3) == 0) return pick(random, typed);
        return pick(random, GENERAL);
    }

    public static List<String> generalEpithets() {
        return List.of(GENERAL);
    }

    private static String pick(Random random, String[] from) {
        return from[random.nextInt(from.length)];
    }
}
