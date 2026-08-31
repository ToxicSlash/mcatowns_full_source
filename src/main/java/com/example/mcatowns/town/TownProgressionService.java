package com.example.mcatowns.town;

import com.example.mcatowns.network.ModNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class TownProgressionService {
    private TownProgressionService() {
    }

    public static void advance(ServerPlayerEntity player, BlockPos bellPos) {
        TownContext town = PlayerTownRegistry.get(player.getServerWorld()).getTownAt(bellPos).orElse(null);
        if (town == null || !PlayerTownRegistry.get(player.getServerWorld()).isOwner(town.townId(), player)) return;
        TownSavedData data = TownSavedData.get(player.getServerWorld(), town.townId());
        TownRank next = data.getTownRank().next();
        if (next == null || !requirementsMet(data, next)) {
            player.sendMessage(Text.translatable("text.mcatowns.rank_requirements_missing"), true);
            return;
        }
        int tokenCost = upgradeTokenCost(next);
        if (!data.spendTownTokens(tokenCost)) {
            player.sendMessage(Text.translatable("text.mcatowns.need_town_tokens", tokenCost), true);
            return;
        }
        data.advanceTo(next);
        TownBuildingService.refreshDerivedValues(data);
        player.sendMessage(Text.translatable("text.mcatowns.town_upgraded", next.displayName()), false);
        TownManagerService.open(player, town, bellPos);
    }

    public static boolean requirementsMet(TownSavedData data, TownRank next) {
        if (data.getProsperity() < next.requiredProsperity()
                || data.getPopulation() < next.requiredPopulation()) return false;
        if (next == TownRank.CAMP) {
            return data.countBuildings("residence") >= 2
                    && data.hasBuilding("bounty_board")
                    && data.hasBuilding("civic_office")
                    && data.hasBuilding("campfire")
                    && data.hasBuilding("farm");
        }
        return data.getRegisteredBuildingCount() >= next.requiredBuildings();
    }

    public static List<String> checklist(TownSavedData data) {
        TownRank next = data.getTownRank().next();
        if (next == null) return List.of("Maximum rank reached");
        List<String> lines = new ArrayList<>();
        if (next == TownRank.CAMP) {
            add(lines, data.countBuildings("residence") >= 2, "2 Residences");
            add(lines, data.hasBuilding("civic_office"), "Civic Office");
            add(lines, data.hasBuilding("bounty_board"), "Bounty Board");
            add(lines, data.hasBuilding("campfire"), "Campfire");
            add(lines, data.hasBuilding("farm"), "Basic Food infrastructure");
        } else {
            add(lines, data.getRegisteredBuildingCount() >= next.requiredBuildings(),
                    "Buildings " + data.getRegisteredBuildingCount() + " / " + next.requiredBuildings());
            add(lines, data.getPopulation() >= next.requiredPopulation(),
                    "Population " + data.getPopulation() + " / " + next.requiredPopulation());
        }
        add(lines, data.getProsperity() >= next.requiredProsperity(),
                "Prosperity " + data.getProsperity() + " / " + next.requiredProsperity());
        int tokens = upgradeTokenCost(next);
        if (tokens > 0) add(lines, data.getTownTokens() >= tokens, "Town Tokens " + data.getTownTokens() + " / " + tokens);
        return lines;
    }

    public static int upgradeTokenCost(TownRank next) {
        return switch (next) {
            case UNRANKED, CAMP -> 0;
            case HAMLET -> 8;
            case VILLAGE -> 15;
            case TOWNSHIP -> 25;
            case TOWN -> 40;
        };
    }

    private static void add(List<String> lines, boolean complete, String text) {
        lines.add((complete ? "✓ " : "✗ ") + text);
    }
}
