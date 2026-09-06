package com.example.mcatowns.town;

/** Lightweight player-facing labels for existing town events. */
public final class TownEventPresentation {
    private TownEventPresentation() { }

    public static String displayName(String eventId) {
        if (eventId == null || eventId.isBlank()) return "";
        return switch (eventId) {
            case "good_harvest" -> "Good Harvest";
            case "mine_collapse" -> "Mine Collapse";
            case "fire" -> "Town Fire";
            case "guard_training_day" -> "Guard Training Day";
            case "refugees_arrive" -> "Refugees Arrive";
            case "drought" -> "Drought";
            case "disease" -> "Disease Outbreak";
            default -> titleCase(eventId);
        };
    }

    public static boolean isDisaster(String eventId) {
        if (eventId == null || eventId.isBlank()) return false;
        return switch (eventId) {
            case "mine_collapse", "fire", "drought", "disease" -> true;
            default -> false;
        };
    }

    private static String titleCase(String value) {
        String[] parts = value.replace('_', ' ').split(" ");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }
}
