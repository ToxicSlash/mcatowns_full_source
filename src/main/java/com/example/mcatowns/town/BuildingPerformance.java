package com.example.mcatowns.town;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

/** Shared, deliberately small output calculation used by every town production system. */
public final class BuildingPerformance {
    private static final int FURNITURE_RADIUS = 4;
    private static final int SYNERGY_RADIUS_SQUARED = 32 * 32;
    private static final Map<String, Set<String>> SYNERGIES = Map.ofEntries(
            Map.entry("farm", Set.of("granary", "storehouse")),
            Map.entry("granary", Set.of("farm", "storehouse")),
            Map.entry("storehouse", Set.of("farm", "granary", "inn")),
            Map.entry("residence", Set.of("campfire", "park", "inn")),
            Map.entry("campfire", Set.of("residence", "park", "inn")),
            Map.entry("park", Set.of("residence", "campfire", "inn")),
            Map.entry("inn", Set.of("residence", "park", "storehouse")),
            Map.entry("guard_post", Set.of("civic_office", "bounty_board")),
            Map.entry("blacksmith", Set.of("guard_post", "storehouse")),
            Map.entry("jeweler", Set.of("civic_office", "storehouse")),
            Map.entry("scholar", Set.of("civic_office", "park")),
            Map.entry("civic_office", Set.of("bounty_board", "guard_post", "scholar"))
    );

    private BuildingPerformance() { }

    public static int calculateOutput(ServerWorld world, TownSavedData data, RegisteredTownBuilding building) {
        TownBuildingDefinition definition = TownBuildingDefinition.get(building.type());
        if (definition == null) return 0;
        int requiredWorkers = definition.workersRequired();
        int staffingPercent = requiredWorkers == 0 ? 100
                : Math.min(100, building.workers().size() * 100 / requiredWorkers);
        int furniture = countFurniture(world, building.anchor());
        int synergies = countSynergies(data.getRegisteredBuildings(), building);
        return outputFromFactors(building.tier(), staffingPercent, furniture, synergies);
    }

    public static int outputFromFactors(int tier, int staffingPercent, int furnitureCount, int synergyCount) {
        int staffing = Math.max(0, Math.min(100, staffingPercent));
        if (staffing == 0) return 0;
        int bonuses = Math.max(0, Math.min(2, tier - 1)) * 5
                + Math.min(10, Math.max(0, furnitureCount) * 2)
                + Math.min(10, Math.max(0, synergyCount) * 5);
        return Math.min(150, staffing + bonuses * staffing / 100);
    }

    public static int outputPercent(TownSavedData data, RegisteredTownBuilding building) {
        if (building.status() == BuildingStatus.INFRASTRUCTURE_BLOCKED
                || building.status() == BuildingStatus.NEEDS_INSPECTION) return 0;
        TownBuildingDefinition definition = TownBuildingDefinition.get(building.type());
        int required = definition == null ? 0 : definition.workersRequired();
        int staffing = required == 0 ? 100 : Math.min(100, building.workers().size() * 100 / required);
        return Math.max(0, Math.min(150, building.output()));
    }

    public static int averageOutputPercent(TownSavedData data, Collection<String> buildingTypes, int fallback) {
        int total = 0;
        int count = 0;
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            if (!buildingTypes.contains(building.type())) continue;
            total += outputPercent(data, building);
            count++;
        }
        return count == 0 ? Math.max(0, Math.min(100, fallback)) : total / count;
    }

    private static int countFurniture(ServerWorld world, BlockPos anchor) {
        if (!world.isChunkLoaded(anchor)) return 0;
        int count = 0;
        for (BlockPos pos : BlockPos.iterate(anchor.add(-FURNITURE_RADIUS, -2, -FURNITURE_RADIUS),
                anchor.add(FURNITURE_RADIUS, 3, FURNITURE_RADIUS))) {
            if (!world.getWorldBorder().contains(pos) || !world.isChunkLoaded(pos)) continue;
            if (FurnitureBlocks.SIMPLE.contains(world.getBlockState(pos).getBlock()) && ++count >= 5) break;
        }
        return count;
    }

    private static int countSynergies(List<RegisteredTownBuilding> buildings, RegisteredTownBuilding target) {
        Set<String> accepted = SYNERGIES.getOrDefault(target.type(), Set.of());
        if (accepted.isEmpty()) return 0;
        int count = 0;
        for (RegisteredTownBuilding other : buildings) {
            if (!other.id().equals(target.id()) && accepted.contains(other.type())
                    && other.status() != BuildingStatus.NEEDS_INSPECTION
                    && other.anchor().getSquaredDistance(target.anchor()) <= SYNERGY_RADIUS_SQUARED) {
                if (++count >= 2) break;
            }
        }
        return count;
    }

    /** Delays Minecraft registry access so the arithmetic can be unit-tested without bootstrapping a game. */
    private static final class FurnitureBlocks {
        private static final Set<Block> SIMPLE = Set.of(
                Blocks.BARREL, Blocks.CHEST, Blocks.CRAFTING_TABLE, Blocks.FURNACE,
                Blocks.LANTERN, Blocks.TORCH, Blocks.BOOKSHELF, Blocks.HAY_BLOCK,
                Blocks.SMITHING_TABLE, Blocks.LECTERN, Blocks.BELL);
    }
}
