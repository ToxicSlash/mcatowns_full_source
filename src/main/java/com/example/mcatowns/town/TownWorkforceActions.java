package com.example.mcatowns.town;

import com.example.mcatowns.event.BlueprintTownCreationHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

/** Server-authoritative entry points for the small worker assignment UI. */
public final class TownWorkforceActions {
    private static final UUID UNASSIGNED = new UUID(0L, 0L);

    private TownWorkforceActions() { }

    public static void assign(ServerPlayerEntity player, UUID resident, UUID building) {
        TownContext town = TownBuildingService.editableTown(player);
        if (town == null) return;
        TownSavedData data = TownSavedData.get(player.getServerWorld(), town.townId());
        RegisteredTownBuilding previous = TownWorkforceSystem.assignedBuilding(data, resident);

        if (UNASSIGNED.equals(building)) {
            boolean changed = TownWorkforceSystem.unassign(data, resident);
            player.sendMessage(Text.literal(changed ? "Worker unassigned." : "That resident was already unassigned."), true);
            if (changed && previous != null) refreshOutput(player, data, previous.id());
        } else {
            TownWorkforceSystem.AssignmentResult result = TownWorkforceSystem.assign(data, resident, building);
            player.sendMessage(Text.literal(assignmentMessage(result)), true);
            if (result == TownWorkforceSystem.AssignmentResult.ASSIGNED) {
                if (previous != null) refreshOutput(player, data, previous.id());
                refreshOutput(player, data, building);
            }
        }
        BlueprintTownCreationHandler.openRequestedBlueprint(player);
    }

    public static void autoAssign(ServerPlayerEntity player) {
        TownContext town = TownBuildingService.editableTown(player);
        if (town == null) return;
        TownSavedData data = TownSavedData.get(player.getServerWorld(), town.townId());
        TownWorkforceSystem.autoAssign(data);
        refreshAllOutputs(player, data);
        player.sendMessage(Text.literal("Available residents assigned to open workplaces."), true);
        BlueprintTownCreationHandler.openRequestedBlueprint(player);
    }

    private static void refreshOutput(ServerPlayerEntity player, TownSavedData data, UUID buildingId) {
        if (buildingId == null) return;
        RegisteredTownBuilding building = data.getRegisteredBuildings().stream()
                .filter(entry -> buildingId.equals(entry.id()))
                .findFirst().orElse(null);
        if (building == null || !player.getServerWorld().isChunkLoaded(building.anchor())) return;
        int output = BuildingPerformance.calculateOutput(player.getServerWorld(), data, building);
        data.replaceBuilding(building.withOutput(output));
    }

    private static void refreshAllOutputs(ServerPlayerEntity player, TownSavedData data) {
        List<RegisteredTownBuilding> buildings = List.copyOf(data.getRegisteredBuildings());
        for (RegisteredTownBuilding building : buildings) {
            if (!player.getServerWorld().isChunkLoaded(building.anchor())) continue;
            int output = BuildingPerformance.calculateOutput(player.getServerWorld(), data, building);
            data.replaceBuilding(building.withOutput(output));
        }
    }

    private static String assignmentMessage(TownWorkforceSystem.AssignmentResult result) {
        return switch (result) {
            case ASSIGNED -> "Worker assigned.";
            case NOT_RESIDENT -> "That villager is not a town resident.";
            case BUILDING_NOT_FOUND -> "That workplace no longer exists.";
            case NO_WORKER_SLOTS -> "That building has no worker slots.";
            case WRONG_SPECIALIST_WORKPLACE -> "That specialist requires their matching workplace.";
            case BUILDING_FULL -> "That workplace is full.";
        };
    }
}
