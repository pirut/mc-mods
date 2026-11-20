package io.github.pirut.bloodfeud.feud;

import java.util.Locale;

/** How far a pack's camp has grown: from a rough camp in the wild to a full gang-run town. */
public enum SettlementTier {
    NONE(0, "none", "None", 0, 0, 0),
    CAMP(1, "camp", "Camp", 7, 1, 4),
    OUTPOST(2, "outpost", "Outpost", 13, 2, 7),
    STRONGHOLD(3, "stronghold", "Stronghold", 20, 4, 13),
    TOWN(4, "town", "Town", 29, 5, 18);

    public final int level;
    public final String key;
    public final String title;
    public final int radius;
    public final int minBrutes;
    public final int minGrunts;

    SettlementTier(int level, String key, String title, int radius, int minBrutes, int minGrunts) {
        this.level = level;
        this.key = key;
        this.title = title;
        this.radius = radius;
        this.minBrutes = minBrutes;
        this.minGrunts = minGrunts;
    }

    public static SettlementTier byKey(String key) {
        if (key == null) return null;
        String k = key.toLowerCase(Locale.ROOT);
        for (SettlementTier t : values()) {
            if (t.key.equals(k)) return t;
        }
        return null;
    }

    public static SettlementTier byLevel(int level) {
        if (level <= 0) return NONE;
        if (level == 1) return CAMP;
        if (level == 2) return OUTPOST;
        if (level == 3) return STRONGHOLD;
        return TOWN;
    }

    public SettlementTier next() {
        return switch (this) {
            case NONE -> CAMP;
            case CAMP -> OUTPOST;
            case OUTPOST -> STRONGHOLD;
            case STRONGHOLD, TOWN -> TOWN;
        };
    }

    /** The settlement tier a pack qualifies for based on its living roster, strength and rank. */
    public static SettlementTier forPack(int livingMembers, int grunts, double strength, boolean king) {
        if (king || (livingMembers >= 5 && grunts >= 18) || strength >= 46) return TOWN;
        if ((livingMembers >= 4 && grunts >= 12) || strength >= 30) return STRONGHOLD;
        if ((livingMembers >= 2 && grunts >= 6) || strength >= 16) return OUTPOST;
        return CAMP;
    }
}
