package com.example.mcatowns.town;

public enum BuildingStatus {
    ACTIVE,
    UNDERSTAFFED,
    INFRASTRUCTURE_BLOCKED,
    NEEDS_INSPECTION;

    public static BuildingStatus fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return ACTIVE;
        }
    }
}
