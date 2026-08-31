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

public class SiloScreenHandler extends ScreenHandler {
    private static final int FOOD = 0;
    private static final int PLAYER_BREAD = 1;
    private static final int SILO_BREAD_EQ = 2;

    private final BlockPos pos;
    private final PropertyDelegate properties;
    private final PlayerEntity player;
    private String townId;

    public SiloScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, buf.readBlockPos(), new ArrayPropertyDelegate(3));
    }

    public SiloScreenHandler(int syncId, PlayerInventory inventory, BlockPos pos, PropertyDelegate properties) {
        super(ModScreenHandlers.SILO, syncId);
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
                townId = TownManager.resolveTownContext(world, pos).townId();
            }
            TownSavedData data = TownSavedData.get(world, townId);
            properties.set(FOOD, data.getFoodReserves());
            properties.set(PLAYER_BREAD, InventoryHelper.count(serverPlayer.getInventory(), Items.BREAD));
            properties.set(SILO_BREAD_EQ, Math.max(0, data.getFoodReserves()));
        }
        super.sendContentUpdates();
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (player.getWorld() == null) return false;
        if (!player.getWorld().getBlockState(pos).isOf(ModBlocks.SILO)) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getFood() {
        return properties.get(FOOD);
    }

    public int getPlayerBread() {
        return properties.get(PLAYER_BREAD);
    }

    public int getSiloBreadEquivalent() {
        return properties.get(SILO_BREAD_EQ);
    }

    public void sendDeposit() {
        ModNetworking.sendSiloDeposit(pos);
    }

}
