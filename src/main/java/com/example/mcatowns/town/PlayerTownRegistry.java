package com.example.mcatowns.town;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerTownRegistry extends PersistentState {
    private final List<Entry> towns = new ArrayList<>();

    public static PlayerTownRegistry get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                PlayerTownRegistry::fromNbt,
                PlayerTownRegistry::new,
                "mcatowns_player_towns"
        );
    }

    public static PlayerTownRegistry fromNbt(NbtCompound nbt) {
        PlayerTownRegistry registry = new PlayerTownRegistry();
        NbtList list = nbt.getList("Towns", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound town = list.getCompound(i);
            if (town.getString("TownId").isBlank() || !town.containsUuid("Owner") || !town.contains("Anchor")) {
                continue;
            }
            BlockPos anchor = BlockPos.fromLong(town.getLong("Anchor"));
            registry.towns.add(new Entry(
                    town.getString("TownId"),
                    anchor,
                    town.contains("Center") ? BlockPos.fromLong(town.getLong("Center")) : anchor,
                    town.getUuid("Owner"),
                    town.getString("McaTownId")
            ));
        }
        return registry;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Entry entry : towns) {
            NbtCompound town = new NbtCompound();
            town.putString("TownId", entry.townId());
            town.putLong("Anchor", entry.anchor().asLong());
            town.putLong("Center", entry.center().asLong());
            town.putUuid("Owner", entry.owner());
            if (!entry.mcaTownId().isBlank()) town.putString("McaTownId", entry.mcaTownId());
            list.add(town);
        }
        nbt.put("Towns", list);
        return nbt;
    }

    public TownContext createTown(ServerWorld world, ServerPlayerEntity owner, BlockPos anchor) {
        String dim = world.getRegistryKey().getValue().toString().replace(':', '_').replace('/', '_');
        String townId = "player_" + dim + "_" + anchor.getX() + "_" + anchor.getY() + "_" + anchor.getZ();
        Optional<TownContext> existing = getTownById(townId);
        if (existing.isPresent()) {
            return existing.get();
        }
        Entry entry = new Entry(townId, anchor.toImmutable(), anchor.toImmutable(), owner.getUuid(), "");
        towns.add(entry);

        TownSavedData data = TownSavedData.get(world, townId);
        data.setPopulation(1);
        data.setJobless(0);
        data.setFoodReserves(0);
        data.setWeeklyTaxIncome(0);
        data.setHappiness(50);
        data.setDefenseRating(0);
        data.setUnrest(0);
        data.initializeFoundedTown(com.example.mcatowns.config.MCATownsConfig.get().foundedTownStartingProsperity);
        data.setTownName(TownNameGenerator.create(anchor, owner.getUuid()));
        data.setTownCenter(anchor);
        data.setLastProgressionDay(TownManager.getDay(world));

        markDirty();
        return entry.toContext();
    }

    public Optional<TownContext> findNearest(BlockPos pos, int margin) {
        int radius = Math.max(16, margin);
        double maxDistanceSq = (double) radius * radius;
        Entry nearest = null;
        double best = Double.MAX_VALUE;
        for (Entry entry : towns) {
            double distanceSq = entry.anchor().getSquaredDistance(pos);
            if (distanceSq <= maxDistanceSq && distanceSq < best) {
                nearest = entry;
                best = distanceSq;
            }
        }
        return nearest == null ? Optional.empty() : Optional.of(nearest.toContext());
    }

    public Optional<TownContext> getTownById(String townId) {
        return towns.stream()
                .filter(entry -> entry.townId().equals(townId))
                .findFirst()
                .map(Entry::toContext);
    }

    public Optional<TownContext> getTownAt(BlockPos anchor) {
        return towns.stream().filter(entry -> entry.anchor().equals(anchor)).findFirst().map(Entry::toContext);
    }

    public Optional<TownContext> getTownByMcaId(String mcaTownId) {
        if (mcaTownId == null || mcaTownId.isBlank()) return Optional.empty();
        return towns.stream().filter(entry -> mcaTownId.equals(entry.mcaTownId())).findFirst().map(Entry::toContext);
    }

    public Optional<TownContext> getOwnedTown(UUID owner) {
        return towns.stream().filter(entry -> entry.owner().equals(owner)).findFirst().map(Entry::toContext);
    }

    public void bindMcaTown(String townId, String mcaTownId) {
        if (mcaTownId == null || mcaTownId.isBlank()) return;
        for (int i = 0; i < towns.size(); i++) {
            Entry entry = towns.get(i);
            if (entry.townId().equals(townId) && !mcaTownId.equals(entry.mcaTownId())) {
                towns.set(i, new Entry(entry.townId(), entry.anchor(), entry.center(), entry.owner(), mcaTownId));
                markDirty();
                return;
            }
        }
    }

    public Optional<RemovedTown> removeOwnedTown(BlockPos anchor, UUID owner) {
        return removeTown(anchor, owner, false);
    }

    public Optional<RemovedTown> removeTown(BlockPos anchor, UUID owner, boolean bypassOwner) {
        for (int i = 0; i < towns.size(); i++) {
            Entry entry = towns.get(i);
            if (entry.anchor().equals(anchor) && (bypassOwner || entry.owner().equals(owner))) {
                towns.remove(i);
                markDirty();
                return Optional.of(new RemovedTown(entry.townId(), entry.mcaTownId()));
            }
        }
        return Optional.empty();
    }

    public List<TownContext> getAllContexts() {
        return towns.stream().map(Entry::toContext).toList();
    }

    public boolean isOwner(String townId, ServerPlayerEntity player) {
        return towns.stream()
                .anyMatch(entry -> entry.townId().equals(townId) && entry.owner().equals(player.getUuid()));
    }

    private record Entry(String townId, BlockPos anchor, BlockPos center, UUID owner, String mcaTownId) {
        TownContext toContext() {
            return new TownContext(townId, anchor, center, "player_created", false);
        }
    }

    public record RemovedTown(String townId, String mcaTownId) {
    }
}
