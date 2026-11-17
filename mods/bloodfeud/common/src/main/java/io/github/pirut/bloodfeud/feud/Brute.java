package io.github.pirut.bloodfeud.feud;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** A named mob. It is only a record until a player is near it; then the game spawns it as a real mob. */
public final class Brute {
    public static final int MAX_LEVEL = 20;

    public final UUID id;
    public String entityType;
    public final Family family;
    public String dimension;
    public final String name;
    public String epithet;
    public int level = 1;
    public final Set<Trait> traits = EnumSet.noneOf(Trait.class);
    public final List<Scar> scars = new ArrayList<>();
    /** The pack it belongs to, or null if it is on its own. */
    public UUID pack;
    /** Where it was last seen (or where it is on paper while it travels). */
    public int x, z;
    public boolean alive = true;
    /** Killed in a way it can't come back from. */
    public boolean finalDeath;
    public long diedAt;
    public DeathCause lastCause;
    /** Who last killed it, for the memory it comes back with. */
    public UUID killedBy;
    public int timesReturned;
    public final long risenAt;
    /** A player it is following across the world, or null. */
    public UUID hunting;
    public final Map<UUID, Relation> relations = new HashMap<>();

    public Brute(UUID id, String entityType, Family family, String dimension, String name, long risenAt) {
        this.id = id;
        this.entityType = entityType;
        this.family = family;
        this.dimension = dimension;
        this.name = name;
        this.risenAt = risenAt;
    }

    public String fullName() {
        return epithet == null || epithet.isEmpty() ? name : name + " " + epithet;
    }

    public Relation relation(UUID player) {
        return relations.computeIfAbsent(player, p -> new Relation());
    }

    public boolean isNemesisOf(UUID player) {
        Relation r = relations.get(player);
        return r != null && r.nemesis;
    }

    /** Adds a trait unless it clashes with one it already has. */
    public boolean addTrait(Trait trait) {
        for (Trait t : traits) if (t.conflicts().contains(trait)) return false;
        return traits.add(trait);
    }

    public void levelUp(int by) {
        level = Math.max(1, Math.min(MAX_LEVEL, level + by));
    }

    public int strengths() {
        int n = 0;
        for (Trait t : traits) {
            if (t.kind == Trait.Kind.STRENGTH || t.kind == Trait.Kind.IMMUNITY) n++;
            if (t.kind == Trait.Kind.WEAKNESS) n--;
        }
        return n;
    }

    /** How hard it is in a fight on paper. */
    public double power() {
        return level + 0.5 * strengths();
    }
}
