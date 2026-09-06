package com.example.mcatowns.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnloadedActivityCatchUpPolicyTest {
    @Test
    void newChunksHaveNoSyntheticCatchUp() {
        assertEquals(0L, UnloadedActivityCatchUpPolicy.effectiveWeatheringTicks(0L));
        assertEquals(0, UnloadedActivityCatchUpPolicy.environmentalAttempts(0L, 3));
    }

    @Test
    void weatheringSaturatesInsteadOfGrowingForever() {
        int oneDay = UnloadedActivityCatchUpPolicy.environmentalAttempts(24_000L, 3);
        int sevenDays = UnloadedActivityCatchUpPolicy.environmentalAttempts(7L * 24_000L, 3);
        int hundredDays = UnloadedActivityCatchUpPolicy.environmentalAttempts(100L * 24_000L, 3);

        assertTrue(oneDay > 0);
        assertTrue(sevenDays >= oneDay);
        assertEquals(sevenDays, hundredDays);
        assertTrue(sevenDays <= UnloadedActivityCatchUpPolicy.MAX_ENVIRONMENTAL_ATTEMPTS);
    }

    @Test
    void disabledRandomTicksProduceNoEnvironmentalAttempts() {
        assertEquals(0, UnloadedActivityCatchUpPolicy.environmentalAttempts(24_000L, 0));
    }

    @Test
    void customCropReplayIsAlwaysBounded() {
        assertEquals(0, UnloadedActivityCatchUpPolicy.capCustomCropRandomTicks(-10));
        assertEquals(12, UnloadedActivityCatchUpPolicy.capCustomCropRandomTicks(12));
        assertEquals(UnloadedActivityCatchUpPolicy.MAX_CUSTOM_CROP_RANDOM_TICKS,
                UnloadedActivityCatchUpPolicy.capCustomCropRandomTicks(Integer.MAX_VALUE));
    }
}
