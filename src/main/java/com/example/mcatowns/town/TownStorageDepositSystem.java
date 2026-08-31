package com.example.mcatowns.town;

import com.example.mcatowns.integration.MCAIntegration;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

public final class TownStorageDepositSystem {
    private static final int STORAGE_SCAN_RADIUS = 16;

    private TownStorageDepositSystem() {
    }

    public static List<ItemStack> deposit(ServerWorld world, BlockPos townAnchor, List<ItemStack> stacks) {
        List<ItemStack> remaining = copyNonEmpty(stacks);
        if (remaining.isEmpty()) {
            return List.of();
        }

        List<BlockPos> storageCenters = MCAIntegration.getNearestTownStorageCenters(world, townAnchor);
        for (BlockPos storageCenter : storageCenters) {
            if (remaining.isEmpty()) {
                return List.of();
            }
            remaining = depositToInventoriesNear(world, storageCenter, remaining, STORAGE_SCAN_RADIUS);
        }

        if (remaining.isEmpty()) {
            return List.of();
        }
        return remaining;
    }

    private static List<ItemStack> depositToInventoriesNear(ServerWorld world, BlockPos center, List<ItemStack> stacks, int radius) {
        List<ItemStack> remaining = copyNonEmpty(stacks);
        int chunkRadius = Math.max(1, radius / 16);
        int baseChunkX = center.getX() >> 4;
        int baseChunkZ = center.getZ() >> 4;
        int radiusSq = radius * radius;

        for (int cx = baseChunkX - chunkRadius; cx <= baseChunkX + chunkRadius; cx++) {
            for (int cz = baseChunkZ - chunkRadius; cz <= baseChunkZ + chunkRadius; cz++) {
                if (remaining.isEmpty()) {
                    return List.of();
                }
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
                    BlockEntity be = world.getBlockEntity(pos);
                    if (!(be instanceof Inventory inv)) {
                        continue;
                    }
                    remaining = insertIntoInventory(inv, remaining);
                }
            }
        }
        return remaining;
    }

    private static List<ItemStack> insertIntoInventory(Inventory inventory, List<ItemStack> stacks) {
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack stack : stacks) {
            ItemStack left = stack.copy();
            if (left.isEmpty()) {
                continue;
            }
            left = insertStack(inventory, left);
            if (!left.isEmpty()) {
                remaining.add(left);
            }
        }
        inventory.markDirty();
        return remaining;
    }

    private static ItemStack insertStack(Inventory inventory, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < inventory.size() && !remaining.isEmpty(); slot++) {
            ItemStack at = inventory.getStack(slot);
            if (at.isEmpty()) {
                int add = Math.min(remaining.getCount(), remaining.getMaxCount());
                ItemStack placed = remaining.copy();
                placed.setCount(add);
                inventory.setStack(slot, placed);
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
            inventory.setStack(slot, at);
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
