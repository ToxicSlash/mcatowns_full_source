package com.example.mcatowns.town;

import com.example.mcatowns.blockentity.TreasuryBlockEntity;
import com.example.mcatowns.registry.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

public final class TownBonusGoodsStorageSystem {
    private TownBonusGoodsStorageSystem() {
    }

    public static List<ItemStack> deposit(ServerWorld world, BlockPos center, List<ItemStack> stacks, int radius) {
        List<ItemStack> remaining = copyNonEmpty(stacks);
        if (remaining.isEmpty()) {
            return List.of();
        }

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
                for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                    if (remaining.isEmpty()) {
                        return List.of();
                    }
                    if (pos.getSquaredDistance(center) > radiusSq) {
                        continue;
                    }
                    if (!world.getBlockState(pos).isOf(ModBlocks.TREASURY)) {
                        continue;
                    }
                    if (!(world.getBlockEntity(pos) instanceof TreasuryBlockEntity treasury)) {
                        continue;
                    }
                    remaining = insertIntoTreasury(treasury, remaining);
                }
            }
        }
        return remaining;
    }

    private static List<ItemStack> insertIntoTreasury(TreasuryBlockEntity treasury, List<ItemStack> stacks) {
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack stack : stacks) {
            ItemStack left = stack.copy();
            if (left.isEmpty()) {
                continue;
            }
            left = insertStack(treasury, left);
            if (!left.isEmpty()) {
                remaining.add(left);
            }
        }
        treasury.markDirty();
        return remaining;
    }

    private static ItemStack insertStack(TreasuryBlockEntity treasury, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < treasury.size() && !remaining.isEmpty(); slot++) {
            ItemStack at = treasury.getStack(slot);
            if (at.isEmpty()) {
                int add = Math.min(remaining.getCount(), remaining.getMaxCount());
                ItemStack placed = remaining.copy();
                placed.setCount(add);
                treasury.setStack(slot, placed);
                remaining.decrement(add);
                continue;
            }
            if (!ItemStack.canCombine(at, remaining) || at.getCount() >= at.getMaxCount()) {
                continue;
            }
            int room = at.getMaxCount() - at.getCount();
            int add = Math.min(room, remaining.getCount());
            at.increment(add);
            remaining.decrement(add);
            treasury.setStack(slot, at);
        }
        return remaining;
    }

    private static List<ItemStack> copyNonEmpty(List<ItemStack> stacks) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                out.add(stack.copy());
            }
        }
        return out;
    }
}
