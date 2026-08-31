package com.example.mcatowns.town;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildingPerformanceTest {
    @Test
    void qualityUsesTierWorkersFurnitureAndSynergies() {
        assertEquals(42, BuildingPerformance.qualityFromFactors(1, 50, 2, 1));
        assertEquals(100, BuildingPerformance.qualityFromFactors(3, 100, 20, 20));
    }

    @Test
    void qualityClampsUnsafeInputs() {
        assertEquals(12, BuildingPerformance.qualityFromFactors(-5, -20, -2, -3));
        assertEquals(100, BuildingPerformance.qualityFromFactors(50, 500, 500, 500));
    }
}
