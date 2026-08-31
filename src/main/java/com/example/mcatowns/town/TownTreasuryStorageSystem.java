package com.example.mcatowns.town;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class TownTreasuryStorageSystem {
    private TownTreasuryStorageSystem() {
    }

    // Legacy API retained for compatibility with older calls. Treasury is now kept in TownSavedData.
    public static int depositEmeraldValue(ServerWorld world, BlockPos center, int emeralds, int radius) {
        return 0;
    }
}
