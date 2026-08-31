package com.example.mcatowns.town;

import net.minecraft.util.math.BlockPos;

public record TownContext(
        String townId,
        BlockPos anchor,
        BlockPos center,
        String source,
        boolean validMcaTown
) {
}
