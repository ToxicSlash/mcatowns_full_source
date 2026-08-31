package com.example.mcatowns.town;

import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.integration.MCAIntegration;
import net.minecraft.server.world.ServerWorld;

public final class TownHealthService {
    private TownHealthService() { }

    public static void tickDaily(ServerWorld world, TownContext context, TownSavedData data, long day) {
        if (day <= data.getLastProgressionDay()) return;
        TownBuildingService.refreshInspectionState(world, data, day);

        int decayed = Math.max(data.getProsperityBase(),
                data.getProsperity() - MCATownsConfig.get().prosperityDecayPerDay);
        data.addProsperity(decayed - data.getProsperity());

        int communityMood = Math.min(3, data.countBuildings("campfire")
                + data.countBuildings("park") * 2 + data.countBuildings("inn") * 2);
        int foodPercent = TownFoodSystem.foodPercent(data);
        int foodMood = foodPercent >= 25 ? 0 : foodPercent >= 10 ? -1 : foodPercent > 0 ? -3 : -5;
        MCAIntegration.modifyResidentMoods(world, data.getResidents(), communityMood + foodMood);
        MCAIntegration.averageResidentMood(world, data.getResidents()).ifPresent(data::setHappiness);
        data.setLastProgressionDay(day);
    }
}
