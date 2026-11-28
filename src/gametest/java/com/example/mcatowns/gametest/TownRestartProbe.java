package com.example.mcatowns.gametest;

import com.example.mcatowns.town.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import java.util.List;
import java.util.UUID;

/** Isolated production server start/stop probe, never active in normal gameplay. */
public final class TownRestartProbe implements ModInitializer {
    private static final BlockPos SOIL = new BlockPos(2, 240, 2);
    private static final UUID FARM_ID = UUID.fromString("c7159acf-3243-4b78-8b65-1de530779b11");
    @Override public void onInitialize() {
        String phase = System.getProperty("town.test.restart", "");
        if (!phase.equals("create") && !phase.equals("verify")) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var world = server.getOverworld();
            world.getChunk(SOIL);
            var data = TownSavedData.get(world, "ci_farm_restart");
            if (phase.equals("create")) {
                if (!data.getRegisteredBuildings().isEmpty()) throw new IllegalStateException("Restart fixture was not fresh");
                world.getGameRules().get(GameRules.RANDOM_TICK_SPEED).set(0, server);
                for (int x = 0; x < 6; x++) for (int z = 0; z < 3; z++) {
                    BlockPos p = SOIL.add(x, 0, z);
                    world.setBlockState(p.down(), Blocks.DIRT.getDefaultState());
                    world.setBlockState(p, Blocks.FARMLAND.getDefaultState().with(FarmlandBlock.MOISTURE, 7));
                    world.setBlockState(p.up(), Blocks.WHEAT.getDefaultState());
                }
                var field = FarmFieldScanner.scan(world, SOIL);
                if (field.crops() != 18) throw new IllegalStateException("Expected 18 planted crops");
                if (!data.registerBuilding(new RegisteredTownBuilding(FARM_ID, "farm", field.tier(), field.anchor(),
                        field.min(), field.max(), BuildingStatus.ACTIVE, 50, List.of(), 1L, field.contributingCrops()))) {
                    throw new IllegalStateException("Could not register restart farm");
                }
                System.out.println("TOWN_RESTART_CREATE_OK");
            } else {
                if (data.getRegisteredBuildings().size() != 1) throw new IllegalStateException("Missing saved farm");
                var farm = data.getRegisteredBuildings().get(0);
                var field = FarmFieldScanner.scan(world, SOIL.add(5, 0, 2));
                if (!farm.id().equals(FARM_ID) || farm.cropState() != 18 || !farm.anchor().equals(SOIL)
                        || !farm.minPos().equals(SOIL) || !farm.maxPos().equals(SOIL.add(5, 1, 2))
                        || field.crops() != 18 || !TownBuildingService.isFarmAlreadyRegistered(data, field)) {
                    throw new IllegalStateException("Restart changed farm state or duplicate protection: " + farm);
                }
                System.out.println("TOWN_RESTART_VERIFY_OK");
            }
            server.stop(false);
        });
    }
}
