package com.example.mcatowns.town;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.TraderLlamaEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Legacy class name retained for save/code compatibility. This is the random physical Wandering Caravan system,
 * not simulated town-to-town trade.
 */
public final class TownCaravanSystem {
    private static final int CARAVAN_INTERVAL_DAYS = 7;
    private static final int DESPAWN_DELAY_TICKS = 3 * 24_000;

    private static final String MERCHANT = "merchant";
    private static final String SUPPLY = "supply";
    private static final String MILITARY = "military";
    private static final String IMMIGRANT = "immigrant";

    private TownCaravanSystem() { }

    public static void tickDaily(ServerWorld world, TownContext context, TownSavedData data, long day) {
        expireBonuses(data, day);

        if (data.getNextCaravanDay() < 0) {
            data.setNextCaravanDay(day + CARAVAN_INTERVAL_DAYS);
            return;
        }
        if (day < data.getNextCaravanDay()) return;

        String type = pickType(world);
        spawnWanderingCaravan(world, context.center(), type);
        applyArrivalBonus(world, context.center(), data, day, type);
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
        return switch (world.getRandom().nextInt(4)) {
            case 0 -> MERCHANT;
            case 1 -> SUPPLY;
            case 2 -> MILITARY;
            default -> IMMIGRANT;
        };
    }

    private static void spawnWanderingCaravan(ServerWorld world, BlockPos anchor, String type) {
        int dx = world.getRandom().nextBetween(8, 14) * (world.getRandom().nextBoolean() ? 1 : -1);
        int dz = world.getRandom().nextBetween(8, 14) * (world.getRandom().nextBoolean() ? 1 : -1);
        BlockPos spawn = world.getTopPosition(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, anchor.add(dx, 0, dz));

        WanderingTraderEntity trader = EntityType.WANDERING_TRADER.create(world);
        if (trader != null) {
            trader.refreshPositionAndAngles(spawn, world.random.nextFloat() * 360.0F, 0.0F);
            trader.setCustomName(Text.literal(displayType(type) + " Caravan Merchant"));
            trader.setDespawnDelay(DESPAWN_DELAY_TICKS);
            world.spawnEntity(trader);
            // Keep vanilla Wandering Trader behaviour/offers as the base. Theme-specific trade pools are layered later
            // once their exact economy balance is approved.
        }

        for (int i = 0; i < 2; i++) {
            TraderLlamaEntity llama = EntityType.TRADER_LLAMA.create(world);
            if (llama == null) continue;
            BlockPos at = spawn.add(i + 1, 0, i % 2 == 0 ? 1 : -1);
            llama.refreshPositionAndAngles(at, world.random.nextFloat() * 360.0F, 0.0F);
            llama.setDespawnDelay(DESPAWN_DELAY_TICKS);
            world.spawnEntity(llama);
        }
    }

    private static void applyArrivalBonus(ServerWorld world, BlockPos anchor, TownSavedData data, long day, String type) {
        int tradeBonusPercent = getTradingPostCaravanBonus(data);
        switch (type) {
            case MERCHANT -> {
                int emeralds = applyTradeBonus(8 + world.getRandom().nextInt(9), tradeBonusPercent);
                data.setTreasury(Math.min(data.getMaxTreasury(), data.getTreasury() + emeralds));
                notifyNearby(world, anchor, "A merchant wandering caravan has arrived." + bonusText(tradeBonusPercent));
            }
            case SUPPLY -> {
                int food = applyTradeBonus(10 + world.getRandom().nextInt(11), tradeBonusPercent);
                data.setFoodReserves(data.getFoodReserves() + food);
                notifyNearby(world, anchor, "A supply wandering caravan has arrived with a small town delivery." + bonusText(tradeBonusPercent));
            }
            case MILITARY -> {
                data.setCaravanDefenseBonus(applyTradeBonus(8, tradeBonusPercent));
                data.setCaravanDefenseUntilDay(day + 3);
                notifyNearby(world, anchor, "A military wandering caravan has arrived; guards are encouraged by the visitors." + bonusText(tradeBonusPercent));
            }
            case IMMIGRANT -> {
                data.setCaravanPopulationBonus(applyTradeBonus(2, tradeBonusPercent));
                data.setCaravanPopulationUntilDay(day + 3);
                notifyNearby(world, anchor, "A settler wandering caravan has arrived." + bonusText(tradeBonusPercent));
            }
            default -> { }
        }
    }

    private static int getTradingPostCaravanBonus(TownSavedData data) {
        if (data.isTradingPostLinked()) return 50;
        if (data.getDetectedTradingPostBuildings() > 0) return 25;
        return 0;
    }

    private static int applyTradeBonus(int amount, int bonusPercent) {
        return Math.max(0, amount * (100 + Math.max(0, bonusPercent)) / 100);
    }

    private static String bonusText(int bonusPercent) {
        return bonusPercent > 0 ? " Trading Post bonus: +" + bonusPercent + "% arrival benefit." : "";
    }

    private static String displayType(String type) {
        if (type == null || type.isBlank()) return "Wandering";
        return Character.toUpperCase(type.charAt(0)) + type.substring(1);
    }

    private static void notifyNearby(ServerWorld world, BlockPos anchor, String msg) {
        for (var player : world.getPlayers()) {
            if (player.squaredDistanceTo(anchor.getX() + 0.5D, anchor.getY() + 0.5D, anchor.getZ() + 0.5D) <= 256.0D * 256.0D) {
                player.sendMessage(Text.literal("[MCA Towns] " + msg), false);
            }
        }
    }
}
