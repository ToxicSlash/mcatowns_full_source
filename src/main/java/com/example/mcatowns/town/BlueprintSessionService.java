package com.example.mcatowns.town;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlueprintSessionService {
    private static final Map<UUID, SessionState> STATES = new ConcurrentHashMap<>();

    private BlueprintSessionService() {
    }

    public static SessionState get(ServerPlayerEntity player) {
        return STATES.computeIfAbsent(player.getUuid(), ignored -> new SessionState());
    }

    public static void clear(UUID playerId) {
        if (playerId != null) STATES.remove(playerId);
    }

    public static void setDetected(ServerPlayerEntity player, List<BlockPos> detected, BlockPos primary) {
        SessionState state = get(player);
        state.detected = List.copyOf(detected);
        state.primaryDetected = primary;
        state.detectedAtTick = player.getServerWorld().getTime();
        state.inspectionType = "";
        state.inspectionPassed = false;
        state.inspectionTarget = BlockPos.ORIGIN;
        state.costLines = List.of();
        state.furnitureLines = List.of();
    }

    /** Updates the live map marker without discarding an active inspection. */
    public static void updateDetected(ServerPlayerEntity player, List<BlockPos> detected, BlockPos primary) {
        SessionState state = get(player);
        BlockPos nextPrimary = primary == null ? BlockPos.ORIGIN : primary.toImmutable();
        if (!state.primaryDetected.equals(nextPrimary)) {
            state.inspectionType = "";
            state.inspectionPassed = false;
            state.inspectionTarget = BlockPos.ORIGIN;
            state.costLines = List.of();
            state.furnitureLines = List.of();
            state.detectedAtTick = player.getServerWorld().getTime();
        }
        state.detected = List.copyOf(detected == null ? List.of() : detected);
        state.primaryDetected = nextPrimary;
    }

    public static void setInspection(ServerPlayerEntity player, String type, boolean passed, BlockPos target,
                                     List<RequirementLine> costLines, List<RequirementLine> furnitureLines) {
        SessionState state = get(player);
        state.inspectionType = type == null ? "" : type;
        state.inspectionPassed = passed;
        state.inspectionTarget = target == null ? BlockPos.ORIGIN : target.toImmutable();
        state.costLines = List.copyOf(costLines);
        state.furnitureLines = List.copyOf(furnitureLines);
    }

    public static final class SessionState {
        private List<BlockPos> detected = List.of();
        private BlockPos primaryDetected = BlockPos.ORIGIN;
        private long detectedAtTick;
        private String inspectionType = "";
        private boolean inspectionPassed;
        private BlockPos inspectionTarget = BlockPos.ORIGIN;
        private List<RequirementLine> costLines = List.of();
        private List<RequirementLine> furnitureLines = List.of();

        public List<BlockPos> detected() { return detected; }
        public BlockPos primaryDetected() { return primaryDetected; }
        public long detectedAtTick() { return detectedAtTick; }
        public String inspectionType() { return inspectionType; }
        public boolean inspectionPassed() { return inspectionPassed; }
        public BlockPos inspectionTarget() { return inspectionTarget; }
        public List<RequirementLine> costLines() { return costLines; }
        public List<RequirementLine> furnitureLines() { return furnitureLines; }
    }

    public record RequirementLine(String text, int state) {
    }
}
