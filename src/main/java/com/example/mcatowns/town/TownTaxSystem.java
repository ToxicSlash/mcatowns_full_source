package com.example.mcatowns.town;

import com.example.mcatowns.config.MCATownsConfig;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TownTaxSystem {
    public static int calculateWeeklyTaxes(TownSavedData data, TownBuildingSnapshot snapshot, int rankValue) {
        int effectivePopulation = Math.max(0, data.getPopulation() + data.getRefugeePopulationBonus() + data.getCaravanPopulationBonus());
        int populationValue = effectivePopulation * 2;
        int happinessValue = data.getHappiness() / 5;
        int buildingValue = snapshot.totalBuildings() * 3;
        int mcaNormalTaxValue = data.getMcaNormalTaxIncome();
        int addonBaseValue = populationValue + happinessValue + buildingValue + rankValue;
        int weightedMcaTaxValue = mcaNormalTaxValue * MCATownsConfig.get().mcaTaxContributionPercent / 100;

        int itemTaxBonusPercent = snapshot.workshops() * 15;
        int generalTaxBonusPercent = snapshot.markets() * 20;
        if (data.isTradingPostLinked()) {
            itemTaxBonusPercent += 10;
            generalTaxBonusPercent += 15;
        }
        int businessBonusPercent = Math.min(25, Math.max(0, data.getBusinessActivityScore()) * 2);
        data.setBusinessTaxBonusPercent(businessBonusPercent);
        itemTaxBonusPercent += businessBonusPercent;
        generalTaxBonusPercent += businessBonusPercent;
        weightedMcaTaxValue = weightedMcaTaxValue * (100 + itemTaxBonusPercent) / 100;
        addonBaseValue = addonBaseValue * (100 + generalTaxBonusPercent) / 100;
        int commercePerformance = BuildingPerformance.averageOutputPercent(data,
                Set.of("inn", "storehouse", "bounty_board", "civic_office", "blacksmith", "jeweler"),
                data.getWorkforceEfficiencyPercent());
        weightedMcaTaxValue = weightedMcaTaxValue * commercePerformance / 100;
        addonBaseValue = addonBaseValue * commercePerformance / 100;
        weightedMcaTaxValue = weightedMcaTaxValue * Math.max(0, data.getEventTaxPercent()) / 100;
        addonBaseValue = addonBaseValue * Math.max(0, data.getEventTaxPercent()) / 100;
        weightedMcaTaxValue = weightedMcaTaxValue * Math.max(0, data.getEventWorkforcePercent()) / 100;
        addonBaseValue = addonBaseValue * Math.max(0, data.getEventWorkforcePercent()) / 100;

        int unrestPenalty = Math.min(40, data.getUnrest());
        int taxMultiplier = switch (data.getTaxRate()) {
            case 0 -> 75;
            case 1 -> 100;
            case 2 -> 125;
            case 3 -> 150;
            default -> 100;
        };

        data.setAddonTaxIncomeBase(Math.max(0, addonBaseValue));
        data.setWeightedMcaTaxIncome(Math.max(0, weightedMcaTaxValue));

        int addonAfterPenalty = addonBaseValue * (100 - unrestPenalty) / 100;
        int mcaAfterPenalty = weightedMcaTaxValue * (100 - unrestPenalty) / 100;
        if (data.hasTreasuryBuilding()) {
            addonAfterPenalty = addonAfterPenalty * 110 / 100;
            mcaAfterPenalty = mcaAfterPenalty * 110 / 100;
        }
        int addonFinal = Math.max(0, addonAfterPenalty * taxMultiplier / 100);
        int mcaFinal = Math.max(0, mcaAfterPenalty * taxMultiplier / 100);

        data.setWeeklyAddonTaxContribution(addonFinal);
        data.setWeeklyMcaTaxContribution(mcaFinal);
        return addonFinal + mcaFinal;
    }

    public static void collectWeeklyTaxes(TownSavedData data) {
        int total = Math.min(data.getMaxTreasury(), data.getTreasury() + data.getWeeklyTaxIncome());
        data.setTreasury(total);
    }

    public static List<ItemStack> applyWorkshopItemTaxBonus(List<ItemStack> stacks, TownBuildingSnapshot snapshot) {
        int bonusPercent = Math.max(0, snapshot.workshops()) * 15;
        if (bonusPercent <= 0 || stacks.isEmpty()) {
            return stacks;
        }

        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            int boostedCount = Math.max(1, stack.getCount() * (100 + bonusPercent) / 100);
            splitStack(out, stack, boostedCount);
        }
        return out;
    }

    private static void splitStack(List<ItemStack> out, ItemStack template, int count) {
        int remaining = Math.max(0, count);
        while (remaining > 0) {
            ItemStack copy = template.copy();
            int add = Math.min(remaining, copy.getMaxCount());
            copy.setCount(add);
            out.add(copy);
            remaining -= add;
        }
    }
}
