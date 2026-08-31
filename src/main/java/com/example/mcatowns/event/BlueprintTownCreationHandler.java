package com.example.mcatowns.event;

import com.example.mcatowns.network.ModNetworking;
import com.example.mcatowns.network.TownBlueprintView;
import com.example.mcatowns.registry.ModItems;
import com.example.mcatowns.town.PlayerTownRegistry;
import com.example.mcatowns.town.BlueprintSessionService;
import com.example.mcatowns.town.TownContext;
import com.example.mcatowns.town.TownManager;
import com.example.mcatowns.town.TownRank;
import com.example.mcatowns.town.TownSavedData;
import com.example.mcatowns.town.TownBuildingDefinition;
import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.util.InventoryHelper;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.stream.Stream;

public final class BlueprintTownCreationHandler {
    private static final Identifier MCA_BLUEPRINT_ID = new Identifier("mca", "blueprint");
    private static final Identifier CROWN_ID = new Identifier("numismatics", "crown");
    public static final int FOUNDING_SCRAP_COST = 10;

    private BlueprintTownCreationHandler() {
    }

    public static void register() {
        UseItemCallback.EVENT.register(BlueprintTownCreationHandler::useBlueprint);
    }

    private static TypedActionResult<ItemStack> useBlueprint(net.minecraft.entity.player.PlayerEntity player,
                                                              World world, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!isBlueprint(stack)) {
            return TypedActionResult.pass(stack);
        }
        // MCA opens its old screen locally. Use our own packet before consuming that action.
        if (world.isClient) {
            ModNetworking.sendOpenTownBlueprint();
            return TypedActionResult.success(stack, false);
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return TypedActionResult.pass(stack);

        openRequestedBlueprint(serverPlayer);
        return TypedActionResult.success(stack, false);
    }

    public static void openRequestedBlueprint(ServerPlayerEntity player) {
        if (!isHoldingBlueprint(player)) return;
        openBlueprint(player, findTown(player).orElse(null));
    }

    public static void foundTown(ServerPlayerEntity player) {
        if (!isHoldingBlueprint(player) || findTown(player).isPresent()) return;

        if (PlayerTownRegistry.get(player.getServerWorld()).getOwnedTown(player.getUuid()).isPresent()) {
            player.sendMessage(Text.translatable("text.mcatowns.already_owns_town"), true);
            return;
        }

        Item crown = getCrown();
        if (crown == null) {
            player.sendMessage(Text.translatable("text.mcatowns.missing_crown_mod"), true);
            return;
        }
        if (!player.getAbilities().creativeMode) {
            if (InventoryHelper.count(player.getInventory(), crown) < 1) {
                player.sendMessage(Text.translatable("text.mcatowns.need_crown"), true);
                return;
            }
            if (InventoryHelper.count(player.getInventory(), ModItems.BLUEPRINT_SCRAP) < FOUNDING_SCRAP_COST) {
                player.sendMessage(Text.translatable("text.mcatowns.need_blueprint_scraps", FOUNDING_SCRAP_COST), true);
                return;
            }
        }

        ServerWorld world = player.getServerWorld();
        BlockPos anchor = findPlacementPos(world, player);
        if (anchor == null || !world.setBlockState(anchor, Blocks.BELL.getDefaultState())) {
            player.sendMessage(Text.translatable("text.mcatowns.no_town_bell_space"), true);
            return;
        }

        if (!player.getAbilities().creativeMode) {
            InventoryHelper.remove(player.getInventory(), crown, 1);
            InventoryHelper.remove(player.getInventory(), ModItems.BLUEPRINT_SCRAP, FOUNDING_SCRAP_COST);
        }
        TownContext town = PlayerTownRegistry.get(world).createTown(world, player, anchor);
        MCAIntegration.registerBuilding(world, anchor)
                .ifPresent(mcaTown -> PlayerTownRegistry.get(world).bindMcaTown(town.townId(), mcaTown.townId()));
        player.sendMessage(Text.translatable("text.mcatowns.town_created"), false);
        openBlueprint(player, town);
    }

    private static void openBlueprint(ServerPlayerEntity player, TownContext town) {
        Item crown = getCrown();
        int scraps = InventoryHelper.count(player.getInventory(), ModItems.BLUEPRINT_SCRAP);
        if (town == null) {
            ModNetworking.openTownBlueprint(player, new TownBlueprintView(
                    true, "New Settlement", "Founder", TownRank.UNRANKED,
                    com.example.mcatowns.config.MCATownsConfig.get().foundedTownStartingProsperity,
                    0, "No infrastructure", 50, 1, 0, 0, 0, 0, 0, 1, true, false,
                    BlockPos.ORIGIN, BlockPos.ORIGIN, player.getBlockPos(),
                    player.getAbilities().creativeMode || crown != null && InventoryHelper.count(player.getInventory(), crown) > 0,
                    player.getAbilities().creativeMode ? FOUNDING_SCRAP_COST : scraps,
                    java.util.List.of(), "", false, java.util.List.of(), java.util.List.of(),
                    java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of()));
            return;
        }

        TownSavedData data = TownSavedData.get(player.getServerWorld(), town.townId());
        BlueprintSessionService.SessionState session = BlueprintSessionService.get(player);
        boolean canManage = hasAdminAccess(player) || ("player_created".equals(town.source())
                ? PlayerTownRegistry.get(player.getServerWorld()).isOwner(town.townId(), player)
                : TownManager.hasMayorAuthority(player, town));
        if (data.getTownCenter().equals(BlockPos.ORIGIN)) data.setTownCenter(town.center());
        ModNetworking.openTownBlueprint(player, new TownBlueprintView(
                false, data.getTownName(), playerRank(player, town), data.getTownRank(), data.getProsperity(),
                data.getProsperityBase(), infrastructureSummary(data), data.getHappiness(), data.getPopulation(),
                data.getPopulationCapacity(), data.getFoodReserves(), data.getFoodCapacity(),
                data.getRegisteredBuildingCount(), data.getSpecialistCount(), data.getTaxRate(),
                canManage, TownRemovalHandler.canRemove(player, town.anchor()),
                town.anchor(), data.getTownCenter(), player.getBlockPos(), false, scraps,
                session.detected().stream().map(pos -> buildingEntry(player.getServerWorld(), "detected", "Detected", pos, "minecraft:spyglass")).toList(),
                session.inspectionType(), session.inspectionPassed(),
                session.costLines().stream().map(line -> new TownBlueprintView.RequirementLine(line.text(), line.state())).toList(),
                session.furnitureLines().stream().map(line -> new TownBlueprintView.RequirementLine(line.text(), line.state())).toList(),
                Stream.concat(TownBuildingDefinition.ALL.stream().map(definition -> new TownBlueprintView.BuildingOption(
                        definition.id(), definition.displayName(), definition.category(), definition.description(),
                        data.isBuildingUnlocked(definition.id()), definition.registrationTokens(),
                        definition.registrationCurrency(), definition.prosperityRequired(), "", false)),
                        legacyBuildings()).toList(),
                com.example.mcatowns.town.TownProgressionService.checklist(data),
                data.getResidents().stream().map(id -> {
                    net.minecraft.entity.Entity entity = player.getServerWorld().getEntity(id);
                    String name = entity == null ? id.toString().substring(0, 8) : entity.getName().getString();
                    return new TownBlueprintView.ResidentEntry(id, name,
                            data.getSpecialists().getOrDefault(id, ""));
                }).toList(),
                data.getRegisteredBuildings().stream().map(building -> {
                    TownBuildingDefinition definition = TownBuildingDefinition.get(building.type());
                    String name = definition == null ? building.type() : definition.displayName();
                    return buildingEntry(building, name, "");
                }).toList()));
    }

    private static TownBlueprintView.BuildingEntry buildingEntry(ServerWorld world, String type, String name, BlockPos pos, String icon) {
        MCAIntegration.BuildingBounds bounds = MCAIntegration.getBuildingBoundsAt(world, pos)
                .orElse(new MCAIntegration.BuildingBounds(pos, pos));
        return new TownBlueprintView.BuildingEntry(new java.util.UUID(0L, 0L), type, name, pos, icon,
                bounds.min(), bounds.max(), 1, "DETECTED", 0, 0, 0);
    }

    private static TownBlueprintView.BuildingEntry buildingEntry(com.example.mcatowns.town.RegisteredTownBuilding building,
                                                                  String name, String icon) {
        return new TownBlueprintView.BuildingEntry(building.id(), building.type(), name, building.anchor(), icon,
                building.minPos(), building.maxPos(), building.tier(), building.status().name(), building.quality(),
                building.workers().size(), building.cropState());
    }

    private static String infrastructureSummary(TownSavedData data) {
        return java.util.Arrays.stream(com.example.mcatowns.town.InfrastructureType.values())
                .map(type -> type.displayName().substring(0, 3) + " " + data.getInfrastructureAvailable(type)
                        + "/" + data.getInfrastructureProvided(type))
                .collect(java.util.stream.Collectors.joining("  "));
    }

    private static String playerRank(ServerPlayerEntity player, TownContext town) {
        if (hasAdminAccess(player)) {
            return "Mayor";
        }
        if ("player_created".equals(town.source())) {
            return PlayerTownRegistry.get(player.getServerWorld()).isOwner(town.townId(), player)
                    ? "Mayor"
                    : MCAIntegration.getPlayerRankName(player, town.anchor());
        }
        return MCAIntegration.getPlayerRankName(player, town.anchor());
    }

    private static Stream<TownBlueprintView.BuildingOption> legacyBuildings() {
        return Stream.of(
                legacy("mca_house", "House", com.example.mcatowns.town.TownBuildingCategory.RESIDENTIAL, "Residential home for villagers.", "minecraft:red_bed"),
                legacy("mca_big_house", "Big House", com.example.mcatowns.town.TownBuildingCategory.RESIDENTIAL, "Large residential home with greater villager capacity.", "minecraft:white_bed"),
                legacy("mca_farm", "MCA Farm", com.example.mcatowns.town.TownBuildingCategory.FOOD, "Food production area with crop and farm supplies.", "minecraft:wheat"),
                legacy("mca_bakery", "Bakery", com.example.mcatowns.town.TownBuildingCategory.FOOD, "Bakery: produces 16 food/day, but only one bakery works per farm.", "minecraft:bread"),
                legacy("mca_butcher", "Butcher", com.example.mcatowns.town.TownBuildingCategory.FOOD, "Butcher: produces 6 food/day.", "minecraft:cooked_beef"),
                legacy("mca_fishermans_hut", "Fisherman's Hut", com.example.mcatowns.town.TownBuildingCategory.FOOD, "Fisherman's hut: produces 6 food/day.", "minecraft:fishing_rod"),
                legacy("mca_inn", "MCA Inn", com.example.mcatowns.town.TownBuildingCategory.COMMUNITY, "Inn and social hub for visitors and residents.", "minecraft:barrel"),
                legacy("mca_graveyard", "Graveyard", com.example.mcatowns.town.TownBuildingCategory.COMMUNITY, "Burial grounds for the settlement.", "minecraft:stone_bricks"),
                legacy("mca_library", "Library", com.example.mcatowns.town.TownBuildingCategory.UTILITY, "Library for knowledge, study, and town services.", "minecraft:bookshelf"),
                legacy("mca_armory", "Armory", com.example.mcatowns.town.TownBuildingCategory.UTILITY, "Training and housing area for guards.", "minecraft:iron_chestplate"),
                legacy("mca_town_hall", "Town Hall", com.example.mcatowns.town.TownBuildingCategory.UTILITY, "Expands town operational range for systems and scans.", "minecraft:bell"),
                legacy("mca_market", "Market", com.example.mcatowns.town.TownBuildingCategory.UTILITY, "Increases weekly town tax income by 20%.", "minecraft:emerald")
        );
    }

    private static TownBlueprintView.BuildingOption legacy(String id, String name,
                                                           com.example.mcatowns.town.TownBuildingCategory category,
                                                           String description, String icon) {
        return new TownBlueprintView.BuildingOption(id, name, category, description, true, 0, 0, 0, icon, true);
    }

    private static Optional<TownContext> findTown(ServerPlayerEntity player) {
        PlayerTownRegistry registry = PlayerTownRegistry.get(player.getServerWorld());
        Optional<TownContext> owned = registry.getOwnedTown(player.getUuid());
        if (owned.isPresent()) {
            TownContext town = owned.get();
            double distanceToAnchor = town.anchor().getSquaredDistance(player.getBlockPos());
            double distanceToCenter = town.center().getSquaredDistance(player.getBlockPos());
            double maxDistance = Math.max(64, TownManager.getTownSearchMargin());
            if (distanceToAnchor <= maxDistance * maxDistance || distanceToCenter <= maxDistance * maxDistance) {
                return owned;
            }
        }
        return TownManager.findExistingTown(player.getServerWorld(), player.getBlockPos(),
                Math.max(64, TownManager.getTownSearchMargin()));
    }

    private static boolean hasAdminAccess(ServerPlayerEntity player) {
        return player.getAbilities().creativeMode || player.hasPermissionLevel(2);
    }

    private static boolean isHoldingBlueprint(ServerPlayerEntity player) {
        return isBlueprint(player.getMainHandStack()) || isBlueprint(player.getOffHandStack());
    }

    private static boolean isBlueprint(ItemStack stack) {
        return MCA_BLUEPRINT_ID.equals(Registries.ITEM.getId(stack.getItem()));
    }

    private static Item getCrown() {
        Item crown = Registries.ITEM.get(CROWN_ID);
        return Registries.ITEM.getId(crown).equals(Registries.ITEM.getDefaultId()) ? null : crown;
    }

    private static BlockPos findPlacementPos(ServerWorld world, ServerPlayerEntity player) {
        BlockPos front = player.getBlockPos().offset(player.getHorizontalFacing());
        for (BlockPos candidate : new BlockPos[]{front, front.down(), front.up()}) {
            if (world.getWorldBorder().contains(candidate) && world.getBlockState(candidate).isReplaceable()) {
                return candidate;
            }
        }
        return null;
    }
}
