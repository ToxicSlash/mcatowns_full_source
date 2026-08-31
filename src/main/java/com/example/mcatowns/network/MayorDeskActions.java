package com.example.mcatowns.network;

import com.example.mcatowns.blockentity.MayorDeskBlockEntity;
import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.town.TownFestivalSystem;
import com.example.mcatowns.town.TownManager;
import com.example.mcatowns.town.TownSavedData;
import com.example.mcatowns.town.TownUpgradeSystem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class MayorDeskActions {
    public static void setTaxRate(ServerPlayerEntity player, BlockPos pos, int rate) {
        TownSavedData data = getAuthorizedData(player, pos);
        if (data == null) return;
        int clamped = Math.max(0, Math.min(3, rate));
        data.setTaxRate(clamped);
        player.sendMessage(Text.translatable("text.mcatowns.tax_rate_set", clamped), true);
    }

    public static void holdFestival(ServerPlayerEntity player, BlockPos pos) {
        TownSavedData data = getAuthorizedData(player, pos);
        if (data == null) return;
        long day = TownManager.getDay(player.getServerWorld());
        if (!TownFestivalSystem.canHoldFestival(data, day)) {
            player.sendMessage(Text.translatable("text.mcatowns.festival_cooldown"), true);
            return;
        }
        if (TownFestivalSystem.tryHoldFestival(data, day)) {
            player.sendMessage(Text.translatable("text.mcatowns.festival_done"), false);
        } else {
            if (data.getFoodReserves() < TownFestivalSystem.FESTIVAL_COST_FOOD) {
                player.sendMessage(Text.translatable("text.mcatowns.no_food"), true);
            } else {
                player.sendMessage(Text.translatable("text.mcatowns.no_funds"), true);
            }
        }
    }

    public static void emergencyFoodRelief(ServerPlayerEntity player, BlockPos pos) {
        TownSavedData data = getAuthorizedData(player, pos);
        if (data == null) return;
        if (TownFestivalSystem.tryEmergencyFoodRelief(data)) {
            player.sendMessage(Text.translatable("text.mcatowns.food_relief_done"), false);
        } else {
            player.sendMessage(Text.translatable("text.mcatowns.no_funds"), true);
        }
    }

    public static void buyBarracksUpgrade(ServerPlayerEntity player, BlockPos pos) {
        TownSavedData data = getAuthorizedData(player, pos);
        if (data == null) return;
        if (data.getDetectedBarracksBuildings() <= 0) {
            player.sendMessage(Text.translatable("text.mcatowns.no_barracks"), true);
            return;
        }
        if (data.getBarracksLevel() >= 3) {
            player.sendMessage(Text.translatable("text.mcatowns.barracks_max"), true);
            return;
        }
        if (!TownUpgradeSystem.buyNextBarracksUpgrade(data)) {
            player.sendMessage(Text.translatable("text.mcatowns.no_funds"), true);
            return;
        }
        player.sendMessage(Text.translatable("text.mcatowns.barracks_upgraded", data.getBarracksLevel()), false);
    }

    private static TownSavedData getAuthorizedData(ServerPlayerEntity player, BlockPos pos) {
        var context = resolveDeskContext(player, pos);
        if ("mca_unbound".equals(context.source())) {
            player.sendMessage(Text.translatable("text.mcatowns.no_town_center"), true);
            player.sendMessage(Text.literal("[MCA Towns Debug] " + MCAIntegration.getLinkDebug(
                    player.getServerWorld(), pos, MCATownsConfig.get().mcaVillageSearchMargin)), false);
            return null;
        }
        if (!TownManager.hasMayorAuthority(player, context)) {
            player.sendMessage(Text.translatable("text.mcatowns.not_mayor"), true);
            return null;
        }
        return TownSavedData.get(player.getServerWorld(), context.townId());
    }

    private static com.example.mcatowns.town.TownContext resolveDeskContext(ServerPlayerEntity player, BlockPos pos) {
        var be = player.getServerWorld().getBlockEntity(pos);
        if (be instanceof MayorDeskBlockEntity desk) {
            return desk.getGovernedTownContext(player.getServerWorld());
        }
        return TownManager.resolveTownContext(player.getServerWorld(), pos);
    }
}
