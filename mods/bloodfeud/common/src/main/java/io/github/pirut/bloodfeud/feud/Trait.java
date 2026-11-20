package io.github.pirut.bloodfeud.feud;

import java.util.EnumSet;
import java.util.Set;

/**
 * What makes one Brute different from the next: what it can't be hurt by, what it is good at, what kills it fast,
 * and what it runs from. A Brute rolls a few of each when it rises, and can gain or lose them as it goes.
 */
public enum Trait {
    // Immunities
    ARROWPROOF(Kind.IMMUNITY, "Arrowproof", "Arrows and other projectiles do nothing to it."),
    FIREPROOF(Kind.IMMUNITY, "Fireproof", "Fire and lava do nothing to it."),
    UNSHAKABLE(Kind.IMMUNITY, "Unshakable", "It can't be knocked back."),
    WATCHFUL(Kind.IMMUNITY, "Watchful", "It can't be killed by a sneak attack."),
    DEATHLESS(Kind.IMMUNITY, "Deathless", "No killing blow is ever final: it always has a chance to come back."),

    // Strengths
    VENOM_BLADE(Kind.STRENGTH, "Venom Blade", "Its hits poison."),
    BONEBREAKER(Kind.STRENGTH, "Bonebreaker", "Its hits slow you down."),
    THIEF(Kind.STRENGTH, "Thief", "Its hits can snatch the item in your hand."),
    BLINK(Kind.STRENGTH, "Blink", "When hit, it may vanish and reappear nearby."),
    BERSERKER(Kind.STRENGTH, "Berserker", "It hits harder when badly hurt."),
    REGENERATING(Kind.STRENGTH, "Regenerating", "Its wounds close by themselves."),
    PACKCALLER(Kind.STRENGTH, "Packcaller", "It arrives with a pack of its kind."),
    SHIELDBEARER(Kind.STRENGTH, "Shieldbearer", "Blows from the front do half damage."),
    NIGHT_STALKER(Kind.STRENGTH, "Night Stalker", "In the dark it is hard to see until it is close."),

    // Weaknesses
    ARROW_WEAK(Kind.WEAKNESS, "Headshot", "An arrow to the head kills it outright."),
    SNEAK_WEAK(Kind.WEAKNESS, "Unguarded", "A sneak attack from behind kills it outright."),
    BEAST_WEAK(Kind.WEAKNESS, "Beast-Bait", "Wolves do triple damage to it."),
    SMITE_WEAK(Kind.WEAKNESS, "Hallowed Bane", "Smite and holy damage hurt it twice as much."),
    GLASS_JAW(Kind.WEAKNESS, "Glass Jaw", "Critical hits do double damage."),

    // Fears
    FEARS_FIRE(Kind.FEAR, "Fears Fire", "It runs from fire, and from torches in your hand."),
    FEARS_WOLVES(Kind.FEAR, "Fears Wolves", "It runs from wolves."),
    FEARS_CATS(Kind.FEAR, "Fears Cats", "It runs from cats."),
    FEARS_BELLS(Kind.FEAR, "Fears Bells", "It runs when it hears a bell.");

    public enum Kind { IMMUNITY, STRENGTH, WEAKNESS, FEAR }

    public final Kind kind;
    public final String title;
    public final String description;

    Trait(Kind kind, String title, String description) {
        this.kind = kind;
        this.title = title;
        this.description = description;
    }

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Traits that can't be held together with this one. */
    public Set<Trait> conflicts() {
        return switch (this) {
            case ARROWPROOF -> EnumSet.of(ARROW_WEAK);
            case ARROW_WEAK -> EnumSet.of(ARROWPROOF);
            case FIREPROOF -> EnumSet.of(FEARS_FIRE);
            case FEARS_FIRE -> EnumSet.of(FIREPROOF);
            case WATCHFUL -> EnumSet.of(SNEAK_WEAK);
            case SNEAK_WEAK -> EnumSet.of(WATCHFUL);
            case FEARS_WOLVES -> EnumSet.of(BEAST_WEAK);
            case BEAST_WEAK -> EnumSet.of(FEARS_WOLVES);
            default -> EnumSet.noneOf(Trait.class);
        };
    }

    public static Trait byKey(String key) {
        for (Trait t : values()) if (t.key().equals(key)) return t;
        return null;
    }
}
