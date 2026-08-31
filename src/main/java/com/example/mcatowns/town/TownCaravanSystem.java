package com.example.mcatowns.town;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.TraderLlamaEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class TownCaravanSystem {
    private static final int CARAVAN_INTERVAL_DAYS = 7;

    private static final String MERCHANT = "merchant";
    private static final String SUPPLY = "supply";
    private static final String MILITARY = "military";
    private static final String IMMIGRANT = "immigrant";

    private TownCaravanSystem() {
    }

    public static void tickDaily(ServerWorld world, TownContext context, TownSavedData data, long day) {
        expireBonuses(data, day);

        if (data.getNextCaravanDay() < 0) {
            data.setNextCaravanDay(day + CARAVAN_INTERVAL_DAYS);
            return;
        }
        if (day < data.getNextCaravanDay()) {
            return;
        }

        String type = pickType(world);
        applyCaravanEffects(world, context.center(), data, day, type);
        data.setLastCaravanType(type);
        data.setLastCaravanDay(day);
        data.setNextCaravanDay(day + CARAVAN_INTERVAL_DAYS);
    }

    private static void expireBonuses(TownSavedData data, long day) {
        if (data.getCaravanDefenseBonus() != 0 && day > data.getCaravanDefenseUntilDay()) {
            data.setCaravanDefenseBonus(0);
            data.setCaravanDefenseUntilDay(-1L);
        }
        if (data.getCaravanPopulationBonus() != 0 && day > data.getCaravanPopulationUntilDay()) {
            data.setCaravanPopulationBonus(0);
            data.setCaravanPopulationUntilDay(-1L);
        }
    }

    private static String pickType(ServerWorld world) {
        int roll = world.getRandom().nextInt(4);
        if (roll == 0) return MERCHANT;
        if (roll == 1) return SUPPLY;
        if (roll == 2) return MILITARY;
        return IMMIGRANT;
    }

    private static void applyCaravanEffects(ServerWorld world, BlockPos anchor, TownSavedData data, long day, String type) {
        spawnCaravan(world, anchor, type);
        int tradeBonusPercent = getTradingPostCaravanBonus(data);
        switch (type) {
            case MERCHANT -> {
                int emeralds = applyTradeBonus(80 + world.getRandom().nextInt(81), tradeBonusPercent);
                data.setTreasury(Math.min(data.getMaxTreasury(), data.getTreasury() + emeralds));
                notifyNearby(world, anchor, "Merchant caravan arrived: +" + emeralds + " treasury" + bonusText(tradeBonusPercent) + ".");
            }
            case SUPPLY -> {
                int food = applyTradeBonus(30 + world.getRandom().nextInt(21), tradeBonusPercent);
                int supplies = applyTradeBonus(15 + world.getRandom().nextInt(16), tradeBonusPercent);
                data.setFoodReserves(data.getFoodReserves() + food);
                data.setTreasury(Math.min(data.getMaxTreasury(), data.getTreasury() + supplies));
                List<ItemStack> goods = new ArrayList<>();
                goods.add(new ItemStack(Items.BREAD, applyTradeBonus(12 + world.getRandom().nextInt(13), tradeBonusPercent)));
                goods.add(new ItemStack(Items.COAL, applyTradeBonus(16 + world.getRandom().nextInt(17), tradeBonusPercent)));
                goods.add(new ItemStack(Items.IRON_INGOT, applyTradeBonus(4 + world.getRandom().nextInt(5), tradeBonusPercent)));
                List<ItemStack> overflow = TownStorageDepositSystem.deposit(
                        world,
                        anchor,
                        goods
                );
                if (!overflow.isEmpty()) {
                    dropOverflow(world, anchor, overflow);
                }
                notifyNearby(world, anchor, "Supply caravan arrived: +" + food + " food, +" + supplies + " supplies and bonus goods" + bonusText(tradeBonusPercent) + ".");
            }
            case MILITARY -> {
                data.setCaravanDefenseBonus(applyTradeBonus(18, tradeBonusPercent));
                data.setCaravanDefenseUntilDay(day + 3);
                notifyNearby(world, anchor, "Military caravan arrived: temporary defense boost" + bonusText(tradeBonusPercent) + ".");
            }
            case IMMIGRANT -> {
                data.setCaravanPopulationBonus(applyTradeBonus(6, tradeBonusPercent));
                data.setCaravanPopulationUntilDay(day + 7);
                notifyNearby(world, anchor, "Immigrant caravan arrived: new families joined the town" + bonusText(tradeBonusPercent) + ".");
            }
            default -> {
            }
        }
    }

    private static int getTradingPostCaravanBonus(TownSavedData data) {
        if (data.isTradingPostLinked()) {
            return 50;
        }
        if (data.getDetectedTradingPostBuildings() > 0) {
            return 25;
        }
        return 0;
    }

    private static int applyTradeBonus(int amount, int bonusPercent) {
        return Math.max(0, amount * (100 + Math.max(0, bonusPercent)) / 100);
    }

    private static String bonusText(int bonusPercent) {
        return bonusPercent > 0 ? " (trading post +" + bonusPercent + "%)" : "";
    }

    private static void spawnCaravan(ServerWorld world, BlockPos anchor, String type) {
        int dx = world.getRandom().nextBetween(8, 14) * (world.getRandom().nextBoolean() ? 1 : -1);
        int dz = world.getRandom().nextBetween(8, 14) * (world.getRandom().nextBoolean() ? 1 : -1);
        BlockPos spawn = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, anchor.add(dx, 0, dz));

        WanderingTraderEntity trader = EntityType.WANDERING_TRADER.create(world);
        if (trader != null) {
            trader.refreshPositionAndAngles(spawn, world.random.nextFloat() * 360.0F, 0.0F);
            trader.setCustomName(Text.literal(type.substring(0, 1).toUpperCase() + type.substring(1) + " Caravan"));
            world.spawnEntity(trader);
        }

        for (int i = 0; i < 2; i++) {
            TraderLlamaEntity llama = EntityType.TRADER_LLAMA.create(world);
            if (llama == null) continue;
            BlockPos at = spawn.add(i + 1, 0, i % 2 == 0 ? 1 : -1);
            llama.refreshPositionAndAngles(at, world.random.nextFloat() * 360.0F, 0.0F);
            world.spawnEntity(llama);
        }
    }

    private static void notifyNearby(ServerWorld world, BlockPos anchor, String msg) {
        for (var player : world.getPlayers()) {
            if (player.squaredDistanceTo(anchor.getX() + 0.5D, anchor.getY() + 0.5D, anchor.getZ() + 0.5D) <= 256.0D * 256.0D) {
                player.sendMessage(Text.literal("[MCA Towns] " + msg), false);
            }
        }
    }

    private static void dropOverflow(ServerWorld world, BlockPos anchor, List<ItemStack> overflow) {
        for (ItemStack stack : overflow) {
            if (stack.isEmpty()) continue;
            ItemEntity item = new ItemEntity(
                    world,
                    anchor.getX() + 0.5D,
                    anchor.getY() + 1.0D,
                    anchor.getZ() + 0.5D,
                    stack.copy()
            );
            world.spawnEntity(item);
        }
        notifyNearby(world, anchor, "Treasury storage full: overflow goods dropped near town center.");
    }
}
