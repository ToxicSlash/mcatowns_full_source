package com.example.mcatowns.util;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public final class InventoryHelper {
    private InventoryHelper() {
    }

    public static int count(PlayerInventory inventory, Item item) {
        int total = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isOf(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static int remove(PlayerInventory inventory, Item item, int amount) {
        int remaining = Math.max(0, amount);
        for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isOf(item)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.decrement(removed);
            remaining -= removed;
        }
        int removed = amount - remaining;
        if (removed > 0) {
            inventory.markDirty();
        }
        return removed;
    }

    public static int give(ServerPlayerEntity player, Item item, int amount) {
        int remaining = Math.max(0, amount);
        int given = 0;
        while (remaining > 0) {
            int count = Math.min(item.getMaxCount(), remaining);
            ItemStack stack = new ItemStack(item, count);
            player.giveItemStack(stack);
            int inserted = count - stack.getCount();
            given += inserted;
            remaining -= inserted;
            if (inserted < count) {
                break;
            }
        }
        return given;
    }
}
