package com.example.mcatowns.gametest;

import com.example.mcatowns.town.BuildingFootprint;
import com.example.mcatowns.town.FarmFieldScanner;
import com.example.mcatowns.town.TownBuildingService;
import com.example.mcatowns.town.TownSavedData;
import com.example.mcatowns.town.RegisteredTownBuilding;
import com.example.mcatowns.town.BuildingStatus;
import net.minecraft.nbt.NbtCompound;
import java.util.UUID;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.CropBlock;
import com.example.mcatowns.integration.MCAIntegration;
import net.minecraft.block.Blocks;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashSet;
import java.util.Set;

/** Real server-world fixtures. The test mod is not included in the production jar. */
public final class TownWorldGameTests implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void installedMcaAndFarmRegistry(TestContext context) {
        if (Boolean.getBoolean("town.test.mca")) {
            context.assertTrue(MCAIntegration.isMcaLoaded(), "MCA must be installed for the integration suite");
            context.assertTrue(Registries.ITEM.containsId(new Identifier("mca", "blueprint")),
                    "MCA blueprint must be registered");
            for (var id : Registries.BLOCK.getIds()) {
                var block = Registries.BLOCK.get(id);
                if (!id.getNamespace().equals("minecraft") && (block instanceof FarmlandBlock || block instanceof CropBlock)) {
                    System.out.println("TOWN_INSTALLED_FARM_BLOCK " + id);
                }
            }
            // Check installed CropBlock implementations using the same scanner as registration.
            BlockPos soil = new BlockPos(2, 2, 2);
            context.setBlockState(soil.down(), Blocks.DIRT);
            context.setBlockState(soil, Blocks.FARMLAND);
            int verified = 0;
            for (var id : Registries.BLOCK.getIds()) {
                var block = Registries.BLOCK.get(id);
                if (id.getNamespace().equals("minecraft") || !(block instanceof CropBlock)) continue;
                var state = block.getDefaultState();
                BlockPos absolute = context.getAbsolutePos(soil.up());
                if (!state.canPlaceAt(context.getWorld(), absolute)) continue;
                context.setBlockState(soil.up(), state);
                var result = FarmFieldScanner.scan(context.getWorld(), context.getAbsolutePos(soil));
                boolean weed = id.equals(new Identifier("immersive_weathering", "weeds"));
                context.assertTrue(result.crops() == (weed ? 0 : 1), "Incorrect crop contribution: " + id);
                context.setBlockState(soil.up(), Blocks.AIR);
                if (!weed) verified++;
                System.out.println((weed ? "TOWN_WEED_EXCLUDED " : "TOWN_INSTALLED_CROP_OK ") + id);
            }
            context.assertTrue(verified > 0, "No installed farmland-compatible crop was exercised");
            int soilTypes = 0;
            for (var id : Registries.BLOCK.getIds()) {
                var block = Registries.BLOCK.get(id);
                if (id.getNamespace().equals("minecraft") || !(block instanceof FarmlandBlock)) continue;
                context.setBlockState(soil, block.getDefaultState());
                context.setBlockState(soil.up(), Blocks.WHEAT);
                var result = FarmFieldScanner.scan(context.getWorld(), context.getAbsolutePos(soil));
                context.assertTrue(result.tiles().size() == 1 && result.crops() == 1,
                        "Installed farmland not detected: " + id);
                context.setBlockState(soil.up(), Blocks.AIR);
                soilTypes++;
                System.out.println("TOWN_INSTALLED_SOIL_OK " + id);
            }
            context.assertTrue(soilTypes > 0, "No installed farmland variants exercised");
            System.out.println("TOWN_MCA_WORLD_OK " + verified);
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void irrigatedAndPathedFarm(TestContext context) {
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 6; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT);
                if (x == 2) {
                    context.setBlockState(new BlockPos(x, 2, z), Blocks.WATER);
                } else if (x == 4) {
                    context.setBlockState(new BlockPos(x, 2, z), Blocks.DIRT_PATH);
                } else {
                    context.setBlockState(new BlockPos(x, 2, z), Blocks.FARMLAND);
                    context.setBlockState(new BlockPos(x, 3, z), Blocks.WHEAT);
                }
            }
        }
        for (BlockPos seed : Set.of(new BlockPos(1, 3, 1), new BlockPos(5, 2, 6))) {
            var field = FarmFieldScanner.scan(context.getWorld(), context.getAbsolutePos(seed));
            context.assertTrue(field.valid(), "Field should meet the 12 crop minimum");
            context.assertTrue(field.crops() == 18, "Expected all 18 planted tiles, got " + field.crops());
            context.assertTrue(field.tiles().size() == 18, "Expected all 18 farmland positions");
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void fenceSeparatesFields(TestContext context) {
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 6; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT);
                if (x == 2) {
                    context.setBlockState(new BlockPos(x, 2, z), Blocks.WATER);
                    context.setBlockState(new BlockPos(x, 3, z), Blocks.OAK_FENCE);
                } else {
                    context.setBlockState(new BlockPos(x, 2, z), Blocks.FARMLAND);
                    context.setBlockState(new BlockPos(x, 3, z), Blocks.WHEAT);
                }
            }
        }
        var field = FarmFieldScanner.scan(context.getWorld(), context.getAbsolutePos(new BlockPos(1, 3, 1)));
        context.assertTrue(field.crops() == 6, "Fence must prevent joining adjacent fields");
        context.assertFalse(field.valid(), "Six planted tiles cannot register a farm");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void rectangleWallsIgnoreRoofOverhang(TestContext context) {
        Set<BlockPos> interior = new HashSet<>();
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) interior.add(new BlockPos(x, 2, z));
        assertRoom(context, interior, new BlockPos(3, 2, 3), 12);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void concaveWallsIgnoreRoofOverhang(TestContext context) {
        Set<BlockPos> interior = new HashSet<>();
        for (int x = 2; x <= 5; x++) {
            for (int z = 2; z <= 5; z++) {
                if (x < 4 || z > 3) interior.add(new BlockPos(x, 2, z));
            }
        }
        assertRoom(context, interior, new BlockPos(2, 2, 4), 16);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void uShapeWithCourtyard(TestContext context) {
        Set<BlockPos> interior = new HashSet<>();
        for (int x = 2; x <= 6; x++) for (int z = 2; z <= 5; z++) {
            if (x < 4 || x > 5 || z == 5) interior.add(new BlockPos(x, 2, z));
        }
        assertRoom(context, interior, new BlockPos(2, 2, 3), 24);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void doorAndWindowDoNotExtendOutline(TestContext context) {
        Set<BlockPos> interior = new HashSet<>();
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++) interior.add(new BlockPos(x, 2, z));
        assertRoom(context, interior, new BlockPos(3, 2, 3), 12, true);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void furnishedRoomKeepsWallFootprint(TestContext context) {
        Set<BlockPos> interior = new HashSet<>();
        for (int x = 2; x <= 5; x++) for (int z = 2; z <= 5; z++) interior.add(new BlockPos(x, 2, z));
        assertRoom(context, interior, new BlockPos(3, 2, 3), 16, false, true);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void registeredFarmSaveDataAndDuplicateGuard(TestContext context) {
        for (int x = 1; x <= 6; x++) for (int z = 1; z <= 3; z++) {
            context.setBlockState(new BlockPos(x, 1, z), Blocks.DIRT);
            context.setBlockState(new BlockPos(x, 2, z), Blocks.FARMLAND);
            context.setBlockState(new BlockPos(x, 3, z), Blocks.WHEAT);
        }
        var world = context.getWorld();
        var field = FarmFieldScanner.scan(world, context.getAbsolutePos(new BlockPos(1, 3, 1)));
        var otherEnd = FarmFieldScanner.scan(world, context.getAbsolutePos(new BlockPos(6, 3, 3)));
        context.assertTrue(field.valid(), "Farm should be eligible");
        var data = TownSavedData.create();
        var farm = new RegisteredTownBuilding(UUID.randomUUID(), "farm", field.tier(), field.anchor(),
                field.min(), field.max(), BuildingStatus.ACTIVE, 50, List.of(), 1L, field.contributingCrops());
        context.assertTrue(!TownBuildingService.isFarmAlreadyRegistered(data, field), "New farm must not be duplicate");
        context.assertTrue(data.registerBuilding(farm), "Farm must register in town save");
        context.assertTrue(TownBuildingService.isFarmAlreadyRegistered(data, otherEnd), "Other end of same field must be duplicate");
        var restored = TownSavedData.fromNbt(((net.minecraft.world.PersistentState) data).writeNbt(new NbtCompound()));
        context.assertTrue(restored.getRegisteredBuildings().size() == 1, "Exactly one farm must persist");
        context.assertTrue(farm.equals(restored.getRegisteredBuildings().get(0)), "Saved farm properties must round-trip");
        context.assertTrue(TownBuildingService.isFarmAlreadyRegistered(restored, otherEnd), "Duplicate guard must survive save/load");
        context.complete();
    }

    private static void assertRoom(TestContext context, Set<BlockPos> interior, BlockPos start, int perimeter) {
        assertRoom(context, interior, start, perimeter, false);
    }

    private static void assertRoom(TestContext context, Set<BlockPos> interior, BlockPos start, int perimeter, boolean openings) {
        assertRoom(context, interior, start, perimeter, openings, false);
    }

    private static void assertRoom(TestContext context, Set<BlockPos> interior, BlockPos start, int perimeter, boolean openings, boolean furniture) {
        // Floor and roof deliberately extend outside the actual wall boundary.
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                context.setBlockState(new BlockPos(x, 1, z), Blocks.STONE);
                context.setBlockState(new BlockPos(x, 4, z), Blocks.OAK_PLANKS);
            }
        }
        for (BlockPos cell : interior) {
            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockPos wall = cell.offset(direction);
                if (!interior.contains(wall)) {
                    context.setBlockState(wall, Blocks.STONE_BRICKS);
                    context.setBlockState(wall.up(), Blocks.STONE_BRICKS);
                }
            }
        }
        if (openings) {
            var door = Blocks.OAK_DOOR.getDefaultState().with(net.minecraft.block.DoorBlock.OPEN, true);
            context.setBlockState(new BlockPos(1, 2, 3), door);
            context.setBlockState(new BlockPos(1, 3, 3), door.with(net.minecraft.block.DoorBlock.HALF,
                    net.minecraft.block.enums.DoubleBlockHalf.UPPER));
            context.setBlockState(new BlockPos(3, 3, 1), Blocks.GLASS_PANE);
            for (int x = 0; x < 8; x++) context.setBlockState(new BlockPos(x, 4, 0), Blocks.OAK_SLAB);
        }
        if (furniture) {
            context.setBlockState(new BlockPos(2, 2, 2), Blocks.CRAFTING_TABLE);
            context.setBlockState(new BlockPos(3, 2, 2), Blocks.BARREL);
            context.setBlockState(new BlockPos(4, 2, 2), Blocks.FURNACE);
            context.setBlockState(new BlockPos(5, 2, 4), Blocks.BOOKSHELF);
        }
        var detected = TownBuildingService.describeDetectedStructure(context.getWorld(), context.getAbsolutePos(start));
        context.assertTrue(detected.isPresent(), "Enclosed building must be detected");
        var structure = detected.orElseThrow();
        Set<BlockPos> expected = new HashSet<>();
        for (BlockPos cell : interior) expected.add(context.getAbsolutePos(cell));
        context.assertTrue(structure.enclosed(), "Structure must be enclosed");
        context.assertTrue(expected.equals(new HashSet<>(structure.footprint())), "Floor footprint must match the walls, not the roof");
        context.assertTrue(BuildingFootprint.boundary(structure.footprint()).size() == perimeter,
                "Unexpected wall perimeter");
        context.complete();
    }
}
