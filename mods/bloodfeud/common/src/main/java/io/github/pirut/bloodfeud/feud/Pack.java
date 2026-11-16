package io.github.pirut.bloodfeud.feud;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A group of mobs of one family that formed around a Boss, with a home it stays near. */
public final class Pack {
    public final UUID id;
    public final Family family;
    public final String dimension;
    /** The place it formed, for its name: "Mossy Ravine" makes "the Mossy Ravine Pack". */
    public final String place;
    /** Dye color index (0 to 15), worn by every member. */
    public final int color;
    public final int homeX, homeZ;
    public UUID boss;
    /** The King's pack this one kneels to, or null. */
    public UUID liege;
    /** Plain mobs that run with it, on paper. */
    public int grunts;
    public final long formedAt;
    /** The last time it had a night with a player nearby. */
    public long lastAwake;
    public int playerKills;
    /** Players who have seen it. */
    public final Set<UUID> met = new HashSet<>();

    public Pack(UUID id, Family family, String dimension, String place, int color, int homeX, int homeZ, long formedAt) {
        this.id = id;
        this.family = family;
        this.dimension = dimension;
        this.place = place;
        this.color = color;
        this.homeX = homeX;
        this.homeZ = homeZ;
        this.formedAt = formedAt;
        this.lastAwake = formedAt;
    }

    public String title() {
        return "the " + place + " Pack";
    }

    public double distanceTo(String dim, double x, double z) {
        if (!dimension.equals(dim)) return Double.POSITIVE_INFINITY;
        return Math.hypot(homeX - x, homeZ - z);
    }

    public double distanceTo(Pack other) {
        return distanceTo(other.dimension, other.homeX, other.homeZ);
    }
}
