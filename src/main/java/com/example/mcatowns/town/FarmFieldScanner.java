package com.example.mcatowns.town;

import net.minecraft.block.AttachedStemBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Bounded flood fill of connected farmland, including rows separated by a single irrigation channel or path. */
public final class FarmFieldScanner {
    public static final int MIN_CROPS = 12;
    public static final int MAX_CONTRIBUTING_CROPS = 144;
    private static final int MAX_VISITED_FARMLAND = 256;
    // Registry-dependent initialization is deferred until a real world crop query.
    private static final class CropTags {
        private static final TagKey<Block> COMMON_CROPS = TagKey.of(RegistryKeys.BLOCK, new Identifier("c", "crops"));
        private static final TagKey<Block> NON_PRODUCTIVE = TagKey.of(RegistryKeys.BLOCK, new Identifier("mcatowns", "non_productive_crops"));
    }

    private FarmFieldScanner() { }

    public static Result scan(ServerWorld world, BlockPos selected) {
        BlockPos seed = farmlandAt(world, selected);
        if (seed == null) return Result.empty(selected);
        return scanConnected(seed, pos -> isLoadedFarmland(world, pos),
                pos -> isCrop(world, pos.up()), pos -> canBridge(world, pos));
    }

    // Separate traversal from world queries so irregular fields can be tested without a running server.
    static Result scanConnected(BlockPos seed, Predicate<BlockPos> farmland,
                                Predicate<BlockPos> planted, Predicate<BlockPos> bridge) {
        if (!farmland.test(seed)) return Result.empty(seed);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> discovered = new HashSet<>();
        List<BlockPos> tiles = new ArrayList<>();
        enqueue(seed, farmland, discovered, queue);
        BlockPos min = seed;
        BlockPos max = seed;
        int crops = 0;
        while (!queue.isEmpty() && tiles.size() < MAX_VISITED_FARMLAND) {
            BlockPos current = queue.removeFirst();
            tiles.add(current);
            min = new BlockPos(Math.min(min.getX(), current.getX()), Math.min(min.getY(), current.getY()),
                    Math.min(min.getZ(), current.getZ()));
            max = new BlockPos(Math.max(max.getX(), current.getX()), Math.max(max.getY(), current.getY()),
                    Math.max(max.getZ(), current.getZ()));
            if (planted.test(current)) crops++;
            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockPos adjacent = current.offset(direction);
                // Check all three elevations, not just the first one found.
                for (int dy = -1; dy <= 1; dy++) {
                    enqueue(adjacent.add(0, dy, 0), farmland, discovered, queue);
                }
                // Only bridge a known passable one-block gap on the same level.
                // A two-block gap, wall or unloaded chunk must never connect fields.
                if (bridge.test(adjacent)) {
                    enqueue(adjacent.offset(direction), farmland, discovered, queue);
                }
            }
        }
        int contributing = Math.min(crops, MAX_CONTRIBUTING_CROPS);
        return new Result(seed.toImmutable(), min.toImmutable(), max.up().toImmutable(), crops,
                contributing, tierForCropCount(contributing), !queue.isEmpty(), List.copyOf(tiles));
    }

    private static void enqueue(BlockPos pos, Predicate<BlockPos> farmland, Set<Long> discovered,
                                ArrayDeque<BlockPos> queue) {
        if (!discovered.contains(pos.asLong()) && farmland.test(pos) && discovered.add(pos.asLong())) {
            queue.addLast(pos.toImmutable());
        }
    }

    public static int tierForCropCount(int crops) {
        if (crops > 64) return 3;
        if (crops > 25) return 2;
        return 1;
    }

    public static int cropCapForTier(int tier) {
        return tier >= 3 ? 144 : tier == 2 ? 64 : 25;
    }

    static BlockPos farmlandAt(ServerWorld world, BlockPos pos) {
        if (pos == null || !loaded(world, pos)) return null;
        if (isLoadedFarmland(world, pos)) return pos.toImmutable();
        if (isLoadedFarmland(world, pos.down()) && (isCrop(world, pos) || world.getBlockState(pos).isAir())) {
            return pos.down().toImmutable();
        }
        return null;
    }

    private static boolean loaded(ServerWorld world, BlockPos pos) {
        return world.getWorldBorder().contains(pos) && world.isChunkLoaded(pos)
                && !world.isOutOfHeightLimit(pos);
    }

    private static boolean isLoadedFarmland(ServerWorld world, BlockPos pos) {
        return loaded(world, pos) && world.getBlockState(pos).getBlock() instanceof FarmlandBlock;
    }

    private static boolean canBridge(ServerWorld world, BlockPos pos) {
        if (!loaded(world, pos) || !loaded(world, pos.up())) return false;
        var state = world.getBlockState(pos);
        if (!(state.isAir() || state.isOf(Blocks.WATER) || state.isOf(Blocks.DIRT_PATH))) return false;
        return world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty();
    }

    private static boolean isCrop(ServerWorld world, BlockPos pos) {
        if (!loaded(world, pos)) return false;
        var state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (state.isIn(CropTags.NON_PRODUCTIVE)) return false;
        return block instanceof CropBlock || block instanceof StemBlock || block instanceof AttachedStemBlock
                || block instanceof NetherWartBlock || state.isIn(CropTags.COMMON_CROPS);
    }

    public record Result(BlockPos anchor, BlockPos min, BlockPos max, int crops, int contributingCrops,
                         int tier, boolean scanLimited, List<BlockPos> tiles) {
        static Result empty(BlockPos selected) {
            BlockPos safe = selected == null ? BlockPos.ORIGIN : selected.toImmutable();
            return new Result(safe, safe, safe, 0, 0, 1, false, List.of());
        }

        public boolean valid() { return crops >= MIN_CROPS; }
        public int cropPercent() { return Math.min(100, contributingCrops * 100 / MAX_CONTRIBUTING_CROPS); }
    }
}
