package io.github.pirut.bloodfeud.warband;

/** How someone died, reduced to what the Warband cares about (names, scars and memories). */
public enum DeathCause {
    MELEE, ARROW, FIRE, FALL, EXPLOSION, DROWNING, BEAST, OTHER;

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public Scar scar() {
        return switch (this) {
            case FIRE -> Scar.BURNED;
            case FALL -> Scar.BROKEN;
            case EXPLOSION -> Scar.BLASTED;
            case ARROW -> Scar.PIERCED;
            case DROWNING -> Scar.DROWNED;
            case BEAST -> Scar.MAULED;
            default -> Scar.CUT;
        };
    }

    public static DeathCause byKey(String key) {
        for (DeathCause c : values()) if (c.key().equals(key)) return c;
        return OTHER;
    }
}
