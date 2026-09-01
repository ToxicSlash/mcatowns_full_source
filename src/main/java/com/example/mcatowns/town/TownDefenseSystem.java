package com.example.mcatowns.town;

public class TownDefenseSystem {
    private TownDefenseSystem() { }

    public static int calculateDefense(TownBuildingSnapshot snapshot, int barracksLevel) {
        return snapshot.armories() * 6
                + snapshot.blacksmiths() * 4
                + snapshot.townCenters() * 2
                + snapshot.graveyards()
                + barracksLevel * 8;
    }

    public static int getRaidScaling(TownSavedData data) {
        return Math.max(1, data.getPopulation() / 5);
    }

    public static int getRaidUnrestImpact(TownSavedData data) {
        int baseImpact = Math.max(2, getRaidScaling(data));
        int mitigation = Math.max(0, data.getDefenseRating()) / 10;
        return Math.max(2, baseImpact - mitigation);
    }

    public static int getUnrestRecovery(TownSavedData data) {
        int defense = Math.max(0, data.getDefenseRating());
        if (defense >= 100) return 3;
        if (defense >= 50) return 2;
        return defense > 25 ? 1 : 0;
    }

    public static int mitigateVillagerDeathPenalty(TownSavedData data, int penalty) {
        int mitigation = Math.max(0, data.getDefenseRating()) / 25;
        return Math.max(1, penalty - mitigation);
    }
}
