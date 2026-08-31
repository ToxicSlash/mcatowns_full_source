package com.example.mcatowns.town;

import com.example.mcatowns.event.BlueprintTownCreationHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/** Server-authoritative entry points for the small worker assignment UI. */
public final class TownWorkforceActions {
    private static final UUID UNASSIGNED = new UUID(0L, 0L);

    private TownWorkforceActions() { }

    public static void assign(ServerPlayerEntity player, UUID resident, UUID building) {
        TownContext town = TownBuildingService.editableTown(player);
        if (town == null) return;
        TownSavedData data = TownSavedData.get(player.getServerWorld(), town.townId());
        if (UNASSIGNED.equals(building)) {
            boolean changed = TownWorkforceSystem.unassign(data, resident);
            player.sendMessage(Text.literal(changed ? "Worker unassigned." : "That resident was already unassigned."), true);
        } else {
            TownWorkforceSystem.AssignmentResult result = TownWorkforceSystem.assign(data, resident, building);
            player.sendMessage(Text.literal(assignmentMessage(result)), true);
        }
        TownBuildingService.refreshDerivedValues(data);
        BlueprintTownCreationHandler.openRequestedBlueprint(player);
    }

    public static void autoAssign(ServerPlayerEntity player) {
        TownContext town = TownBuildingService.editableTown(player);
        if (town == null) return;
        TownSavedData data = TownSavedData.get(player.getServerWorld(), town.townId());
        TownWorkforceSystem.autoAssign(data);
        TownBuildingService.refreshDerivedValues(data);
        player.sendMessage(Text.literal("Available residents assigned to open workplaces."), true);
        BlueprintTownCreationHandler.openRequestedBlueprint(player);
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
