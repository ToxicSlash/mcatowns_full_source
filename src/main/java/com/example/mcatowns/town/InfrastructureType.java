package com.example.mcatowns.town;

public enum InfrastructureType {
    AGRICULTURE("Agriculture"),
    SECURITY("Security"),
    COMMERCE("Commerce"),
    COMMUNITY("Community"),
    LOGISTICS("Logistics");

    private final String displayName;

    InfrastructureType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() { return displayName; }
}
