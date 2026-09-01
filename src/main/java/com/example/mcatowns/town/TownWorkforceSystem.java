package com.example.mcatowns.town;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Validates explicit villager assignments and provides an optional deterministic auto-fill. */
public final class TownWorkforceSystem {
    private TownWorkforceSystem() { }

    public static void refresh(TownSavedData data) {
        Set<UUID> assignedTownWide = new HashSet<>();
        int requiredTotal = 0;
        int assignedTotal = 0;
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            TownBuildingDefinition definition = TownBuildingDefinition.get(building.type());
            if (definition == null) continue;
            int required = definition.workersRequired();
            requiredTotal += required;
            List<UUID> valid = building.workers().stream()
                    .filter(data.getResidents()::contains)
                    .filter(id -> eligibleFor(data, id, building.type()))
                    .filter(assignedTownWide::add)
                    .limit(required)
                    .toList();
            assignedTotal += valid.size();
            BuildingStatus status = building.status();
            if (status != BuildingStatus.INFRASTRUCTURE_BLOCKED && status != BuildingStatus.NEEDS_INSPECTION) {
                status = valid.size() >= required ? BuildingStatus.ACTIVE : BuildingStatus.UNDERSTAFFED;
            }
            data.replaceBuilding(building.withWorkers(valid, status));
        }
        int efficiency = requiredTotal <= 0 ? 100 : Math.min(100, assignedTotal * 100 / requiredTotal);
        data.setWorkforceAvailable(Math.max(0, data.getResidents().size() - assignedTownWide.size()));
        data.setWorkforceRequired(requiredTotal);
        data.setWorkforceEfficiencyPercent(efficiency);
    }

    public static AssignmentResult assign(TownSavedData data, UUID residentId, UUID buildingId) {
        if (residentId == null || !data.getResidents().contains(residentId)) return AssignmentResult.NOT_RESIDENT;
        RegisteredTownBuilding target = findBuilding(data, buildingId);
        if (target == null) return AssignmentResult.BUILDING_NOT_FOUND;
        TownBuildingDefinition definition = TownBuildingDefinition.get(target.type());
        if (definition == null || definition.workersRequired() <= 0) return AssignmentResult.NO_WORKER_SLOTS;
        if (!eligibleFor(data, residentId, target.type())) return AssignmentResult.WRONG_SPECIALIST_WORKPLACE;
        if (!target.workers().contains(residentId)
                && target.workers().size() >= definition.workersRequired()) return AssignmentResult.BUILDING_FULL;
        unassignInternal(data, residentId);
        target = findBuilding(data, buildingId);
        if (target == null) return AssignmentResult.BUILDING_NOT_FOUND;
        if (target.workers().size() >= definition.workersRequired()) return AssignmentResult.BUILDING_FULL;
        List<UUID> workers = new ArrayList<>(target.workers());
        workers.add(residentId);
        data.replaceBuilding(target.withWorkers(workers, BuildingStatus.ACTIVE));
        refresh(data);
        return AssignmentResult.ASSIGNED;
    }

    public static boolean unassign(TownSavedData data, UUID residentId) {
        boolean changed = unassignInternal(data, residentId);
        if (changed) refresh(data);
        return changed;
    }

    public static void autoAssign(TownSavedData data) {
        refresh(data);
        Set<UUID> used = new LinkedHashSet<>();
        data.getRegisteredBuildings().forEach(building -> used.addAll(building.workers()));
        List<UUID> available = data.getResidents().stream()
                .filter(id -> !used.contains(id))
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            TownBuildingDefinition definition = TownBuildingDefinition.get(building.type());
            if (definition == null || building.workers().size() >= definition.workersRequired()) continue;
            List<UUID> workers = new ArrayList<>(building.workers());
            for (UUID resident : available) {
                if (workers.size() >= definition.workersRequired()) break;
                if (!used.contains(resident) && eligibleFor(data, resident, building.type())) {
                    workers.add(resident);
                    used.add(resident);
                }
            }
            data.replaceBuilding(building.withWorkers(workers, building.status()));
        }
        refresh(data);
    }

    public static RegisteredTownBuilding assignedBuilding(TownSavedData data, UUID residentId) {
        return data.getRegisteredBuildings().stream()
                .filter(building -> building.workers().contains(residentId))
                .findFirst().orElse(null);
    }

    private static boolean unassignInternal(TownSavedData data, UUID residentId) {
        boolean changed = false;
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            if (!building.workers().contains(residentId)) continue;
            List<UUID> workers = new ArrayList<>(building.workers());
            workers.remove(residentId);
            data.replaceBuilding(building.withWorkers(workers, BuildingStatus.UNDERSTAFFED));
            changed = true;
        }
        return changed;
    }

    private static RegisteredTownBuilding findBuilding(TownSavedData data, UUID id) {
        if (id == null) return null;
        return data.getRegisteredBuildings().stream()
                .filter(building -> id.equals(building.id()))
                .findFirst().orElse(null);
    }

    private static boolean eligibleFor(TownSavedData data, UUID residentId, String buildingType) {
        String specialistId = data.getSpecialists().get(residentId);
        if (specialistId == null || specialistId.isBlank()) return true;
        try {
            return SpecialistType.valueOf(specialistId.toUpperCase(Locale.ROOT)).workplace().equals(buildingType);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public enum AssignmentResult {
        ASSIGNED, NOT_RESIDENT, BUILDING_NOT_FOUND, NO_WORKER_SLOTS,
        WRONG_SPECIALIST_WORKPLACE, BUILDING_FULL
    }
}
