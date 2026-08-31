package com.example.mcatowns.town;

import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;

public final class TownTradeSystem {
    private TownTradeSystem() {
    }

    public static boolean hasLinkedTradingPost(
            TownContext source,
            TownSavedData sourceData,
            List<TownContext> allContexts,
            Map<String, TownSavedData> allData,
            int linkRange
    ) {
        if (sourceData.getDetectedTradingPostBuildings() <= 0) {
            return false;
        }
        int rangeSq = linkRange * linkRange;
        BlockPos sourceAnchor = source.center();
        for (TownContext other : allContexts) {
            if (other.townId().equals(source.townId())) {
                continue;
            }
            TownSavedData otherData = allData.get(other.townId());
            if (otherData == null || otherData.getDetectedTradingPostBuildings() <= 0) {
                continue;
            }
            if (sourceAnchor.getSquaredDistance(other.center()) <= rangeSq) {
                return true;
            }
        }
        return false;
    }
}
