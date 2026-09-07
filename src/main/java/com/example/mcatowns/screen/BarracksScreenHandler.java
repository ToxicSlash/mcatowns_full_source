package com.example.mcatowns.screen;

import com.example.mcatowns.blockentity.BarracksBlockEntity;
import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.network.ModNetworking;
import com.example.mcatowns.registry.ModBlocks;
import com.example.mcatowns.registry.ModScreenHandlers;
import com.example.mcatowns.town.BarracksHireService;
import com.example.mcatowns.town.TownDefenceInfrastructure;
import com.example.mcatowns.town.TownGuardRosterSavedData;
import com.example.mcatowns.town.TownManager;
import com.example.mcatowns.town.TownSavedData;
import com.example.mcatowns.util.InventoryHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class BarracksScreenHandler extends ScreenHandler {
    private static final int COST = 0;
    private static final int PLAYER_CURRENCY = 1;
    private static final int GUARDS = 2;
    private static final int GUARD_CAPACITY = 3;
    private static final int POPULATION = 4;
    private static final int POPULATION_CAPACITY = 5;
    private static final int REMAINING_SECONDS = 6;
    private static final int SEARCHING = 7;

    private final BlockPos pos;
    private final PropertyDelegate properties;
    private final PlayerEntity player;
    private String townId;

    public BarracksScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, buf.readBlockPos(), new ArrayPropertyDelegate(8));
    }

    public BarracksScreenHandler(int syncId, PlayerInventory inventory, BlockPos pos, PropertyDelegate properties) {
        super(ModScreenHandlers.BARRACKS, syncId);
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
                townId = TownManager.findExistingTown(world, pos, Math.max(64, TownManager.getTownSearchMargin()))
                        .map(context -> context.townId()).orElse("");
            }

            properties.set(COST, MCATownsConfig.get().guardRecruitmentCost);
            properties.set(PLAYER_CURRENCY, InventoryHelper.count(serverPlayer.getInventory(), currencyItem()));
            if (townId.isBlank()) {
                properties.set(GUARDS, 0);
                properties.set(GUARD_CAPACITY, 0);
                properties.set(POPULATION, 0);
                properties.set(POPULATION_CAPACITY, 0);
            } else {
                TownSavedData data = TownSavedData.get(world, townId);
                properties.set(GUARDS, TownGuardRosterSavedData.get(world).count(townId));
                properties.set(GUARD_CAPACITY, TownDefenceInfrastructure.guardCapacity(data));
                properties.set(POPULATION, data.getResidents().size());
                properties.set(POPULATION_CAPACITY, data.getPopulationCapacity());
            }

            if (world.getBlockEntity(pos) instanceof BarracksBlockEntity barracks) {
                properties.set(REMAINING_SECONDS, BarracksHireService.remainingSeconds(world, barracks));
                properties.set(SEARCHING, barracks.isSearching() ? 1 : 0);
            } else {
                properties.set(REMAINING_SECONDS, 0);
                properties.set(SEARCHING, 0);
            }
        }
        super.sendContentUpdates();
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (player.getWorld() == null || !player.getWorld().getBlockState(pos).isOf(ModBlocks.BARRACKS)) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    public BlockPos getPos() { return pos; }
    public int getCost() { return properties.get(COST); }
    public int getPlayerCurrency() { return properties.get(PLAYER_CURRENCY); }
    public int getGuards() { return properties.get(GUARDS); }
    public int getGuardCapacity() { return properties.get(GUARD_CAPACITY); }
    public int getPopulation() { return properties.get(POPULATION); }
    public int getPopulationCapacity() { return properties.get(POPULATION_CAPACITY); }
    public int getRemainingSeconds() { return properties.get(REMAINING_SECONDS); }
    public boolean isSearching() { return properties.get(SEARCHING) != 0; }
    public void sendSearchForHires() { ModNetworking.sendSearchGuardHire(pos); }

    private static Item currencyItem() {
        Identifier configured = Identifier.tryParse(MCATownsConfig.get().currencyItemId);
        Identifier fallback = new Identifier("minecraft", "emerald");
        return configured != null && Registries.ITEM.containsId(configured)
                ? Registries.ITEM.get(configured) : Registries.ITEM.get(fallback);
    }
}
