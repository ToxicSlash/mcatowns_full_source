package com.example.mcatowns.town;

import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TownManager {
    public static TownSavedData get(ServerWorld world, BlockPos pos) {
        return TownSavedData.get(world, resolveTownContext(world, pos).townId());
    }

    public static TownContext resolveTownContext(ServerWorld world, BlockPos pos) {
        return findExistingTown(world, pos, getTownSearchMargin())
                .orElseGet(() -> {
                    String dim = world.getRegistryKey().getValue().toString().replace(':', '_').replace('/', '_');
                    String fallbackId = "fallback_" + dim
                            + "_" + (pos.getX() >> 7) + "_" + (pos.getZ() >> 7);
                    return new TownContext(fallbackId, pos, pos, "fallback", false);
                });
    }

    public static int getTownSearchMargin() {
        return MCATownsConfig.get().mcaVillageSearchMargin;
    }

    public static Optional<TownContext> findExistingTown(ServerWorld world, BlockPos pos, int margin) {
        PlayerTownRegistry playerTowns = PlayerTownRegistry.get(world);
        Optional<TownContext> founded = playerTowns.findNearest(pos, margin);
        if (founded.isPresent()) return founded;
        return MCAIntegration.findNearestTown(world, pos, margin)
                .flatMap(mca -> playerTowns.getTownByMcaId(mca.townId()).or(() -> Optional.of(mca)));
    }

    public static boolean hasMayorAuthority(ServerPlayerEntity player, TownContext context) {
        if ("player_created".equals(context.source())) {
            return PlayerTownRegistry.get(player.getServerWorld()).isOwner(context.townId(), player);
        }
        return MCAIntegration.hasMayorRank(player, context.anchor());
    }

    public static List<TownContext> getTickContexts(ServerWorld world) {
        PlayerTownRegistry playerTowns = PlayerTownRegistry.get(world);
        List<TownContext> contexts = new ArrayList<>(playerTowns.getAllContexts());
        for (TownContext mcaTown : MCAIntegration.getAllKnownTowns(world)) {
            if (playerTowns.getTownByMcaId(mcaTown.townId()).isEmpty()) contexts.add(mcaTown);
        }
        if (contexts.isEmpty() && !MCAIntegration.isMcaLoaded()) {
            contexts.add(resolveTownContext(world, world.getSpawnPos()));
        }
        Map<String, TownContext> unique = new LinkedHashMap<>();
        for (TownContext context : contexts) {
            unique.putIfAbsent(context.townId(), context);
        }
        return List.copyOf(unique.values());
    }

    public static int countAddonBlocks(ServerWorld world, BlockPos center, net.minecraft.block.Block target, int radius) {
        int total = 0;
        int chunkRadius = Math.max(1, radius / 16);
        int baseChunkX = center.getX() >> 4;
        int baseChunkZ = center.getZ() >> 4;
        int radiusSq = radius * radius;
        for (int cx = baseChunkX - chunkRadius; cx <= baseChunkX + chunkRadius; cx++) {
            for (int cz = baseChunkZ - chunkRadius; cz <= baseChunkZ + chunkRadius; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockPos p : chunk.getBlockEntities().keySet()) {
                    if (p.getSquaredDistance(center) > radiusSq) {
                        continue;
                    }
                    if (world.getBlockState(p).isOf(target) && isAddonBlockInBuilding(world, p, target)) {
                        total++;
                    }
                }
            }
        }
        return total;
    }

    private static boolean isAddonBlockInBuilding(ServerWorld world, BlockPos pos, net.minecraft.block.Block target) {
        if (!MCATownsConfig.get().requireAddonBlocksInBuilding) {
            return true;
        }
        if (!matchesRequiredMcaBuildingType(world, pos, target)) {
            return false;
        }
        if (!isSolidStructureBlock(world, pos.down())) {
            return false;
        }
        if (!hasRoof(world, pos, 5)) {
            return false;
        }
        return countWallSides(world, pos, 4) >= 3;
    }

    private static boolean matchesRequiredMcaBuildingType(ServerWorld world, BlockPos pos, net.minecraft.block.Block target) {
        if (!MCAIntegration.isMcaLoaded()) {
            return true;
        }
        var typeOpt = MCAIntegration.getBuildingTypeAt(world, pos);
        if (typeOpt.isEmpty()) {
            return false;
        }
        String type = typeOpt.get();
        if (target == ModBlocks.TREASURY) {
            return "mcatowns_treasury".equals(type);
        }
        if (target == ModBlocks.BARRACKS) {
            return "mcatowns_barracks".equals(type) && hasNearbyArmorStand(world, pos);
        }
        return true;
    }

    private static boolean hasNearbyArmorStand(ServerWorld world, BlockPos pos) {
        Box area = new Box(pos).expand(6.0D, 3.0D, 6.0D);
        return !world.getEntitiesByClass(ArmorStandEntity.class, area, ArmorStandEntity::isAlive).isEmpty();
    }

    private static boolean hasRoof(ServerWorld world, BlockPos pos, int maxHeight) {
        for (int dy = 1; dy <= maxHeight; dy++) {
            if (isSolidStructureBlock(world, pos.up(dy))) {
                return true;
            }
        }
        return false;
    }

    private static int countWallSides(ServerWorld world, BlockPos pos, int maxDistance) {
        int walls = 0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (hasWallInDirection(world, pos, direction, maxDistance)) {
                walls++;
            }
        }
        return walls;
    }

    private static boolean hasWallInDirection(ServerWorld world, BlockPos pos, Direction direction, int maxDistance) {
        for (int i = 1; i <= maxDistance; i++) {
            BlockPos atFeet = pos.offset(direction, i);
            BlockPos atHead = atFeet.up();
            if (isSolidStructureBlock(world, atFeet) || isSolidStructureBlock(world, atHead)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSolidStructureBlock(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isSolidBlock(world, pos);
    }

    public static long getDay(ServerWorld world) {
        return world.getTimeOfDay() / 24000L;
    }
}
