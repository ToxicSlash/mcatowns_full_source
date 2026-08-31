package com.example.mcatowns.town;

public class TownImmigrationSystem {
    private static final int HOUSE_CAPACITY = 4;

    public static int calculateImmigrationChance(TownSavedData data, TownBuildingSnapshot snapshot) {
        if (snapshot.inns() <= 0) return 0;
        int effectivePopulation = Math.max(0, data.getPopulation() + data.getRefugeePopulationBonus() + data.getCaravanPopulationBonus());
        int housingCapacity = snapshot.houses() * HOUSE_CAPACITY;
        int freeHousing = Math.max(0, housingCapacity - effectivePopulation);
        if (freeHousing <= 0) return 0;

        int chance = 5;
        chance += data.getHappiness() / 10;
        chance += Math.min(15, data.getFoodReserves() / 5);
        chance += snapshot.inns() * 5;
        chance += Math.min(10, freeHousing * 2);
        chance -= data.getUnrest() / 5;

        return Math.max(0, Math.min(100, chance));
    }
}
