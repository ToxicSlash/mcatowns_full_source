package com.example.mcatowns.gametest;

import com.example.mcatowns.town.*;
import com.example.mcatowns.registry.ModItems;
import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.util.InventoryHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameMode;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TownConnectedServerProbe implements ModInitializer {
    private static final BlockPos BASE = new BlockPos(2, 100, 2);
    private static final BlockPos FARM = BASE.add(8, 0, 0);
    private static final BlockPos CAMPFIRE = BASE.add(0, 0, 14);
    private Path shared;
    private String phase = "";
    private int age;
    private int acknowledged;
    private Item crown;

    @Override public void onInitialize() {
        String path = System.getProperty("town.test.connected");
        if (path == null) return;
        shared = Path.of(path);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var player = server.getPlayerManager().getPlayer("TownVisualCI");
            if (player == null || phase.equals("complete")) return;
            try {
                if (++age > 900) throw new IllegalStateException("Connected phase timed out: " + phase);
                var world = player.getServerWorld();
                var registry = PlayerTownRegistry.get(world);
                if (phase.isEmpty()) {
                    crown = Registries.ITEM.get(new Identifier("numismatics", "crown"));
                    require(crown != Items.AIR, "Installed Numismatics crown missing");
                    require(MCAIntegration.isMcaLoaded(), "MCA absent");
                    player.changeGameMode(GameMode.SURVIVAL);
                    world.getGameRules().get(GameRules.RANDOM_TICK_SPEED).set(0, server);
                    for (int x = -1; x <= 14; x++) for (int z = -1; z <= 6; z++) {
                        BlockPos p = BASE.add(x, -1, z);
                        world.setBlockState(p, Blocks.STONE.getDefaultState());
                    }
                    for (int x = 0; x < 6; x++) for (int z = 0; z < 3; z++) {
                        BlockPos p = FARM.add(x, -1, z);
                        world.setBlockState(p, Blocks.FARMLAND.getDefaultState().with(FarmlandBlock.MOISTURE, 7));
                        world.setBlockState(p.up(), Blocks.WHEAT.getDefaultState());
                    }
                    for (int x = -2; x <= 2; x++) for (int z = -4; z <= 2; z++) {
                        world.setBlockState(CAMPFIRE.add(x, -1, z), Blocks.STONE.getDefaultState());
                    }
                    world.setBlockState(CAMPFIRE, Blocks.CAMPFIRE.getDefaultState());
                    player.getInventory().clear();
                    player.getInventory().selectedSlot = 0;
                    player.getInventory().setStack(0, new ItemStack(Registries.ITEM.get(new Identifier("mca", "blueprint"))));
                    player.getInventory().setStack(1, new ItemStack(ModItems.BLUEPRINT_SCRAP, 10));
                    player.getInventory().setStack(2, new ItemStack(Items.EMERALD, 16));
                    player.teleport(world, BASE.getX() + .5, BASE.getY(), BASE.getZ() + .5, 0, 0);
                    require(registry.getOwnedTown(player.getUuid()).isEmpty(), "Expected fresh owner");
                    advance("deny");
                    return;
                }
                if (!Files.exists(shared.resolve("ack-" + phase))) return;
                if (++acknowledged < 30) return;
                if (phase.equals("deny")) {
                    require(registry.getOwnedTown(player.getUuid()).isEmpty(), "Founding without crown must fail");
                    require(InventoryHelper.count(player.getInventory(), ModItems.BLUEPRINT_SCRAP) == 10, "Rejected founding charged scraps");
                    player.getInventory().setStack(3, new ItemStack(crown));
                    System.out.println("TOWN_CONNECTED_FOUND_DENIED_OK");
                    advance("found");
                    return;
                }
                var town = registry.getOwnedTown(player.getUuid()).orElseThrow(() -> new IllegalStateException("No founded town after " + phase));
                var data = TownSavedData.get(world, town.townId());
                if (phase.equals("found")) {
                    require(registry.isOwner(town.townId(), player), "Founder does not own town");
                    require(world.getBlockState(town.anchor()).isOf(Blocks.BELL), "Town bell missing");
                    require(InventoryHelper.count(player.getInventory(), crown) == 0, "Founding did not consume crown");
                    require(InventoryHelper.count(player.getInventory(), ModItems.BLUEPRINT_SCRAP) == 0, "Founding did not consume scraps");
                    require(data.getRegisteredBuildings().isEmpty(), "New town contains unexpected buildings");
                    System.out.println("TOWN_CONNECTED_FOUNDED_OK " + town.townId());
                    System.out.println("TOWN_CONNECTED_MCA_LINK " + MCAIntegration.getLinkDebug(world, town.anchor(), 16));
                    player.teleport(world, FARM.getX() + .5, FARM.getY(), FARM.getZ() + .5, 0, 0);
                    BlueprintSessionService.clear(player.getUuid());
                    advance("inspect");
                } else if (phase.equals("inspect")) {
                    var inspection = BlueprintSessionService.get(player);
                    require(inspection.inspectionPassed() && inspection.inspectionType().equals("farm"), "Real farm inspection did not pass");
                    require(!TownBuildingService.canAllocateInfrastructure(data, TownBuildingDefinition.get("farm")),
                            "Fresh settlement must not have free community capacity");
                    advance("blocked_register");
                } else if (phase.equals("blocked_register")) {
                    require(data.getRegisteredBuildings().isEmpty(), "Farm registered without community infrastructure");
                    require(InventoryHelper.count(player.getInventory(), Items.EMERALD) == 16, "Blocked registration charged currency");
                    System.out.println("TOWN_CONNECTED_INFRASTRUCTURE_DENIED_OK");
                    player.teleport(world, CAMPFIRE.getX() + .5, CAMPFIRE.getY(), CAMPFIRE.getZ() - 1.5, 0, 25);
                    BlueprintSessionService.clear(player.getUuid());
                    advance("campfire_inspect");
                } else if (phase.equals("campfire_inspect")) {
                    var session = BlueprintSessionService.get(player);
                    require(session.inspectionPassed() && session.inspectionType().equals("campfire"),
                            "Campfire inspection failed: " + session.primaryDetected());
                    advance("campfire_register");
                } else if (phase.equals("campfire_register")) {
                    require(data.getRegisteredBuildings().size() == 1
                            && data.getRegisteredBuildings().get(0).type().equals("campfire"), "Campfire registration failed");
                    require(TownBuildingService.canAllocateInfrastructure(data, TownBuildingDefinition.get("farm")),
                            "Registered campfire did not unlock community capacity");
                    require(InventoryHelper.count(player.getInventory(), Items.EMERALD) == 16 - campfirePrice(), "Campfire charged wrong currency");
                    System.out.println("TOWN_CONNECTED_CAMPFIRE_OK");
                    player.teleport(world, FARM.getX() + .5, FARM.getY(), FARM.getZ() + .5, 0, 0);
                    BlueprintSessionService.clear(player.getUuid());
                    advance("farm_inspect");
                } else if (phase.equals("farm_inspect")) {
                    require(BlueprintSessionService.get(player).inspectionPassed(), "Farm inspection after community failed");
                    advance("register");
                } else if (phase.equals("register")) {
                    var session = BlueprintSessionService.get(player);
                    require(data.getRegisteredBuildings().size() == 2,
                            "Farm registration failed: inspection=" + session.inspectionPassed() + " target=" + session.inspectionTarget()
                                    + " community=" + data.getInfrastructureAvailable(InfrastructureType.COMMUNITY));
                    var farm = data.getRegisteredBuildings().stream().filter(x -> x.type().equals("farm")).findFirst().orElseThrow();
                    require(farm.type().equals("farm") && farm.cropState() == 18, "Farm count incorrect " + farm);
                    int price = TownBuildingDefinition.get("farm").registrationCurrency();
                    require(InventoryHelper.count(player.getInventory(), Items.EMERALD) == 16 - campfirePrice() - price, "Wrong registration currency");
                    System.out.println("TOWN_CONNECTED_REGISTER_OK " + farm.id());
                    player.teleport(world, FARM.getX() + 5.5, FARM.getY(), FARM.getZ() + 2.5, 0, 0);
                    BlueprintSessionService.clear(player.getUuid());
                    advance("duplicate_inspect");
                } else if (phase.equals("duplicate_inspect")) {
                    require(BlueprintSessionService.get(player).inspectionPassed(), "Duplicate candidate did not inspect");
                    advance("duplicate_register");
                } else if (phase.equals("duplicate_register")) {
                    require(data.getRegisteredBuildings().size() == 2, "Connected farm registered twice");
                    int price = TownBuildingDefinition.get("farm").registrationCurrency();
                    require(InventoryHelper.count(player.getInventory(), Items.EMERALD) == 16 - campfirePrice() - price, "Duplicate registration consumed currency");
                    System.out.println("TOWN_CONNECTED_DUPLICATE_OK");
                    advance("map");
                } else if (phase.equals("map")) {
                    System.out.println("TOWN_CONNECTED_SERVER_OK");
                    advance("complete");
                }
            } catch (Exception e) {
                throw new IllegalStateException("Connected Towns probe failed at " + phase, e);
            }
        });
    }

    private static int campfirePrice() {
        return TownBuildingDefinition.get("campfire").registrationCurrency();
    }

    private void advance(String next) throws Exception {
        phase = next;
        age = 0;
        acknowledged = 0;
        Files.writeString(shared.resolve("phase"), phase);
        System.out.println("TOWN_CONNECTED_PHASE " + phase);
    }

    private static void require(boolean value, String reason) {
        if (!value) throw new IllegalStateException(reason);
    }
}
