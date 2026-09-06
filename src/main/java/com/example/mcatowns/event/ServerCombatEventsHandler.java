package com.example.mcatowns.event;

import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.town.TownBuildingSnapshot;
import com.example.mcatowns.town.TownDefenseSystem;
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
