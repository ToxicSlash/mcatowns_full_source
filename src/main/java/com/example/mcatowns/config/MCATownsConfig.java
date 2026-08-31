package com.example.mcatowns.config;

import com.example.mcatowns.MCATowns;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class MCATownsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mcatowns.json");

    private static MCATownsConfig INSTANCE = defaults();

    public int taxCollectionDays = 7;
    public int statsRefreshDays = 1;
    public boolean requireMayorRank = true;
    public boolean requireStrictTownCenter = false;
    public boolean requireAddonBlocksInBuilding = true;
    public int storeVisitTickRate = 600;
    public int mcaVillageSearchMargin = 48;
    public int addonBuildingScanRadius = 96;
    public int guardBuffRadius = 96;
    public int villagerBusinessRadius = 64;
    public int villagerBusinessMaxAssignmentsPerCycle = 4;
    public int villagerBusinessMinCooldownTicks = 600;
    public int villagerBusinessMaxCooldownTicks = 1200;
    public int tradingPostLinkRange = 512;
    public int randomEventMinDays = 7;
    public int randomEventMaxDays = 14;
    public int festivalCooldownDays = 3;
    public int raidUnrestCooldownTicks = 1200;
    public int deathHappinessPenalty = 6;
    public int mcaTaxContributionPercent = 100;
    public int foundedTownStartingProsperity = 3;
    public int prosperityDecayPerDay = 1;
    public String currencyItemId = "minecraft:emerald";
    public String greatEssenceItemId = "";
    public int residentFriendshipHearts = 50;
    public int residentRecruitmentCost = 4;
    public int specialistChancePercent = 3;
    public int specialistRecruitmentCost = 12;
    public int foodPerResidentPerDay = 1;

    public static MCATownsConfig get() {
        return INSTANCE;
    }

    public static void setAndSave(MCATownsConfig config) {
        INSTANCE = config != null ? config : defaults();
        INSTANCE.normalize();
        save();
    }

    public static void save() {
        try {
            if (Files.notExists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            MCATowns.LOGGER.error("Failed saving mcatowns config", e);
        }
    }

    public static void load() {
        try {
            if (Files.notExists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }

            if (Files.notExists(CONFIG_PATH)) {
                INSTANCE = defaults();
                try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                    GSON.toJson(INSTANCE, writer);
                }
                return;
            }

            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                MCATownsConfig loaded = GSON.fromJson(reader, MCATownsConfig.class);
                INSTANCE = loaded != null ? loaded : defaults();
                INSTANCE.normalize();
            }

            save();
        } catch (IOException | JsonParseException e) {
            INSTANCE = defaults();
            MCATowns.LOGGER.error("Failed loading mcatowns config, using defaults", e);
        }
    }

    private static MCATownsConfig defaults() {
        MCATownsConfig cfg = new MCATownsConfig();
        cfg.normalize();
        return cfg;
    }

    public MCATownsConfig() {
    }

    private void normalize() {
        taxCollectionDays = clamp(taxCollectionDays, 1, 365);
        statsRefreshDays = clamp(statsRefreshDays, 1, 30);
        storeVisitTickRate = clamp(storeVisitTickRate, 20, 72000);
        mcaVillageSearchMargin = clamp(mcaVillageSearchMargin, 16, 512);
        addonBuildingScanRadius = clamp(addonBuildingScanRadius, 16, 512);
        guardBuffRadius = clamp(guardBuffRadius, 16, 512);
        villagerBusinessRadius = clamp(villagerBusinessRadius, 16, 256);
        villagerBusinessMaxAssignmentsPerCycle = clamp(villagerBusinessMaxAssignmentsPerCycle, 1, 64);
        villagerBusinessMinCooldownTicks = clamp(villagerBusinessMinCooldownTicks, 100, 72000);
        villagerBusinessMaxCooldownTicks = clamp(villagerBusinessMaxCooldownTicks, villagerBusinessMinCooldownTicks, 144000);
        tradingPostLinkRange = clamp(tradingPostLinkRange, 64, 4096);
        randomEventMinDays = clamp(randomEventMinDays, 1, 365);
        randomEventMaxDays = clamp(randomEventMaxDays, randomEventMinDays, 730);
        festivalCooldownDays = clamp(festivalCooldownDays, 0, 365);
        raidUnrestCooldownTicks = clamp(raidUnrestCooldownTicks, 20, 72000);
        deathHappinessPenalty = clamp(deathHappinessPenalty, 0, 100);
        mcaTaxContributionPercent = clamp(mcaTaxContributionPercent, 0, 300);
        foundedTownStartingProsperity = clamp(foundedTownStartingProsperity, 0, 15);
        prosperityDecayPerDay = clamp(prosperityDecayPerDay, 0, 100);
        residentFriendshipHearts = clamp(residentFriendshipHearts, 0, 1000);
        residentRecruitmentCost = clamp(residentRecruitmentCost, 0, 1024);
        specialistChancePercent = clamp(specialistChancePercent, 0, 100);
        specialistRecruitmentCost = clamp(specialistRecruitmentCost, 0, 1024);
        foodPerResidentPerDay = clamp(foodPerResidentPerDay, 0, 100);
        if (currencyItemId == null || currencyItemId.isBlank()) currencyItemId = "minecraft:emerald";
        if (greatEssenceItemId == null) greatEssenceItemId = "";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
