package io.github.pirut.bloodfeud.warband;

/** How a Captain nearly died, carried for the rest of its life when it comes back. */
public enum Scar {
    BURNED("the Charred", Trait.FIREPROOF, Trait.FEARS_FIRE),
    BROKEN("Broke-Leg", Trait.UNSHAKABLE, null),
    BLASTED("Half-Blown", Trait.UNSHAKABLE, null),
    PIERCED("Pin-Cushion", Trait.ARROWPROOF, null),
    DROWNED("the Twice-Drowned", null, null),
    CUT("Stitch-Face", Trait.WATCHFUL, null),
    MAULED("Wolf-Chewed", null, Trait.FEARS_WOLVES);

    /** The epithet a Captain may take when it comes back with this scar. */
    public final String epithet;
    /** What it may learn from it (hardened), or null. */
    public final Trait hardened;
    /** What it may come to fear (haunted), or null. */
    public final Trait haunted;

    Scar(String epithet, Trait hardened, Trait haunted) {
        this.epithet = epithet;
        this.hardened = hardened;
        this.haunted = haunted;
    }

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Scar byKey(String key) {
        for (Scar s : values()) if (s.key().equals(key)) return s;
        return null;
    }
}
