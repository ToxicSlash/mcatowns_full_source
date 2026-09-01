package com.example.mcatowns.town;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TownWorkforceSystemTest {
    @Test
    void aFailedMoveToAFullWorkplaceKeepsTheExistingAssignment() {
        TownSavedData data = TownSavedData.create();
        data.setProgressionCapacities(2, 0);
        UUID firstResident = UUID.randomUUID();
        UUID secondResident = UUID.randomUUID();
        data.addResident(firstResident);
        data.addResident(secondResident);

        RegisteredTownBuilding firstFarm = building("farm", new BlockPos(1, 64, 1));
        RegisteredTownBuilding secondFarm = building("farm", new BlockPos(20, 64, 20));
        data.registerBuilding(firstFarm);
        data.registerBuilding(secondFarm);
        assertEquals(TownWorkforceSystem.AssignmentResult.ASSIGNED,
                TownWorkforceSystem.assign(data, firstResident, firstFarm.id()));
        assertEquals(TownWorkforceSystem.AssignmentResult.ASSIGNED,
                TownWorkforceSystem.assign(data, secondResident, secondFarm.id()));

        assertEquals(TownWorkforceSystem.AssignmentResult.BUILDING_FULL,
                TownWorkforceSystem.assign(data, firstResident, secondFarm.id()));
        assertEquals(firstFarm.id(), TownWorkforceSystem.assignedBuilding(data, firstResident).id());
    }

    @Test
    void separateTownStatesDoNotLeakWorkerAssignments() {
        TownSavedData firstTown = TownSavedData.create();
        TownSavedData secondTown = TownSavedData.create();
        firstTown.setProgressionCapacities(2, 0);
        secondTown.setProgressionCapacities(2, 0);

        UUID firstResident = UUID.randomUUID();
        UUID secondResident = UUID.randomUUID();
        firstTown.addResident(firstResident);
        secondTown.addResident(secondResident);

        RegisteredTownBuilding firstFarm = building("farm", new BlockPos(1, 64, 1));
        RegisteredTownBuilding secondFarm = building("farm", new BlockPos(101, 64, 101));
        firstTown.registerBuilding(firstFarm);
        secondTown.registerBuilding(secondFarm);

        assertEquals(TownWorkforceSystem.AssignmentResult.ASSIGNED,
                TownWorkforceSystem.assign(firstTown, firstResident, firstFarm.id()));
        assertEquals(firstFarm.id(), TownWorkforceSystem.assignedBuilding(firstTown, firstResident).id());
        assertNull(TownWorkforceSystem.assignedBuilding(secondTown, firstResident));
        assertEquals(TownWorkforceSystem.AssignmentResult.NOT_RESIDENT,
                TownWorkforceSystem.assign(secondTown, firstResident, secondFarm.id()));
        assertNull(TownWorkforceSystem.assignedBuilding(secondTown, secondResident));
    }

    private static RegisteredTownBuilding building(String type, BlockPos pos) {
        return new RegisteredTownBuilding(UUID.randomUUID(), type, 1, pos, pos, pos,
                BuildingStatus.ACTIVE, 50, List.of(), -1L, 12);
    }
}
