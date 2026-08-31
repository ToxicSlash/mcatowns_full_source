package com.example.mcatowns.integration;

import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.town.TownSavedData;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public class VillagerBusinessIntegration {
    private static final Map<UUID, Long> NEXT_ELIGIBLE_VISIT_TICK = new HashMap<>();
    private static long lastCleanupTick = Long.MIN_VALUE;

    public static void tickMcaVillagersForStores(ServerWorld world, BlockPos townPos, TownSavedData data) {
        tickMcaVillagersForStores(world, townPos, data, MCATownsConfig.get().villagerBusinessRadius);
    }

    public static void tickMcaVillagersForStores(ServerWorld world, BlockPos townPos, TownSavedData data, int radius) {
        long worldTime = world.getTime();
        if (Math.floorMod(worldTime, MCATownsConfig.get().storeVisitTickRate) >= 200) return;
        int current = Math.max(0, data.getBusinessActivityScore() - 1);

        List<BlockPos> stands = findNearbyStands(world, townPos, radius);
        if (stands.isEmpty()) {
            data.setBusinessActivityScore(current);
            return;
        }

        Box villagerBox = new Box(townPos).expand(radius);
        List<PathAwareEntity> villagers = world.getEntitiesByClass(PathAwareEntity.class, villagerBox, VillagerBusinessIntegration::isMcaVillagerCandidate);
        if (villagers.isEmpty()) {
            data.setBusinessActivityScore(current);
            return;
        }

        if (worldTime < lastCleanupTick || worldTime - lastCleanupTick >= 24000L) {
            NEXT_ELIGIBLE_VISIT_TICK.entrySet().removeIf(e -> e.getValue() + 24000L < worldTime);
            lastCleanupTick = worldTime;
        }

        float visitMultiplier = getStoreVisitMultiplier(data.getHappiness());
        int assigned = 0;
        for (PathAwareEntity villager : villagers) {
            if (assigned >= MCATownsConfig.get().villagerBusinessMaxAssignmentsPerCycle) {
                break;
            }
            if (world.random.nextFloat() > 0.18f * visitMultiplier) continue;
            if (!villager.getNavigation().isIdle()) continue;
            long eligibleAt = NEXT_ELIGIBLE_VISIT_TICK.getOrDefault(villager.getUuid(), 0L);
            if (worldTime < eligibleAt) continue;

            BlockPos target = stands.get(world.random.nextInt(stands.size()));
            if (villager.getNavigation().startMovingTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.8D)) {
                int cooldown = MCATownsConfig.get().villagerBusinessMinCooldownTicks
                        + world.random.nextInt(MCATownsConfig.get().villagerBusinessMaxCooldownTicks
                        - MCATownsConfig.get().villagerBusinessMinCooldownTicks + 1);
                NEXT_ELIGIBLE_VISIT_TICK.put(villager.getUuid(), worldTime + cooldown);
                assigned++;
            }
        }
        data.setBusinessActivityScore(Math.min(100, current + assigned * 2));
    }

    public static float getStoreVisitMultiplier(int happiness) {
        if (happiness >= 80) return 1.25f;
        if (happiness >= 60) return 1.10f;
        return 1.0f;
    }

    private static boolean isMcaVillagerCandidate(PathAwareEntity entity) {
        String className = entity.getClass().getName();
        if (!"net.mca.entity.VillagerEntityMCA".equals(className)) {
            return false;
        }
        if (entity.isBaby() || entity.isSleeping()) {
            return false;
        }
        return entity.getTarget() == null;
    }

    private static List<BlockPos> findNearbyStands(ServerWorld world, BlockPos center, int radius) {
        List<BlockPos> stands = new ArrayList<>();
        int chunkRadius = Math.max(1, radius / 16);
        int baseChunkX = center.getX() >> 4;
        int baseChunkZ = center.getZ() >> 4;
        int r2 = radius * radius;

        for (int cx = baseChunkX - chunkRadius; cx <= baseChunkX + chunkRadius; cx++) {
            for (int cz = baseChunkZ - chunkRadius; cz <= baseChunkZ + chunkRadius; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;
                for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                    if (pos.getSquaredDistance(center) > r2) continue;
                    Identifier id = Registries.BLOCK.getId(world.getBlockState(pos).getBlock());
                    String path = id.getPath();
                    if (path.contains("sales_stand") || path.contains("request_stand")) {
                        stands.add(pos.toImmutable());
                    }
                }
            }
        }
        return stands;
    }
}
