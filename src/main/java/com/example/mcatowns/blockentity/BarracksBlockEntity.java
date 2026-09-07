package com.example.mcatowns.blockentity;

import com.example.mcatowns.registry.ModBlockEntities;
import com.example.mcatowns.screen.BarracksScreenHandler;
import com.example.mcatowns.town.BarracksHireService;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BarracksBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {
    private long hireReadyTick = -1L;
    private UUID hiringPlayer;
    private String hiringTownId = "";
    private String chargedCurrencyId = "minecraft:emerald";
    private int chargedCost;

    public BarracksBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARRACKS, pos, state);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(getPos());
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Barracks");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new BarracksScreenHandler(syncId, playerInventory, getPos(), new ArrayPropertyDelegate(8));
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, BarracksBlockEntity barracks) {
        BarracksHireService.completeSearch(world, pos, barracks);
    }

    public void startSearch(long readyTick, UUID playerId, String townId, String currencyId, int cost) {
        this.hireReadyTick = Math.max(0L, readyTick);
        this.hiringPlayer = playerId;
        this.hiringTownId = townId == null ? "" : townId;
        this.chargedCurrencyId = currencyId == null || currencyId.isBlank() ? "minecraft:emerald" : currencyId;
        this.chargedCost = Math.max(0, cost);
        markDirty();
    }

    public void clearSearch() {
        hireReadyTick = -1L;
        hiringPlayer = null;
        hiringTownId = "";
        chargedCurrencyId = "minecraft:emerald";
        chargedCost = 0;
        markDirty();
    }

    public boolean isSearching() { return hireReadyTick >= 0L && hiringPlayer != null && !hiringTownId.isBlank(); }
    public long getHireReadyTick() { return hireReadyTick; }
    public UUID getHiringPlayer() { return hiringPlayer; }
    public String getHiringTownId() { return hiringTownId; }
    public String getChargedCurrencyId() { return chargedCurrencyId; }
    public int getChargedCost() { return chargedCost; }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("HireReadyTick", hireReadyTick);
        if (hiringPlayer != null) nbt.putUuid("HiringPlayer", hiringPlayer);
        if (!hiringTownId.isBlank()) nbt.putString("HiringTownId", hiringTownId);
        nbt.putString("ChargedCurrencyId", chargedCurrencyId);
        nbt.putInt("ChargedCost", chargedCost);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        hireReadyTick = nbt.contains("HireReadyTick") ? nbt.getLong("HireReadyTick") : -1L;
        hiringPlayer = nbt.containsUuid("HiringPlayer") ? nbt.getUuid("HiringPlayer") : null;
        hiringTownId = nbt.getString("HiringTownId");
        chargedCurrencyId = nbt.getString("ChargedCurrencyId");
        if (chargedCurrencyId.isBlank()) chargedCurrencyId = "minecraft:emerald";
        chargedCost = Math.max(0, nbt.getInt("ChargedCost"));
    }
}
