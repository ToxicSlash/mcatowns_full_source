package com.example.mcatowns.town;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;

/** Lightweight saved state for Bandit Activity. Physical encounters are deliberately stored elsewhere/later. */
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
            data.states.put(townId, new State(
                    TownBanditSystem.clampActivity(entry.getInt("Activity")),
                    entry.contains("LastGrowthTick") ? entry.getLong("LastGrowthTick") : -1L,
                    entry.contains("LastBandAttemptTick") ? entry.getLong("LastBandAttemptTick") : -1L,
                    entry.contains("LastCampAttemptTick") ? entry.getLong("LastCampAttemptTick") : -1L,
                    entry.contains("CampCooldownUntilTick") ? entry.getLong("CampCooldownUntilTick") : -1L
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
                state.campCooldownUntilTick()
        );
        State previous = states.put(townId, sanitized);
        if (!sanitized.equals(previous)) markDirty();
    }

    public void remove(String townId) {
        if (townId != null && states.remove(townId) != null) markDirty();
    }

    public record State(int activity, long lastGrowthTick, long lastBandAttemptTick,
                        long lastCampAttemptTick, long campCooldownUntilTick) {
        public static State initial() {
            return new State(0, -1L, -1L, -1L, -1L);
        }

        public State withActivity(int value) {
            return new State(TownBanditSystem.clampActivity(value), lastGrowthTick, lastBandAttemptTick,
                    lastCampAttemptTick, campCooldownUntilTick);
        }

        public State withLastGrowthTick(long tick) {
            return new State(activity, tick, lastBandAttemptTick, lastCampAttemptTick, campCooldownUntilTick);
        }

        public State withLastBandAttemptTick(long tick) {
            return new State(activity, lastGrowthTick, tick, lastCampAttemptTick, campCooldownUntilTick);
        }

        public State withLastCampAttemptTick(long tick) {
            return new State(activity, lastGrowthTick, lastBandAttemptTick, tick, campCooldownUntilTick);
        }

        public State withCampCooldownUntilTick(long tick) {
            return new State(activity, lastGrowthTick, lastBandAttemptTick, lastCampAttemptTick, tick);
        }
    }
}
