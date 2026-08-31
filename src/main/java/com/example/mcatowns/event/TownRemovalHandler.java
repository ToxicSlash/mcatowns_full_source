package com.example.mcatowns.event;

import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.town.PlayerTownRegistry;
import com.example.mcatowns.town.TownContext;
import com.example.mcatowns.town.TownManager;
import com.example.mcatowns.town.TownSavedData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class TownRemovalHandler {
    private TownRemovalHandler() {
    }

    public static boolean canRemove(ServerPlayerEntity player, BlockPos bellPos) {
        ServerWorld world = player.getServerWorld();
        if (player.squaredDistanceTo(bellPos.getX() + 0.5, bellPos.getY() + 0.5, bellPos.getZ() + 0.5) > 64.0) {
            return false;
        }
        if (hasAdminAccess(player)) {
            return TownManager.findExistingTown(world, bellPos, 64).isPresent();
        }
        return PlayerTownRegistry.get(world).getTownAt(bellPos)
                .filter(town -> PlayerTownRegistry.get(world).isOwner(town.townId(), player))
                .isPresent();
    }

    public static boolean canRename(ServerPlayerEntity player, BlockPos anchor) {
        return canRemove(player, anchor);
    }

    public static void remove(ServerPlayerEntity player, BlockPos bellPos) {
        if (!canRemove(player, bellPos)) return;
        ServerWorld world = player.getServerWorld();
        TownContext context = TownManager.findExistingTown(world, bellPos, 64).orElse(null);
        if (context == null) return;
        PlayerTownRegistry registry = PlayerTownRegistry.get(world);
        registry.removeTown(context.anchor(), player.getUuid(), hasAdminAccess(player)).ifPresentOrElse(removed -> {
                    MCAIntegration.removeTown(world, removed.mcaTownId());
                    TownSavedData.get(world, removed.townId()).prepareForDeletion();
                    player.sendMessage(Text.translatable("text.mcatowns.town_removed"), false);
                },
                () -> {
                    if (hasAdminAccess(player) && context.townId().startsWith("mca_")) {
                        MCAIntegration.removeTown(world, context.townId());
                        TownSavedData.get(world, context.townId()).prepareForDeletion();
                        player.sendMessage(Text.translatable("text.mcatowns.town_removed"), false);
                    }
                });
    }

    public static boolean hasAdminAccess(ServerPlayerEntity player) {
        return player.getAbilities().creativeMode || player.hasPermissionLevel(2);
    }
}
