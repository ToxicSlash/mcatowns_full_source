package com.example.mcatowns.town;

import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Saved reservation for a potential/active bandit encounter. It is intentionally tiny so distant encounters can
 * exist without loading chunks or keeping physical mobs alive.
 */
public record BanditEncounter(
        UUID id,
        BanditEncounterType type,
        BlockPos pos,
        long createdTick,
        long spawnedTick,
        int remainingMembers
) {
    public BanditEncounter {
        id = id == null ? UUID.randomUUID() : id;
        type = type == null ? BanditEncounterType.WILD : type;
        pos = pos == null ? BlockPos.ORIGIN : pos.toImmutable();
        remainingMembers = Math.max(0, remainingMembers);
    }

    public static BanditEncounter reserve(BanditEncounterType type, BlockPos pos, long now) {
        return new BanditEncounter(UUID.randomUUID(), type, pos, now, -1L, 0);
    }

    public boolean spawned() {
        return spawnedTick >= 0L;
    }

    /** Wild reservations have bounded slot ownership; Town/Camp reservations remain until explicitly cleared. */
    public boolean occupiesSlot(long now) {
        if (type != BanditEncounterType.WILD) return true;
        if (!spawned()) {
            return now - createdTick < TownBanditSystem.UNSPAWNED_WILD_BAND_EXPIRY_TICKS;
        }
        return now - spawnedTick < TownBanditSystem.SPAWNED_WILD_SLOT_RELEASE_TICKS;
    }

    public BanditEncounter withSpawned(long now, int memberCount) {
        return new BanditEncounter(id, type, pos, createdTick, now, Math.max(1, memberCount));
    }

    public BanditEncounter withOneMemberRemoved() {
        return new BanditEncounter(id, type, pos, createdTick, spawnedTick, Math.max(0, remainingMembers - 1));
    }
}
