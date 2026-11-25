package io.github.pirut.bloodfeud.game;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mobs waiting to rise because they watched a player kill their kind, and the rule that a mob rises only once.
 * A mark is used up only when its mob rises or never can; while it can't rise yet (it isn't loaded, or too many
 * Brutes are out) the mark waits, for up to {@link #EXPIRE} ticks.
 */
final class Rises {
    /** Ticks between seeing the kills and rising. */
    static final long DELAY = 200;
    /** A mark that still can't rise this long after it was made is dropped. */
    static final long EXPIRE = 20L * 120;

    /** Where the player who did the killing stood, and when. */
    record Mark(ResourceKey<Level> dimension, UUID player, long at, UUID pack, double witnessX, double witnessZ) {
    }

    /** What happened when a mature mark's mob was asked to rise. */
    enum Outcome { ROSE, NOT_YET, NEVER }

    interface Riser {
        Outcome rise(UUID mob, Mark mark);
    }

    private final Map<UUID, Mark> marked = new HashMap<>();

    /** Marks a mob to rise, unless it already is. */
    void mark(UUID mob, Mark mark) {
        marked.putIfAbsent(mob, mark);
    }

    boolean isMarked(UUID mob) {
        return marked.containsKey(mob);
    }

    /** Whether one of this player's kills already has a mob waiting to rise. */
    boolean hasMarkFor(UUID player) {
        for (Mark m : marked.values()) if (m.player().equals(player)) return true;
        return false;
    }

    /** The mob rose (by any path) or died: it has nothing left to rise for. */
    void forget(UUID mob) {
        marked.remove(mob);
    }

    void clear() {
        marked.clear();
    }

    int size() {
        return marked.size();
    }

    /** Asks every mature mark's mob to rise. */
    void tick(long gameTime, Riser riser) {
        for (Map.Entry<UUID, Mark> e : new HashMap<>(marked).entrySet()) {
            Mark m = e.getValue();
            if (gameTime - m.at() < DELAY) continue;
            Outcome o = riser.rise(e.getKey(), m);
            if (o != Outcome.NOT_YET || gameTime - m.at() > EXPIRE) marked.remove(e.getKey());
        }
    }

    /**
     * Whether this mob may become a Brute now: it is alive and isn't one already, by the live map or by its tag.
     * Every path that makes a Brute out of a mob asks this first, so one mob never makes two Brute records.
     */
    static boolean canRise(Mob mob, Map<UUID, UUID> bruteOfEntity) {
        return mob.isAlive() && !mob.isRemoved() && !bruteOfEntity.containsKey(mob.getUUID())
                && !mob.getTags().contains(Live.TAG_BRUTE);
    }
}
