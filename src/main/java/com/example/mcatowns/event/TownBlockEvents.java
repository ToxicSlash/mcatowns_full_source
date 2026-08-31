package com.example.mcatowns.event;

import com.example.mcatowns.town.PlayerTownRegistry;
import com.example.mcatowns.town.TownBuildingService;
import com.example.mcatowns.town.TownManager;
import com.example.mcatowns.town.TownSavedData;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class TownBlockEvents {
    private TownBlockEvents() { }

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld
                    && PlayerTownRegistry.get(serverWorld).getTownAt(pos).isPresent()) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.sendMessage(Text.translatable("text.mcatowns.use_remove_town"), true);
                }
                return false;
            }
            return true;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient || !(world instanceof net.minecraft.server.world.ServerWorld serverWorld)) return;
            TownManager.findExistingTown(serverWorld, pos, 64).ifPresent(town -> {
                TownSavedData data = TownSavedData.get(serverWorld, town.townId());
                if (data.unregisterBuilding(pos)) TownBuildingService.refreshDerivedValues(data);
            });
        });
    }
}
