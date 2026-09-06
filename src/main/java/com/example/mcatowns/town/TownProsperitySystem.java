package com.example.mcatowns.town;

/**
 * Small prosperity helper that keeps permanent civic Prosperity Base separate from temporary suppression.
 * Threat systems can supply a suppression value later without rewriting the stored civic base.
 */
public final class TownProsperitySystem {
    private TownProsperitySystem() { }

    public static int effectiveBase(TownSavedData data) {
        return effectiveBase(data.getProsperityBase(), 0);
    }

    public static int effectiveBase(int prosperityBase, int temporarySuppression) {
        return Math.max(0, Math.max(0, prosperityBase) - Math.max(0, temporarySuppression));
    }
}
