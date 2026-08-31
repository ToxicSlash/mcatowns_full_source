package com.example.mcatowns.town;

import java.util.Locale;

public enum TownRank {
    UNRANKED("Unranked Settlement", 15, 4, 8, 1, 0, 0, 0),
    CAMP("Camp", 30, 8, 10, 1, 10, 6, 0),
    HAMLET("Hamlet", 50, 12, 16, 2, 25, 8, 4),
    VILLAGE("Village", 80, 24, 24, 3, 45, 12, 8),
    TOWNSHIP("Township", 120, 40, 36, 4, 70, 18, 16),
    TOWN("Town", 180, 64, 50, 6, 110, 28, 28);

    private final String displayName;
    private final int maxProsperity;
    private final int maxOccupancy;
    private final int maxBuildings;
    private final int maxSpecialists;
    private final int requiredProsperity;
    private final int requiredBuildings;
    private final int requiredPopulation;

    TownRank(String displayName, int maxProsperity, int maxOccupancy, int maxBuildings, int maxSpecialists,
             int requiredProsperity, int requiredBuildings, int requiredPopulation) {
        this.displayName = displayName;
        this.maxProsperity = maxProsperity;
        this.maxOccupancy = maxOccupancy;
        this.maxBuildings = maxBuildings;
        this.maxSpecialists = maxSpecialists;
        this.requiredProsperity = requiredProsperity;
        this.requiredBuildings = requiredBuildings;
        this.requiredPopulation = requiredPopulation;
    }

    public TownRank next() {
        int next = ordinal() + 1;
        return next < values().length ? values()[next] : null;
    }

    public boolean unlocks(TownRank requiredRank) {
        return ordinal() >= requiredRank.ordinal();
    }

    public static TownRank fromName(String name) {
        if (name != null) {
            return switch (name.toUpperCase(Locale.ROOT)) {
                case "UNRANKED" -> CAMP;
                case "SETTLEMENT" -> TOWNSHIP;
                case "STRONGHOLD", "CITY" -> TOWN;
                default -> parseCurrentName(name);
            };
        }
        return CAMP;
    }

    private static TownRank parseCurrentName(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return CAMP;
        }
    }

    public String displayName() { return displayName; }
    public int maxProsperity() { return maxProsperity; }
    public int maxOccupancy() { return maxOccupancy; }
    public int maxBuildings() { return maxBuildings; }
    public int maxSpecialists() { return maxSpecialists; }
    public int requiredProsperity() { return requiredProsperity; }
    public int requiredBuildings() { return requiredBuildings; }
    public int requiredPopulation() { return requiredPopulation; }
}
