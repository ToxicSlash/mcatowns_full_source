package com.example.mcatowns.town;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent affiliation roster for Guard Villagers hired through town Barracks. */
public final class TownGuardRosterSavedData extends PersistentState {
    private static final String SAVE_ID = "mcatowns_guard_roster";
    private final Map<String, Set<UUID>> guardsByTown = new HashMap<>();

    public static TownGuardRosterSavedData get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                TownGuardRosterSavedData::fromNbt,
                TownGuardRosterSavedData::new,
                SAVE_ID
        );
    }

    public static TownGuardRosterSavedData fromNbt(NbtCompound nbt) {
        TownGuardRosterSavedData data = new TownGuardRosterSavedData();
        NbtList towns = nbt.getList("Towns", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < towns.size(); i++) {
            NbtCompound town = towns.getCompound(i);
            String townId = town.getString("TownId");
            if (townId.isBlank()) continue;
            Set<UUID> guards = new HashSet<>();
            NbtList guardList = town.getList("Guards", NbtElement.COMPOUND_TYPE);
            for (int j = 0; j < guardList.size(); j++) {
                NbtCompound guard = guardList.getCompound(j);
                if (guard.containsUuid("Id")) guards.add(guard.getUuid("Id"));
            }
            if (!guards.isEmpty()) data.guardsByTown.put(townId, guards);
        }
        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList towns = new NbtList();
        guardsByTown.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            NbtCompound town = new NbtCompound();
            town.putString("TownId", entry.getKey());
            NbtList guards = new NbtList();
            entry.getValue().stream().sorted().forEach(id -> {
                NbtCompound guard = new NbtCompound();
                guard.putUuid("Id", id);
                guards.add(guard);
            });
            town.put("Guards", guards);
            towns.add(town);
        });
        nbt.put("Towns", towns);
        return nbt;
    }

    public int count(String townId) {
        return guardsByTown.getOrDefault(townId, Collections.emptySet()).size();
    }

    public boolean contains(String townId, UUID guardId) {
        return guardId != null && guardsByTown.getOrDefault(townId, Collections.emptySet()).contains(guardId);
    }

    public boolean add(String townId, UUID guardId) {
        if (townId == null || townId.isBlank() || guardId == null) return false;
        boolean added = guardsByTown.computeIfAbsent(townId, ignored -> new HashSet<>()).add(guardId);
        if (added) markDirty();
        return added;
    }

    public boolean remove(String townId, UUID guardId) {
        Set<UUID> guards = guardsByTown.get(townId);
        if (guards == null || guardId == null || !guards.remove(guardId)) return false;
        if (guards.isEmpty()) guardsByTown.remove(townId);
        markDirty();
        return true;
    }

    public void removeTown(String townId) {
        if (townId != null && guardsByTown.remove(townId) != null) markDirty();
    }
}
