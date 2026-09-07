package com.example.mcatowns.town;

/** Player-facing bands for the hidden 0-120 Bandit Activity score. */
public enum BanditActivityTier {
    MINIMAL("Minimal", 0, 15, 1, 0, 0, 0),
    LOW("Low", 16, 40, 1, 1, 20, 1),
    MODERATE("Moderate", 41, 70, 2, 1, 40, 2),
    HIGH("High", 71, 99, 3, 2, 70, 3),
    EXTREME("Extreme", 100, 120, 4, 4, 70, 3);

    private final String displayName;
    private final int min;
    private final int max;
    private final int wildBandCapacity;
    private final int townBandCapacity;
    private final int campAttemptChancePercent;
    private final int campCapacity;

    BanditActivityTier(String displayName, int min, int max, int wildBandCapacity, int townBandCapacity,
                       int campAttemptChancePercent, int campCapacity) {
        this.displayName = displayName;
        this.min = min;
        this.max = max;
        this.wildBandCapacity = wildBandCapacity;
        this.townBandCapacity = townBandCapacity;
        this.campAttemptChancePercent = campAttemptChancePercent;
        this.campCapacity = campCapacity;
    }

    public static BanditActivityTier fromActivity(int activity) {
        int value = Math.max(0, Math.min(TownBanditSystem.MAX_ACTIVITY, activity));
        for (BanditActivityTier tier : values()) {
            if (value >= tier.min && value <= tier.max) return tier;
        }
        return EXTREME;
    }

    public String displayName() { return displayName; }
    public int min() { return min; }
    public int max() { return max; }
    public int wildBandCapacity() { return wildBandCapacity; }
    public int townBandCapacity() { return townBandCapacity; }
    public int campAttemptChancePercent() { return campAttemptChancePercent; }
    public int campCapacity() { return campCapacity; }
}
