package io.github.pirut.bloodfeud.feud;

/** Where a named mob stands. Plain mobs (grunts) have no record at all. */
public enum Rank {
    /** A named mob with a level, traits and a memory of you. */
    BRUTE("Brute"),
    /** A Brute that leads a pack. */
    BOSS("Boss"),
    /** A Boss whose pack has swallowed other packs of its family. */
    KING("King");

    public final String title;

    Rank(String title) {
        this.title = title;
    }
}
