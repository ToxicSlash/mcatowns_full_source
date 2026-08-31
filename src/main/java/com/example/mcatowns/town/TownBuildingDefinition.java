package com.example.mcatowns.town;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record TownBuildingDefinition(
        String id,
        String displayName,
        TownBuildingCategory category,
        String description,
        boolean foundingKnowledge,
        int registrationTokens,
        int registrationCurrency,
        int prosperityRequired,
        int populationCapacity,
        int foodCapacity,
        int prosperityBase
) {
    public Map<InfrastructureType, Integer> providedInfrastructure() {
        return switch (id) {
            case "residence" -> Map.of(InfrastructureType.COMMUNITY, 1);
            case "farm" -> Map.of(InfrastructureType.AGRICULTURE, 2);
            case "granary" -> Map.of(InfrastructureType.LOGISTICS, 2);
            case "campfire" -> Map.of(InfrastructureType.COMMUNITY, 2);
            case "park" -> Map.of(InfrastructureType.COMMUNITY, 3);
            case "inn" -> Map.of(InfrastructureType.COMMERCE, 2);
            case "storehouse" -> Map.of(InfrastructureType.LOGISTICS, 3);
            case "bounty_board" -> Map.of(InfrastructureType.SECURITY, 1, InfrastructureType.COMMERCE, 1);
            case "civic_office" -> Map.of(InfrastructureType.COMMUNITY, 4,
                    InfrastructureType.LOGISTICS, 2, InfrastructureType.COMMERCE, 1);
            case "guard_post" -> Map.of(InfrastructureType.SECURITY, 3);
            case "blacksmith" -> Map.of(InfrastructureType.SECURITY, 1, InfrastructureType.COMMERCE, 1);
            case "jeweler" -> Map.of(InfrastructureType.COMMERCE, 2);
            case "scholar" -> Map.of(InfrastructureType.COMMUNITY, 1);
            default -> Map.of();
        };
    }

    public Map<InfrastructureType, Integer> reservedInfrastructure() {
        return switch (id) {
            case "farm" -> Map.of(InfrastructureType.COMMUNITY, 1);
            case "granary" -> Map.of(InfrastructureType.AGRICULTURE, 1);
            case "inn" -> Map.of(InfrastructureType.COMMUNITY, 2, InfrastructureType.LOGISTICS, 1);
            case "storehouse", "bounty_board" -> Map.of(InfrastructureType.COMMUNITY, 1);
            case "guard_post" -> Map.of(InfrastructureType.LOGISTICS, 1, InfrastructureType.COMMUNITY, 1);
            case "blacksmith", "jeweler", "scholar" -> Map.of(InfrastructureType.LOGISTICS, 1);
            default -> Map.of();
        };
    }

    public int workersRequired() {
        return switch (id) {
            case "farm", "granary", "storehouse", "civic_office", "blacksmith", "jeweler", "scholar" -> 1;
            case "inn", "guard_post" -> 2;
            default -> 0;
        };
    }

    public static final List<TownBuildingDefinition> ALL = List.of(
            new TownBuildingDefinition("residence", "Residence", TownBuildingCategory.RESIDENTIAL,
                    "+2 population capacity. Requires an MCA-valid enclosed home with a bed.", true, 0, 2, 0, 2, 0, 0),
            new TownBuildingDefinition("farm", "Farm", TownBuildingCategory.FOOD,
                    "Basic food infrastructure. Crops remain physical and player-managed.", true, 0, 2, 0, 0, 0, 0),
            new TownBuildingDefinition("granary", "Granary", TownBuildingCategory.FOOD,
                    "+100 Town Food capacity. Uses the existing Silo block.", false, 3, 8, 15, 0, 100, 0),
            new TownBuildingDefinition("campfire", "Campfire", TownBuildingCategory.COMMUNITY,
                    "+2 Prosperity Base and a small mood modifier.", true, 0, 1, 0, 0, 0, 2),
            new TownBuildingDefinition("park", "Park", TownBuildingCategory.COMMUNITY,
                    "+4 Prosperity Base and a positive outdoor mood modifier.", false, 4, 10, 20, 0, 0, 4),
            new TownBuildingDefinition("inn", "Inn", TownBuildingCategory.COMMUNITY,
                    "+6 Prosperity Base. MCA-valid Inn structure required.", false, 6, 16, 35, 0, 0, 6),
            new TownBuildingDefinition("storehouse", "Storehouse", TownBuildingCategory.UTILITY,
                    "Town-owned communal inventory and +20 Town Food capacity.", true, 0, 3, 0, 0, 20, 0),
            new TownBuildingDefinition("bounty_board", "Bounty Board", TownBuildingCategory.UTILITY,
                    "Uses Bountiful's existing Bounty Board.", true, 0, 2, 0, 0, 0, 0),
            new TownBuildingDefinition("civic_office", "Civic Office", TownBuildingCategory.UTILITY,
                    "Specialist Building: Architect. Uses the Mayor Desk.", true, 0, 3, 0, 0, 0, 0),
            new TownBuildingDefinition("guard_post", "Guard Post", TownBuildingCategory.UTILITY,
                    "Defence workplace. Uses the existing Barracks block.", false, 5, 14, 30, 0, 0, 0),
            new TownBuildingDefinition("blacksmith", "Blacksmith", TownBuildingCategory.UTILITY,
                    "Required workplace for the Blacksmith specialist.", false, 5, 14, 25, 0, 0, 0),
            new TownBuildingDefinition("jeweler", "Jeweler", TownBuildingCategory.UTILITY,
                    "Required workplace for the Jeweler specialist.", false, 5, 14, 30, 0, 0, 0),
            new TownBuildingDefinition("scholar", "Scholar", TownBuildingCategory.UTILITY,
                    "Required workplace for the Scholar specialist.", false, 5, 14, 30, 0, 0, 0)
    );

    private static final Map<String, TownBuildingDefinition> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(TownBuildingDefinition::id, Function.identity()));

    public static TownBuildingDefinition get(String id) {
        return BY_ID.get(id);
    }
}
