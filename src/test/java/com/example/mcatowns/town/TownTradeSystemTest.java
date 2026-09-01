package com.example.mcatowns.town;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownTradeSystemTest {
    @Test
    void linksOnlyTwoEligibleTownsWithinRange() {
        TownContext source = new TownContext("source", BlockPos.ORIGIN, BlockPos.ORIGIN, "player_created", false);
        TownContext nearby = new TownContext("nearby", new BlockPos(100, 64, 0), new BlockPos(100, 64, 0), "player_created", false);
        TownSavedData sourceData = TownSavedData.create();
        TownSavedData nearbyData = TownSavedData.create();
        sourceData.setDetectedTradingPostBuildings(1);
        nearbyData.setDetectedTradingPostBuildings(1);

        List<TownContext> contexts = List.of(source, nearby);
        Map<String, TownSavedData> data = Map.of("source", sourceData, "nearby", nearbyData);

        assertTrue(TownTradeSystem.hasLinkedTradingPost(source, sourceData, contexts, data, 128));
        assertFalse(TownTradeSystem.hasLinkedTradingPost(source, sourceData, contexts, data, 64));

        nearbyData.setDetectedTradingPostBuildings(0);
        assertFalse(TownTradeSystem.hasLinkedTradingPost(source, sourceData, contexts, data, 128));
    }

    @Test
    void sourceWithoutTradingPostNeverLinks() {
        TownContext source = new TownContext("source", BlockPos.ORIGIN, BlockPos.ORIGIN, "player_created", false);
        TownSavedData sourceData = TownSavedData.create();
        assertFalse(TownTradeSystem.hasLinkedTradingPost(source, sourceData, List.of(source), Map.of("source", sourceData), 512));
    }
}
