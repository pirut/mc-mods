package io.github.pirut.bloodfeud.warband;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Set;

/** What one Captain and one player have between them. */
public final class Relation {
    public static final int MAX_MEMORIES = 32;

    /** The Captain has killed this player and has it in for them. */
    public boolean nemesis;
    /** Traits this player has found out about. */
    public final Set<Trait> known = EnumSet.noneOf(Trait.class);
    /** Whether this player knows the Captain exists (has met it or been told about it). */
    public boolean met;
    public int killedPlayer;
    public int killedByPlayer;
    public final Deque<Memory> memories = new ArrayDeque<>();

    public void remember(Memory memory) {
        memories.addLast(memory);
        while (memories.size() > MAX_MEMORIES) memories.removeFirst();
    }

    public Memory last() {
        return memories.peekLast();
    }
}
