package com.example.mcatowns.town;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildingPerformanceTest {
    @Test
    void outputUsesTierWorkersFurnitureAndSynergies() {
        assertEquals(54, BuildingPerformance.outputFromFactors(1, 50, 2, 1));
        assertEquals(130, BuildingPerformance.outputFromFactors(3, 100, 20, 20));
    }

    @Test
    void outputClampsUnsafeInputs() {
        assertEquals(0, BuildingPerformance.outputFromFactors(-5, -20, -2, -3));
        assertEquals(130, BuildingPerformance.outputFromFactors(50, 500, 500, 500));
    }

    @Test
    void productionUsesHalfUpRounding() {
        assertEquals(3, BuildingPerformance.roundOutput(3, 110));
        assertEquals(4, BuildingPerformance.roundOutput(3, 117));
    }
}
