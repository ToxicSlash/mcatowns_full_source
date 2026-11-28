package com.example.mcatowns.town;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class FarmInspectionTest {
    @Test
    void allChunksOfSavedFieldMustBeLoadedBeforeInspection() {
        var farm = new RegisteredTownBuilding(UUID.randomUUID(), "farm", 2, new BlockPos(15, 64, 15),
                new BlockPos(14, 64, 14), new BlockPos(17, 65, 17), BuildingStatus.ACTIVE, 100, List.of(), 2, 32);
        assertFalse(TownBuildingService.farmBoundsLoaded(farm, p -> p.getX() < 16));
        assertTrue(TownBuildingService.farmBoundsLoaded(farm, p -> true));
    }
}
