package io.github.pirut.bloodfeud.feud;

/** Mobs only band together with their own kind. Which mob belongs to which family comes from entity tags. */
public enum Family {
    ROTTEN("the Rotten", "minecraft:zombie"),
    BONES("the Bones", "minecraft:skeleton"),
    ILLAGERS("the Illagers", "minecraft:vindicator"),
    NETHER("the Nether", "minecraft:wither_skeleton"),
    SPIDERS("the Spiders", "minecraft:spider"),
    WITCHES("the Witches", "minecraft:witch");

    public final String title;
    /** What a new Brute of this family is when nothing says otherwise (a command, a pack's first Brute). */
    public final String defaultType;

    Family(String title, String defaultType) {
        this.title = title;
        this.defaultType = defaultType;
    }

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Family byKey(String key) {
        for (Family f : values()) if (f.key().equals(key)) return f;
        return null;
    }
}
