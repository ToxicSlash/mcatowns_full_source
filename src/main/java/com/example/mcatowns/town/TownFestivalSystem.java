package com.example.mcatowns.town;

import com.example.mcatowns.config.MCATownsConfig;

public class TownFestivalSystem {
    public static final int FESTIVAL_COST_EMERALDS = 120;
    public static final int FESTIVAL_COST_FOOD = 20;
    public static final int FOOD_RELIEF_COST = 80;

    public static boolean canHoldFestival(TownSavedData data, long day) {
        int cooldown = MCATownsConfig.get().festivalCooldownDays;
        if (cooldown <= 0) {
            return true;
        }
        return day - data.getLastFestivalDay() >= cooldown;
    }

    public static boolean tryHoldFestival(TownSavedData data, long day) {
        if (!canHoldFestival(data, day)) {
            return false;
        }
        if (data.getTreasury() < FESTIVAL_COST_EMERALDS || data.getFoodReserves() < FESTIVAL_COST_FOOD) {
            return false;
        }

        data.setTreasury(data.getTreasury() - FESTIVAL_COST_EMERALDS);
        data.setFoodReserves(Math.max(0, data.getFoodReserves() - FESTIVAL_COST_FOOD));
        data.setHappiness(Math.min(100, data.getHappiness() + 15));
        data.setUnrest(Math.max(0, data.getUnrest() - 5));
        data.setLastFestivalDay(day);
        return true;
    }

    public static boolean tryEmergencyFoodRelief(TownSavedData data) {
        if (data.getTreasury() < FOOD_RELIEF_COST) {
            return false;
        }

        data.setTreasury(data.getTreasury() - FOOD_RELIEF_COST);
        int capacity = data.getFoodCapacity() > 0 ? data.getFoodCapacity() : 1000;
        data.setFoodReserves(Math.min(capacity, data.getFoodReserves() + 25));
        data.setHappiness(Math.min(100, data.getHappiness() + 8));
        data.setUnrest(Math.max(0, data.getUnrest() - 8));
        return true;
    }
}
