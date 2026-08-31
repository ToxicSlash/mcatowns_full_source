package com.example.mcatowns.util;

public class TownTextHelper {
    public static String taxRateName(int taxRate) {
        return switch (taxRate) {
            case 0 -> "Low";
            case 1 -> "Normal";
            case 2 -> "High";
            case 3 -> "Very High";
            default -> "Normal";
        };
    }
}
