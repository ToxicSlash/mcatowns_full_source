package com.example.mcatowns.town;

import java.util.LinkedHashMap;
import java.util.Map;

public record TownRequest(
        String id,
        String name,
        Type type,
        Map<String, Integer> requirements,
        long dueDay,
        int prosperityReward,
        int tokenReward
) {
    public TownRequest {
        requirements = Map.copyOf(new LinkedHashMap<>(requirements));
    }

    public enum Type {
        ROUTINE("Routine"), IMPORTANT("Important");
        private final String displayName;
        Type(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }
}
