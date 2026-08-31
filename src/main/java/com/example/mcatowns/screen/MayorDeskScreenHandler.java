package com.example.mcatowns.screen;

import com.example.mcatowns.blockentity.MayorDeskPropertyDelegate;
import com.example.mcatowns.network.ModNetworking;
import com.example.mcatowns.registry.ModBlocks;
import com.example.mcatowns.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

public class MayorDeskScreenHandler extends ScreenHandler {
    private final BlockPos pos;
    private final PropertyDelegate properties;

    public MayorDeskScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, buf.readBlockPos(), new MayorDeskPropertyDelegate());
    }

    public MayorDeskScreenHandler(int syncId, PlayerInventory inventory, BlockPos pos, PropertyDelegate properties) {
        super(ModScreenHandlers.MAYOR_DESK, syncId);
        this.pos = pos;
        this.properties = properties;
        addProperties(properties);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (player.getWorld() == null) return false;
        if (!player.getWorld().getBlockState(pos).isOf(ModBlocks.MAYOR_DESK)) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getPopulation() { return properties.get(0); }
    public int getHappiness() { return properties.get(1); }
    public int getFood() { return properties.get(2); }
    public int getDefense() { return properties.get(3); }
    public int getImmigration() { return properties.get(4); }
    public int getTreasury() { return properties.get(5); }
    public int getWeeklyTax() { return properties.get(6); }
    public int getUnrest() { return properties.get(7); }
    public int getTaxRate() { return properties.get(8); }
    public int getTreasuryCap() { return properties.get(9); }
    public int getBarracksLevel() { return properties.get(10); }
    public int getBonusSourceFlags() { return properties.get(11); }
    public boolean canFestival() { return properties.get(12) > 0; }
    public boolean canFoodRelief() { return properties.get(13) > 0; }
    public boolean canBarracksUpgrade() { return properties.get(14) > 0; }
    public boolean hasBarracksBuilding() { return properties.get(15) > 0; }
    public int getMcaNormalTax() { return properties.get(16); }
    public int getMcaWeightedTax() { return properties.get(17); }
    public int getAddonTaxBase() { return properties.get(18); }
    public int getNextBarracksCost() { return properties.get(19); }
    public int getMcaTaxPercent() { return properties.get(20); }
    public int getWeeklyAddonTax() { return properties.get(21); }
    public int getWeeklyMcaTax() { return properties.get(22); }
    public int getJobless() { return properties.get(23); }
    public int getDailyFoodConsumed() { return properties.get(24); }
    public int getDailyFoodProduced() { return properties.get(25); }
    public int getWorkforceAvailable() { return properties.get(26); }
    public int getWorkforceRequired() { return properties.get(27); }
    public int getWorkforceEfficiency() { return properties.get(28); }
    public int getDailyFoodPotential() { return properties.get(29); }
    public int getCropFarms() { return properties.get(30); }
    public int getActiveBakeries() { return properties.get(31); }
    public int getInactiveBakeries() { return properties.get(32); }
    public int getButchers() { return properties.get(33); }
    public int getFishermansHuts() { return properties.get(34); }
    public int getInvalidCropFarms() { return properties.get(35); }
    public int getFarmOutputPercent() { return properties.get(36); }

    public void sendSetTaxRate(int rate) {
        ModNetworking.sendSetTaxRate(pos, rate);
    }

    public void sendFestival() {
        ModNetworking.sendFestival(pos);
    }

    public void sendEmergencyFoodRelief() {
        ModNetworking.sendEmergencyFoodRelief(pos);
    }

    public void sendBarracksUpgrade() {
        ModNetworking.sendBarracksUpgrade(pos);
    }
}
