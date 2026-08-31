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
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TownBuildingService {
    private static final Identifier BLUEPRINT = new Identifier("mca", "blueprint");
    private static final Set<String> RESIDENCE_TYPES = Set.of("house", "big_house", "building");

    private TownBuildingService() {
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
        MCAIntegration.BuildingBounds detectedBounds = MCAIntegration.getBuildingBoundsAt(world, pos)
                .orElseGet(() -> "farm".equals(type)
                        ? new MCAIntegration.BuildingBounds(pos.add(-8, -3, -8), pos.add(8, 3, 8))
                        : new MCAIntegration.BuildingBounds(pos, pos));
        int cropCount = "farm".equals(type)
                ? countNearby(world, pos, 8, block -> block instanceof net.minecraft.block.CropBlock) : 0;
        int tier = cropCount >= 48 ? 3 : cropCount >= 24 ? 2 : 1;
        int cropState = Math.min(100, cropCount * 100 / 48);
        int quality = "farm".equals(type) ? Math.max(25, cropState) : 50;
        RegisteredTownBuilding registered = new RegisteredTownBuilding(UUID.randomUUID(), type, tier, pos,
                detectedBounds.min(), detectedBounds.max(), BuildingStatus.ACTIVE, quality, List.of(),
                TownManager.getDay(world), cropState);
        if (!data.registerBuilding(registered)) return;

        if (!player.getAbilities().creativeMode) {
            data.spendTownTokens(definition.registrationTokens());
            InventoryHelper.remove(player.getInventory(), currency, definition.registrationCurrency());
        }
        refreshDerivedValues(data);
        player.sendMessage(Text.translatable("text.mcatowns.building_registered_named", definition.displayName()), false);
    }

    public static void detect(ServerPlayerEntity player) {
        TownContext town = editableTown(player);
        if (town == null || !isHoldingBlueprint(player)) return;
        BlockPos target = detectPosition(player.getServerWorld(), player);
        if (target == null) {
            player.sendMessage(Text.literal("No building could be detected here."), true);
            return;
        }
        BlueprintSessionService.setDetected(player, List.of(target), target);
        player.sendMessage(Text.literal("Building detected."), true);
    }

    public static void inspect(ServerPlayerEntity player, String type) {
        TownBuildingDefinition definition = TownBuildingDefinition.get(type);
        TownContext town = editableTown(player);
        if (definition == null || town == null || !isHoldingBlueprint(player)) return;
        BlueprintSessionService.SessionState session = BlueprintSessionService.get(player);
        BlockPos target = session.primaryDetected().equals(BlockPos.ORIGIN) ? detectPosition(player.getServerWorld(), player) : session.primaryDetected();
        if (target == null) {
            player.sendMessage(Text.literal("Detect a building first."), true);
            return;
        }
        BlockPos validated = validateAndLocate(player.getServerWorld(), target, definition);
        boolean passed = validated != null;
        BlueprintSessionService.setInspection(player, type, passed, passed ? validated : target,
                costRequirementLines(player, definition), furnitureRequirementLines(player.getServerWorld(), target, definition, passed));
        player.sendMessage(Text.literal(passed ? "Inspection passed." : "Inspection failed."), true);
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
            populationCapacity += definition.populationCapacity();
            foodCapacity += definition.foodCapacity();
            floor += definition.prosperityBase();
        }
        data.setProgressionCapacities(Math.min(data.getTownRank().maxOccupancy(), populationCapacity), foodCapacity);
        data.setProsperityBase(Math.min(data.getTownRank().maxProsperity(), floor));
    }

    public static void refreshInspectionState(ServerWorld world, TownSavedData data, long day) {
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            if (!"farm".equals(building.type())) continue;
            int crops = countNearby(world, building.anchor(), 8,
                    block -> block instanceof net.minecraft.block.CropBlock);
            int cropState = Math.min(100, crops * 100 / 48);
            int tier = crops >= 48 ? 3 : crops >= 24 ? 2 : 1;
            int quality = Math.max(0, Math.min(100, cropState));
            RegisteredTownBuilding updated = building.inspected(day, quality, cropState, tier);
            updated = updated.withStatus(crops < 12 ? BuildingStatus.NEEDS_INSPECTION : BuildingStatus.ACTIVE);
            data.replaceBuilding(updated);
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
            case "campfire" -> isEither(world, lookedAt, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE) ? lookedAt : null;
            case "bounty_board" -> isBlock(world, lookedAt, "bountiful:bountyboard") ? lookedAt : null;
            case "civic_office" -> world.getBlockState(lookedAt).isOf(ModBlocks.MAYOR_DESK) ? lookedAt : null;
            case "storehouse" -> world.getBlockState(lookedAt).isOf(ModBlocks.STOREHOUSE) ? lookedAt : null;
            case "granary" -> world.getBlockState(lookedAt).isOf(ModBlocks.SILO) ? lookedAt : null;
            case "guard_post" -> world.getBlockState(lookedAt).isOf(ModBlocks.BARRACKS) ? lookedAt : null;
            case "park" -> countNearby(world, lookedAt, 5, block -> block instanceof net.minecraft.block.FlowerBlock
                    || block instanceof net.minecraft.block.LeavesBlock) >= 8 ? lookedAt : null;
            case "jeweler" -> isEither(world, lookedAt, Blocks.SMITHING_TABLE, Blocks.GOLD_BLOCK)
                    && countNearby(world, lookedAt, 4, block -> block == Blocks.GOLD_BLOCK) >= 1 ? lookedAt : null;
            default -> null;
        };
    }

    private static TownContext editableTown(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        if (TownRemovalHandler.hasAdminAccess(player)) {
            return TownManager.findExistingTown(world, player.getBlockPos(), Math.max(64, TownManager.getTownSearchMargin())).orElse(null);
        }
        return PlayerTownRegistry.get(world).getOwnedTown(player.getUuid()).orElse(null);
    }

    private static BlockPos detectPosition(ServerWorld world, ServerPlayerEntity player) {
        BlockPos lookedAt = lookedAt(player);
        MCAIntegration.registerBuilding(world, lookedAt);
        return MCAIntegration.getBuildingCenterAt(world, lookedAt)
                .or(() -> MCAIntegration.getBuildingCenterAt(world, player.getBlockPos()))
                .orElse(lookedAt);
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
                    new BlueprintSessionService.RequirementLine("- 1x Bed", 0)
            );
            case "farm" -> List.of(
                    new BlueprintSessionService.RequirementLine("- 1x Composter or Hay Bale", 0),
                    new BlueprintSessionService.RequirementLine("- 4x Hay Bale", 0),
                    new BlueprintSessionService.RequirementLine("- 12x Nearby Crop", 0)
            );
            case "campfire" -> List.of(new BlueprintSessionService.RequirementLine("- Campfire", 0));
            case "bounty_board" -> List.of(new BlueprintSessionService.RequirementLine("- Bounty Board", 0));
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
        return switch (definition.id()) {
            case "residence" -> List.of(
                    new BlueprintSessionService.RequirementLine("- Enclosed Building",
                            isEnclosedBuilding(world, target) || MCAIntegration.getBuildingTypeAt(world, target).filter(RESIDENCE_TYPES::contains).isPresent() ? 1 : -1),
                    new BlueprintSessionService.RequirementLine("- 1x Bed", hasBed(world, target) ? 1 : -1)
            );
            case "farm" -> List.of(
                    new BlueprintSessionService.RequirementLine("- 1x Composter or Hay Bale", isEither(world, target, Blocks.COMPOSTER, Blocks.HAY_BLOCK) ? 1 : -1),
                    new BlueprintSessionService.RequirementLine("- 4x Hay Bale", countNearby(world, target, 6, block -> block == Blocks.HAY_BLOCK) >= 4 ? 1 : -1),
                    new BlueprintSessionService.RequirementLine("- 12x Nearby Crop", countNearby(world, target, 8, block -> block instanceof net.minecraft.block.CropBlock) >= 12 ? 1 : -1)
            );
            case "campfire" -> List.of(new BlueprintSessionService.RequirementLine("- Campfire", isEither(world, target, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE) ? 1 : -1));
            case "bounty_board" -> List.of(new BlueprintSessionService.RequirementLine("- Bounty Board", isBlock(world, target, "bountiful:bountyboard") ? 1 : -1));
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
        return isEither(world, center, Blocks.COMPOSTER, Blocks.HAY_BLOCK)
                && countNearby(world, center, 6, block -> block == Blocks.HAY_BLOCK) >= 4
                && countNearby(world, center, 8, block -> block instanceof net.minecraft.block.CropBlock) >= 12;
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
        return hasRoof(world, center, 6) && countWallSides(world, center, 5) >= 3;
    }

    private static boolean hasRoof(ServerWorld world, BlockPos center, int maxHeight) {
        for (int dy = 1; dy <= maxHeight; dy++) {
            if (isSolidStructureBlock(world, center.up(dy))) return true;
        }
        return false;
    }

    private static int countWallSides(ServerWorld world, BlockPos center, int maxDistance) {
        int walls = 0;
        for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            for (int i = 1; i <= maxDistance; i++) {
                BlockPos atFeet = center.offset(direction, i);
                BlockPos atHead = atFeet.up();
                if (isSolidStructureBlock(world, atFeet) || isSolidStructureBlock(world, atHead)) {
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
