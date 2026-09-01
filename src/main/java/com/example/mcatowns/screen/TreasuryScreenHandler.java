package com.example.mcatowns.screen;

import com.example.mcatowns.network.ModNetworking;
import com.example.mcatowns.registry.ModBlocks;
import com.example.mcatowns.registry.ModScreenHandlers;
import com.example.mcatowns.town.TownManager;
import com.example.mcatowns.town.TownSavedData;
import com.example.mcatowns.util.InventoryHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class TreasuryScreenHandler extends ScreenHandler {
    private static final int TREASURY = 0;
    private static final int CAP = 1;
    private static final int PLAYER_EMERALDS = 2;

    private final BlockPos pos;
    private final PropertyDelegate properties;
    private final PlayerEntity player;
    private String townId;

    public TreasuryScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, buf.readBlockPos(), new ArrayPropertyDelegate(3));
    }

    public TreasuryScreenHandler(int syncId, PlayerInventory inventory, BlockPos pos, PropertyDelegate properties) {
        super(ModScreenHandlers.TREASURY, syncId);
        this.pos = pos;
        this.properties = properties;
        this.player = inventory.player;
        addProperties(properties);
    }

    @Override
    public void sendContentUpdates() {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            var world = serverPlayer.getServerWorld();
            if (townId == null) {
                townId = TownManager.findExistingTown(world, pos, TownManager.getTownSearchMargin())
                        .map(context -> context.townId()).orElse("");
            }
            if (townId.isBlank()) {
                properties.set(TREASURY, 0);
                properties.set(CAP, 0);
            } else {
                TownSavedData data = TownSavedData.get(world, townId);
                properties.set(TREASURY, data.getTreasury());
                properties.set(CAP, data.getMaxTreasury());
            }
            properties.set(PLAYER_EMERALDS, InventoryHelper.count(serverPlayer.getInventory(), Items.EMERALD));
        }
        super.sendContentUpdates();
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (player.getWorld() == null) return false;
        if (!player.getWorld().getBlockState(pos).isOf(ModBlocks.TREASURY)) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    public BlockPos getPos() { return pos; }
    public int getTreasury() { return properties.get(TREASURY); }
    public int getCap() { return properties.get(CAP); }
    public int getPlayerEmeralds() { return properties.get(PLAYER_EMERALDS); }
    public void sendDeposit() { ModNetworking.sendTreasuryDeposit(pos); }
    public void sendRetrieve() { ModNetworking.sendTreasuryRetrieve(pos); }
}
