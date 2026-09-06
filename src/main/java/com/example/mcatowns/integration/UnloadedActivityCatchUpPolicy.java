package com.example.mcatowns.integration;

/**
 * Pure catch-up limits used by the optional Unloaded Activity / Immersive Weathering integration.
 *
 * <p>Unloaded Activity remains the authority for elapsed time. These limits only compress
 * Immersive Weathering's environmental work so an ancient chunk cannot replay every missed
 * environmental tick when it becomes active again.</p>
 */
public final class UnloadedActivityCatchUpPolicy {
    static final long TICKS_PER_DAY = 24_000L;
    static final long MAX_EFFECTIVE_WEATHERING_TICKS = 7L * TICKS_PER_DAY;
    static final int MAX_ENVIRONMENTAL_ATTEMPTS = 256;
    static final int MAX_CUSTOM_CROP_RANDOM_TICKS = 64;
    static final double STAFFED_FARM_WEED_SUPPRESSION = 0.75D;

    private static final double IW_SKY_TICK_DIVISOR = 48.0D;
    private static final double ENVIRONMENTAL_SATURATION_SCALE = 1_500.0D;

    private UnloadedActivityCatchUpPolicy() { }

    public static long effectiveWeatheringTicks(long elapsedTicks) {
        if (elapsedTicks <= 0L) return 0L;
        return Math.min(elapsedTicks, MAX_EFFECTIVE_WEATHERING_TICKS);
    }

    /**
     * Converts Immersive Weathering's expected sky-access opportunities into a small,
     * diminishing sample. The result approaches a hard cap instead of growing forever.
     */
    public static int environmentalAttempts(long elapsedTicks, int randomTickSpeed) {
        long effectiveTicks = effectiveWeatheringTicks(elapsedTicks);
        if (effectiveTicks <= 0L || randomTickSpeed <= 0) return 0;

        double virtualAttempts = effectiveTicks * (randomTickSpeed / IW_SKY_TICK_DIVISOR);
        double saturation = 1.0D - Math.exp(-virtualAttempts / ENVIRONMENTAL_SATURATION_SCALE);
        int attempts = (int) Math.round(MAX_ENVIRONMENTAL_ATTEMPTS * saturation);
        return Math.max(0, Math.min(MAX_ENVIRONMENTAL_ATTEMPTS, attempts));
    }

    public static int capCustomCropRandomTicks(int sampledOccurrences) {
        return Math.max(0, Math.min(MAX_CUSTOM_CROP_RANDOM_TICKS, sampledOccurrences));
    }

    public static double staffedFarmWeedSuppression() {
        return STAFFED_FARM_WEED_SUPPRESSION;
    }
}
