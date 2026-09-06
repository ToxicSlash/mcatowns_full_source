package com.example.mcatowns.town;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TownResidentDetailsTest {
    @Test
    void unloadedResidentDetailsStaySafeAndReadable() {
        assertFalse(TownResidentDetails.isGuard(null));
        assertEquals(-1, TownResidentDetails.happiness(null));
        assertEquals("Blacksmith", TownResidentDetails.occupation(null, "blacksmith"));
        assertEquals("Resident", TownResidentDetails.occupation(null, ""));
        assertEquals("Idle", TownResidentDetails.status(null, null));
    }
}
