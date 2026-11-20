package io.github.pirut.bloodfeud.feud;

/**
 * One thing that happened between a Brute and a player, kept so the Brute can bring it up later.
 *
 * @param kind   what happened
 * @param time   game time it happened
 * @param detail a short extra (a damage cause key, an item name), or ""
 */
public record Memory(Kind kind, long time, String detail) {
    public enum Kind {
        /** It killed the player. */
        KILLED_PLAYER,
        /** The player killed it, and it came back. */
        WAS_KILLED,
        /** The player ran from the fight. */
        PLAYER_FLED,
        /** It ran (or withdrew) from the player. */
        IT_FLED,
        /** It took the player's item. */
        STOLE,
        /** It rose to Brute by killing the player. */
        ROSE,
        /** It saw the player kill its own kind and walked away. */
        SAW_KILLS,
        /** It came back from the dead. */
        RETURNED;

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Kind byKey(String key) {
            for (Kind k : values()) if (k.key().equals(key)) return k;
            return null;
        }
    }
}
