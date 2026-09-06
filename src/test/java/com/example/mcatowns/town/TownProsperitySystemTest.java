package com.example.mcatowns.town;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownProsperitySystemTest {
    @Test
    void suppressionReducesOnlyEffectiveBase() {
        assertEquals(40, TownProsperitySystem.effectiveBase(40, 0));
        assertEquals(30, TownProsperitySystem.effectiveBase(40, 10));
    }

    @Test
    void suppressionCannotProduceNegativeBase() {
        assertEquals(0, TownProsperitySystem.effectiveBase(20, 99));
        assertEquals(0, TownProsperitySystem.effectiveBase(-5, 0));
    }

    @Test
    void negativeSuppressionDoesNotIncreaseBase() {
        assertEquals(40, TownProsperitySystem.effectiveBase(40, -10));
    }
}
