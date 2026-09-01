package com.example.mcatowns.event;

import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.town.PlayerTownRegistry;
import com.example.mcatowns.town.TownBuildingService;
import com.example.mcatowns.town.TownContext;
import com.example.mcatowns.town.TownSavedData;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class TownBlockEvents {
    private static final int BREAK_TOWN_MARGIN = 64;
    private static final int MCA_CACHE_LOOKUP_MARGIN = BREAK_TOWN_MARGIN + 16;
    private static final long MCA_LOOKUP_CACHE_TICKS = 1200L;
    private static final int MAX_CACHED_CHUNKS = 512;
    private static final Map<ChunkLookupKey, CachedTown> MCA_TOWN_CACHE = new HashMap<>();

    private TownBlockEvents() { }

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerWorld serverWorld
                    && PlayerTownRegistry.get(serverWorld).getTownAt(pos).isPresent()) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.sendMessage(Text.translatable("text.mcatowns.use_remove_town"), true);
                }
                return false;
            }
            return true;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)) return;
            findTownForBreak(serverWorld, pos).ifPresent(town -> {
                TownSavedData data = TownSavedData.get(serverWorld, town.townId());
                if (data.unregisterBuilding(pos)) {
                    TownBuildingService.refreshDerivedValues(data);
                }
            });
        });
    }

    private static Optional<TownContext> findTownForBreak(ServerWorld world, BlockPos pos) {
        PlayerTownRegistry registry = PlayerTownRegistry.get(world);
        Optional<TownContext> founded = registry.findNearest(pos, BREAK_TOWN_MARGIN);
        if (founded.isPresent() || !MCAIntegration.isMcaLoaded()) return founded;

        long now = world.getTime();
        ChunkLookupKey key = new ChunkLookupKey(world.getRegistryKey().getValue().toString(), pos.getX() >> 4, pos.getZ() >> 4);
        CachedTown cached = MCA_TOWN_CACHE.get(key);
        if (cached == null || now < cached.cachedAtTick() || now - cached.cachedAtTick() >= MCA_LOOKUP_CACHE_TICKS) {
            Optional<TownContext> discovered = MCAIntegration.findNearestTown(world, pos, MCA_CACHE_LOOKUP_MARGIN)
                    .flatMap(mca -> registry.getTownByMcaId(mca.townId()).or(() -> Optional.of(mca)));
            cached = new CachedTown(now, discovered.orElse(null));
            MCA_TOWN_CACHE.put(key, cached);
            trimCache(now);
        }

        TownContext town = cached.town();
        if (town == null || town.center().getSquaredDistance(pos) > (double) BREAK_TOWN_MARGIN * BREAK_TOWN_MARGIN) {
            return Optional.empty();
        }
        return Optional.of(town);
    }

    private static void trimCache(long now) {
        if (MCA_TOWN_CACHE.size() <= MAX_CACHED_CHUNKS) return;
        MCA_TOWN_CACHE.entrySet().removeIf(entry -> now < entry.getValue().cachedAtTick()
                || now - entry.getValue().cachedAtTick() >= MCA_LOOKUP_CACHE_TICKS);
        if (MCA_TOWN_CACHE.size() <= MAX_CACHED_CHUNKS) return;
        int remove = MCA_TOWN_CACHE.size() - MAX_CACHED_CHUNKS;
        var iterator = MCA_TOWN_CACHE.keySet().iterator();
        while (remove-- > 0 && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private record ChunkLookupKey(String dimension, int chunkX, int chunkZ) { }
    private record CachedTown(long cachedAtTick, TownContext town) { }
}
