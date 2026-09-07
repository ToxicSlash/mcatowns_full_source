package com.example.mcatowns.town;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BanditEncounterTest {
    @Test
    void unspawnedWildBandReleasesAfterFifteenMinutes() {
        BanditEncounter encounter = BanditEncounter.reserve(BanditEncounterType.WILD, BlockPos.ORIGIN, 100L);
        assertTrue(encounter.occupiesSlot(100L + TownBanditSystem.UNSPAWNED_WILD_BAND_EXPIRY_TICKS - 1L));
        assertFalse(encounter.occupiesSlot(100L + TownBanditSystem.UNSPAWNED_WILD_BAND_EXPIRY_TICKS));
    }

    @Test
    void spawnedWildBandReleasesSlotAfterTenMinutes() {
        BanditEncounter encounter = BanditEncounter.reserve(BanditEncounterType.WILD, BlockPos.ORIGIN, 100L)
                .withSpawned(500L, 4);
        assertTrue(encounter.occupiesSlot(500L + TownBanditSystem.SPAWNED_WILD_SLOT_RELEASE_TICKS - 1L));
        assertFalse(encounter.occupiesSlot(500L + TownBanditSystem.SPAWNED_WILD_SLOT_RELEASE_TICKS));
    }

    @Test
    void townBandsAndCampsStayReservedUntilExplicitlyCleared() {
        long muchLater = 20L * 60L * 60L * 24L;
        assertTrue(BanditEncounter.reserve(BanditEncounterType.TOWN, BlockPos.ORIGIN, 0L).occupiesSlot(muchLater));
        assertTrue(BanditEncounter.reserve(BanditEncounterType.CAMP, BlockPos.ORIGIN, 0L).occupiesSlot(muchLater));
    }

    @Test
    void memberTrackingReachesZeroCleanly() {
        BanditEncounter encounter = BanditEncounter.reserve(BanditEncounterType.WILD, BlockPos.ORIGIN, 0L)
                .withSpawned(20L, 2);
        assertTrue(encounter.withOneMemberRemoved().remainingMembers() == 1);
        assertTrue(encounter.withOneMemberRemoved().withOneMemberRemoved().remainingMembers() == 0);
    }
}
