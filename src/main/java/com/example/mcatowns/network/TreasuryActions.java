package com.example.mcatowns.network;

import com.example.mcatowns.town.TownContext;
import com.example.mcatowns.town.TownManager;
import com.example.mcatowns.town.TownSavedData;
import com.example.mcatowns.util.InventoryHelper;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class TreasuryActions {
    private TreasuryActions() { }

    public static void deposit(ServerPlayerEntity player, BlockPos pos) {
        TownContext context = findTown(player, pos);
        if (context == null) return;
        TownSavedData data = TownSavedData.get(player.getServerWorld(), context.townId());

        int capacity = Math.max(0, data.getMaxTreasury() - data.getTreasury());
        if (capacity <= 0) {
            player.sendMessage(Text.translatable("text.mcatowns.treasury_full"), true);
            return;
        }

        int emeraldsInInventory = InventoryHelper.count(player.getInventory(), Items.EMERALD);
        if (emeraldsInInventory <= 0) {
            player.sendMessage(Text.translatable("text.mcatowns.no_emeralds"), true);
            return;
        }

        int toDeposit = Math.min(capacity, emeraldsInInventory);
        int removed = InventoryHelper.remove(player.getInventory(), Items.EMERALD, toDeposit);
        if (removed <= 0) {
            player.sendMessage(Text.translatable("text.mcatowns.no_emeralds"), true);
            return;
        }
        data.setTreasury(data.getTreasury() + removed);
        player.sendMessage(Text.translatable("text.mcatowns.treasury_deposited", removed), true);
    }

    public static void retrieve(ServerPlayerEntity player, BlockPos pos) {
        TownContext context = findTown(player, pos);
        if (context == null) return;
        if (!TownManager.hasMayorAuthority(player, context)) {
            player.sendMessage(Text.translatable("text.mcatowns.not_mayor"), true);
            return;
        }
        TownSavedData data = TownSavedData.get(player.getServerWorld(), context.townId());

        int amount = data.getTreasury();
        if (amount <= 0) {
            player.sendMessage(Text.translatable("text.mcatowns.no_funds"), true);
            return;
        }

        int given = InventoryHelper.give(player, Items.EMERALD, amount);
        if (given <= 0) {
            player.sendMessage(Text.translatable("text.mcatowns.inventory_full"), true);
            return;
        }
        data.setTreasury(amount - given);
        player.sendMessage(Text.translatable("text.mcatowns.treasury_retrieved", given), true);
    }

    private static TownContext findTown(ServerPlayerEntity player, BlockPos pos) {
        TownContext context = TownManager.findExistingTown(player.getServerWorld(), pos, TownManager.getTownSearchMargin()).orElse(null);
        if (context == null) player.sendMessage(Text.translatable("text.mcatowns.no_town_center"), true);
        return context;
    }
}
