package com.example.mcatowns.town;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownEventPresentationTest {
    @Test
    void disastersAreSeparatedFromPositiveTownEvents() {
        assertTrue(TownEventPresentation.isDisaster("fire"));
        assertTrue(TownEventPresentation.isDisaster("disease"));
        assertFalse(TownEventPresentation.isDisaster("good_harvest"));
        assertFalse(TownEventPresentation.isDisaster("guard_training_day"));
    }

    @Test
    void eventIdsHaveReadableNames() {
        assertEquals("Mine Collapse", TownEventPresentation.displayName("mine_collapse"));
        assertEquals("Custom Event", TownEventPresentation.displayName("custom_event"));
        assertEquals("", TownEventPresentation.displayName(""));
    }
}
