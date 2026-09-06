package com.example.mcatowns.integration;

import com.example.mcatowns.town.BuildingStatus;
import com.example.mcatowns.town.PlayerTownRegistry;
import com.example.mcatowns.town.RegisteredTownBuilding;
import com.example.mcatowns.town.TownContext;
import com.example.mcatowns.town.TownSavedData;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight town-side maintenance for Immersive Weathering weeds.
 *
 * <p>This does not track unloaded time. The unloaded simulation belongs to Unloaded Activity.
 * We only cache which registered farm footprints are currently staffed and periodically remove
 * a very small number of weeds while those farms are loaded.</p>
 */
public final class FarmWeedMaintenanceService {
    private static final Identifier WEEDS_ID = new Identifier("immersive_weathering", "weeds");
    private static final long TOWN_TICK_INTERVAL = 200L;
    private static final long MAINTENANCE_INTERVAL_TICKS = 1_200L;
    private static final int SAMPLES_PER_FARM = 24;
    private static final int MAX_REMOVALS_PER_FARM = 2;
    private static final int MAX_VERTICAL_SAMPLE = 16;

    private static final Map<String, Map<String, List<FarmArea>>> STAFFED_FARMS = new ConcurrentHashMap<>();
    private static final Set<String> BOOTSTRAPPED_PLAYER_TOWNS = ConcurrentHashMap.newKeySet();

    private FarmWeedMaintenanceService() { }

    public static void refreshTown(ServerWorld world, TownContext context, TownSavedData data) {
        if (world == null || context == null || data == null) return;
        String dimension = dimensionKey(world);
        List<FarmArea> farms = staffedFarms(data);
        Map<String, List<FarmArea>> byTown = STAFFED_FARMS.computeIfAbsent(dimension, ignored -> new ConcurrentHashMap<>());
        if (farms.isEmpty()) {
            byTown.remove(context.townId());
        } else {
            byTown.put(context.townId(), farms);
        }
    }

    /** Returns only cached, already-known staffed farms intersecting this chunk. */
    public static List<FarmArea> staffedFarmsForChunk(ServerWorld world, ChunkPos chunkPos) {
        if (world == null || chunkPos == null) return List.of();
        bootstrapPlayerCreatedTowns(world);

        Map<String, List<FarmArea>> byTown = STAFFED_FARMS.get(dimensionKey(world));
        if (byTown == null || byTown.isEmpty()) return List.of();

        int minX = chunkPos.getStartX();
        int maxX = minX + 15;
        int minZ = chunkPos.getStartZ();
        int maxZ = minZ + 15;
        List<FarmArea> result = new ArrayList<>();
        for (List<FarmArea> farms : byTown.values()) {
            for (FarmArea farm : farms) {
                if (farm.intersects(minX, maxX, minZ, maxZ)) result.add(farm);
            }
        }
        return List.copyOf(result);
    }

    public static void maintainLoadedFarms(ServerWorld world, TownContext context, TownSavedData data) {
        refreshTown(world, context, data);
        if (!isMaintenanceTick(world, context)) return;

        Random random = world.random;
        for (FarmArea farm : staffedFarms(data)) {
            int removed = 0;
            for (int sample = 0; sample < SAMPLES_PER_FARM && removed < MAX_REMOVALS_PER_FARM; sample++) {
                int x = randomCoordinate(random, farm.minX(), farm.maxX());
                int z = randomCoordinate(random, farm.minZ(), farm.maxZ());
                int minY = Math.max(world.getBottomY(), farm.minY() - 1);
                int maxY = Math.min(world.getTopY() - 1, farm.maxY() + 2);
                maxY = Math.min(maxY, minY + MAX_VERTICAL_SAMPLE - 1);

                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!world.isChunkLoaded(pos)) continue;
                    if (WEEDS_ID.equals(Registries.BLOCK.getId(world.getBlockState(pos).getBlock()))) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                        removed++;
                        break;
                    }
                }
            }
        }
    }

    private static void bootstrapPlayerCreatedTowns(ServerWorld world) {
        String dimension = dimensionKey(world);
        if (!BOOTSTRAPPED_PLAYER_TOWNS.add(dimension)) return;
        PlayerTownRegistry registry = PlayerTownRegistry.get(world);
        for (TownContext context : registry.getAllContexts()) {
            refreshTown(world, context, TownSavedData.get(world, context.townId()));
        }
    }

    private static List<FarmArea> staffedFarms(TownSavedData data) {
        List<FarmArea> result = new ArrayList<>();
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            if (!"farm".equals(building.type())) continue;
            if (building.status() != BuildingStatus.ACTIVE || building.workers().isEmpty()) continue;
            result.add(FarmArea.from(building));
        }
        return List.copyOf(result);
    }

    private static boolean isMaintenanceTick(ServerWorld world, TownContext context) {
        long townTick = Math.floorDiv(world.getTime(), TOWN_TICK_INTERVAL);
        int maintenanceTownTicks = Math.max(1, (int) (MAINTENANCE_INTERVAL_TICKS / TOWN_TICK_INTERVAL));
        int phase = Math.floorMod(context.townId().hashCode(), maintenanceTownTicks);
        return Math.floorMod(townTick + phase, maintenanceTownTicks) == 0;
    }

    private static int randomCoordinate(Random random, int min, int max) {
        if (max <= min) return min;
        long span = (long) max - min + 1L;
        if (span <= Integer.MAX_VALUE) return min + random.nextInt((int) span);
        return min + random.nextInt(256);
    }

    private static String dimensionKey(ServerWorld world) {
        return world.getRegistryKey().getValue().toString();
    }

    public record FarmArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        static FarmArea from(RegisteredTownBuilding building) {
            return new FarmArea(
                    building.minPos().getX(), building.minPos().getY(), building.minPos().getZ(),
                    building.maxPos().getX(), building.maxPos().getY(), building.maxPos().getZ());
        }

        public boolean containsXZ(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }

        boolean intersects(int otherMinX, int otherMaxX, int otherMinZ, int otherMaxZ) {
            return maxX >= otherMinX && minX <= otherMaxX && maxZ >= otherMinZ && minZ <= otherMaxZ;
        }
    }
}
