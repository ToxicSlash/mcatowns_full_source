package com.example.mcatowns.screen;

import com.example.mcatowns.blockentity.StorehouseBlockEntity;
import com.example.mcatowns.network.ModNetworking;
import com.example.mcatowns.registry.ModBlocks;
import com.example.mcatowns.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public class StorehouseScreenHandler extends ScreenHandler {
    private static final int TOWN_SLOTS = 27;
    private final Inventory inventory;
    private final BlockPos pos;

    public StorehouseScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, readClientData(playerInventory, buf));
    }

    private StorehouseScreenHandler(int syncId, PlayerInventory playerInventory, ClientData data) {
        this(syncId, playerInventory, data.inventory(), data.pos());
    }

    private static ClientData readClientData(PlayerInventory playerInventory, PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Inventory inventory = playerInventory.player.getWorld().getBlockEntity(pos) instanceof StorehouseBlockEntity storehouse
                ? storehouse : new SimpleInventory(TOWN_SLOTS);
        return new ClientData(inventory, pos);
    }

    public StorehouseScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, BlockPos pos) {
        super(ModScreenHandlers.STOREHOUSE, syncId);
        this.inventory = inventory;
        this.pos = pos;
        checkSize(inventory, TOWN_SLOTS);
        inventory.onOpen(playerInventory.player);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 85 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, 143));
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.getWorld().getBlockState(pos).isOf(ModBlocks.STOREHOUSE) && inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack original = slot.getStack();
        ItemStack copy = original.copy();
        if (slotIndex < TOWN_SLOTS ? !insertItem(original, TOWN_SLOTS, slots.size(), true)
                : !insertItem(original, 0, TOWN_SLOTS, false)) return ItemStack.EMPTY;
        if (original.isEmpty()) slot.setStack(ItemStack.EMPTY); else slot.markDirty();
        return copy;
    }

    @Override
    public void onClosed(PlayerEntity player) { super.onClosed(player); inventory.onClose(player); }
    public BlockPos getPos() { return pos; }
    public void sendDepositFood() { ModNetworking.sendStorehouseFood(pos); }

    private record ClientData(Inventory inventory, BlockPos pos) { }
}
