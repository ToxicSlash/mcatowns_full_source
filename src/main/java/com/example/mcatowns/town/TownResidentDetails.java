package com.example.mcatowns.town;

import com.example.mcatowns.integration.GuardVillagersIntegration;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Resolves small, display-only resident details when the town UI is opened. This never scans the world and does not
 * persist transient entity state; unloaded residents simply report unavailable details.
 */
public final class TownResidentDetails {
    private TownResidentDetails() { }

    public static boolean isGuard(Entity entity) {
        return entity instanceof LivingEntity living && GuardVillagersIntegration.isGuardEntity(living);
    }

    public static String occupation(Entity entity, String specialistType) {
        if (specialistType != null && !specialistType.isBlank()) return titleCase(specialistType);
        if (isGuard(entity)) return "Guard";
        if (entity instanceof VillagerEntity villager) {
            var profession = villager.getVillagerData().getProfession();
            String path = Registries.VILLAGER_PROFESSION.getId(profession).getPath();
            if (!"none".equals(path) && !"nitwit".equals(path)) return titleCase(path);
        }
        return "Resident";
    }

    /** Returns 0-100 for MCA mood when available, otherwise -1. */
    public static int happiness(Entity entity) {
        if (entity == null) return -1;
        try {
            Method brainGetter = entity.getClass().getMethod("getVillagerBrain");
            Object brain = brainGetter.invoke(entity);
            Method moodGetter = brain.getClass().getMethod("getMoodValue");
            Object value = moodGetter.invoke(brain);
            if (value instanceof Number number) {
                return Math.max(0, Math.min(100, 50 + number.intValue()));
            }
        } catch (ReflectiveOperationException ignored) {
            // Non-MCA residents such as Guard Villagers do not expose MCA mood.
        }
        return -1;
    }

    public static String status(Entity entity, RegisteredTownBuilding workplace) {
        if (isGuard(entity)) return workplace == null ? "On Patrol" : "Working";
        return workplace == null ? "Idle" : "Working";
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) return "";
        String text = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
