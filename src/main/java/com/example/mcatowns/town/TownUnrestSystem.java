package com.example.mcatowns.town;

public class TownUnrestSystem {
    public static void tick(TownSavedData data) {
        int unrest = data.getUnrest();

        if (data.getTaxRate() >= 2) unrest += 1;
        if (data.getFoodReserves() < 15) unrest += 2;
        if (data.getHappiness() < 30) unrest += 1;
        if (unrest > 0) unrest -= TownDefenseSystem.getUnrestRecovery(data);

        unrest = Math.max(0, Math.min(100, unrest));
        data.setUnrest(unrest);
    }

    public static void onRaid(TownSavedData data) {
        onRaid(data, 10);
    }

    public static void onRaid(TownSavedData data, int amount) {
        data.setUnrest(Math.min(100, data.getUnrest() + Math.max(1, amount)));
    }
}
