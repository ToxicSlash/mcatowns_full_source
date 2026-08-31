package com.example.mcatowns.event;

import com.example.mcatowns.town.TownSpecialistRegistry;
import com.example.mcatowns.town.VillagerRecruitmentService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public final class VillagerTownInteractionHandler {
    private VillagerTownInteractionHandler() { }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> TownSpecialistRegistry.get(world).inspect(entity));
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!player.isSneaking() || !(entity instanceof VillagerEntity villager)) return ActionResult.PASS;
            if (world.isClient) return ActionResult.SUCCESS;
            if (player instanceof ServerPlayerEntity serverPlayer) VillagerRecruitmentService.open(serverPlayer, villager);
            return ActionResult.CONSUME;
        });
    }
}
