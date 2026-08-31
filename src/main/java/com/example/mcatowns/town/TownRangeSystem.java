package com.example.mcatowns.town;

public final class TownRangeSystem {
    private static final int RANGE_PER_TOWN_HALL = 32;
    private static final int RANGE_CAP_BONUS = 192;

    private TownRangeSystem() {
    }

    public static int getBonusRange(int townHalls) {
        int bonus = Math.max(0, townHalls) * RANGE_PER_TOWN_HALL;
        return Math.min(RANGE_CAP_BONUS, bonus);
    }

    public static int getEffectiveRange(int base, int townHalls) {
        return Math.max(16, base + getBonusRange(townHalls));
    }
}
