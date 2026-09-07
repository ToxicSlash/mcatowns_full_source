package com.example.mcatowns.town;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownBanditSystemTest {
    @Test
    void activityBandsUseApprovedRanges() {
        assertEquals(BanditActivityTier.MINIMAL, TownBanditSystem.tier(0));
        assertEquals(BanditActivityTier.MINIMAL, TownBanditSystem.tier(15));
        assertEquals(BanditActivityTier.LOW, TownBanditSystem.tier(16));
        assertEquals(BanditActivityTier.LOW, TownBanditSystem.tier(40));
        assertEquals(BanditActivityTier.MODERATE, TownBanditSystem.tier(41));
        assertEquals(BanditActivityTier.HIGH, TownBanditSystem.tier(99));
        assertEquals(BanditActivityTier.EXTREME, TownBanditSystem.tier(100));
        assertEquals(BanditActivityTier.EXTREME, TownBanditSystem.tier(120));
    }

    @Test
    void encounterCapacitiesScaleWithActivity() {
        assertEquals(1, BanditActivityTier.MINIMAL.wildBandCapacity());
        assertEquals(0, BanditActivityTier.MINIMAL.townBandCapacity());
        assertEquals(2, BanditActivityTier.MODERATE.wildBandCapacity());
        assertEquals(1, BanditActivityTier.MODERATE.townBandCapacity());
        assertEquals(3, BanditActivityTier.HIGH.wildBandCapacity());
        assertEquals(2, BanditActivityTier.HIGH.townBandCapacity());
        assertEquals(4, BanditActivityTier.EXTREME.wildBandCapacity());
        assertEquals(4, BanditActivityTier.EXTREME.townBandCapacity());
        assertEquals(3, BanditActivityTier.EXTREME.campCapacity());
    }

    @Test
    void defenceReducesGrowthChanceButNeverBelowFortyPercent() {
        assertEquals(1.0D, TownBanditSystem.activityIncreaseChance(0), 0.00001D);
        assertEquals(0.625D, TownBanditSystem.activityIncreaseChance(15), 0.00001D);
        assertEquals(0.4D, TownBanditSystem.activityIncreaseChance(24), 0.00001D);
        assertEquals(0.4D, TownBanditSystem.activityIncreaseChance(100), 0.00001D);
    }

    @Test
    void growthAddsFiveAndHonoursOfflineHighCap() {
        assertEquals(55, TownBanditSystem.applyGrowthRoll(50, 0, true, 0.0D));
        assertEquals(104, TownBanditSystem.applyGrowthRoll(99, 0, true, 0.0D));
        assertEquals(99, TownBanditSystem.applyGrowthRoll(99, 0, false, 0.0D));
        assertEquals(99, TownBanditSystem.applyGrowthRoll(98, 0, false, 0.0D));
        assertEquals(120, TownBanditSystem.applyGrowthRoll(119, 0, true, 0.0D));
    }

    @Test
    void failedGrowthRollLeavesActivityUnchanged() {
        assertEquals(50, TownBanditSystem.applyGrowthRoll(50, 15, true, 0.9D));
    }
}
