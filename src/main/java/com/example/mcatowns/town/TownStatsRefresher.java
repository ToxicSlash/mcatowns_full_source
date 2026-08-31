package com.example.mcatowns.town;

import com.example.mcatowns.integration.GuardVillagersIntegration;
import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.registry.ModBlocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class TownStatsRefresher {
    public static void refresh(ServerWorld world, BlockPos pos, TownSavedData data) {
        refresh(world, pos, data, MCAIntegration.scanBuildings(world, pos));
    }

    public static void refresh(ServerWorld world, BlockPos pos, TownSavedData data, TownBuildingSnapshot snapshot) {
        int effectiveAddonRadius = TownRangeSystem.getEffectiveRange(
                com.example.mcatowns.config.MCATownsConfig.get().addonBuildingScanRadius,
                snapshot.townHalls()
        );
        int population = MCAIntegration.getPopulation(world, pos);
        int treasuryCount = TownManager.countAddonBlocks(world, pos, ModBlocks.TREASURY, effectiveAddonRadius);
        int barracksCount = TownManager.countAddonBlocks(world, pos, ModBlocks.BARRACKS, effectiveAddonRadius);

        if (barracksCount <= 0) {
            data.setBarracksLevel(0);
        }

        int defense = TownDefenseSystem.calculateDefense(world, pos, snapshot, data.getBarracksLevel());
        defense += GuardVillagersIntegration.countNearbyGuards(world, pos) * 4;
        defense += data.getEventDefenseBonus();
        defense += data.getCaravanDefenseBonus();

        data.setPopulation(population);
        int mcaJobless = MCAIntegration.getJoblessPopulation(world, pos);
        data.setJobless(mcaJobless >= 0 ? mcaJobless : estimateJobless(population, snapshot));
        data.setImmigrationChance(TownImmigrationSystem.calculateImmigrationChance(data, snapshot));
        data.setDefenseRating(defense);
        data.setBuildingBonusScore(TownBonusSystem.calculateBonusScore(snapshot));
        data.setDetectedTreasuryBuildings(treasuryCount);
        data.setDetectedBarracksBuildings(barracksCount);
        data.setDetectedArmoryBuildings(snapshot.armories());
        data.setDetectedTownHallBuildings(snapshot.townHalls());
        data.setDetectedQuarryBuildings(snapshot.quarries());
        data.setDetectedWorkshopBuildings(snapshot.workshops());
        data.setDetectedMarketBuildings(snapshot.markets());
        data.setDetectedTradingPostBuildings(snapshot.tradingPosts());
        data.setDetectedCropFarmBuildings(snapshot.cropFarms());
        data.setInvalidCropFarmBuildings(snapshot.invalidCropFarms());
        data.setActiveBakeryBuildings(snapshot.activeBakeries());
        data.setInactiveBakeryBuildings(snapshot.inactiveBakeries());
        data.setDetectedButcherBuildings(snapshot.butchers());
        data.setDetectedFishermansHutBuildings(snapshot.fishermansHuts());
        data.setDailyFoodPotential(snapshot.dailyFoodProduction());
        data.setHasTreasuryBuilding(treasuryCount > 0);
        TownWorkforceSystem.refresh(data, snapshot);

        int projectedMcaTax = MCAIntegration.getMcaProjectedTaxIncome(world, pos);
        data.setMcaNormalTaxIncome(projectedMcaTax);

        data.setWeeklyTaxIncome(TownTaxSystem.calculateWeeklyTaxes(data, snapshot, MCAIntegration.getTownRankValue(world, pos)));

        int bonusFlags = 0;
        if (snapshot.totalBuildings() > 0) bonusFlags |= 1;
        if (treasuryCount > 0) bonusFlags |= 2;
        if (barracksCount > 0) bonusFlags |= 4;
        data.setBonusSourceFlags(bonusFlags);

    }

    private static int estimateJobless(int population, TownBuildingSnapshot snapshot) {
        // Approximate available work slots from detected functional buildings.
        int workSlots = snapshot.farms() * 2
                + snapshot.blacksmiths() * 2
                + snapshot.libraries()
                + snapshot.armories() * 2
                + snapshot.inns() * 2
                + snapshot.storages()
                + snapshot.quarries() * 2
                + snapshot.workshops() * 2
                + snapshot.markets() * 2
                + snapshot.tradingPosts() * 2
                + Math.max(0, snapshot.houses() / 3);
        return Math.max(0, population - workSlots);
    }
}
