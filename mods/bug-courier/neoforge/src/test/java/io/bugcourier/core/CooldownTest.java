package io.bugcourier.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CooldownTest {
    @Test
    void waitsPerMod() {
        Instant t0 = Instant.parse("2026-10-25T23:00:00Z");
        Cooldown c = new Cooldown(Clock.fixed(t0, ZoneOffset.UTC), Duration.ofMinutes(5));
        c.mark("earthplatform");
        assertEquals(Duration.ofMinutes(5), c.remaining("earthplatform"));
        assertTrue(c.remaining("sodium").isZero());
    }
}
