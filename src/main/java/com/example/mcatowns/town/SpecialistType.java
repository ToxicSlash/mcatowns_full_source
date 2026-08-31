package com.example.mcatowns.town;

import java.util.Locale;

public enum SpecialistType {
    ARCHITECT("Architect", "civic_office"),
    BLACKSMITH("Blacksmith", "blacksmith"),
    JEWELER("Jeweler", "jeweler"),
    SCHOLAR("Scholar", "scholar");

    private final String displayName;
    private final String workplace;

    SpecialistType(String displayName, String workplace) {
        this.displayName = displayName;
        this.workplace = workplace;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String displayName() { return displayName; }
    public String workplace() { return workplace; }
}
