package com.example.mcatowns.town;

import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

public record RegisteredTownBuilding(
        UUID id,
        String type,
        int tier,
        BlockPos anchor,
        BlockPos minPos,
        BlockPos maxPos,
        BuildingStatus status,
        int quality,
        List<UUID> workers,
        long lastInspectionDay,
        int cropState
) {
    public RegisteredTownBuilding {
        id = id == null ? UUID.randomUUID() : id;
        type = type == null ? "legacy" : type;
        tier = Math.max(1, Math.min(3, tier));
        anchor = anchor == null ? BlockPos.ORIGIN : anchor.toImmutable();
        minPos = minPos == null ? anchor : minPos.toImmutable();
        maxPos = maxPos == null ? anchor : maxPos.toImmutable();
        status = status == null ? BuildingStatus.ACTIVE : status;
        quality = Math.max(0, Math.min(100, quality));
        workers = workers == null ? List.of() : List.copyOf(workers);
        cropState = Math.max(0, Math.min(100, cropState));
    }

    public static RegisteredTownBuilding legacy(String type, BlockPos pos) {
        return new RegisteredTownBuilding(UUID.randomUUID(), type, 1, pos, pos, pos,
                BuildingStatus.ACTIVE, 50, List.of(), -1L, 0);
    }

    public BlockPos pos() { return anchor; }

    public RegisteredTownBuilding withStatus(BuildingStatus newStatus) {
        return new RegisteredTownBuilding(id, type, tier, anchor, minPos, maxPos,
                newStatus, quality, workers, lastInspectionDay, cropState);
    }

    public RegisteredTownBuilding withWorkers(List<UUID> assignedWorkers, BuildingStatus newStatus) {
        return new RegisteredTownBuilding(id, type, tier, anchor, minPos, maxPos,
                newStatus, quality, assignedWorkers, lastInspectionDay, cropState);
    }

    public RegisteredTownBuilding inspected(long day, int newQuality, int newCropState, int newTier) {
        return new RegisteredTownBuilding(id, type, newTier, anchor, minPos, maxPos,
                status, newQuality, workers, day, newCropState);
    }
}
