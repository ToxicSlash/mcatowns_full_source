package com.example.mcatowns.town;

public record TownBuildingSnapshot(
        int townCenters,
        int townHalls,
        int storages,
        int libraries,
        int armories,
        int blacksmiths,
        int inns,
        int graveyards,
        int houses,
        int farms,
        int dailyFoodProduction,
        int cropFarms,
        int invalidCropFarms,
        int activeBakeries,
        int inactiveBakeries,
        int butchers,
        int fishermansHuts,
        int quarries,
        int workshops,
        int markets,
        int tradingPosts
) {
    public int totalBuildings() {
        return townCenters + townHalls + storages + libraries + armories + blacksmiths + inns + graveyards + houses + farms
                + quarries + workshops + markets + tradingPosts;
    }
}
