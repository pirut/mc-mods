package io.github.pirut.bloodfeud.feud;

import java.util.List;
import java.util.UUID;

/**
 * Something that happened on paper, for the players it matters to.
 *
 * @param kind     what sort of thing it was
 * @param text     what to tell players
 * @param dimension where it happened
 * @param x        roughly where (x)
 * @param z        roughly where (z)
 * @param brutes   the Brutes it is about
 */
public record News(Kind kind, String text, String dimension, int x, int z, List<UUID> brutes) {
    public enum Kind { ROSE, FORMED, JOINED, GREW, SETTLED, CHALLENGE, RAID, SWALLOWED, CROWNED, SPLIT, RETURNED, HUNTING, FADED }
}
