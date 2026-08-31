package com.example.mcatowns.network;

import com.example.mcatowns.town.TownFoodSystem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public final class SiloActions {
    private SiloActions() {
    }

    public static void deposit(ServerPlayerEntity player, BlockPos pos) {
        TownFoodSystem.contributeFromInventory(player, pos);
    }

}
