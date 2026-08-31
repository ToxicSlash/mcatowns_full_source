package com.example.mcatowns.town;

import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public final class TownNameGenerator {
    private static final String[] PREFIXES = {
            "Willow", "Ash", "Stone", "River", "Oak", "Briar", "Hearth", "Moss",
            "Copper", "Dawn", "Frost", "Green", "High", "Iron", "Maple", "Red"
    };
    private static final String[] SUFFIXES = {
            "brook", "ford", "field", "stead", "haven", "mere", "wick", "vale",
            "fall", "watch", "grove", "hill", "cross", "reach", "ton", "hold"
    };

    private TownNameGenerator() {
    }

    public static String create(BlockPos pos, UUID owner) {
        int seed = Long.hashCode(pos.asLong()) ^ owner.hashCode();
        int prefix = Math.floorMod(seed, PREFIXES.length);
        int suffix = Math.floorMod(seed / PREFIXES.length, SUFFIXES.length);
        return PREFIXES[prefix] + SUFFIXES[suffix];
    }
}
