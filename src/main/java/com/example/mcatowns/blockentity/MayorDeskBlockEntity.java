package com.example.mcatowns.blockentity;

import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.registry.ModBlockEntities;
import com.example.mcatowns.screen.MayorDeskScreenHandler;
import com.example.mcatowns.town.TownContext;
import com.example.mcatowns.town.TownFestivalSystem;
import com.example.mcatowns.town.TownManager;
import com.example.mcatowns.town.TownSavedData;
import com.example.mcatowns.town.TownStatsRefresher;
import com.example.mcatowns.town.TownUpgradeSystem;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class MayorDeskBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {
    private static final long BINDING_RECHECK_INTERVAL_TICKS = 200L;

    private final PropertyDelegate properties = new MayorDeskPropertyDelegate();
    private String boundTownId = "";
    private long lastBindingCheckTick = -1L;
    private TownContext cachedContext;

    public MayorDeskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAYOR_DESK, pos, state);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(getPos());
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("screen.mcatowns.mayor_desk");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        if (world instanceof ServerWorld serverWorld) {
            TownContext context = resolveBoundContext(serverWorld, true);
            if ("mca_unbound".equals(context.source())) {
                player.sendMessage(Text.translatable("text.mcatowns.no_town_center"), true);
                player.sendMessage(Text.literal("[MCA Towns Debug] " + MCAIntegration.getLinkDebug(
                        serverWorld, getPos(), MCATownsConfig.get().mcaVillageSearchMargin)), false);
                return null;
            }
            // Expensive scans happen on demand when the desk is opened; background town ticks handle routine refreshes.
            syncProperties(serverWorld, context, true);
            if (player instanceof ServerPlayerEntity serverPlayer && !TownManager.hasMayorAuthority(serverPlayer, context)) {
                player.sendMessage(Text.translatable("text.mcatowns.not_mayor"), true);
                return null;
            }
        } else if (world != null && !MCAIntegration.hasMayorRank(player)) {
            player.sendMessage(Text.translatable("text.mcatowns.not_mayor"), true);
            return null;
        }
        return new MayorDeskScreenHandler(syncId, playerInventory, getPos(), properties);
    }

    public static void tick(net.minecraft.world.World world, BlockPos pos, BlockState state, MayorDeskBlockEntity blockEntity) {
        if (!(world instanceof ServerWorld serverWorld) || serverWorld.getTime() % 20 != 0) return;
        TownContext context = blockEntity.resolveBoundContext(serverWorld, false);
        if ("mca_unbound".equals(context.source())) {
            blockEntity.clearPropertiesForUnboundTown();
            return;
        }
        blockEntity.syncProperties(serverWorld, context, false);
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("BoundTownId", boundTownId);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        boundTownId = nbt.getString("BoundTownId");
        cachedContext = null;
        lastBindingCheckTick = -1L;
    }

    private TownContext resolveBoundContext(ServerWorld world, boolean forceRecheck) {
        long now = world.getTime();
        if (!forceRecheck && cachedContext != null && lastBindingCheckTick >= 0
                && now - lastBindingCheckTick < BINDING_RECHECK_INTERVAL_TICKS) {
            return cachedContext;
        }
        lastBindingCheckTick = now;

        TownContext resolved = null;
        if (!boundTownId.isEmpty()) {
            resolved = com.example.mcatowns.town.PlayerTownRegistry.get(world).getTownById(boundTownId).orElse(null);
            if (resolved == null && MCAIntegration.isMcaLoaded()) {
                resolved = MCAIntegration.getTownById(world, boundTownId).orElse(null);
            }
        }
        if (resolved == null) {
            TownContext nearest = TownManager.resolveTownContext(world, getPos());
            if (nearest.validMcaTown() || "player_created".equals(nearest.source()) || !MCAIntegration.isMcaLoaded()) {
                resolved = nearest;
            }
        }
        if (resolved == null || MCAIntegration.isMcaLoaded() && "fallback".equals(resolved.source())) {
            resolved = new TownContext("unbound", getPos(), getPos(), "mca_unbound", false);
        } else if (!resolved.townId().equals(boundTownId)) {
            boundTownId = resolved.townId();
            markDirty();
        }
        cachedContext = resolved;
        return resolved;
    }

    public TownContext getGovernedTownContext(ServerWorld world) {
        return resolveBoundContext(world, true);
    }

    private void clearPropertiesForUnboundTown() {
        for (int i = 0; i < properties.size(); i++) properties.set(i, 0);
        properties.set(MayorDeskPropertyDelegate.MCA_TAX_PERCENT, MCATownsConfig.get().mcaTaxContributionPercent);
        properties.set(MayorDeskPropertyDelegate.NEXT_BARRACKS_COST, -1);
    }

    private void syncProperties(ServerWorld world, TownContext context, boolean refreshStats) {
        TownSavedData data = TownSavedData.get(world, context.townId());
        if (refreshStats) TownStatsRefresher.refresh(world, context.center(), data);

        properties.set(MayorDeskPropertyDelegate.POPULATION, data.getPopulation() + data.getRefugeePopulationBonus() + data.getCaravanPopulationBonus());
        properties.set(MayorDeskPropertyDelegate.HAPPINESS, data.getHappiness());
        properties.set(MayorDeskPropertyDelegate.FOOD, data.getFoodReserves());
        properties.set(MayorDeskPropertyDelegate.DEFENSE, data.getDefenseRating());
        properties.set(MayorDeskPropertyDelegate.IMMIGRATION, data.getImmigrationChance());
        properties.set(MayorDeskPropertyDelegate.TREASURY, data.getTreasury());
        properties.set(MayorDeskPropertyDelegate.WEEKLY_TAX, data.getWeeklyTaxIncome());
        properties.set(MayorDeskPropertyDelegate.UNREST, data.getUnrest());
        properties.set(MayorDeskPropertyDelegate.TAX_RATE, data.getTaxRate());
        properties.set(MayorDeskPropertyDelegate.TREASURY_CAP, data.getMaxTreasury());
        properties.set(MayorDeskPropertyDelegate.BARRACKS_LEVEL, data.getBarracksLevel());
        properties.set(MayorDeskPropertyDelegate.BONUS_SOURCES, data.getBonusSourceFlags());
        long day = TownManager.getDay(world);
        properties.set(MayorDeskPropertyDelegate.CAN_FESTIVAL,
                data.getTreasury() >= TownFestivalSystem.FESTIVAL_COST_EMERALDS
                        && data.getFoodReserves() >= TownFestivalSystem.FESTIVAL_COST_FOOD
                        && TownFestivalSystem.canHoldFestival(data, day) ? 1 : 0);
        properties.set(MayorDeskPropertyDelegate.CAN_FOOD_RELIEF,
                data.getTreasury() >= TownFestivalSystem.FOOD_RELIEF_COST ? 1 : 0);
        properties.set(MayorDeskPropertyDelegate.CAN_BARRACKS_UPGRADE,
                TownUpgradeSystem.canBuyNextBarracksUpgrade(data) ? 1 : 0);
        properties.set(MayorDeskPropertyDelegate.HAS_BARRACKS,
                data.getDetectedBarracksBuildings() > 0 ? 1 : 0);
        properties.set(MayorDeskPropertyDelegate.MCA_NORMAL_TAX, data.getMcaNormalTaxIncome());
        properties.set(MayorDeskPropertyDelegate.MCA_WEIGHTED_TAX, data.getWeightedMcaTaxIncome());
        properties.set(MayorDeskPropertyDelegate.ADDON_TAX_BASE, data.getAddonTaxIncomeBase());
        properties.set(MayorDeskPropertyDelegate.NEXT_BARRACKS_COST, TownUpgradeSystem.getNextBarracksCost(data.getBarracksLevel()));
        properties.set(MayorDeskPropertyDelegate.MCA_TAX_PERCENT, MCATownsConfig.get().mcaTaxContributionPercent);
        properties.set(MayorDeskPropertyDelegate.WEEKLY_ADDON_TAX, data.getWeeklyAddonTaxContribution());
        properties.set(MayorDeskPropertyDelegate.WEEKLY_MCA_TAX, data.getWeeklyMcaTaxContribution());
        properties.set(MayorDeskPropertyDelegate.JOBLESS, data.getJobless());
        properties.set(MayorDeskPropertyDelegate.DAILY_FOOD_CONSUMED, data.getDailyFoodConsumed());
        properties.set(MayorDeskPropertyDelegate.DAILY_FOOD_PRODUCED, data.getDailyFoodProduced());
        properties.set(MayorDeskPropertyDelegate.WORKFORCE_AVAILABLE, data.getWorkforceAvailable());
        properties.set(MayorDeskPropertyDelegate.WORKFORCE_REQUIRED, data.getWorkforceRequired());
        properties.set(MayorDeskPropertyDelegate.WORKFORCE_EFFICIENCY, data.getWorkforceEfficiencyPercent());
        properties.set(MayorDeskPropertyDelegate.DAILY_FOOD_POTENTIAL, data.getDailyFoodPotential());
        properties.set(MayorDeskPropertyDelegate.CROP_FARMS, data.getDetectedCropFarmBuildings());
        properties.set(MayorDeskPropertyDelegate.ACTIVE_BAKERIES, data.getActiveBakeryBuildings());
        properties.set(MayorDeskPropertyDelegate.INACTIVE_BAKERIES, data.getInactiveBakeryBuildings());
        properties.set(MayorDeskPropertyDelegate.BUTCHERS, data.getDetectedButcherBuildings());
        properties.set(MayorDeskPropertyDelegate.FISHERMANS_HUTS, data.getDetectedFishermansHutBuildings());
        properties.set(MayorDeskPropertyDelegate.INVALID_CROP_FARMS, data.getInvalidCropFarmBuildings());
        properties.set(MayorDeskPropertyDelegate.FARM_OUTPUT_PERCENT, data.getEventFarmOutputPercent());
    }
}
