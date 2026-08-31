package com.example.mcatowns.town;

import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Bounded flood fill for irregular connected fields. Nodes are farmland blocks, not every block in a cube. */
public final class FarmFieldScanner {
    public static final int MIN_CROPS = 12;
    public static final int MAX_CONTRIBUTING_CROPS = 144;
    private static final int MAX_VISITED_FARMLAND = 256;

    private FarmFieldScanner() { }

    public static Result scan(ServerWorld world, BlockPos selected) {
        BlockPos seed = farmlandAt(world, selected);
        if (seed == null || !world.getWorldBorder().contains(seed) || !world.isChunkLoaded(seed)) {
            return Result.empty(selected);
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(seed);
        BlockPos min = seed;
        BlockPos max = seed;
        int crops = 0;

        while (!queue.isEmpty() && visited.size() < MAX_VISITED_FARMLAND) {
            BlockPos farmland = queue.removeFirst();
            if (!visited.add(farmland.asLong())) continue;
            min = new BlockPos(Math.min(min.getX(), farmland.getX()), Math.min(min.getY(), farmland.getY()),
                    Math.min(min.getZ(), farmland.getZ()));
            max = new BlockPos(Math.max(max.getX(), farmland.getX()), Math.max(max.getY(), farmland.getY()),
                    Math.max(max.getZ(), farmland.getZ()));
            if (world.getBlockState(farmland.up()).getBlock() instanceof CropBlock) crops++;

            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockPos adjacent = farmland.offset(direction);
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos next = adjacent.add(0, dy, 0);
                    if (!visited.contains(next.asLong()) && isLoadedFarmland(world, next)) {
                        queue.addLast(next.toImmutable());
                        break;
                    }
                }
            }
        }

        int contributing = Math.min(crops, MAX_CONTRIBUTING_CROPS);
        int tier = tierForCropCount(contributing);
        return new Result(seed.toImmutable(), min.toImmutable(), max.up().toImmutable(), crops,
                contributing, tier, visited.size() >= MAX_VISITED_FARMLAND);
    }

    public static int tierForCropCount(int crops) {
        if (crops > 64) return 3;
        if (crops > 25) return 2;
        return 1;
    }

    public static int cropCapForTier(int tier) {
        return tier >= 3 ? 144 : tier == 2 ? 64 : 25;
    }

    private static BlockPos farmlandAt(ServerWorld world, BlockPos pos) {
        if (world.getBlockState(pos).isOf(Blocks.FARMLAND)) return pos.toImmutable();
        if (world.getBlockState(pos).getBlock() instanceof CropBlock && world.getBlockState(pos.down()).isOf(Blocks.FARMLAND)) {
            return pos.down().toImmutable();
        }
        return null;
    }

    private static boolean isLoadedFarmland(ServerWorld world, BlockPos pos) {
        return world.getWorldBorder().contains(pos) && world.isChunkLoaded(pos)
                && world.getBlockState(pos).isOf(Blocks.FARMLAND);
    }

    public record Result(BlockPos anchor, BlockPos min, BlockPos max, int crops, int contributingCrops,
                         int tier, boolean scanLimited) {
        static Result empty(BlockPos selected) {
            BlockPos safe = selected == null ? BlockPos.ORIGIN : selected.toImmutable();
            return new Result(safe, safe, safe, 0, 0, 1, false);
        }

        public boolean valid() { return crops >= MIN_CROPS; }
        public int cropPercent() { return Math.min(100, contributingCrops * 100 / MAX_CONTRIBUTING_CROPS); }
    }
}
