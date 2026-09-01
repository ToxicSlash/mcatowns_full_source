package com.example.mcatowns.town;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefinitionSanityTest {
    @Test
    void buildingAndResearchIdsAreUniqueAndResolvable() {
        Set<String> buildingIds = new HashSet<>();
        for (TownBuildingDefinition building : TownBuildingDefinition.ALL) {
            assertTrue(buildingIds.add(building.id()), "Duplicate building id: " + building.id());
            assertNotNull(TownBuildingDefinition.get(building.id()), "Building lookup failed: " + building.id());
        }

        Set<String> researchIds = new HashSet<>();
        for (TownResearchDefinition research : TownResearchDefinition.ALL) {
            assertTrue(researchIds.add(research.id()), "Duplicate research id: " + research.id());
            assertNotNull(TownResearchDefinition.get(research.id()), "Research lookup failed: " + research.id());
            assertNotNull(TownBuildingDefinition.get(research.buildingId()),
                    "Research references missing building: " + research.buildingId());
        }
    }

    @Test
    void everySpecialistHasARegisteredWorkplace() {
        for (SpecialistType specialist : SpecialistType.values()) {
            assertNotNull(TownBuildingDefinition.get(specialist.workplace()),
                    specialist.id() + " references missing workplace " + specialist.workplace());
        }
    }
}
