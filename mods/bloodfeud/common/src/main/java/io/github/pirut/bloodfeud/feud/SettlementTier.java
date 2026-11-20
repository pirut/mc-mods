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

    public String title(boolean ruined) {
        if (this == NONE) return title;
        return ruined ? "Ruined " + title : title;
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

    /**
     * The highest settlement tier a pack currently sustains based on its living roster, grunts, strength and rank.
     * Returns {@link #NONE} when the pack drops below even the Camp threshold (no living Brutes or fewer than 2 grunts
     * without King status), causing a Camp to fall into ruin.
     */
    public static SettlementTier forPack(int livingMembers, int grunts, double strength, boolean king) {
        if (livingMembers <= 0) return NONE;
        if (king) return TOWN;
        if (grunts <= 0) return NONE;
        if ((livingMembers >= 5 && grunts >= 18) || strength >= 46) return TOWN;
        if ((livingMembers >= 4 && grunts >= 12) || strength >= 30) return STRONGHOLD;
        if ((livingMembers >= 2 && grunts >= 6) || strength >= 16) return OUTPOST;
        if (grunts >= 2) return CAMP;
        return NONE;
    }

    /**
     * Lower hysteresis floor for keeping an intact settlement of this tier from falling into ruin.
     * Reaching or rebuilding a tier still requires the full {@link #forPack} threshold, preventing
     * single-kill collapses and night-to-night ruin/repair oscillation.
     */
    public boolean sustains(int livingMembers, int grunts, double strength, boolean king) {
        if (this == NONE) return true;
        if (livingMembers <= 0) return false;
        if (king) return true;
        if (grunts <= 0) return false;
        return switch (this) {
            case NONE -> true;
            case CAMP -> grunts >= 1;
            case OUTPOST -> (livingMembers >= 2 && grunts >= 4) || strength >= 13;
            case STRONGHOLD -> (livingMembers >= 3 && grunts >= 9) || strength >= 25;
            case TOWN -> (livingMembers >= 4 && grunts >= 14) || strength >= 38;
        };
    }
}
