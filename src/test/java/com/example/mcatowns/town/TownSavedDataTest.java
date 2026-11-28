package com.example.mcatowns.town;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TownSavedDataTest {
    @Test
    void unchangedValuesDoNotDirtyTownSave() {
        TownSavedData data = TownSavedData.create();
        data.setDirty(false);
        data.setTreasury(data.getTreasury());
        data.setPopulation(data.getPopulation());
        data.setWorkforceAvailable(data.getWorkforceAvailable());
        data.setWorkforceRequired(data.getWorkforceRequired());
        data.setWorkforceEfficiencyPercent(data.getWorkforceEfficiencyPercent());
        data.setLastCaravanType(data.getLastCaravanType());
        org.junit.jupiter.api.Assertions.assertFalse(data.isDirty());
        data.setTreasury(data.getTreasury() + 1);
        org.junit.jupiter.api.Assertions.assertTrue(data.isDirty());
        data.setDirty(false);
        data.setLastCaravanType("test");
        org.junit.jupiter.api.Assertions.assertTrue(data.isDirty());
    }

    @Test
    void malformedSavedEnumsAndUnsafeBuildingValuesFallBackSafely() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("TownRank", "NOT_A_REAL_RANK");

        NbtCompound building = new NbtCompound();
        building.putString("Type", "farm");
        building.putLong("Pos", new BlockPos(4, 64, 4).asLong());
        building.putInt("Tier", 999);
        building.putString("Status", "BROKEN_STATUS");
        building.putInt("Output", -500);
        NbtList buildings = new NbtList();
        buildings.add(building);
        nbt.put("TypedBuildings", buildings);

        NbtCompound request = new NbtCompound();
        request.putString("Type", "BROKEN_REQUEST_TYPE");
        nbt.put("ActiveRequest", request);

        TownSavedData data = assertDoesNotThrow(() -> TownSavedData.fromNbt(nbt));
        assertEquals(TownRank.CAMP, data.getTownRank());
        RegisteredTownBuilding restored = data.getRegisteredBuildings().get(0);
        assertEquals(3, restored.tier());
        assertEquals(BuildingStatus.ACTIVE, restored.status());
        assertEquals(0, restored.output());
        assertNull(data.getActiveRequest());
    }
}
