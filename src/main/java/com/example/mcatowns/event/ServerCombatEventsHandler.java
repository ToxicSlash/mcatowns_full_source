package com.example.mcatowns.event;

import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.registry.ModItems;
import com.example.mcatowns.town.PlayerTownRegistry;
import com.example.mcatowns.town.TownBuildingSnapshot;
import com.example.mcatowns.town.TownDefenseSystem;
import com.example.mcatowns.town.TownManager;
import com.example.mcatowns.town.TownSavedData;
import com.example.mcatowns.town.TownSpecialistRegistry;
import com.example.mcatowns.town.TownStatsRefresher;
import com.example.mcatowns.util.InventoryHelper;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public final class ServerCombatEventsHandler {
    private ServerCombatEventsHandler() { }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.getWorld() instanceof ServerWorld world)) return;

            if (entity instanceof HostileEntity && source.getAttacker() instanceof ServerPlayerEntity player) {
                // Bounties only apply to player-founded towns, so avoid an MCA reflection lookup on every mob kill.
                PlayerTownRegistry registry = PlayerTownRegistry.get(world);
                registry.findNearest(entity.getBlockPos(), 64)
                        .filter(context -> registry.isOwner(context.townId(), player))
                        .ifPresent(context -> {
                            TownSavedData data = TownSavedData.get(world, context.townId());
                            if (data.hasBuilding("bounty_board") && data.recordBountyKill()) {
                                InventoryHelper.give(player, ModItems.BLUEPRINT_SCRAP, 1);
                                player.sendMessage(net.minecraft.text.Text.translatable("text.mcatowns.bounty_complete"), false);
                            }
                        });
            }

            if (!isVillagerLike(entity)) return;

            TownSpecialistRegistry.get(world).remove(entity.getUuid());
            var context = TownManager.findExistingTown(world, entity.getBlockPos(), TownManager.getTownSearchMargin()).orElse(null);
            if (context == null) return;

            TownSavedData data = TownSavedData.get(world, context.townId());
            if ("player_created".equals(context.source())) data.removeResident(entity.getUuid());
            TownBuildingSnapshot snapshot = MCAIntegration.scanBuildings(world, context.center());
            TownStatsRefresher.refresh(world, context.center(), data, snapshot);
            int graveyardMitigation = Math.min(5, snapshot.graveyards() * 2);
            int penalty = TownDefenseSystem.mitigateVillagerDeathPenalty(
                    data,
                    Math.max(1, MCATownsConfig.get().deathHappinessPenalty - graveyardMitigation)
            );
            data.setHappiness(Math.max(0, data.getHappiness() - penalty));
        });
    }

    private static boolean isVillagerLike(LivingEntity entity) {
        if (entity instanceof VillagerEntity) return true;
        Identifier id = Registries.ENTITY_TYPE.getId(entity.getType());
        return "mca".equals(id.getNamespace())
                || "net.mca.entity.VillagerEntityMCA".equals(entity.getClass().getName());
    }
}
