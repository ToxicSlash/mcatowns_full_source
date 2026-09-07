package com.example.mcatowns.town;

import com.example.mcatowns.blockentity.BarracksBlockEntity;
import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.integration.GuardVillagersIntegration;
import com.example.mcatowns.registry.ModBlocks;
import com.example.mcatowns.util.InventoryHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** Simple Barracks guard hiring flow. Search starts immediately and resolves after 60 seconds (instant in creative). */
public final class BarracksHireService {
    public static final long SEARCH_DURATION_TICKS = 60L * 20L;
    private static final Identifier GUARD_ENTITY_ID = new Identifier("guardvillagers", "guard");

    private BarracksHireService() { }

    public static void searchForHires(ServerPlayerEntity player, BlockPos pos) {
        ServerWorld world = player.getServerWorld();
        if (!world.isChunkLoaded(pos) || !world.getBlockState(pos).isOf(ModBlocks.BARRACKS)) return;
        if (!(world.getBlockEntity(pos) instanceof BarracksBlockEntity barracks)) return;

        TownContext context = TownManager.findExistingTown(world, pos, Math.max(64, TownManager.getTownSearchMargin())).orElse(null);
        if (context == null || !TownManager.hasMayorAuthority(player, context)) {
            player.sendMessage(Text.literal("You do not manage this town."), true);
            return;
        }
        TownSavedData data = TownSavedData.get(world, context.townId());
        if (!isRegisteredBarracks(data, pos)) {
            player.sendMessage(Text.literal("Register this Barracks with the town before hiring guards."), true);
            return;
        }
        if (barracks.isSearching()) {
            player.sendMessage(Text.literal("This Barracks is already searching for a hire."), true);
            return;
        }
        if (!Registries.ENTITY_TYPE.containsId(GUARD_ENTITY_ID)) {
            player.sendMessage(Text.literal("Guard Villagers is not available, so no guard can be hired."), true);
            return;
        }

        TownGuardRosterSavedData roster = TownGuardRosterSavedData.get(world);
        if (!TownDefenceInfrastructure.canAddGuard(data, roster.count(context.townId()))) {
            if (data.getResidents().size() >= data.getPopulationCapacity()) {
                player.sendMessage(Text.literal("The town needs more Residence capacity before hiring another guard."), true);
            } else {
                player.sendMessage(Text.literal("The town needs more Barracks Guard Capacity before hiring another guard."), true);
            }
            return;
        }

        Item currency = currencyItem();
        int cost = MCATownsConfig.get().guardRecruitmentCost;
        if (!player.getAbilities().creativeMode && InventoryHelper.count(player.getInventory(), currency) < cost) {
            player.sendMessage(Text.literal("Not enough hiring currency."), true);
            return;
        }

        if (!player.getAbilities().creativeMode && cost > 0) {
            InventoryHelper.remove(player.getInventory(), currency, cost);
        }

        if (player.getAbilities().creativeMode) {
            if (spawnGuard(world, pos, context.townId(), data)) {
                player.sendMessage(Text.literal("A guard has been hired."), false);
            } else {
                player.sendMessage(Text.literal("The guard could not be spawned."), true);
            }
            return;
        }

        barracks.startSearch(world.getTime() + SEARCH_DURATION_TICKS, player.getUuid(), context.townId(),
                Registries.ITEM.getId(currency).toString(), cost);
        player.sendMessage(Text.literal("Searching for hires... a guard will arrive in 60 seconds."), false);
    }

    public static void completeSearch(ServerWorld world, BlockPos pos, BarracksBlockEntity barracks) {
        if (!barracks.isSearching() || world.getTime() < barracks.getHireReadyTick()) return;

        UUID hiringPlayer = barracks.getHiringPlayer();
        String townId = barracks.getHiringTownId();
        String chargedCurrency = barracks.getChargedCurrencyId();
        int chargedCost = barracks.getChargedCost();
        barracks.clearSearch();

        TownContext context = PlayerTownRegistry.get(world).getTownById(townId).orElse(null);
        if (context == null) {
            refund(world, pos, hiringPlayer, chargedCurrency, chargedCost);
            return;
        }
        TownSavedData data = TownSavedData.get(world, townId);
        TownGuardRosterSavedData roster = TownGuardRosterSavedData.get(world);
        if (!isRegisteredBarracks(data, pos)
                || !TownDefenceInfrastructure.canAddGuard(data, roster.count(townId))) {
            refund(world, pos, hiringPlayer, chargedCurrency, chargedCost);
            notifyPlayer(world, hiringPlayer, "Guard hire cancelled because the town no longer has enough capacity.");
            return;
        }

        if (!spawnGuard(world, pos, townId, data)) {
            refund(world, pos, hiringPlayer, chargedCurrency, chargedCost);
            notifyPlayer(world, hiringPlayer, "Guard hire failed; your hiring currency was refunded.");
            return;
        }
        notifyPlayer(world, hiringPlayer, "A guard has arrived at the Barracks.");
    }

    public static int remainingSeconds(ServerWorld world, BarracksBlockEntity barracks) {
        if (!barracks.isSearching()) return 0;
        long remaining = Math.max(0L, barracks.getHireReadyTick() - world.getTime());
        return (int) Math.ceil(remaining / 20.0D);
    }

    private static boolean spawnGuard(ServerWorld world, BlockPos pos, String townId, TownSavedData data) {
        if (!Registries.ENTITY_TYPE.containsId(GUARD_ENTITY_ID)) return false;
        EntityType<?> type = Registries.ENTITY_TYPE.get(GUARD_ENTITY_ID);
        Entity entity = type.create(world);
        if (!(entity instanceof LivingEntity living) || !GuardVillagersIntegration.isGuardEntity(living)) return false;

        living.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                world.getRandom().nextFloat() * 360.0F, 0.0F);
        if (living instanceof MobEntity mob) mob.setPersistent();
        if (!world.spawnEntity(living)) return false;

        UUID guardId = living.getUuid();
        if (!data.addResident(guardId)) {
            living.discard();
            return false;
        }
        if (!TownGuardRosterSavedData.get(world).add(townId, guardId)) {
            data.removeResident(guardId);
            living.discard();
            return false;
        }
        data.setPopulation(Math.max(data.getPopulation(), data.getResidents().size()));
        return true;
    }

    private static boolean isRegisteredBarracks(TownSavedData data, BlockPos pos) {
        return data.getRegisteredBuildings().stream().anyMatch(building ->
                ("guard_post".equals(building.type()) || "barracks".equals(building.type()))
                        && building.anchor().getSquaredDistance(pos) <= 4.0D);
    }

    private static Item currencyItem() {
        Identifier configured = Identifier.tryParse(MCATownsConfig.get().currencyItemId);
        Identifier fallback = new Identifier("minecraft", "emerald");
        return configured != null && Registries.ITEM.containsId(configured)
                ? Registries.ITEM.get(configured) : Registries.ITEM.get(fallback);
    }

    private static void refund(ServerWorld world, BlockPos pos, UUID playerId, String currencyId, int cost) {
        if (cost <= 0) return;
        Identifier id = Identifier.tryParse(currencyId);
        if (id == null || !Registries.ITEM.containsId(id)) id = new Identifier("minecraft", "emerald");
        Item currency = Registries.ITEM.get(id);
        ServerPlayerEntity player = playerId == null ? null : world.getServer().getPlayerManager().getPlayer(playerId);
        if (player != null) {
            int given = InventoryHelper.give(player, currency, cost);
            if (given >= cost) return;
            cost -= given;
        }
        int remaining = cost;
        while (remaining > 0) {
            int count = Math.min(currency.getMaxCount(), remaining);
            world.spawnEntity(new ItemEntity(world, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    new ItemStack(currency, count)));
            remaining -= count;
        }
    }

    private static void notifyPlayer(ServerWorld world, UUID playerId, String message) {
        if (playerId == null) return;
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerId);
        if (player != null) player.sendMessage(Text.literal(message), false);
    }
}
