package com.example.mcatowns.town;

public class TownHappinessSystem {
    public static void tick(TownSavedData data, TownBuildingSnapshot snapshot) {
        int happiness = 40;
        happiness += Math.min(20, data.getFoodReserves() / 5);
        happiness += Math.min(15, data.getDefenseRating() / 8);
        happiness += Math.min(10, snapshot.totalBuildings() / 2);
        happiness -= data.getUnrest();
        happiness += data.getEventHappinessModifier();

        if (data.getTaxRate() >= 2) happiness -= 10;
        if (data.getTaxRate() >= 3) happiness -= 10;

        data.setHappiness(Math.max(0, Math.min(100, happiness)));
    }
}
