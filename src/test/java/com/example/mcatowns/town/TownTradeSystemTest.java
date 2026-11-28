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
    void indexedLinksMatchOriginalAcrossBoundariesAndLiveChanges() {
        var rng = new java.util.Random(123);
        var towns = new java.util.ArrayList<TownContext>();
        var data = new java.util.HashMap<String, TownSavedData>();
        for (int i = 0; i < 240; i++) {
            BlockPos p = new BlockPos(rng.nextInt(4000) - 2000, rng.nextInt(320) - 64, rng.nextInt(4000) - 2000);
            String id = "town" + i;
            towns.add(new TownContext(id, p, p, "player_created", false));
            TownSavedData state = TownSavedData.create();
            state.setDetectedTradingPostBuildings(i % 3 == 0 ? 0 : 1);
            data.put(id, state);
        }
        for (int range : new int[]{0, 64, 128, 512, 50000}) {
            var index = new TownTradeSystem.LinkIndex(towns, data, range);
            for (TownContext town : towns) {
                var state = data.get(town.townId());
                org.junit.jupiter.api.Assertions.assertEquals(
                        TownTradeSystem.hasLinkedTradingPost(town, state, towns, data, range), index.hasLink(town, state));
                state.setDetectedTradingPostBuildings(state.getDetectedTradingPostBuildings() == 0 ? 1 : 0);
            }
        }
    }

    @Test
    void sourceWithoutTradingPostNeverLinks() {
        TownContext source = new TownContext("source", BlockPos.ORIGIN, BlockPos.ORIGIN, "player_created", false);
        TownSavedData sourceData = TownSavedData.create();
        assertFalse(TownTradeSystem.hasLinkedTradingPost(source, sourceData, List.of(source), Map.of("source", sourceData), 512));
    }
}
