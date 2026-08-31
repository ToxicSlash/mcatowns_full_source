package com.example.mcatowns.blockentity;

import com.example.mcatowns.registry.ModBlockEntities;
import com.example.mcatowns.screen.StorehouseScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public class StorehouseBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory, Inventory {
    private DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);

    public StorehouseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STOREHOUSE, pos, state);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override
    public Text getDisplayName() { return Text.translatable("screen.mcatowns.storehouse"); }
    @Override
    public int size() { return items.size(); }
    @Override
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override
    public ItemStack getStack(int slot) { return items.get(slot); }
    @Override
    public ItemStack removeStack(int slot, int amount) { ItemStack stack = Inventories.splitStack(items, slot, amount); if (!stack.isEmpty()) markDirty(); return stack; }
    @Override
    public ItemStack removeStack(int slot) { ItemStack stack = Inventories.removeStack(items, slot); if (!stack.isEmpty()) markDirty(); return stack; }
    @Override
    public void setStack(int slot, ItemStack stack) { items.set(slot, stack); stack.setCount(Math.min(stack.getCount(), getMaxCountPerStack())); markDirty(); }
    @Override
    public void clear() { items.clear(); markDirty(); }
    @Override
    public boolean canPlayerUse(PlayerEntity player) { return world != null && world.getBlockEntity(pos) == this && player.squaredDistanceTo(pos.toCenterPos()) <= 64.0; }

    @Override
    protected void writeNbt(NbtCompound nbt) { super.writeNbt(nbt); Inventories.writeNbt(nbt, items); }
    @Override
    public void readNbt(NbtCompound nbt) { super.readNbt(nbt); items = DefaultedList.ofSize(27, ItemStack.EMPTY); Inventories.readNbt(nbt, items); }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new StorehouseScreenHandler(syncId, inventory, this, pos);
    }
}
