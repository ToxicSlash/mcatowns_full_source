package com.example.mcatowns.town;

public class TownBonusSystem {
    public static int calculateBonusScore(TownBuildingSnapshot snapshot) {
        int score = 0;
        score += snapshot.libraries() * 4;
        score += snapshot.storages() * 3;
        score += snapshot.armories() * 3;
        score += snapshot.blacksmiths() * 3;
        score += snapshot.inns() * 4;
        score += snapshot.townCenters() * 5;
        score += snapshot.graveyards() * 2;
        return score;
    }
}
