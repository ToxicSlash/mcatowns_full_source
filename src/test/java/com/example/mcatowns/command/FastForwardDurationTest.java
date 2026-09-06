package com.example.mcatowns.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FastForwardDurationTest {
    @Test
    void parsesDaysAndTicks() {
        assertEquals(240_000L, FastForwardDuration.parseTicks("10d"));
        assertEquals(24_000L, FastForwardDuration.parseTicks("24000t"));
        assertEquals(48_000L, FastForwardDuration.parseTicks("2D"));
    }

    @Test
    void rejectsInvalidAndUnsafeDurations() {
        assertThrows(IllegalArgumentException.class, () -> FastForwardDuration.parseTicks("0d"));
        assertThrows(IllegalArgumentException.class, () -> FastForwardDuration.parseTicks("1h"));
        assertThrows(IllegalArgumentException.class, () -> FastForwardDuration.parseTicks("1.5d"));
        assertThrows(IllegalArgumentException.class, () -> FastForwardDuration.parseTicks("366d"));
    }

    @Test
    void formatsWholeDaysOtherwiseTicks() {
        assertEquals("10d", FastForwardDuration.display(240_000L));
        assertEquals("12000t", FastForwardDuration.display(12_000L));
    }
}
