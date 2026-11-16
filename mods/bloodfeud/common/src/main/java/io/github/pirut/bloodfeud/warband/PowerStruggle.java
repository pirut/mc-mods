package io.github.pirut.bloodfeud.warband;

import java.util.UUID;

/**
 * Something the Warband does on its own time: Captains fighting each other, feasting, hunting, raising followers,
 * or challenging a Warlord. Each one is queued with a time; when that time comes (or a player dies or sleeps
 * past it) it resolves.
 *
 * @param id     a small number, for commands
 * @param type   what kind of struggle
 * @param first  the Captain that starts it
 * @param second the other Captain (Duel, Ambush, Challenge), or null
 * @param due    game time it resolves
 */
public record PowerStruggle(int id, Type type, UUID first, UUID second, long due) {
    public enum Type {
        DUEL("Duel", true), FEAST("Feast", false), HUNT("Hunt", false), RECRUIT("Recruitment", false),
        AMBUSH("Ambush", true), CHALLENGE("Challenge", true);

        public final String title;
        public final boolean versus;

        Type(String title, boolean versus) {
            this.title = title;
            this.versus = versus;
        }

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Type byKey(String key) {
            for (Type t : values()) if (t.key().equals(key)) return t;
            return null;
        }
    }
}
