package com.example.mcatowns.event;

import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.town.TownBuildingSnapshot;
import com.example.mcatowns.town.TownDefenseSystem;
import com.example.mcatowns.town.TownManager;
import com.example.mcatowns.town.TownSavedData;
import com.example.mcatowns.town.TownStatsRefresher;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import com.example.mcatowns.town.PlayerTownRegistry;
import com.example.mcatowns.util.InventoryHelper;
import com.example.mcatowns.registry.ModItems;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

public class ServerCombatEventsHandler {
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.getWorld() instanceof ServerWorld world)) {
                return;
            }
            if (entity instanceof HostileEntity && source.getAttacker() instanceof ServerPlayerEntity player) {
                var context = TownManager.findExistingTown(world, entity.getBlockPos(), 64).orElse(null);
                if (context != null && "player_created".equals(context.source())
                        && PlayerTownRegistry.get(world).isOwner(context.townId(), player)) {
                    TownSavedData data = TownSavedData.get(world, context.townId());
                    if (data.hasBuilding("bounty_board") && data.recordBountyKill()) {
                        InventoryHelper.give(player, ModItems.BLUEPRINT_SCRAP, 1);
                        player.sendMessage(net.minecraft.text.Text.translatable("text.mcatowns.bounty_complete"), false);
                    }
                }
            }
            if (!isVillagerLike(entity)) {
                return;
            }

            var context = TownManager.resolveTownContext(world, entity.getBlockPos());
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
        if (entity instanceof VillagerEntity) {
            return true;
        }
        Identifier id = Registries.ENTITY_TYPE.getId(entity.getType());
        if ("mca".equals(id.getNamespace())) {
            return true;
        }
        return "net.mca.entity.VillagerEntityMCA".equals(entity.getClass().getName());
    }
}
