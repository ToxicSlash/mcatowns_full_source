package com.example.mcatowns.town;

import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public final class TownTradeSystem {
    private TownTradeSystem() {
    }

    /**
     * One spatial index per simulation cycle. Positions are immutable for that
     * cycle; eligibility is read from live saved data, so a town refreshing
     * its trading posts mid-cycle is immediately reflected in later queries.
     */
    public static final class LinkIndex {
        private final long cellSize;
        private final double rangeSquared;
        private final Map<Cell, List<TownContext>> cells = new HashMap<>();
        private final Map<String, TownSavedData> data;

        public LinkIndex(List<TownContext> contexts, Map<String, TownSavedData> data, int linkRange) {
            this.cellSize = Math.max(1L, Math.abs((long) linkRange));
            this.rangeSquared = (double) linkRange * linkRange;
            this.data = data;
            for (TownContext town : contexts) {
                cells.computeIfAbsent(cell(town.center()), ignored -> new ArrayList<>()).add(town);
            }
        }

        private Cell cell(BlockPos pos) {
            return new Cell(Math.floorDiv((long) pos.getX(), cellSize), Math.floorDiv((long) pos.getZ(), cellSize));
        }

        public boolean hasLink(TownContext source, TownSavedData sourceData) {
            if (sourceData.getDetectedTradingPostBuildings() <= 0) return false;
            Cell origin = cell(source.center());
            for (long x = origin.x() - 1; x <= origin.x() + 1; x++) {
                for (long z = origin.z() - 1; z <= origin.z() + 1; z++) {
                    List<TownContext> nearby = cells.get(new Cell(x, z));
                    if (nearby == null) continue;
                    for (TownContext other : nearby) {
                        if (source.townId().equals(other.townId())) continue;
                        TownSavedData target = data.get(other.townId());
                        if (target != null && target.getDetectedTradingPostBuildings() > 0
                                && source.center().getSquaredDistance(other.center()) <= rangeSquared) return true;
                    }
                }
            }
            return false;
        }
        private record Cell(long x, long z) { }
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
        double rangeSq = (double) linkRange * linkRange;
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
