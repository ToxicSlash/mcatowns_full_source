package com.example.mcatowns.town;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildingPerformanceTest {
    @Test
    void outputUsesTierWorkersFurnitureAndSynergies() {
        assertEquals(54, BuildingPerformance.outputFromFactors(1, 50, 2, 1));
        assertEquals(130, BuildingPerformance.outputFromFactors(3, 100, 20, 20));
        assertEquals(80, BuildingPerformance.outputFromFactors(2, 75, 1, 0));
    }

    @Test
    void outputClampsUnsafeInputs() {
        assertEquals(0, BuildingPerformance.outputFromFactors(-5, -20, -2, -3));
        assertEquals(130, BuildingPerformance.outputFromFactors(50, 500, 500, 500));
        assertEquals(0, BuildingPerformance.outputFromFactors(3, 0, 5, 2));
    }

    @Test
    void productionUsesHalfUpRounding() {
        assertEquals(3, BuildingPerformance.roundOutput(3, 110));
        assertEquals(4, BuildingPerformance.roundOutput(3, 117));
        assertEquals(11, BuildingPerformance.roundOutput(10, 105));
        assertEquals(4, BuildingPerformance.roundOutput(7, 50));
        assertEquals(0, BuildingPerformance.roundOutput(-10, 150));
    }
}
