package com.example.mcatowns.town;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record TownResearchDefinition(
        String id,
        String buildingId,
        int scraps,
        int greatEssence,
        int currency,
        int townTokens
) {
    public static final List<TownResearchDefinition> ALL = List.of(
            new TownResearchDefinition("granary", "granary", 3, 1, 8, 3),
            new TownResearchDefinition("park", "park", 3, 1, 10, 3),
            new TownResearchDefinition("inn", "inn", 5, 2, 16, 5),
            new TownResearchDefinition("guard_post", "guard_post", 4, 1, 14, 4),
            new TownResearchDefinition("blacksmith", "blacksmith", 4, 1, 14, 4),
            new TownResearchDefinition("jeweler", "jeweler", 5, 2, 14, 5),
            new TownResearchDefinition("scholar", "scholar", 5, 2, 14, 5)
    );

    private static final Map<String, TownResearchDefinition> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(TownResearchDefinition::id, Function.identity()));

    public static TownResearchDefinition get(String id) {
        return BY_ID.get(id);
    }
}
