package com.example.mcatowns.town;

import com.example.mcatowns.config.MCATownsConfig;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

public final class TownRandomEventSystem {
    private static final String NONE = "";
    private static final String GOOD_HARVEST = "good_harvest";
    private static final String MINE_COLLAPSE = "mine_collapse";
    private static final String FIRE = "fire";
    private static final String GUARD_TRAINING_DAY = "guard_training_day";
    private static final String REFUGEES_ARRIVE = "refugees_arrive";
    private static final String DROUGHT = "drought";
    private static final String DISEASE = "disease";

    private TownRandomEventSystem() {
    }

    public static void tickDaily(ServerWorld world, TownSavedData data, TownBuildingSnapshot snapshot, long day) {
        if (!data.getActiveRandomEvent().isEmpty() && day > data.getActiveRandomEventUntilDay()) {
            clearEvent(data);
        }

        if (data.getNextRandomEventDay() < 0) {
            scheduleNextEvent(world, data, day);
        }

        if (!data.getActiveRandomEvent().isEmpty() || day < data.getNextRandomEventDay()) {
            return;
        }

        String event = pickEvent(world, snapshot);
        applyEvent(data, event, day);
        scheduleNextEvent(world, data, day);
    }

    private static String pickEvent(ServerWorld world, TownBuildingSnapshot snapshot) {
        List<String> candidates = new ArrayList<>();
        candidates.add(FIRE);
        candidates.add(GUARD_TRAINING_DAY);
        candidates.add(REFUGEES_ARRIVE);
        candidates.add(DISEASE);
        if (snapshot.farms() > 0) {
            candidates.add(GOOD_HARVEST);
            candidates.add(DROUGHT);
        }
        if (snapshot.quarries() > 0) {
            candidates.add(MINE_COLLAPSE);
        }
        if (candidates.isEmpty()) {
            return NONE;
        }
        return candidates.get(world.getRandom().nextInt(candidates.size()));
    }

    private static void applyEvent(TownSavedData data, String event, long day) {
        clearEvent(data);
        if (event == null || event.isEmpty()) {
            return;
        }

        data.setActiveRandomEvent(event);
        data.setLastRandomEventDay(day);
        switch (event) {
            case GOOD_HARVEST -> {
                data.setEventFarmOutputPercent(150);
                data.setEventTaxPercent(110);
                data.setActiveRandomEventUntilDay(day + 2);
            }
            case MINE_COLLAPSE -> {
                data.setEventQuarryOutputPercent(40);
                data.setActiveRandomEventUntilDay(day + 3);
            }
            case FIRE -> {
                data.setEventTaxPercent(80);
                data.setEventDefenseBonus(-6);
                data.setEventHappinessModifier(-5);
                data.setActiveRandomEventUntilDay(day + 3);
            }
            case GUARD_TRAINING_DAY -> {
                data.setEventDefenseBonus(16);
                data.setActiveRandomEventUntilDay(day + 2);
            }
            case REFUGEES_ARRIVE -> {
                data.setRefugeePopulationBonus(6);
                data.setEventExtraFoodDemand(10);
                data.setActiveRandomEventUntilDay(day + 4);
            }
            case DROUGHT -> {
                data.setEventFarmOutputPercent(55);
                data.setActiveRandomEventUntilDay(day + 4);
            }
            case DISEASE -> {
                data.setEventWorkforcePercent(75);
                data.setEventHappinessModifier(-15);
                data.setActiveRandomEventUntilDay(day + 4);
            }
            default -> clearEvent(data);
        }
    }

    private static void clearEvent(TownSavedData data) {
        data.setActiveRandomEvent(NONE);
        data.setActiveRandomEventUntilDay(-1);
        data.setEventFarmOutputPercent(100);
        data.setEventQuarryOutputPercent(100);
        data.setEventTaxPercent(100);
        data.setEventWorkforcePercent(100);
        data.setEventDefenseBonus(0);
        data.setEventHappinessModifier(0);
        data.setEventExtraFoodDemand(0);
        data.setRefugeePopulationBonus(0);
    }

    private static void scheduleNextEvent(ServerWorld world, TownSavedData data, long day) {
        int min = MCATownsConfig.get().randomEventMinDays;
        int max = MCATownsConfig.get().randomEventMaxDays;
        int inDays = min + world.getRandom().nextInt(Math.max(1, max - min + 1));
        data.setNextRandomEventDay(day + inDays);
    }
}
