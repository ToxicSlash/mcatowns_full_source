package com.example.mcatowns.town;

/** Contiguous, balanced slices retain the existing town order within each cycle. */
public final class TownTickSchedule {
    public static final int SLICES = 20;
    private TownTickSchedule() { }

    public static Slice slice(int towns, int phase) {
        if (towns < 0 || phase < 0 || phase >= SLICES) throw new IllegalArgumentException("Invalid town cycle");
        int from = (int) (((long) towns * phase + SLICES - 1) / SLICES);
        int to = (int) (((long) towns * (phase + 1) + SLICES - 1) / SLICES);
        return new Slice(from, to);
    }

    public record Slice(int from, int to) { }
}
