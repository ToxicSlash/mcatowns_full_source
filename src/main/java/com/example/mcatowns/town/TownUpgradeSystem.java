package com.example.mcatowns.town;

public class TownUpgradeSystem {
    private static final int[] BARRACKS_COSTS = {150, 250, 400};

    public static boolean buyNextBarracksUpgrade(TownSavedData data) {
        if (data.getDetectedBarracksBuildings() <= 0) return false;
        int current = data.getBarracksLevel();
        if (current >= 3) return false;

        int cost = getNextBarracksCost(current);
        if (data.getTreasury() < cost) return false;

        data.setTreasury(data.getTreasury() - cost);
        data.setBarracksLevel(current + 1);
        return true;
    }

    public static int getNextBarracksCost(int currentLevel) {
        if (currentLevel < 0 || currentLevel >= BARRACKS_COSTS.length) {
            return -1;
        }
        return BARRACKS_COSTS[currentLevel];
    }

    public static boolean canBuyNextBarracksUpgrade(TownSavedData data) {
        if (data.getDetectedBarracksBuildings() <= 0 || data.getBarracksLevel() >= 3) {
            return false;
        }
        int cost = getNextBarracksCost(data.getBarracksLevel());
        return cost >= 0 && data.getTreasury() >= cost;
    }
}
