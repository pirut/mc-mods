package io.bugcourier.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** One report per mod per interval, so a stuck key or an impatient player can't flood a tracker. */
public final class Cooldown {
    private final Clock clock;
    private final Duration interval;
    private final Map<String, Instant> last = new HashMap<>();

    public Cooldown(Clock clock, Duration interval) {
        this.clock = clock;
        this.interval = interval;
    }

    /** Zero when a report for this mod may go now. */
    public synchronized Duration remaining(String modId) {
        Instant prev = last.get(modId);
        if (prev == null) return Duration.ZERO;
        Duration left = interval.minus(Duration.between(prev, clock.instant()));
        return left.isNegative() ? Duration.ZERO : left;
    }

    public synchronized void mark(String modId) {
        last.put(modId, clock.instant());
    }
}
