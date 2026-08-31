package com.example.mcatowns.config.screen;

import com.example.mcatowns.config.MCATownsConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class MCATownsConfigScreen {
    private MCATownsConfigScreen() {
    }

    public static Screen build(Screen parent) {
        MCATownsConfig current = MCATownsConfig.get();
        MCATownsConfig draft = copyOf(current);

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("MCA Towns Config"));

        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
        general.addEntry(entries.startIntField(Text.literal("Tax Collection Days"), draft.taxCollectionDays)
                .setDefaultValue(7)
                .setMin(1)
                .setMax(30)
                .setSaveConsumer(v -> draft.taxCollectionDays = v)
                .build());
        general.addEntry(entries.startIntField(Text.literal("Stats Refresh Days"), draft.statsRefreshDays)
                .setDefaultValue(1)
                .setMin(1)
                .setMax(30)
                .setSaveConsumer(v -> draft.statsRefreshDays = v)
                .build());
        general.addEntry(entries.startBooleanToggle(Text.literal("Require Mayor Rank"), draft.requireMayorRank)
                .setDefaultValue(true)
                .setSaveConsumer(v -> draft.requireMayorRank = v)
                .build());
        general.addEntry(entries.startIntField(Text.literal("MCA Tax Contribution %"), draft.mcaTaxContributionPercent)
                .setDefaultValue(100)
                .setMin(0)
                .setMax(300)
                .setSaveConsumer(v -> draft.mcaTaxContributionPercent = v)
                .build());
        general.addEntry(entries.startIntField(Text.literal("MCA Village Search Margin"), draft.mcaVillageSearchMargin)
                .setDefaultValue(48)
                .setMin(16)
                .setMax(256)
                .setSaveConsumer(v -> draft.mcaVillageSearchMargin = v)
                .build());
        general.addEntry(entries.startBooleanToggle(Text.literal("Require Strict MCA Town Center"), draft.requireStrictTownCenter)
                .setDefaultValue(false)
                .setSaveConsumer(v -> draft.requireStrictTownCenter = v)
                .build());

        ConfigCategory progression = builder.getOrCreateCategory(Text.literal("Town Progression"));
        progression.addEntry(entries.startIntField(Text.literal("Founded Town Starting Prosperity"), draft.foundedTownStartingProsperity)
                .setDefaultValue(3)
                .setMin(0)
                .setMax(15)
                .setSaveConsumer(v -> draft.foundedTownStartingProsperity = v)
                .build());
        progression.addEntry(entries.startIntField(Text.literal("Prosperity Decay Per Day"), draft.prosperityDecayPerDay)
                .setDefaultValue(1)
                .setMin(0)
                .setMax(10)
                .setSaveConsumer(v -> draft.prosperityDecayPerDay = v)
                .build());
        progression.addEntry(entries.startStrField(Text.literal("Currency Item ID"), draft.currencyItemId)
                .setDefaultValue("minecraft:emerald")
                .setSaveConsumer(v -> draft.currencyItemId = v)
                .build());
        progression.addEntry(entries.startStrField(Text.literal("Great Essence Item ID (optional)"), draft.greatEssenceItemId)
                .setDefaultValue("")
                .setSaveConsumer(v -> draft.greatEssenceItemId = v)
                .build());
        progression.addEntry(entries.startIntField(Text.literal("Resident Friendship Hearts"), draft.residentFriendshipHearts)
                .setDefaultValue(50).setMin(0).setMax(1000)
                .setSaveConsumer(v -> draft.residentFriendshipHearts = v).build());
        progression.addEntry(entries.startIntField(Text.literal("Resident Recruitment Cost"), draft.residentRecruitmentCost)
                .setDefaultValue(4).setMin(0).setMax(64)
                .setSaveConsumer(v -> draft.residentRecruitmentCost = v).build());
        progression.addEntry(entries.startIntField(Text.literal("Specialist Chance %"), draft.specialistChancePercent)
                .setDefaultValue(3).setMin(0).setMax(100)
                .setSaveConsumer(v -> draft.specialistChancePercent = v).build());
        progression.addEntry(entries.startIntField(Text.literal("Specialist Recruitment Cost"), draft.specialistRecruitmentCost)
                .setDefaultValue(12).setMin(0).setMax(128)
                .setSaveConsumer(v -> draft.specialistRecruitmentCost = v).build());
        progression.addEntry(entries.startIntField(Text.literal("Food Per Resident Per Day"), draft.foodPerResidentPerDay)
                .setDefaultValue(1).setMin(0).setMax(10)
                .setSaveConsumer(v -> draft.foodPerResidentPerDay = v).build());

        ConfigCategory simulation = builder.getOrCreateCategory(Text.literal("Simulation"));
        simulation.addEntry(entries.startIntField(Text.literal("Addon Building Scan Radius"), draft.addonBuildingScanRadius)
                .setDefaultValue(96)
                .setMin(16)
                .setMax(256)
                .setSaveConsumer(v -> draft.addonBuildingScanRadius = v)
                .build());
        simulation.addEntry(entries.startBooleanToggle(Text.literal("Require Addon Blocks In Building"), draft.requireAddonBlocksInBuilding)
                .setDefaultValue(true)
                .setSaveConsumer(v -> draft.requireAddonBlocksInBuilding = v)
                .build());
        simulation.addEntry(entries.startIntField(Text.literal("Guard Buff Radius"), draft.guardBuffRadius)
                .setDefaultValue(96)
                .setMin(16)
                .setMax(256)
                .setSaveConsumer(v -> draft.guardBuffRadius = v)
                .build());
        simulation.addEntry(entries.startIntField(Text.literal("Raid Unrest Cooldown Ticks"), draft.raidUnrestCooldownTicks)
                .setDefaultValue(1200)
                .setMin(20)
                .setMax(12000)
                .setSaveConsumer(v -> draft.raidUnrestCooldownTicks = v)
                .build());
        simulation.addEntry(entries.startIntField(Text.literal("Death Happiness Penalty"), draft.deathHappinessPenalty)
                .setDefaultValue(6)
                .setMin(0)
                .setMax(50)
                .setSaveConsumer(v -> draft.deathHappinessPenalty = v)
                .build());
        simulation.addEntry(entries.startIntField(Text.literal("Festival Cooldown Days"), draft.festivalCooldownDays)
                .setDefaultValue(3)
                .setMin(0)
                .setMax(30)
                .setSaveConsumer(v -> draft.festivalCooldownDays = v)
                .build());

        ConfigCategory villagerBusiness = builder.getOrCreateCategory(Text.literal("Villager Business"));
        villagerBusiness.addEntry(entries.startIntField(Text.literal("Store Visit Tick Rate"), draft.storeVisitTickRate)
                .setDefaultValue(600)
                .setMin(20)
                .setMax(24000)
                .setSaveConsumer(v -> draft.storeVisitTickRate = v)
                .build());
        villagerBusiness.addEntry(entries.startIntField(Text.literal("Villager Business Radius"), draft.villagerBusinessRadius)
                .setDefaultValue(64)
                .setMin(16)
                .setMax(256)
                .setSaveConsumer(v -> draft.villagerBusinessRadius = v)
                .build());
        villagerBusiness.addEntry(entries.startIntField(Text.literal("Max Assignments Per Cycle"), draft.villagerBusinessMaxAssignmentsPerCycle)
                .setDefaultValue(4)
                .setMin(1)
                .setMax(64)
                .setSaveConsumer(v -> draft.villagerBusinessMaxAssignmentsPerCycle = v)
                .build());
        villagerBusiness.addEntry(entries.startIntField(Text.literal("Min Visit Cooldown Ticks"), draft.villagerBusinessMinCooldownTicks)
                .setDefaultValue(600)
                .setMin(100)
                .setMax(48000)
                .setSaveConsumer(v -> draft.villagerBusinessMinCooldownTicks = v)
                .build());
        villagerBusiness.addEntry(entries.startIntField(Text.literal("Max Visit Cooldown Ticks"), draft.villagerBusinessMaxCooldownTicks)
                .setDefaultValue(1200)
                .setMin(100)
                .setMax(48000)
                .setSaveConsumer(v -> draft.villagerBusinessMaxCooldownTicks = v)
                .build());

        builder.setSavingRunnable(() -> MCATownsConfig.setAndSave(draft));
        return builder.build();
    }

    private static MCATownsConfig copyOf(MCATownsConfig src) {
        MCATownsConfig copy = new MCATownsConfig();
        copy.taxCollectionDays = src.taxCollectionDays;
        copy.statsRefreshDays = src.statsRefreshDays;
        copy.requireMayorRank = src.requireMayorRank;
        copy.storeVisitTickRate = src.storeVisitTickRate;
        copy.requireStrictTownCenter = src.requireStrictTownCenter;
        copy.mcaVillageSearchMargin = src.mcaVillageSearchMargin;
        copy.addonBuildingScanRadius = src.addonBuildingScanRadius;
        copy.requireAddonBlocksInBuilding = src.requireAddonBlocksInBuilding;
        copy.guardBuffRadius = src.guardBuffRadius;
        copy.villagerBusinessRadius = src.villagerBusinessRadius;
        copy.villagerBusinessMaxAssignmentsPerCycle = src.villagerBusinessMaxAssignmentsPerCycle;
        copy.villagerBusinessMinCooldownTicks = src.villagerBusinessMinCooldownTicks;
        copy.villagerBusinessMaxCooldownTicks = src.villagerBusinessMaxCooldownTicks;
        copy.raidUnrestCooldownTicks = src.raidUnrestCooldownTicks;
        copy.deathHappinessPenalty = src.deathHappinessPenalty;
        copy.festivalCooldownDays = src.festivalCooldownDays;
        copy.mcaTaxContributionPercent = src.mcaTaxContributionPercent;
        copy.foundedTownStartingProsperity = src.foundedTownStartingProsperity;
        copy.prosperityDecayPerDay = src.prosperityDecayPerDay;
        copy.currencyItemId = src.currencyItemId;
        copy.greatEssenceItemId = src.greatEssenceItemId;
        copy.residentFriendshipHearts = src.residentFriendshipHearts;
        copy.residentRecruitmentCost = src.residentRecruitmentCost;
        copy.specialistChancePercent = src.specialistChancePercent;
        copy.specialistRecruitmentCost = src.specialistRecruitmentCost;
        copy.foodPerResidentPerDay = src.foodPerResidentPerDay;
        return copy;
    }
}
