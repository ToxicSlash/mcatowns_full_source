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
        int output,
        List<UUID> workers,
        long lastInspectionDay,
        int cropState
) {
    public RegisteredTownBuilding {
        id = id == null ? UUID.randomUUID() : id;
        type = type == null ? "legacy" : type;
        tier = Math.max(1, Math.min(3, tier));
        anchor = anchor == null ? BlockPos.ORIGIN : anchor.toImmutable();
        BlockPos rawMin = minPos == null ? anchor : minPos;
        BlockPos rawMax = maxPos == null ? anchor : maxPos;
        minPos = new BlockPos(Math.min(rawMin.getX(), rawMax.getX()), Math.min(rawMin.getY(), rawMax.getY()),
                Math.min(rawMin.getZ(), rawMax.getZ()));
        maxPos = new BlockPos(Math.max(rawMin.getX(), rawMax.getX()), Math.max(rawMin.getY(), rawMax.getY()),
                Math.max(rawMin.getZ(), rawMax.getZ()));
        status = status == null ? BuildingStatus.ACTIVE : status;
        output = Math.max(0, Math.min(150, output));
        workers = workers == null ? List.of() : List.copyOf(workers);
        cropState = Math.max(0, Math.min(FarmFieldScanner.MAX_CONTRIBUTING_CROPS, cropState));
    }

    public static RegisteredTownBuilding legacy(String type, BlockPos pos) {
        return new RegisteredTownBuilding(UUID.randomUUID(), type, 1, pos, pos, pos,
                BuildingStatus.ACTIVE, 50, List.of(), -1L, 0);
    }

    public BlockPos pos() { return anchor; }

    public RegisteredTownBuilding withStatus(BuildingStatus newStatus) {
        return new RegisteredTownBuilding(id, type, tier, anchor, minPos, maxPos,
                newStatus, output, workers, lastInspectionDay, cropState);
    }

    public RegisteredTownBuilding withWorkers(List<UUID> assignedWorkers, BuildingStatus newStatus) {
        return new RegisteredTownBuilding(id, type, tier, anchor, minPos, maxPos,
                newStatus, output, assignedWorkers, lastInspectionDay, cropState);
    }

    public RegisteredTownBuilding withOutput(int newOutput) {
        return new RegisteredTownBuilding(id, type, tier, anchor, minPos, maxPos,
                status, newOutput, workers, lastInspectionDay, cropState);
    }

    public RegisteredTownBuilding inspected(long day, int newOutput, int newCropState, int newTier) {
        return new RegisteredTownBuilding(id, type, newTier, anchor, minPos, maxPos,
                status, newOutput, workers, day, newCropState);
    }
}
