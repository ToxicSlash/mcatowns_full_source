package com.example.mcatowns.town;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.TraderLlamaEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

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
    private static final String SETTLER = "settler";

    private TownCaravanSystem() { }

    public static void tickDaily(ServerWorld world, TownContext context, TownSavedData data, long day) {
        expireLegacyBonuses(data, day);

        if (data.getNextCaravanDay() < 0) {
            data.setNextCaravanDay(day + CARAVAN_INTERVAL_DAYS);
            return;
        }
        if (day < data.getNextCaravanDay()) return;

        String type = pickType(world);
        spawnWanderingCaravan(world, context.center(), type);
        data.setLastCaravanType(type);
        data.setLastCaravanDay(day);
        data.setNextCaravanDay(day + CARAVAN_INTERVAL_DAYS);
        notifyNearby(world, context.center(), "A " + type + " wandering caravan has arrived and will remain for a few days.");
    }

    /** Clears temporary bonuses left by saves from the old caravan implementation. New caravans do not create them. */
    private static void expireLegacyBonuses(TownSavedData data, long day) {
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
            default -> SETTLER;
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
            // Vanilla Wandering Trader behaviour remains the base. Theme-specific trade pools are added once
            // the exact item/economy pools are approved, instead of hard-coding speculative balance here.
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
