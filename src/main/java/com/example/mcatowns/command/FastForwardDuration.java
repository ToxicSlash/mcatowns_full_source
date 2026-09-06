package com.example.mcatowns.command;

import java.util.Locale;

/** Parses short Minecraft-time durations used by the /fastforward test command. */
public final class FastForwardDuration {
    public static final long TICKS_PER_DAY = 24_000L;
    public static final long MAX_TICKS = 365L * TICKS_PER_DAY;

    private FastForwardDuration() { }

    public static long parseTicks(String input) {
        if (input == null) throw new IllegalArgumentException("Duration is required");
        String value = input.trim().toLowerCase(Locale.ROOT);
        if (value.length() < 2) throw new IllegalArgumentException("Use a duration such as 10d or 24000t");

        char suffix = value.charAt(value.length() - 1);
        long multiplier;
        if (suffix == 'd') {
            multiplier = TICKS_PER_DAY;
        } else if (suffix == 't') {
            multiplier = 1L;
        } else {
            throw new IllegalArgumentException("Duration must end in d (days) or t (ticks)");
        }

        String number = value.substring(0, value.length() - 1);
        if (number.isEmpty() || !number.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Duration must be a positive whole number, such as 10d or 24000t");
        }

        final long amount;
        final long ticks;
        try {
            amount = Long.parseLong(number);
            ticks = Math.multiplyExact(amount, multiplier);
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("Duration is too large");
        }

        if (amount <= 0L) throw new IllegalArgumentException("Duration must be greater than zero");
        if (ticks > MAX_TICKS) {
            throw new IllegalArgumentException("Duration is capped at 365d for safety");
        }
        return ticks;
    }

    public static String display(long ticks) {
        if (ticks > 0L && ticks % TICKS_PER_DAY == 0L) {
            return (ticks / TICKS_PER_DAY) + "d";
        }
        return ticks + "t";
    }
}
