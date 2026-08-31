package com.example.mcatowns.town;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Assigns actual recruited residents to registered buildings and derives the legacy summary fields. */
public final class TownWorkforceSystem {
    private TownWorkforceSystem() { }

    public static void refresh(TownSavedData data) {
        Set<UUID> assigned = new LinkedHashSet<>();
        List<UUID> generalResidents = data.getResidents().stream()
                .filter(id -> !data.getSpecialists().containsKey(id))
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        int requiredTotal = 0;
        int assignedTotal = 0;

        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            TownBuildingDefinition definition = TownBuildingDefinition.get(building.type());
            if (definition == null) continue;
            int required = definition.workersRequired();
            requiredTotal += required;

            if (building.status() == BuildingStatus.INFRASTRUCTURE_BLOCKED
                    || building.status() == BuildingStatus.NEEDS_INSPECTION) {
                data.replaceBuilding(building.withWorkers(List.of(), building.status()));
                continue;
            }

            List<UUID> workers = new ArrayList<>();
            findRequiredSpecialist(building.type(), data.getSpecialists()).ifPresent(id -> {
                if (assigned.add(id)) workers.add(id);
            });
            for (UUID resident : generalResidents) {
                if (workers.size() >= required) break;
                if (assigned.add(resident)) workers.add(resident);
            }

            assignedTotal += workers.size();
            BuildingStatus status = workers.size() >= required ? BuildingStatus.ACTIVE : BuildingStatus.UNDERSTAFFED;
            data.replaceBuilding(building.withWorkers(workers, status));
        }

        int effectivePopulation = Math.max(data.getPopulation(), data.getResidents().size())
                + data.getRefugeePopulationBonus() + data.getCaravanPopulationBonus();
        int efficiency = requiredTotal <= 0 ? 100 : Math.min(100, assignedTotal * 100 / requiredTotal);
        data.setWorkforceAvailable(Math.max(0, effectivePopulation - assignedTotal));
        data.setWorkforceRequired(requiredTotal);
        data.setWorkforceEfficiencyPercent(efficiency);
    }

    /** Compatibility entry point for old detected-building callers. */
    public static void refresh(TownSavedData data, TownBuildingSnapshot ignored) {
        refresh(data);
    }

    private static java.util.Optional<UUID> findRequiredSpecialist(String buildingType, Map<UUID, String> specialists) {
        return specialists.entrySet().stream()
                .filter(entry -> {
                    try {
                        return SpecialistType.valueOf(entry.getValue().toUpperCase(java.util.Locale.ROOT)).workplace().equals(buildingType);
                    } catch (IllegalArgumentException ignored) {
                        return false;
                    }
                })
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
