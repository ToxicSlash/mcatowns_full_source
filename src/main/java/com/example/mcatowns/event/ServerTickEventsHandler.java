package com.example.mcatowns.event;

import com.example.mcatowns.MCATowns;
import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.integration.FarmWeedMaintenanceService;
import com.example.mcatowns.integration.GuardStatIntegration;
import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.integration.UnloadedActivityCompat;
import com.example.mcatowns.integration.VillagerBusinessIntegration;
import com.example.mcatowns.town.TownBanditSystem;
import com.example.mcatowns.town.TownBuildingSnapshot;
import com.example.mcatowns.town.TownCaravanSystem;
import com.example.mcatowns.town.TownContext;
import com.example.mcatowns.town.TownDefenseSystem;
import com.example.mcatowns.town.TownFoodSystem;
import com.example.mcatowns.town.TownHappinessSystem;
import com.example.mcatowns.town.TownHealthService;
import com.example.mcatowns.town.TownIndustrySystem;
import com.example.mcatowns.town.TownManager;
import com.example.mcatowns.town.TownRandomEventSystem;
import com.example.mcatowns.town.TownRangeSystem;
import com.example.mcatowns.town.TownRequestService;
import com.example.mcatowns.town.TownSavedData;
import com.example.mcatowns.town.TownStatsRefresher;
import com.example.mcatowns.town.TownStorageDepositSystem;
import com.example.mcatowns.town.TownTaxSystem;
import com.example.mcatowns.town.TownTradeSystem;
import com.example.mcatowns.town.TownUnrestSystem;
import com.example.mcatowns.town.TownBuildingService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.raid.Raid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ServerTickEventsHandler {
    private static final long TOWN_TICK_INTERVAL = 200L;
    private static final int GUARD_REFRESH_TOWN_TICKS = 3;
    private static final Map<ServerWorld, TownCycle> ACTIVE_CYCLES = new HashMap<>();

    private static final class TownCycle {
        final long start;
        final List<TownContext> contexts;
        final Map<String, TownSavedData> data;
        final TownTradeSystem.LinkIndex links;
        int processed;
        int maxPerTick;
        long peakNanos;
        long totalNanos;

        TownCycle(long start, List<TownContext> contexts, Map<String, TownSavedData> data) {
            this.start = start;
            this.contexts = contexts;
            this.data = data;
            this.links = new TownTradeSystem.LinkIndex(contexts, data, MCATownsConfig.get().tradingPostLinkRange);
        }
    }

    private ServerTickEventsHandler() { }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(ServerTickEventsHandler::tickWorld);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents.UNLOAD.register((server, world) -> {
            ACTIVE_CYCLES.remove(world);
            FarmWeedMaintenanceService.clearWorld(world);
            VillagerBusinessIntegration.clearWorld(world);
            com.example.mcatowns.town.BlueprintSessionService.clearWorld(world);
        });

    }

    private static void tickWorld(ServerWorld world) {
        if (world.getTime() % 40 == 0) {
            world.getProfiler().push("mcatowns_blueprint_detection");
            try {
                world.getPlayers().forEach(TownBuildingService::periodicDetect);
            } finally {
                world.getProfiler().pop();
            }
        }
        long now = world.getTime();
        int phase = (int) Math.floorMod(now, TOWN_TICK_INTERVAL);
        if (phase >= com.example.mcatowns.town.TownTickSchedule.SLICES) {
            ACTIVE_CYCLES.remove(world);
            return;
        }

        world.getProfiler().push("mcatowns_town_tick");
        long started = System.nanoTime();
        try {
            if (phase == 0) {
                List<TownContext> contexts = TownManager.getTickContexts(world);
                Map<String, TownSavedData> data = new HashMap<>();
                for (TownContext town : contexts) data.put(town.townId(), TownSavedData.get(world, town.townId()));
                ACTIVE_CYCLES.put(world, new TownCycle(now, contexts, data));
            }
            TownCycle cycle = ACTIVE_CYCLES.get(world);
            // No replay after a world-time discontinuity or a late world load.
            if (cycle == null || cycle.start != now - phase) return;
            tickSlice(world, cycle, phase);
            long duration = System.nanoTime() - started;
            cycle.totalNanos += duration;
            cycle.peakNanos = Math.max(cycle.peakNanos, duration);
            if (phase == com.example.mcatowns.town.TownTickSchedule.SLICES - 1) {
                if (!cycle.contexts.isEmpty() && Boolean.getBoolean("town.test.profile")) {
                    MCATowns.LOGGER.info("TOWN_TICK_PROFILE towns={} processed={} peak_towns_per_tick={} total_ns={} peak_ns={}",
                            cycle.contexts.size(), cycle.processed, cycle.maxPerTick, cycle.totalNanos, cycle.peakNanos);
                }
                ACTIVE_CYCLES.remove(world);
            }
        } finally {
            world.getProfiler().pop();
        }
    }

    private static void tickSlice(ServerWorld world, TownCycle cycle, int phase) {
        long day = TownManager.getDay(world);
        var slice = com.example.mcatowns.town.TownTickSchedule.slice(cycle.contexts.size(), phase);
        int count = 0;
        for (int i = slice.from(); i < slice.to(); i++) {
            TownContext context = cycle.contexts.get(i);
            try {
                // A player town can be deleted between slices. Never recreate
                // its saved data or reserve new encounters after deletion.
                if ("player_created".equals(context.source())
                        && com.example.mcatowns.town.PlayerTownRegistry.get(world).getTownById(context.townId()).isEmpty()) continue;
                TownSavedData data = cycle.data.get(context.townId());
                if (data != null) {
                    TownBanditSystem.tickActivity(world, context, data);
                    TownBanditSystem.tickEncounterReservations(world, context);
                }
                world.getProfiler().push("town_simulation");
                try {
                    tickTown(world, day, context, cycle.links, cycle.data, cycle.start);
                    count++;
                } finally {
                    world.getProfiler().pop();
                }
            } catch (RuntimeException exception) {
                MCATowns.LOGGER.error("Failed ticking town {} in {}", context.townId(), world.getRegistryKey().getValue(), exception);
            }
        }
        cycle.processed += count;
        cycle.maxPerTick = Math.max(cycle.maxPerTick, count);
    }

    private static void tickTown(ServerWorld world, long day, TownContext context,
                                 TownTradeSystem.LinkIndex links, Map<String, TownSavedData> dataByTown, long cycleStart) {
        BlockPos anchor = context.center();
        TownSavedData data = dataByTown.get(context.townId());
        if (data == null || !world.getWorldBorder().contains(anchor) || !world.isChunkLoaded(anchor)) return;

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
            int mcaPopulation = MCAIntegration.getPopulation(world, anchor);
            int trackedPopulation = Math.max(mcaPopulation, data.getResidents().size());
            if (trackedPopulation > 0 || data.getPopulation() <= 0) data.setPopulation(trackedPopulation);
            TownCaravanSystem.tickDaily(world, context, data, day);
            TownRandomEventSystem.tickDaily(world, data, daySnapshot, day);
            TownRequestService.tickDaily(world, context, data, day);
            TownFoodSystem.applyDailyCycle(data);
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

        data.setTradingPostLinked(links.hasLink(context, data));

        if (isGuardRefreshTick(world, context)) {
            GuardStatIntegration.applyTownDefenceBonuses(world, anchor, data, effectiveGuardRadius);
        }

        if (day - data.getLastTaxCollectionDay() >= config.taxCollectionDays) {
            TownTaxSystem.collectWeeklyTaxes(data);
            TownBuildingSnapshot taxSnapshot = latestSnapshot != null ? latestSnapshot : MCAIntegration.scanBuildings(world, anchor);
            List<ItemStack> taxItems = TownTaxSystem.applyWorkshopItemTaxBonus(
                    MCAIntegration.collectVillagerTradeTaxItems(world, anchor), taxSnapshot);
            if (!taxItems.isEmpty()) {
                List<ItemStack> overflow = TownStorageDepositSystem.deposit(world, anchor, taxItems);
                if (!overflow.isEmpty()) dropOverflow(world, anchor, overflow);
            }
            data.setLastTaxCollectionDay(day);
        }

        Raid raid = world.getRaidAt(anchor);
        if (raid != null) {
            int raidKey = raid.getCenter().hashCode();
            boolean canApplyRaidImpact = raidKey != data.getLastRaidId()
                    || world.getTime() - data.getLastRaidImpactTick() >= config.raidUnrestCooldownTicks;
            if (canApplyRaidImpact) {
                TownUnrestSystem.onRaid(data, TownDefenseSystem.getRaidUnrestImpact(data));
                data.setLastRaidId(raidKey);
                data.setLastRaidImpactTick(world.getTime());
            }
        }

        if (UnloadedActivityCompat.isImmersiveWeatheringLoaded()) {
            FarmWeedMaintenanceService.maintainLoadedFarms(world, context, data);
        }
        VillagerBusinessIntegration.tickMcaVillagersForStores(world, anchor, data, effectiveBusinessRadius, cycleStart);
    }

    private static boolean isGuardRefreshTick(ServerWorld world, TownContext context) {
        long townTick = world.getTime() / TOWN_TICK_INTERVAL;
        int phase = Math.floorMod(context.townId().hashCode(), GUARD_REFRESH_TOWN_TICKS);
        return Math.floorMod(townTick + phase, GUARD_REFRESH_TOWN_TICKS) == 0;
    }

    private static void dropOverflow(ServerWorld world, BlockPos anchor, List<ItemStack> overflow) {
        for (ItemStack stack : overflow) {
            if (stack == null || stack.isEmpty()) continue;
            world.spawnEntity(new ItemEntity(
                    world,
                    anchor.getX() + 0.5D,
                    anchor.getY() + 1.0D,
                    anchor.getZ() + 0.5D,
                    stack.copy()));
        }
    }
}
