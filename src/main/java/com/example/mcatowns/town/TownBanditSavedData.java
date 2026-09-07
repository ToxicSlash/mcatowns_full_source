package com.example.mcatowns.town;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Lightweight saved state for Bandit Activity and virtual encounter reservations. */
public final class TownBanditSavedData extends PersistentState {
    private static final String SAVE_ID = "mcatowns_bandit_activity";
    private final Map<String, State> states = new HashMap<>();

    public static TownBanditSavedData get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                TownBanditSavedData::fromNbt,
                TownBanditSavedData::new,
                SAVE_ID
        );
    }

    public static TownBanditSavedData fromNbt(NbtCompound nbt) {
        TownBanditSavedData data = new TownBanditSavedData();
        NbtList towns = nbt.getList("Towns", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < towns.size(); i++) {
            NbtCompound entry = towns.getCompound(i);
            String townId = entry.getString("TownId");
            if (townId.isBlank()) continue;
            List<BanditEncounter> encounters = new ArrayList<>();
            NbtList encounterList = entry.getList("Encounters", NbtElement.COMPOUND_TYPE);
            for (int j = 0; j < encounterList.size(); j++) {
                NbtCompound encounter = encounterList.getCompound(j);
                if (!encounter.containsUuid("Id") || !encounter.contains("Pos")) continue;
                BanditEncounterType type;
                try {
                    type = BanditEncounterType.valueOf(encounter.getString("Type"));
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                encounters.add(new BanditEncounter(
                        encounter.getUuid("Id"),
                        type,
                        BlockPos.fromLong(encounter.getLong("Pos")),
                        encounter.contains("CreatedTick") ? encounter.getLong("CreatedTick") : -1L,
                        encounter.contains("SpawnedTick") ? encounter.getLong("SpawnedTick") : -1L,
                        encounter.contains("RemainingMembers") ? encounter.getInt("RemainingMembers") : 0
                ));
            }
            data.states.put(townId, new State(
                    TownBanditSystem.clampActivity(entry.getInt("Activity")),
                    entry.contains("LastGrowthTick") ? entry.getLong("LastGrowthTick") : -1L,
                    entry.contains("LastBandAttemptTick") ? entry.getLong("LastBandAttemptTick") : -1L,
                    entry.contains("LastCampAttemptTick") ? entry.getLong("LastCampAttemptTick") : -1L,
                    entry.contains("CampCooldownUntilTick") ? entry.getLong("CampCooldownUntilTick") : -1L,
                    encounters
            ));
        }
        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList towns = new NbtList();
        states.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            NbtCompound town = new NbtCompound();
            town.putString("TownId", entry.getKey());
            State state = entry.getValue();
            town.putInt("Activity", state.activity());
            town.putLong("LastGrowthTick", state.lastGrowthTick());
            town.putLong("LastBandAttemptTick", state.lastBandAttemptTick());
            town.putLong("LastCampAttemptTick", state.lastCampAttemptTick());
            town.putLong("CampCooldownUntilTick", state.campCooldownUntilTick());
            NbtList encounters = new NbtList();
            for (BanditEncounter encounter : state.encounters()) {
                NbtCompound savedEncounter = new NbtCompound();
                savedEncounter.putUuid("Id", encounter.id());
                savedEncounter.putString("Type", encounter.type().name());
                savedEncounter.putLong("Pos", encounter.pos().asLong());
                savedEncounter.putLong("CreatedTick", encounter.createdTick());
                savedEncounter.putLong("SpawnedTick", encounter.spawnedTick());
                savedEncounter.putInt("RemainingMembers", encounter.remainingMembers());
                encounters.add(savedEncounter);
            }
            town.put("Encounters", encounters);
            towns.add(town);
        });
        nbt.put("Towns", towns);
        return nbt;
    }

    public State getOrCreate(String townId) {
        return states.computeIfAbsent(townId, ignored -> State.initial());
    }

    public void put(String townId, State state) {
        if (townId == null || townId.isBlank() || state == null) return;
        State sanitized = new State(
                TownBanditSystem.clampActivity(state.activity()),
                state.lastGrowthTick(),
                state.lastBandAttemptTick(),
                state.lastCampAttemptTick(),
                state.campCooldownUntilTick(),
                state.encounters()
        );
        State previous = states.put(townId, sanitized);
        if (!sanitized.equals(previous)) markDirty();
    }

    public List<BanditEncounter> getEncounters(String townId) {
        return getOrCreate(townId).encounters();
    }

    public Optional<BanditEncounter> getEncounter(String townId, UUID encounterId) {
        if (encounterId == null) return Optional.empty();
        return getOrCreate(townId).encounters().stream().filter(encounter -> encounter.id().equals(encounterId)).findFirst();
    }

    public int countEncounters(String townId, BanditEncounterType type, long now) {
        pruneExpired(townId, now);
        return (int) getOrCreate(townId).encounters().stream().filter(encounter -> encounter.type() == type).count();
    }

    public BanditEncounter reserveEncounter(String townId, BanditEncounterType type, BlockPos pos, long now, int capacity) {
        if (capacity <= 0 || type == null || pos == null) return null;
        pruneExpired(townId, now);
        State state = getOrCreate(townId);
        long existing = state.encounters().stream().filter(encounter -> encounter.type() == type).count();
        if (existing >= capacity) return null;
        BanditEncounter encounter = BanditEncounter.reserve(type, pos, now);
        List<BanditEncounter> updated = new ArrayList<>(state.encounters());
        updated.add(encounter);
        put(townId, state.withEncounters(updated));
        return encounter;
    }

    public boolean markEncounterSpawned(String townId, UUID encounterId, long now, int memberCount) {
        State state = getOrCreate(townId);
        List<BanditEncounter> updated = new ArrayList<>(state.encounters());
        for (int i = 0; i < updated.size(); i++) {
            BanditEncounter encounter = updated.get(i);
            if (encounter.id().equals(encounterId)) {
                updated.set(i, encounter.withSpawned(now, memberCount));
                put(townId, state.withEncounters(updated));
                return true;
            }
        }
        return false;
    }

    /** Returns true when this death removed the final tracked member and therefore freed the encounter slot. */
    public boolean recordEncounterMemberDeath(String townId, UUID encounterId) {
        State state = getOrCreate(townId);
        List<BanditEncounter> updated = new ArrayList<>(state.encounters());
        for (int i = 0; i < updated.size(); i++) {
            BanditEncounter encounter = updated.get(i);
            if (!encounter.id().equals(encounterId)) continue;
            if (encounter.remainingMembers() <= 1) {
                updated.remove(i);
                put(townId, state.withEncounters(updated));
                return true;
            }
            updated.set(i, encounter.withOneMemberRemoved());
            put(townId, state.withEncounters(updated));
            return false;
        }
        return false;
    }

    public boolean clearEncounter(String townId, UUID encounterId) {
        State state = getOrCreate(townId);
        List<BanditEncounter> updated = new ArrayList<>(state.encounters());
        boolean removed = updated.removeIf(encounter -> encounter.id().equals(encounterId));
        if (removed) put(townId, state.withEncounters(updated));
        return removed;
    }

    /** Removes expired Wild reservations without touching physical mobs that may have survived their slot lifetime. */
    public int pruneExpired(String townId, long now) {
        State state = getOrCreate(townId);
        List<BanditEncounter> updated = state.encounters().stream().filter(encounter -> encounter.occupiesSlot(now)).toList();
        int removed = state.encounters().size() - updated.size();
        if (removed > 0) put(townId, state.withEncounters(updated));
        return removed;
    }

    public void remove(String townId) {
        if (townId != null && states.remove(townId) != null) markDirty();
    }

    public record State(int activity, long lastGrowthTick, long lastBandAttemptTick,
                        long lastCampAttemptTick, long campCooldownUntilTick, List<BanditEncounter> encounters) {
        public State {
            encounters = encounters == null ? List.of() : List.copyOf(encounters.stream().filter(java.util.Objects::nonNull).toList());
        }

        public State(int activity, long lastGrowthTick, long lastBandAttemptTick,
                     long lastCampAttemptTick, long campCooldownUntilTick) {
            this(activity, lastGrowthTick, lastBandAttemptTick, lastCampAttemptTick, campCooldownUntilTick, List.of());
        }

        public static State initial() {
            return new State(0, -1L, -1L, -1L, -1L, List.of());
        }

        public State withActivity(int value) {
            return new State(TownBanditSystem.clampActivity(value), lastGrowthTick, lastBandAttemptTick,
                    lastCampAttemptTick, campCooldownUntilTick, encounters);
        }

        public State withLastGrowthTick(long tick) {
            return new State(activity, tick, lastBandAttemptTick, lastCampAttemptTick, campCooldownUntilTick, encounters);
        }

        public State withLastBandAttemptTick(long tick) {
            return new State(activity, lastGrowthTick, tick, lastCampAttemptTick, campCooldownUntilTick, encounters);
        }

        public State withLastCampAttemptTick(long tick) {
            return new State(activity, lastGrowthTick, lastBandAttemptTick, tick, campCooldownUntilTick, encounters);
        }

        public State withCampCooldownUntilTick(long tick) {
            return new State(activity, lastGrowthTick, lastBandAttemptTick, lastCampAttemptTick, tick, encounters);
        }

        public State withEncounters(List<BanditEncounter> values) {
            return new State(activity, lastGrowthTick, lastBandAttemptTick, lastCampAttemptTick, campCooldownUntilTick, values);
        }
    }
}
