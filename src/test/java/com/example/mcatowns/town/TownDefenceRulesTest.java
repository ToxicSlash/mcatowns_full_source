package com.example.mcatowns.town;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownDefenceRulesTest {
    @Test
    void barracksCapacityScalesSixPerTier() {
        TownBuildingDefinition barracks = TownBuildingDefinition.get("guard_post");
        assertEquals(6, barracks.guardCapacityForTier(1));
        assertEquals(12, barracks.guardCapacityForTier(2));
        assertEquals(18, barracks.guardCapacityForTier(3));
        assertEquals(3, barracks.maxPerTown());
    }

    @Test
    void defenceBuildingCapsMatchApprovedLimits() {
        assertEquals(3, TownDefenceInfrastructure.maxPerTown("guard_post"));
        assertEquals(3, TownDefenceInfrastructure.maxPerTown("barracks"));
        assertEquals(10, TownDefenceInfrastructure.maxPerTown("watchtower"));
        assertEquals(10, TownDefenceInfrastructure.maxPerTown("outpost"));
    }

    @Test
    void residencesGainFourSlotsAtTierTwo() {
        TownBuildingDefinition residence = TownBuildingDefinition.get("residence");
        assertEquals(2, residence.populationCapacityForTier(1));
        assertEquals(4, residence.populationCapacityForTier(2));
        assertEquals(4, residence.populationCapacityForTier(3));
    }
}
