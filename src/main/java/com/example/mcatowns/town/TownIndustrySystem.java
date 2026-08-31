package com.example.mcatowns.town;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class TownIndustrySystem {
    private TownIndustrySystem() {
    }

    public static void applyDailyQuarryYield(ServerWorld world, BlockPos anchor, TownSavedData data, TownBuildingSnapshot snapshot) {
        int quarries = Math.max(0, snapshot.quarries());
        int outputPercent = Math.max(0, data.getEventQuarryOutputPercent())
                * Math.max(0, data.getWorkforceEfficiencyPercent()) / 100;
        List<ItemStack> stacks = getQuarryOutput(quarries, outputPercent);
        int generated = stacks.stream().mapToInt(ItemStack::getCount).sum();
        data.setDailyQuarryYield(generated);
        if (stacks.isEmpty()) {
            return;
        }
        List<ItemStack> overflow = TownStorageDepositSystem.deposit(world, anchor, stacks);
        if (!overflow.isEmpty()) {
            dropOverflow(world, anchor, overflow);
        }
    }

    private static List<ItemStack> getQuarryOutput(int quarries, int outputPercent) {
        int scale = Math.max(0, quarries) * Math.max(0, outputPercent);
        List<ItemStack> stacks = new ArrayList<>();
        addScaled(stacks, Items.COAL.getDefaultStack(), scale, 18);
        addScaled(stacks, Items.RAW_COPPER.getDefaultStack(), scale, 9);
        addScaled(stacks, Items.RAW_IRON.getDefaultStack(), scale, 9);
        addScaled(stacks, Items.RAW_GOLD.getDefaultStack(), scale, 3);
        return stacks;
    }

    private static void addScaled(List<ItemStack> stacks, ItemStack template, int scale, int baseCount) {
        int count = baseCount * scale / 100;
        if (count <= 0) {
            return;
        }
        while (count > 0) {
            ItemStack stack = template.copy();
            int add = Math.min(count, stack.getMaxCount());
            stack.setCount(add);
            stacks.add(stack);
            count -= add;
        }
    }

    private static void dropOverflow(ServerWorld world, BlockPos anchor, List<ItemStack> overflow) {
        for (ItemStack stack : overflow) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemEntity item = new ItemEntity(
                    world,
                    anchor.getX() + 0.5D,
                    anchor.getY() + 1.0D,
                    anchor.getZ() + 0.5D,
                    stack.copy()
            );
            world.spawnEntity(item);
        }
    }
}
