package com.example.mcatowns.event;

import com.example.mcatowns.MCATowns;
import com.example.mcatowns.integration.VillagerBusinessIntegration;
import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.integration.GuardStatIntegration;
import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.town.*;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.raid.Raid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerTickEventsHandler {
    private static final long TOWN_TICK_INTERVAL = 200L;
    private static final int GUARD_REFRESH_TOWN_TICKS = 3;

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(ServerTickEventsHandler::tickWorld);
    }

    private static void tickWorld(ServerWorld world) {
        if (world.getTime() % TOWN_TICK_INTERVAL != 0) return;

        long day = TownManager.getDay(world);
        List<TownContext> contexts = TownManager.getTickContexts(world);
        Map<String, TownSavedData> dataByTown = new HashMap<>();
        for (TownContext context : contexts) {
            dataByTown.put(context.townId(), TownSavedData.get(world, context.townId()));
        }

        for (TownContext context : contexts) {
            try {
                tickTown(world, day, context, contexts, dataByTown);
            } catch (RuntimeException exception) {
                MCATowns.LOGGER.error("Failed ticking town {} in {}", context.townId(), world.getRegistryKey().getValue(), exception);
            }
        }
    }

    private static void tickTown(ServerWorld world, long day, TownContext context,
                                 List<TownContext> contexts, Map<String, TownSavedData> dataByTown) {
        BlockPos anchor = context.center();
        TownSavedData data = dataByTown.get(context.townId());
        if (data == null || !world.getWorldBorder().contains(anchor) || !world.isChunkLoaded(anchor)) {
            return;
        }

        TownBuildingSnapshot daySnapshot = null;
        TownBuildingSnapshot latestSnapshot = null;
        MCATownsConfig config = MCATownsConfig.get();
        int townHallCount = data.getDetectedTownHallBuildings();
        int effectiveGuardRadius = TownRangeSystem.getEffectiveRange(config.guardBuffRadius, townHallCount);
        int effectiveBusinessRadius = TownRangeSystem.getEffectiveRange(config.villagerBusinessRadius, townHallCount);

        if (day > data.getLastFoodCycleDay()) {
            daySnapshot = MCAIntegration.scanBuildings(world, anchor);
            int rangeBonusHallCount = daySnapshot.townHalls();
            data.setDetectedArmoryBuildings(daySnapshot.armories());
            data.setDetectedTownHallBuildings(rangeBonusHallCount);
            effectiveGuardRadius = TownRangeSystem.getEffectiveRange(config.guardBuffRadius, rangeBonusHallCount);
            effectiveBusinessRadius = TownRangeSystem.getEffectiveRange(config.villagerBusinessRadius, rangeBonusHallCount);
            int population = MCAIntegration.getPopulation(world, anchor);
            if (population > 0 || data.getPopulation() <= 0) {
                data.setPopulation(population);
            }
            TownCaravanSystem.tickDaily(world, context, data, day);
            TownRandomEventSystem.tickDaily(world, data, daySnapshot, day);
            TownRequestService.tickDaily(world, context, data, day);
            TownFoodSystem.applyDailyCycle(world, context, data);
            TownHealthService.tickDaily(world, context, data, day);
            TownIndustrySystem.applyDailyQuarryYield(world, anchor, data, daySnapshot);
            data.setLastFoodCycleDay(day);
            latestSnapshot = daySnapshot;
        }

        if (day - data.getLastStatsRefreshDay() >= config.statsRefreshDays) {
            TownBuildingSnapshot snapshot = daySnapshot != null ? daySnapshot : MCAIntegration.scanBuildings(world, anchor);
            latestSnapshot = snapshot;
            int rangeBonusHallCount = snapshot.townHalls();
            effectiveGuardRadius = TownRangeSystem.getEffectiveRange(config.guardBuffRadius, rangeBonusHallCount);
            effectiveBusinessRadius = TownRangeSystem.getEffectiveRange(config.villagerBusinessRadius, rangeBonusHallCount);
            TownStatsRefresher.refresh(world, anchor, data, snapshot);
            TownUnrestSystem.tick(data);
            if (!"player_created".equals(context.source())) TownHappinessSystem.tick(data, snapshot);
            data.setLastStatsRefreshDay(day);
        }

        data.setTradingPostLinked(TownTradeSystem.hasLinkedTradingPost(
                context,
                data,
                contexts,
                dataByTown,
                config.tradingPostLinkRange
        ));

        if (isGuardRefreshTick(world, context)) {
            GuardStatIntegration.applyBarracksBonuses(
                    world,
                    anchor,
                    data.getBarracksLevel(),
                    data.getDetectedArmoryBuildings(),
                    effectiveGuardRadius
            );
        }

        if (day - data.getLastTaxCollectionDay() >= config.taxCollectionDays) {
            TownTaxSystem.collectWeeklyTaxes(data);
            TownBuildingSnapshot taxSnapshot = latestSnapshot != null ? latestSnapshot : MCAIntegration.scanBuildings(world, anchor);
            List<ItemStack> taxItems = TownTaxSystem.applyWorkshopItemTaxBonus(
                    MCAIntegration.collectVillagerTradeTaxItems(world, anchor),
                    taxSnapshot
            );
            if (!taxItems.isEmpty()) {
                var overflow = TownStorageDepositSystem.deposit(world, anchor, taxItems);
                if (!overflow.isEmpty()) {
                    dropOverflow(world, anchor, overflow);
                }
            }
            data.setLastTaxCollectionDay(day);
        }

        Raid raid = world.getRaidAt(anchor);
        if (raid != null) {
            int raidKey = raid.getCenter().hashCode();
            boolean canApplyRaidImpact = raidKey != data.getLastRaidId()
                    || world.getTime() - data.getLastRaidImpactTick() >= config.raidUnrestCooldownTicks;
            if (canApplyRaidImpact) {
                int unrestImpact = TownDefenseSystem.getRaidUnrestImpact(data);
                TownUnrestSystem.onRaid(data, unrestImpact);
                data.setLastRaidId(raidKey);
                data.setLastRaidImpactTick(world.getTime());
            }
        }

        VillagerBusinessIntegration.tickMcaVillagersForStores(world, anchor, data, effectiveBusinessRadius);
    }

    private static boolean isGuardRefreshTick(ServerWorld world, TownContext context) {
        long townTick = world.getTime() / TOWN_TICK_INTERVAL;
        int phase = Math.floorMod(context.townId().hashCode(), GUARD_REFRESH_TOWN_TICKS);
        return Math.floorMod(townTick + phase, GUARD_REFRESH_TOWN_TICKS) == 0;
    }

    private static void dropOverflow(ServerWorld world, BlockPos anchor, List<ItemStack> overflow) {
        for (ItemStack stack : overflow) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemEntity item = new ItemEntity(
                    world,
                    anchor.getX() + 0.5D,
                    anchor.getY() + 1.0D,
                    anchor.getZ() + 0.5D,
                    stack.copy()
            );
            world.spawnEntity(item);
        }
    }
}
