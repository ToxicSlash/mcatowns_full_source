package com.example.mcatowns.town;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class TownDefenseSystem {
    public static int calculateDefense(ServerWorld world, BlockPos pos, TownBuildingSnapshot snapshot, int barracksLevel) {
        int defense = 0;
        defense += snapshot.armories() * 6;
        defense += snapshot.blacksmiths() * 4;
        defense += snapshot.townCenters() * 2;
        defense += snapshot.graveyards();
        defense += barracksLevel * 8;
        return defense;
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
        if (defense >= 100) {
            return 3;
        }
        if (defense >= 50) {
            return 2;
        }
        if (defense > 25) {
            return 1;
        }
        return 0;
    }

    public static int mitigateVillagerDeathPenalty(TownSavedData data, int penalty) {
        int mitigation = Math.max(0, data.getDefenseRating()) / 25;
        return Math.max(1, penalty - mitigation);
    }
}
