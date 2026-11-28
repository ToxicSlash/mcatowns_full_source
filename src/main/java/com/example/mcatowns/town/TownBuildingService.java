package com.example.mcatowns.town;

import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.event.TownRemovalHandler;
import com.example.mcatowns.registry.ModBlocks;
import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.util.InventoryHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Comparator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TownBuildingService {
    private static final Identifier BLUEPRINT = new Identifier("mca", "blueprint");
    private static final Set<String> RESIDENCE_TYPES = Set.of("house", "big_house", "building");
    private static final long OPEN_DETECTION_TTL_TICKS = 60L * 20L;
    private static final Set<String> OPEN_STRUCTURE_TYPES = Set.of("campfire", "park", "bounty_board");
    private static final int MIN_ENCLOSED_FLOOR_AREA = 9;
    private static final int ENCLOSED_TRACE_RADIUS = 24;
    private static final int MAX_ENCLOSED_TRACE_CELLS = 2048;

    private TownBuildingService() {
    }

    public record DetectedStructure(String type, String name, boolean enclosed,
                                    BlockPos anchor, BlockPos min, BlockPos max,
                                    List<BlockPos> footprint) {
    }

    public static void register(ServerPlayerEntity player, String type) {
        TownBuildingDefinition definition = TownBuildingDefinition.get(type);
        TownContext town = editableTown(player);
        if (definition == null || town == null || !isHoldingBlueprint(player)) return;

        ServerWorld world = player.getServerWorld();
        TownSavedData data = TownSavedData.get(world, town.townId());
        BlueprintSessionService.SessionState session = BlueprintSessionService.get(player);
        if (player.getBlockPos().getSquaredDistance(town.center()) > 64.0 * 64.0) {
            player.sendMessage(Text.translatable("text.mcatowns.outside_town_boundary"), true);
            return;
        }
        if (!data.isBuildingUnlocked(type)) {
            player.sendMessage(Text.translatable("text.mcatowns.building_locked"), true);
            return;
        }
        if (data.getRegisteredBuildingCount() >= data.getTownRank().maxBuildings()) {
            player.sendMessage(Text.translatable("text.mcatowns.building_limit"), true);
            return;
        }
        if (!TownDefenceInfrastructure.canRegisterMore(data, type)) {
            player.sendMessage(Text.literal("Town limit reached for " + definition.displayName()
                    + " (maximum " + definition.maxPerTown() + ")."), true);
            return;
        }
        if (data.getProsperity() < definition.prosperityRequired()) {
            player.sendMessage(Text.translatable("text.mcatowns.need_prosperity", definition.prosperityRequired()), true);
            return;
        }
        if (!canAllocateInfrastructure(data, definition)) {
            player.sendMessage(Text.translatable("text.mcatowns.infrastructure_missing",
                    infrastructureShortage(data, definition)), true);
            return;
        }
        Identifier currencyId = Identifier.tryParse(MCATownsConfig.get().currencyItemId);
        Item currency = currencyId != null && Registries.ITEM.containsId(currencyId)
                ? Registries.ITEM.get(currencyId) : Registries.ITEM.get(new Identifier("minecraft", "emerald"));
        if (!player.getAbilities().creativeMode
                && (data.getTownTokens() < definition.registrationTokens()
                || InventoryHelper.count(player.getInventory(), currency) < definition.registrationCurrency())) {
            player.sendMessage(Text.translatable("text.mcatowns.registration_cost_missing",
                    definition.registrationTokens(), definition.registrationCurrency()), true);
            return;
        }
        if (!session.inspectionPassed() || !type.equals(session.inspectionType()) || session.inspectionTarget().equals(BlockPos.ORIGIN)) {
            player.sendMessage(Text.literal("Inspect the selected building successfully before registering."), true);
            return;
        }

        BlockPos pos = validateAndLocate(world, session.inspectionTarget(), definition);
        if (pos == null || data.getRegisteredBuildings().stream()
                .anyMatch(building -> building.pos().getSquaredDistance(pos) < 9.0)) {
            player.sendMessage(Text.translatable("text.mcatowns.invalid_building", definition.displayName()), true);
            return;
        }
        // Inspection sessions can outlive a player's movement. Recheck the actual
        // target, not only the player's position, before charging or registering.
        if (pos.getSquaredDistance(town.center()) > 64.0 * 64.0 || !world.isChunkLoaded(pos)) {
            player.sendMessage(Text.translatable("text.mcatowns.outside_town_boundary"), true);
            return;
        }
        FarmFieldScanner.Result farm = "farm".equals(type) ? FarmFieldScanner.scan(world, pos) : null;
        if (farm != null && isFarmAlreadyRegistered(data, farm)) {
            player.sendMessage(Text.literal("This connected farm is already registered."), true);
            return;
        }
        DetectedStructure detected = describeDetectedStructure(world, pos).orElse(null);
        MCAIntegration.BuildingBounds detectedBounds = farm != null
                ? new MCAIntegration.BuildingBounds(farm.min(), farm.max())
                : detected != null && detected.enclosed() && !detected.footprint().isEmpty()
                ? boundsForFootprint(detected.footprint(), pos)
                : MCAIntegration.getBuildingBoundsAt(world, pos)
                .orElse(new MCAIntegration.BuildingBounds(pos, pos));
        BlockPos anchor = farm == null ? pos : farm.anchor();
        int tier = farm == null ? 1 : farm.tier();
        int cropState = farm == null ? 0 : farm.contributingCrops();
        RegisteredTownBuilding registered = new RegisteredTownBuilding(UUID.randomUUID(), type, tier, anchor,
                detectedBounds.min(), detectedBounds.max(), BuildingStatus.ACTIVE, 0, List.of(),
                TownManager.getDay(world), cropState);
        if (!data.registerBuilding(registered)) return;

        // The structure is now represented by the registered entry. Remove
        // the temporary inspection marker so it is not drawn twice.
        BlueprintSessionService.updateDetected(player, List.of(), BlockPos.ORIGIN);

        if (!player.getAbilities().creativeMode) {
            data.spendTownTokens(definition.registrationTokens());
            InventoryHelper.remove(player.getInventory(), currency, definition.registrationCurrency());
        }
        refreshInspectionState(world, data, TownManager.getDay(world));
        player.sendMessage(Text.translatable("text.mcatowns.building_registered_named", definition.displayName()), false);
    }

    public static void detect(ServerPlayerEntity player) {
        TownContext town = editableTown(player);
        if (town == null || !isHoldingBlueprint(player)) return;
        BlockPos target = detectPosition(player.getServerWorld(), player);
        if (target == null) {
            player.sendMessage(Text.literal("No supported structure could be detected here."), true);
            return;
        }
        BlueprintSessionService.setDetected(player, List.of(target), target);
        player.sendMessage(Text.literal("Building detected."), true);
    }

    /** Refreshes the map marker while a player is carrying the blueprint. */
    public static void periodicDetect(ServerPlayerEntity player) {
        TownContext town = editableTown(player);
        if (!isHoldingBlueprint(player) || town == null) return;
        TownSavedData data = TownSavedData.get(player.getServerWorld(), town.townId());
        BlueprintSessionService.SessionState session = BlueprintSessionService.get(player);
        BlockPos target = detectNearbyPlayer(player);
        if (target != null) {
            BlockPos detectedTarget = target;
            if (data.getRegisteredBuildings().stream()
                    .anyMatch(building -> building.pos().getSquaredDistance(detectedTarget) < 9.0)) {
                target = null;
            }
        }
        // Open structures are opt-in: once inspected, keep that inspected
        // marker on the map, but never discover new campfires, parks, or
        // boards merely because the player walked near them.
        if (target == null && !session.inspectionTarget().equals(BlockPos.ORIGIN)
                && !session.inspectionType().isBlank() && !openDetectionExpired(player, session)) {
            target = session.inspectionTarget();
        }
        BlueprintSessionService.updateDetected(player,
                target == null ? List.of() : List.of(target), target);
    }

    public static void expireUnregisteredOpenDetection(ServerPlayerEntity player) {
        BlueprintSessionService.SessionState session = BlueprintSessionService.get(player);
        if (openDetectionExpired(player, session)) {
            BlueprintSessionService.updateDetected(player, List.of(), BlockPos.ORIGIN);
        }
    }

    public static Optional<DetectedStructure> describeDetectedStructure(ServerWorld world, BlockPos pos) {
        if (FarmFieldScanner.farmlandAt(world, pos) != null) {
            FarmFieldScanner.Result field = FarmFieldScanner.scan(world, pos);
            return Optional.of(new DetectedStructure("farm", "Farm", false, field.anchor(),
                    field.min(), field.max(), field.tiles()));
        }
        Optional<String> mcaType = MCAIntegration.getBuildingTypeAt(world, pos);
        if (mcaType.isPresent()) {
            // MCA's block map includes blocks at roof height. Never project that
            // directly onto the floor plan: overhangs are not walls.
            List<BlockPos> footprint = traceEnclosedFootprint(world, pos);
            for (int dy : new int[]{-1, 1, -2, 2}) {
                if (!footprint.isEmpty()) break;
                footprint = traceEnclosedFootprint(world, pos.add(0, dy, 0));
            }
            MCAIntegration.BuildingBounds bounds = boundsForFootprint(footprint, pos);
            return Optional.of(new DetectedStructure("mca_" + mcaType.get(), "Unregistered Structure", true,
                    pos, bounds.min(), bounds.max(), footprint));
        }
        if (isEither(world, pos, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE)) {
            return Optional.of(new DetectedStructure("campfire", "Campfire", false, pos, pos, pos, List.of()));
        }
        if (isBountyBoard(world, pos)) {
            return Optional.of(new DetectedStructure("bounty_board", "Bounty Board", false, pos, pos, pos, List.of()));
        }
        if (validatePark(world, pos)) {
            return Optional.of(new DetectedStructure("park", "Park", false, pos, pos, pos, List.of()));
        }
        List<BlockPos> enclosedFootprint = traceEnclosedFootprint(world, pos);
        if (!enclosedFootprint.isEmpty()) {
            MCAIntegration.BuildingBounds bounds = boundsForFootprint(enclosedFootprint, pos);
            return Optional.of(new DetectedStructure("unregistered_structure", "Unregistered Structure", true,
                    pos, bounds.min(), bounds.max(), enclosedFootprint));
        }
        return Optional.empty();
    }

    public static boolean isEnclosedStructureValid(ServerWorld world, BlockPos pos) {
        return describeDetectedStructure(world, pos)
                .map(DetectedStructure::enclosed)
                .orElse(false);
    }

    public static void inspect(ServerPlayerEntity player, String type) {
        TownBuildingDefinition definition = TownBuildingDefinition.get(type);
        TownContext town = editableTown(player);
        if (definition == null || town == null || !isHoldingBlueprint(player)) return;
        BlueprintSessionService.SessionState session = BlueprintSessionService.get(player);
        BlockPos target = session.primaryDetected().equals(BlockPos.ORIGIN)
                ? java.util.Optional.ofNullable(detectPosition(player.getServerWorld(), player))
                    .orElseGet(() -> detectNearbyPlayer(player))
                : session.primaryDetected();
        if (target == null) {
            player.sendMessage(Text.literal("No supported structure could be detected here."), true);
            return;
        }
        // Inspection is also a detection action. This makes the map show the
        // structure being inspected even when the player never pressed Detect.
        BlueprintSessionService.updateDetected(player, List.of(target), target);
        session = BlueprintSessionService.get(player);
        BlockPos validated = validateAndLocate(player.getServerWorld(), target, definition);
        boolean passed = validated != null;
        BlueprintSessionService.setInspection(player, type, passed, passed ? validated : target,
                costRequirementLines(player, definition), furnitureRequirementLines(player.getServerWorld(),
                        passed ? validated : target, definition, passed));
    }

    public static void refreshDerivedValues(TownSavedData data) {
        refreshInfrastructureStatuses(data);
        TownWorkforceSystem.refresh(data);
        int populationCapacity = 0;
        int foodCapacity = 0;
        int floor = 0;
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            TownBuildingDefinition definition = TownBuildingDefinition.get(building.type());
            if (definition == null || building.status() == BuildingStatus.INFRASTRUCTURE_BLOCKED) continue;
            populationCapacity += definition.populationCapacityForTier(building.tier());
            foodCapacity += definition.foodCapacity();
            floor += definition.prosperityBase();
        }
        data.setProgressionCapacities(Math.min(data.getTownRank().maxOccupancy(), populationCapacity), foodCapacity);
        data.setProsperityBase(Math.min(data.getTownRank().maxProsperity(), floor));
    }

    public static boolean isFarmAlreadyRegistered(TownSavedData data, FarmFieldScanner.Result field) {
        return data.getRegisteredBuildings().stream()
                .anyMatch(building -> "farm".equals(building.type()) && field.tiles().contains(building.anchor()));
    }

    // Do not overwrite known field bounds/crop counts from a partially unloaded field.
    static boolean farmBoundsLoaded(RegisteredTownBuilding building, java.util.function.Predicate<BlockPos> loaded) {
        int minX = building.minPos().getX() >> 4, maxX = building.maxPos().getX() >> 4;
        int minZ = building.minPos().getZ() >> 4, maxZ = building.maxPos().getZ() >> 4;
        // Malformed or ancient bounds must not cause unbounded chunk probes.
        if ((long) (maxX - minX + 1) * (maxZ - minZ + 1) > 4096) return false;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!loaded.test(new BlockPos(x << 4, building.anchor().getY(), z << 4))) return false;
            }
        }
        return true;
    }

    public static void refreshInspectionState(ServerWorld world, TownSavedData data, long day) {
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            if (!world.isChunkLoaded(building.anchor())) continue;
            try {
                RegisteredTownBuilding updated = building;
                if ("farm".equals(building.type())) {
                    if (!farmBoundsLoaded(building, world::isChunkLoaded)) continue;
                    FarmFieldScanner.Result field = FarmFieldScanner.scan(world, building.anchor());
                    // A damaged anchor must not permanently erase the old field bounds.
                    BlockPos min = field.tiles().isEmpty() ? building.minPos() : field.min();
                    BlockPos max = field.tiles().isEmpty() ? building.maxPos() : field.max();
                    updated = new RegisteredTownBuilding(building.id(), building.type(), field.tier(), building.anchor(),
                            min, max, field.valid() ? BuildingStatus.ACTIVE : BuildingStatus.NEEDS_INSPECTION,
                            building.output(), building.workers(), day, field.contributingCrops());
                }
                int output = BuildingPerformance.calculateOutput(world, data, updated);
                updated = updated.inspected(day, output, updated.cropState(), updated.tier());
                data.replaceBuilding(updated);
            } catch (RuntimeException exception) {
                com.example.mcatowns.MCATowns.LOGGER.warn("Could not inspect town building {} at {}",
                        building.id(), building.anchor(), exception);
                data.replaceBuilding(building.withStatus(BuildingStatus.NEEDS_INSPECTION));
            }
        }
        refreshDerivedValues(data);
    }

    public static boolean canAllocateInfrastructure(TownSavedData data, TownBuildingDefinition candidate) {
        for (InfrastructureType type : InfrastructureType.values()) {
            int provided = data.getInfrastructureProvided(type)
                    + candidate.providedInfrastructure().getOrDefault(type, 0);
            int reserved = data.getInfrastructureReserved(type)
                    + candidate.reservedInfrastructure().getOrDefault(type, 0);
            if (reserved > provided) return false;
        }
        return true;
    }

    private static String infrastructureShortage(TownSavedData data, TownBuildingDefinition candidate) {
        return java.util.Arrays.stream(InfrastructureType.values())
                .filter(type -> data.getInfrastructureReserved(type)
                        + candidate.reservedInfrastructure().getOrDefault(type, 0)
                        > data.getInfrastructureProvided(type)
                        + candidate.providedInfrastructure().getOrDefault(type, 0))
                .map(InfrastructureType::displayName)
                .findFirst().orElse("Infrastructure");
    }

    private static void refreshInfrastructureStatuses(TownSavedData data) {
        Set<InfrastructureType> shortages = java.util.Arrays.stream(InfrastructureType.values())
                .filter(type -> data.getInfrastructureReserved(type) > data.getInfrastructureProvided(type))
                .collect(java.util.stream.Collectors.toSet());
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            TownBuildingDefinition definition = TownBuildingDefinition.get(building.type());
            if (definition == null) continue;
            boolean blocked = definition.reservedInfrastructure().keySet().stream().anyMatch(shortages::contains);
            if (blocked && building.status() != BuildingStatus.INFRASTRUCTURE_BLOCKED) {
                data.replaceBuilding(building.withStatus(BuildingStatus.INFRASTRUCTURE_BLOCKED));
            } else if (!blocked && building.status() == BuildingStatus.INFRASTRUCTURE_BLOCKED) {
                data.replaceBuilding(building.withStatus(BuildingStatus.ACTIVE));
            }
        }
    }

    public static void unregisterNearest(ServerPlayerEntity player) {
        TownContext town = editableTown(player);
        if (town == null || !isHoldingBlueprint(player)) return;
        TownSavedData data = TownSavedData.get(player.getServerWorld(), town.townId());
        RegisteredTownBuilding target = data.getRegisteredBuildings().stream()
                .filter(building -> building.pos().getSquaredDistance(player.getBlockPos()) <= 12 * 12)
                .min(Comparator.comparingDouble(building -> building.pos().getSquaredDistance(player.getBlockPos())))
                .orElse(null);
        if (target == null) {
            player.sendMessage(Text.literal("No registered building is close enough to remove."), true);
            return;
        }
        if (data.unregisterBuilding(target.pos())) {
            refreshDerivedValues(data);
            player.sendMessage(Text.literal("Removed building: " + target.type()), true);
        }
    }

    private static BlockPos validateAndLocate(ServerWorld world, BlockPos target, TownBuildingDefinition definition) {
        BlockPos lookedAt = target;
        return switch (definition.id()) {
            case "residence" -> validateResidence(world, lookedAt);
            case "blacksmith" -> validateMcaInterior(world, lookedAt, Set.of("blacksmith", "weaponsmith", "toolsmith"), false);
            case "inn" -> validateMcaInterior(world, lookedAt, Set.of("inn"), false);
            case "scholar" -> validateMcaInterior(world, lookedAt, Set.of("library"), false);
            case "farm" -> validateFarm(world, lookedAt) ? lookedAt : null;
            case "campfire" -> findNearbyCampfire(world, lookedAt, 6);
            case "bounty_board" -> findNearbyBountyBoard(world, lookedAt, 6);
            case "civic_office" -> world.getBlockState(lookedAt).isOf(ModBlocks.MAYOR_DESK) ? lookedAt : null;
            case "storehouse" -> world.getBlockState(lookedAt).isOf(ModBlocks.STOREHOUSE) ? lookedAt : null;
            case "granary" -> world.getBlockState(lookedAt).isOf(ModBlocks.SILO) ? lookedAt : null;
            case "guard_post" -> world.getBlockState(lookedAt).isOf(ModBlocks.BARRACKS) ? lookedAt : null;
            case "park" -> findNearbyPark(world, lookedAt, 6);
            case "jeweler" -> isEither(world, lookedAt, Blocks.SMITHING_TABLE, Blocks.GOLD_BLOCK)
                    && countNearby(world, lookedAt, 4, block -> block == Blocks.GOLD_BLOCK) >= 1 ? lookedAt : null;
            default -> null;
        };
    }

    static TownContext editableTown(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        if (TownRemovalHandler.hasAdminAccess(player)) {
            return TownManager.findExistingTown(world, player.getBlockPos(), Math.max(64, TownManager.getTownSearchMargin())).orElse(null);
        }
        return PlayerTownRegistry.get(world).getOwnedTown(player.getUuid()).orElse(null);
    }

    private static BlockPos detectPosition(ServerWorld world, ServerPlayerEntity player) {
        // A player standing in farmland should detect that field regardless
        // of the block their cursor happens to be pointing at.
        BlockPos fieldUnderPlayer = findNearbyFarm(world, player.getBlockPos(), 1);
        if (fieldUnderPlayer != null) {
            return fieldUnderPlayer;
        }
        BlockPos lookedAt = lookedAt(player);
        Optional<BlockPos> mcaCenter = MCAIntegration.getBuildingCenterAt(world, lookedAt);
        if (mcaCenter.isPresent()) {
            return mcaCenter.get();
        }

        BlockPos bountyBoard = findNearbyBountyBoard(world, lookedAt, 6);
        if (bountyBoard != null) {
            return bountyBoard;
        }

        BlockPos campfire = findNearbyCampfire(world, lookedAt, 6);
        if (campfire != null) {
            return campfire;
        }

        BlockPos selectedFarmland = FarmFieldScanner.farmlandAt(world, lookedAt);
        if (selectedFarmland != null) return selectedFarmland;
        BlockPos nearbyFarm = findNearbyFarm(world, player.getBlockPos(), 8);
        if (nearbyFarm != null) {
            return nearbyFarm;
        }
        if (isEither(world, lookedAt, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE)
                || world.getBlockState(lookedAt).isOf(ModBlocks.MAYOR_DESK)
                || world.getBlockState(lookedAt).isOf(ModBlocks.STOREHOUSE)
                || world.getBlockState(lookedAt).isOf(ModBlocks.SILO)
                || world.getBlockState(lookedAt).isOf(ModBlocks.BARRACKS)) {
            return lookedAt;
        }

        // Parks are intentionally open structures. They are detected from
        // their furniture/decor rather than from an enclosure or a single
        // cursor block.
        BlockPos park = findNearbyPark(world, lookedAt, 6);
        if (park != null) {
            return park;
        }

        // A locally built residence is detected from the player's connected
        // enclosed interior, so looking at a door or wall is not required.
        List<BlockPos> enclosedFootprint = traceEnclosedFootprint(world, player.getBlockPos());
        if (!enclosedFootprint.isEmpty()) {
            return player.getBlockPos();
        }
        return null;
    }

    private static BlockPos detectNearbyPlayer(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos origin = player.getBlockPos();
        BlockPos fieldUnderPlayer = findNearbyFarm(world, origin, 1);
        if (fieldUnderPlayer != null) return fieldUnderPlayer;
        if (!traceEnclosedFootprint(world, origin).isEmpty()) return origin;

        BlockPos farm = findNearbyFarm(world, origin, 8);
        if (farm != null) return farm;

        // The player is usually standing in air inside an MCA building, so
        // probe nearby solid structure blocks instead of requiring the player
        // position itself to be one of MCA's registered blocks.
        for (BlockPos probe : BlockPos.iterate(origin.add(-4, -2, -4), origin.add(4, 3, 4))) {
            if (world.getBlockState(probe).isAir()) continue;
            Optional<BlockPos> center = MCAIntegration.getBuildingCenterAt(world, probe);
            if (center.isPresent() && describeDetectedStructure(world, center.get())
                    .map(DetectedStructure::enclosed).orElse(false)) {
                return center.get();
            }
        }
        return null;
    }

    private static MCAIntegration.BuildingBounds boundsForFootprint(List<BlockPos> footprint, BlockPos fallback) {
        if (footprint == null || footprint.isEmpty()) {
            return new MCAIntegration.BuildingBounds(fallback, fallback);
        }
        BlockPos min = footprint.get(0);
        BlockPos max = footprint.get(0);
        for (BlockPos pos : footprint) {
            min = new BlockPos(Math.min(min.getX(), pos.getX()), min.getY(), Math.min(min.getZ(), pos.getZ()));
            max = new BlockPos(Math.max(max.getX(), pos.getX()), max.getY(), Math.max(max.getZ(), pos.getZ()));
        }
        return new MCAIntegration.BuildingBounds(min, max);
    }

    private static List<BlockPos> traceEnclosedFootprint(ServerWorld world, BlockPos center) {
        BlockPos start = findInteriorStart(world, center);
        if (start == null) return List.of();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> footprint = new ArrayList<>();
        boolean reachedTraceLimit = false;
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (!visited.add(current) || !isInteriorCell(world, current, start.getY())) continue;
            if (footprint.size() >= MAX_ENCLOSED_TRACE_CELLS) return List.of();
            footprint.add(current.toImmutable());
            for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
                BlockPos next = current.offset(direction);
                int dx = Math.abs(next.getX() - start.getX());
                int dz = Math.abs(next.getZ() - start.getZ());
                if (dx < ENCLOSED_TRACE_RADIUS && dz < ENCLOSED_TRACE_RADIUS) {
                    queue.addLast(next);
                } else if (isInteriorCell(world, next, start.getY())) {
                    // A room that reaches the safety boundary is treated as
                    // open/too large rather than returning a partial outline.
                    reachedTraceLimit = true;
                }
            }
        }
        if (reachedTraceLimit || footprint.size() < MIN_ENCLOSED_FLOOR_AREA) {
            return List.of();
        }
        includeInteriorSolidDecorations(world, footprint, start.getY());
        if (!hasClosedInteriorBoundary(world, footprint, start.getY())) {
            return List.of();
        }
        return List.copyOf(footprint);
    }

    private static void includeInteriorSolidDecorations(ServerWorld world, List<BlockPos> footprint, int y) {
        Set<Long> cells = new HashSet<>();
        for (BlockPos pos : footprint) {
            cells.add(mapCellKey(pos.getX(), pos.getZ()));
        }

        boolean changed;
        do {
            changed = false;
            List<BlockPos> additions = new ArrayList<>();
            for (BlockPos pos : footprint) {
                for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
                    BlockPos candidate = pos.offset(direction);
                    if (candidate.getY() != y || cells.contains(mapCellKey(candidate.getX(), candidate.getZ()))
                            || !world.isChunkLoaded(candidate) || !world.getWorldBorder().contains(candidate)
                            || !isSolidStructureBlock(world, candidate) || !hasRoof(world, candidate, 12)) {
                        continue;
                    }
                    if (isInteriorFurnitureBlock(world.getBlockState(candidate).getBlock())
                            && !hasOutsideAirNeighbor(world, candidate, cells)) {
                        cells.add(mapCellKey(candidate.getX(), candidate.getZ()));
                        additions.add(candidate.toImmutable());
                        changed = true;
                    }
                }
            }
            footprint.addAll(additions);
        } while (changed);
    }

    private static boolean hasOutsideAirNeighbor(ServerWorld world, BlockPos pos, Set<Long> interiorCells) {
        for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            BlockPos neighbor = pos.offset(direction);
            if (interiorCells.contains(mapCellKey(neighbor.getX(), neighbor.getZ()))) continue;
            if (world.getBlockState(neighbor).isAir() || world.getBlockState(neighbor).isReplaceable()) return true;
        }
        return false;
    }

    private static BlockPos findInteriorStart(ServerWorld world, BlockPos center) {
        BlockPos nearest = null;
        double distance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.iterate(center.add(-2, 0, -2), center.add(2, 0, 2))) {
            double candidateDistance = candidate.getSquaredDistance(center);
            if (candidateDistance < distance && isInteriorCell(world, candidate, center.getY())) {
                nearest = candidate.toImmutable();
                distance = candidateDistance;
            }
        }
        return nearest;
    }

    private static boolean isInteriorCell(ServerWorld world, BlockPos pos, int y) {
        if (pos.getY() != y || !world.isChunkLoaded(pos) || !world.getWorldBorder().contains(pos)
                || !isSolidStructureBlock(world, pos.down()) || !hasRoof(world, pos, 12)) {
            return false;
        }
        Block block = world.getBlockState(pos).getBlock();
        return world.getBlockState(pos).isAir()
                || world.getBlockState(pos).isReplaceable()
                // Beds are furniture occupying an interior floor cell. They
                // must not make an otherwise enclosed room look open.
                || block instanceof net.minecraft.block.BedBlock
                // Common full-block furniture is also allowed inside the
                // traced room. Ordinary building blocks remain structural.
                || isInteriorFurnitureBlock(block);
    }

    private static boolean isInteriorFurnitureBlock(Block block) {
        return block == Blocks.CRAFTING_TABLE
                || block == Blocks.CHEST
                || block == Blocks.TRAPPED_CHEST
                || block == Blocks.BARREL
                || block == Blocks.FURNACE
                || block == Blocks.BLAST_FURNACE
                || block == Blocks.SMOKER
                || block == Blocks.LOOM
                || block == Blocks.CARTOGRAPHY_TABLE
                || block == Blocks.FLETCHING_TABLE
                || block == Blocks.SMITHING_TABLE
                || block == Blocks.STONECUTTER
                || block == Blocks.GRINDSTONE
                || block == Blocks.LECTERN
                || block == Blocks.ENCHANTING_TABLE
                || block == Blocks.COMPOSTER
                || block == Blocks.JUKEBOX
                || block == Blocks.NOTE_BLOCK
                || block == Blocks.BOOKSHELF
                || block == Blocks.ANVIL
                || block == Blocks.CHIPPED_ANVIL
                || block == Blocks.DAMAGED_ANVIL
                || block == Blocks.BELL
                || block == Blocks.ENDER_CHEST;
    }

    private static boolean hasClosedInteriorBoundary(ServerWorld world, List<BlockPos> footprint, int y) {
        Set<Long> cells = new HashSet<>();
        for (BlockPos pos : footprint) {
            cells.add(mapCellKey(pos.getX(), pos.getZ()));
        }
        for (BlockPos pos : footprint) {
            for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
                BlockPos adjacent = pos.offset(direction);
                if (cells.contains(mapCellKey(adjacent.getX(), adjacent.getZ()))) continue;
                if (!hasEnclosureWallHeight(world, adjacent, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static long mapCellKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private static boolean openDetectionExpired(ServerPlayerEntity player,
                                                BlueprintSessionService.SessionState session) {
        if (session.inspectionTarget().equals(BlockPos.ORIGIN)
                || !OPEN_STRUCTURE_TYPES.contains(session.inspectionType())) {
            return false;
        }
        return player.getServerWorld().getTime() - session.detectedAtTick() >= OPEN_DETECTION_TTL_TICKS;
    }

    public static List<BlueprintSessionService.RequirementLine> neutralCostRequirementLines(TownBuildingDefinition definition) {
        return List.of(
                new BlueprintSessionService.RequirementLine("- " + definition.registrationTokens() + " Town Tokens", 0),
                new BlueprintSessionService.RequirementLine("- " + definition.registrationCurrency() + "x Emerald", 0)
        );
    }

    public static List<BlueprintSessionService.RequirementLine> neutralFurnitureRequirementLines(TownBuildingDefinition definition) {
        return switch (definition.id()) {
            case "residence" -> List.of(
                    new BlueprintSessionService.RequirementLine("- Enclosed Building", 0),
                    new BlueprintSessionService.RequirementLine("- Minimum 3x3 floor area", 0),
                    new BlueprintSessionService.RequirementLine("- 1x Bed", 0)
            );
            case "farm" -> List.of(
                    new BlueprintSessionService.RequirementLine("- Select crop or farmland", 0),
                    new BlueprintSessionService.RequirementLine("- 12 connected crops", 0),
                    new BlueprintSessionService.RequirementLine("- Tier caps: 25 / 64 / 144", 0)
            );
            case "campfire" -> List.of(new BlueprintSessionService.RequirementLine("- Campfire", 0));
            case "bounty_board" -> List.of(new BlueprintSessionService.RequirementLine("- 1x Bounty Board", 0));
            case "park" -> List.of(
                    new BlueprintSessionService.RequirementLine("- 8x flowers or leaves", 0),
                    new BlueprintSessionService.RequirementLine("- 1x park furniture block", 0));
            case "civic_office" -> List.of(new BlueprintSessionService.RequirementLine("- Mayor Desk", 0));
            default -> List.of(new BlueprintSessionService.RequirementLine("- Matching Building Predicates", 0));
        };
    }

    private static List<BlueprintSessionService.RequirementLine> costRequirementLines(ServerPlayerEntity player, TownBuildingDefinition definition) {
        TownContext town = editableTown(player);
        if (town == null) return neutralCostRequirementLines(definition);
        TownSavedData data = TownSavedData.get(player.getServerWorld(), town.townId());
        return List.of(
                new BlueprintSessionService.RequirementLine("- " + definition.registrationTokens() + " Town Tokens",
                        data.getTownTokens() >= definition.registrationTokens() ? 1 : -1),
                new BlueprintSessionService.RequirementLine("- " + definition.registrationCurrency() + "x Emerald",
                        InventoryHelper.count(player.getInventory(), Items.EMERALD) >= definition.registrationCurrency() ? 1 : -1)
        );
    }

    private static List<BlueprintSessionService.RequirementLine> furnitureRequirementLines(ServerWorld world, BlockPos target,
                                                                                           TownBuildingDefinition definition,
                                                                                           boolean passed) {
        if ("farm".equals(definition.id())) {
            FarmFieldScanner.Result field = FarmFieldScanner.scan(world, target);
            return List.of(
                    new BlueprintSessionService.RequirementLine("- Select crop or farmland", field.crops() > 0 ? 1 : -1),
                    new BlueprintSessionService.RequirementLine("- 12 connected crops", field.valid() ? 1 : -1),
                    new BlueprintSessionService.RequirementLine("- Tier caps: 25 / 64 / 144", 1)
            );
        }
        return switch (definition.id()) {
            case "residence" -> List.of(
                    new BlueprintSessionService.RequirementLine("- Enclosed Building",
                            isEnclosedBuilding(world, target) || MCAIntegration.getBuildingTypeAt(world, target).filter(RESIDENCE_TYPES::contains).isPresent() ? 1 : -1),
                    new BlueprintSessionService.RequirementLine("- Minimum 3x3 floor area",
                            enclosedFloorArea(world, target) >= MIN_ENCLOSED_FLOOR_AREA
                                    || MCAIntegration.getBuildingTypeAt(world, target).filter(RESIDENCE_TYPES::contains).isPresent() ? 1 : -1),
                    new BlueprintSessionService.RequirementLine("- 1x Bed", hasBed(world, target) ? 1 : -1)
            );
            case "campfire" -> List.of(new BlueprintSessionService.RequirementLine("- Campfire",
                    findNearbyCampfire(world, target, 6) != null ? 1 : -1));
            case "bounty_board" -> List.of(new BlueprintSessionService.RequirementLine("- 1x Bounty Board",
                    findNearbyBountyBoard(world, target, 6) != null ? 1 : -1));
            case "park" -> List.of(
                    new BlueprintSessionService.RequirementLine("- 8x flowers or leaves",
                            findNearbyPark(world, target, 6) != null
                                    && countNearby(world, target, 6, TownBuildingService::isParkDecoration) >= 8 ? 1 : -1),
                    new BlueprintSessionService.RequirementLine("- 1x park furniture block",
                            findNearbyPark(world, target, 6) != null
                                    && countNearby(world, target, 6, TownBuildingService::isParkFurniture) >= 1 ? 1 : -1));
            case "civic_office" -> List.of(new BlueprintSessionService.RequirementLine("- Mayor Desk", world.getBlockState(target).isOf(ModBlocks.MAYOR_DESK) ? 1 : -1));
            default -> List.of(new BlueprintSessionService.RequirementLine("- Matching Building Predicates", passed ? 1 : -1));
        };
    }

    private static BlockPos validateMcaInterior(ServerWorld world, BlockPos pos, Set<String> acceptedTypes,
                                                boolean requireFurniture) {
        MCAIntegration.registerBuilding(world, pos);
        String type = MCAIntegration.getBuildingTypeAt(world, pos).orElse("");
        if (!acceptedTypes.contains(type)) return null;
        if (requireFurniture && MCAIntegration.countBuildingBlocks(world, pos,
                Set.of("minecraft:chest", "minecraft:barrel", "minecraft:crafting_table", "minecraft:furnace",
                        "minecraft:bookshelf")) < 2) return null;
        return MCAIntegration.getBuildingCenterAt(world, pos).orElse(pos);
    }

    private static BlockPos validateResidence(ServerWorld world, BlockPos pos) {
        MCAIntegration.registerBuilding(world, pos);
        BlockPos center = MCAIntegration.getBuildingCenterAt(world, pos).orElse(pos);
        boolean enclosed = MCAIntegration.getBuildingTypeAt(world, pos).filter(RESIDENCE_TYPES::contains).isPresent()
                || isEnclosedBuilding(world, center);
        return enclosed && hasBed(world, center) ? center : null;
    }

    private static boolean validateFarm(ServerWorld world, BlockPos center) {
        return FarmFieldScanner.scan(world, center).valid();
    }

    private static boolean validatePark(ServerWorld world, BlockPos center) {
        return countNearby(world, center, 6, TownBuildingService::isParkDecoration) >= 8
                && countNearby(world, center, 6, TownBuildingService::isParkFurniture) >= 1;
    }

    private static BlockPos findNearbyBountyBoard(ServerWorld world, BlockPos center, int radius) {
        return findNearbyOpenStructure(world, center, radius,
                TownBuildingService::isBountyBoardBlock, ignored -> true);
    }

    private static BlockPos findNearbyCampfire(ServerWorld world, BlockPos center, int radius) {
        return findNearbyOpenStructure(world, center, radius,
                block -> block == Blocks.CAMPFIRE || block == Blocks.SOUL_CAMPFIRE,
                ignored -> true);
    }

    private static BlockPos findNearbyPark(ServerWorld world, BlockPos center, int radius) {
        return findNearbyOpenStructure(world, center, radius,
                block -> isParkDecoration(block) || isParkFurniture(block),
                pos -> validatePark(world, pos));
    }

    private static BlockPos findNearbyFarm(ServerWorld world, BlockPos center, int radius) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -2, -radius), center.add(radius, 2, radius))) {
            if (!isFarmBlock(world, pos)) continue;
            double distance = pos.getSquaredDistance(center);
            if (distance < nearestDistance) {
                nearest = pos.toImmutable();
                nearestDistance = distance;
            }
        }
        if (nearest == null) return null;
        FarmFieldScanner.Result field = FarmFieldScanner.scan(world, nearest);
        return field.crops() > 0 ? field.anchor() : nearest;
    }

    private static boolean isFarmBlock(ServerWorld world, BlockPos pos) {
        return FarmFieldScanner.farmlandAt(world, pos) != null;
    }

    /** Finds a nearby marker and validates the open structure around it. */
    private static BlockPos findNearbyOpenStructure(ServerWorld world, BlockPos center, int radius,
                                                    java.util.function.Predicate<Block> marker,
                                                    java.util.function.Predicate<BlockPos> validator) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -3, -radius), center.add(radius, 3, radius))) {
            if (!marker.test(world.getBlockState(pos).getBlock()) || !validator.test(pos)) continue;
            double distance = pos.getSquaredDistance(center);
            if (distance < nearestDistance) {
                nearest = pos.toImmutable();
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static boolean isBountyBoard(ServerWorld world, BlockPos pos) {
        return isBountyBoardBlock(world.getBlockState(pos).getBlock());
    }

    private static boolean isBountyBoardBlock(Block block) {
        Identifier id = Registries.BLOCK.getId(block);
        String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
        return "bountiful".equals(id.getNamespace())
                && path.contains("bounty")
                && path.contains("board");
    }

    private static boolean isParkDecoration(Block block) {
        return block instanceof net.minecraft.block.FlowerBlock
                || block instanceof net.minecraft.block.LeavesBlock
                || block instanceof net.minecraft.block.FlowerPotBlock;
    }

    private static boolean isParkFurniture(Block block) {
        return block instanceof net.minecraft.block.StairsBlock
                || block instanceof net.minecraft.block.SlabBlock
                || block instanceof net.minecraft.block.FenceBlock
                || block instanceof net.minecraft.block.LanternBlock
                || block == Blocks.CAMPFIRE
                || block == Blocks.SOUL_CAMPFIRE;
    }

    private static boolean hasBed(ServerWorld world, BlockPos center) {
        return countNearby(world, center, 6, block -> block == Blocks.RED_BED || block == Blocks.WHITE_BED
                || block == Blocks.BLACK_BED || block == Blocks.BLUE_BED || block == Blocks.BROWN_BED
                || block == Blocks.CYAN_BED || block == Blocks.GRAY_BED || block == Blocks.GREEN_BED
                || block == Blocks.LIGHT_BLUE_BED || block == Blocks.LIGHT_GRAY_BED || block == Blocks.LIME_BED
                || block == Blocks.MAGENTA_BED || block == Blocks.ORANGE_BED || block == Blocks.PINK_BED
                || block == Blocks.PURPLE_BED || block == Blocks.YELLOW_BED) > 0;
    }

    private static boolean isEnclosedBuilding(ServerWorld world, BlockPos center) {
        return !traceEnclosedFootprint(world, center).isEmpty();
    }

    private static int enclosedFloorArea(ServerWorld world, BlockPos center) {
        return traceEnclosedFootprint(world, center).size();
    }

    private static int wallDistance(ServerWorld world, BlockPos center,
                                     net.minecraft.util.math.Direction direction, int maxDistance) {
        for (int distance = 1; distance <= maxDistance; distance++) {
            BlockPos atFeet = center.offset(direction, distance);
            if (isEnclosureWallBlock(world, atFeet) || isEnclosureWallBlock(world, atFeet.up())) {
                return distance;
            }
        }
        return 0;
    }

    private static boolean hasRoof(ServerWorld world, BlockPos center, int maxHeight) {
        for (int dy = 2; dy <= maxHeight; dy++) {
            if (isRoofBlock(world, center.up(dy))) return true;
        }
        return false;
    }

    private static boolean hasEnclosureWallHeight(ServerWorld world, BlockPos base, int floorY) {
        BlockPos wallBase = new BlockPos(base.getX(), floorY, base.getZ());
        return isEnclosureWallBlock(world, wallBase)
                && isEnclosureWallBlock(world, wallBase.up());
    }

    private static boolean isRoofBlock(ServerWorld world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        return isSolidStructureBlock(world, pos)
                || block instanceof net.minecraft.block.AbstractGlassBlock
                || block instanceof net.minecraft.block.StairsBlock
                || block instanceof net.minecraft.block.SlabBlock;
    }

    private static int countWallSides(ServerWorld world, BlockPos center, int maxDistance) {
        int walls = 0;
        for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            for (int i = 1; i <= maxDistance; i++) {
                BlockPos atFeet = center.offset(direction, i);
                BlockPos atHead = atFeet.up();
                if (isEnclosureWallBlock(world, atFeet) || isEnclosureWallBlock(world, atHead)) {
                    walls++;
                    break;
                }
            }
        }
        return walls;
    }

    private static boolean isSolidStructureBlock(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).isSolidBlock(world, pos);
    }

    private static boolean isEnclosureWallBlock(ServerWorld world, BlockPos pos) {
        if (!world.isChunkLoaded(pos) || !world.getWorldBorder().contains(pos)) return false;
        net.minecraft.block.Block block = world.getBlockState(pos).getBlock();
        return isSolidStructureBlock(world, pos)
                || block instanceof net.minecraft.block.AbstractGlassBlock
                || block instanceof net.minecraft.block.PaneBlock
                || block instanceof net.minecraft.block.FenceBlock
                || block instanceof net.minecraft.block.TrapdoorBlock
                || block instanceof net.minecraft.block.DoorBlock;
    }

    private static BlockPos lookedAt(ServerPlayerEntity player) {
        HitResult hit = player.raycast(8.0, 0.0F, false);
        return hit instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : player.getBlockPos();
    }

    private static boolean isEither(ServerWorld world, BlockPos pos, Block first, Block second) {
        return world.getBlockState(pos).isOf(first) || world.getBlockState(pos).isOf(second);
    }

    private static boolean isBlock(ServerWorld world, BlockPos pos, String id) {
        return Registries.BLOCK.getId(world.getBlockState(pos).getBlock()).toString().equals(id);
    }

    private static int countNearby(ServerWorld world, BlockPos center, int radius,
                                   java.util.function.Predicate<Block> predicate) {
        int count = 0;
        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -3, -radius), center.add(radius, 3, radius))) {
            if (predicate.test(world.getBlockState(pos).getBlock())) count++;
        }
        return count;
    }

    private static boolean isHoldingBlueprint(ServerPlayerEntity player) {
        return BLUEPRINT.equals(Registries.ITEM.getId(player.getMainHandStack().getItem()))
                || BLUEPRINT.equals(Registries.ITEM.getId(player.getOffHandStack().getItem()));
    }
}
