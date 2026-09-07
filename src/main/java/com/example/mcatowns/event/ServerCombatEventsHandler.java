package com.example.mcatowns.event;

import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.integration.GuardVillagersIntegration;
import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.town.PlayerTownRegistry;
import com.example.mcatowns.town.TownBanditSystem;
import com.example.mcatowns.town.TownBuildingSnapshot;
import com.example.mcatowns.town.TownContext;
import com.example.mcatowns.town.TownDefenseSystem;
import com.example.mcatowns.town.TownGuardRosterSavedData;
import com.example.mcatowns.town.TownManager;
import com.example.mcatowns.town.TownSavedData;
import com.example.mcatowns.town.TownSpecialistRegistry;
import com.example.mcatowns.town.TownStatsRefresher;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/** Server-side combat/death hooks. Bounties are intentionally handled by Bountiful's Bounty Board, not mob-kill counters. */
public final class ServerCombatEventsHandler {
    private ServerCombatEventsHandler() { }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.getWorld() instanceof ServerWorld world)) return;

            // Only MCA Towns-tagged bandits affect Bandit Activity; unrelated Pillagers never count.
            if (TownBanditSystem.handleBanditDeath(world, entity)) return;

            boolean guard = GuardVillagersIntegration.isGuardEntity(entity);
            if (!guard && !isVillagerLike(entity)) return;

            TownContext context;
            TownSavedData data;
            if (guard) {
                // Guard Villagers only count as town residents once explicitly affiliated. Avoid MCA/world discovery for
                // unrelated guard deaths by using the lightweight player-town registry first.
                context = PlayerTownRegistry.get(world).findNearest(entity.getBlockPos(), 64).orElse(null);
                if (context == null) return;
                data = TownSavedData.get(world, context.townId());
                if (!data.getResidents().contains(entity.getUuid())) return;
                TownGuardRosterSavedData.get(world).remove(context.townId(), entity.getUuid());
            } else {
                TownSpecialistRegistry.get(world).remove(entity.getUuid());
                context = TownManager.findExistingTown(world, entity.getBlockPos(), TownManager.getTownSearchMargin()).orElse(null);
                if (context == null) return;
                data = TownSavedData.get(world, context.townId());
            }

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
