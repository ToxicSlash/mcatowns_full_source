package com.example.mcatowns.town;

public final class TownWorkforceSystem {
    private static final int FARM_WORKERS = 1;
    private static final int MARKET_WORKERS = 2;
    private static final int BARRACKS_WORKERS = 2;
    private static final int QUARRY_WORKERS = 3;
    private static final int WORKSHOP_WORKERS = 2;
    private static final int MIN_EFFICIENCY_IF_UNDERSTAFFED = 25;

    private TownWorkforceSystem() {
    }

    public static void refresh(TownSavedData data, TownBuildingSnapshot snapshot) {
        int effectivePopulation = Math.max(0, data.getPopulation() + data.getRefugeePopulationBonus() + data.getCaravanPopulationBonus());
        int available = Math.max(0, effectivePopulation - data.getJobless());
        int required = snapshot.farms() * FARM_WORKERS
                + snapshot.markets() * MARKET_WORKERS
                + snapshot.tradingPosts() * MARKET_WORKERS
                + Math.max(snapshot.armories(), data.getDetectedBarracksBuildings()) * BARRACKS_WORKERS
                + snapshot.quarries() * QUARRY_WORKERS
                + snapshot.workshops() * WORKSHOP_WORKERS;

        int efficiency;
        if (required <= 0) {
            efficiency = 100;
        } else {
            efficiency = available * 100 / required;
            if (efficiency < 100) {
                efficiency = Math.max(MIN_EFFICIENCY_IF_UNDERSTAFFED, efficiency);
            } else {
                efficiency = 100;
            }
        }

        data.setWorkforceAvailable(available);
        data.setWorkforceRequired(required);
        data.setWorkforceEfficiencyPercent(efficiency);
    }
}
