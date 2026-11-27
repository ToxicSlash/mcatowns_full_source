package com.example.mcatowns.gametest;

import com.example.mcatowns.town.BuildingFootprint;
import com.example.mcatowns.town.FarmFieldScanner;
import com.example.mcatowns.town.TownBuildingService;
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
                var result = FarmFieldScanner.scan(context.getWorld(), absolute);
                context.assertTrue(result.crops() == 1, "Installed crop not detected: " + id);
                context.setBlockState(soil.up(), Blocks.AIR);
                verified++;
                System.out.println("TOWN_INSTALLED_CROP_OK " + id);
            }
            context.assertTrue(verified > 0, "No installed farmland-compatible crop was exercised");
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

    private static void assertRoom(TestContext context, Set<BlockPos> interior, BlockPos start, int perimeter) {
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
