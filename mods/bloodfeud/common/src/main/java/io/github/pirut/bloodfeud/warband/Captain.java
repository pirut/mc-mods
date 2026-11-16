package io.github.pirut.bloodfeud.warband;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** One named member of the Warband: a Captain, a Sworn Guard or a Warlord. Plain data; the Board moves it around. */
public final class Captain {
    public static final int MAX_LEVEL = 20;

    public final UUID id;
    /** The mob it is, as a registry id such as "minecraft:zombie". */
    public String entityType;
    /** The dimension it lives in, as a registry id. */
    public String dimension;
    public String name;
    public String epithet;
    public int level;
    public final Set<Trait> traits = EnumSet.noneOf(Trait.class);
    public final List<Scar> scars = new ArrayList<>();
    /** Territory it roams (0..4). */
    public int territory;
    /** The Warlord slot it guards, or -1. */
    public int guarding = -1;
    /** The Warlord slot it holds, or -1. */
    public int warlordOf = -1;
    public boolean alive = true;
    /** Dead, and the kill was final: it will never come back. */
    public boolean finalDeath;
    public long diedAt;
    /** How it last died, for the scar it comes back with. */
    public DeathCause lastCause = DeathCause.OTHER;
    public int timesReturned;
    /** Game time it rose. */
    public long risenAt;
    public final Map<UUID, Relation> relations = new HashMap<>();

    public Captain(UUID id) {
        this.id = id;
    }

    public String fullName() {
        return epithet == null || epithet.isEmpty() ? name : name + " " + epithet;
    }

    public boolean isWarlord() {
        return warlordOf >= 0;
    }

    public boolean isGuard() {
        return guarding >= 0;
    }

    public boolean has(Trait trait) {
        return traits.contains(trait);
    }

    public Relation relation(UUID player) {
        return relations.computeIfAbsent(player, p -> new Relation());
    }

    public Relation relationOrNull(UUID player) {
        return relations.get(player);
    }

    public boolean isNemesisOf(UUID player) {
        Relation r = relations.get(player);
        return r != null && r.nemesis;
    }

    public void levelUp(int by) {
        level = Math.max(1, Math.min(MAX_LEVEL, level + by));
    }

    /** Adds a trait if nothing it already has conflicts with it. Returns whether it was added. */
    public boolean addTrait(Trait trait) {
        if (traits.contains(trait)) return false;
        for (Trait c : trait.conflicts()) if (traits.contains(c)) return false;
        traits.add(trait);
        return true;
    }

    public String rankTitle() {
        if (isWarlord()) return "Warlord";
        if (isGuard()) return "Sworn Guard";
        return "Captain";
    }
}
