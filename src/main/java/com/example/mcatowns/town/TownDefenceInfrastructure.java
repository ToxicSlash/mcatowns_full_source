package com.example.mcatowns.town;

/**
 * Derived values for the first Defence infrastructure release. These values are calculated from registered
 * buildings so removing, disabling or upgrading infrastructure automatically changes the town's bonuses.
 */
public final class TownDefenceInfrastructure {
    public static final int BARRACKS_LIMIT = 3;
    public static final int WATCHTOWER_LIMIT = 10;
    public static final int OUTPOST_LIMIT = 10;
    public static final double ARMOUR_PER_DEFENCE = 1.5D;

    private TownDefenceInfrastructure() { }

    public static int defence(TownSavedData data) {
        int barracks = 0;
        int watchtowers = 0;
        int outposts = 0;
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            if (!contributes(building)) continue;
            switch (building.type()) {
                case "guard_post", "barracks" -> {
                    if (barracks < BARRACKS_LIMIT) barracks++;
                }
                case "watchtower" -> {
                    if (watchtowers < WATCHTOWER_LIMIT) watchtowers++;
                }
                case "outpost" -> {
                    if (outposts < OUTPOST_LIMIT) outposts++;
                }
                default -> { }
            }
        }
        return barracks + watchtowers * 2 + outposts * 2;
    }

    public static int guardCapacity(TownSavedData data) {
        int total = 0;
        int counted = 0;
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            if (!contributes(building)) continue;
            if (("guard_post".equals(building.type()) || "barracks".equals(building.type()))
                    && counted++ < BARRACKS_LIMIT) {
                total += 6 * clampTier(building.tier());
            }
        }
        return Math.max(0, total);
    }

    public static double guardArmourBonus(TownSavedData data) {
        return defence(data) * ARMOUR_PER_DEFENCE;
    }

    /** Barracks add +1 attack damage for every tier above T1. */
    public static double guardAttackBonus(TownSavedData data) {
        int total = 0;
        int counted = 0;
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            if (!contributes(building)) continue;
            if (("guard_post".equals(building.type()) || "barracks".equals(building.type()))
                    && counted++ < BARRACKS_LIMIT) {
                total += Math.max(0, clampTier(building.tier()) - 1);
            }
        }
        return total;
    }

    /** Watchtowers and Outposts add +2 max health for every tier above T1. */
    public static double guardHealthBonus(TownSavedData data) {
        int upgradedTiers = 0;
        int watchtowers = 0;
        int outposts = 0;
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            if (!contributes(building)) continue;
            if ("watchtower".equals(building.type()) && watchtowers++ < WATCHTOWER_LIMIT) {
                upgradedTiers += Math.max(0, clampTier(building.tier()) - 1);
            } else if ("outpost".equals(building.type()) && outposts++ < OUTPOST_LIMIT) {
                upgradedTiers += Math.max(0, clampTier(building.tier()) - 1);
            }
        }
        return upgradedTiers * 2.0D;
    }

    public static int maxPerTown(String buildingType) {
        if (buildingType == null) return Integer.MAX_VALUE;
        return switch (buildingType) {
            case "guard_post", "barracks" -> BARRACKS_LIMIT;
            case "watchtower" -> WATCHTOWER_LIMIT;
            case "outpost" -> OUTPOST_LIMIT;
            default -> Integer.MAX_VALUE;
        };
    }

    public static boolean canRegisterMore(TownSavedData data, String buildingType) {
        int limit = maxPerTown(buildingType);
        return limit == Integer.MAX_VALUE || countEquivalent(data, buildingType) < limit;
    }

    /**
     * Player-created Guard Villagers must satisfy both normal population capacity and Barracks Guard Capacity.
     * currentGuardCount should contain only guards already affiliated to this town.
     */
    public static boolean canAddGuard(TownSavedData data, int currentGuardCount) {
        return data.getResidents().size() < data.getPopulationCapacity()
                && Math.max(0, currentGuardCount) < guardCapacity(data);
    }

    /** Recalculates tier-aware residence capacity without scanning the world. */
    public static int populationCapacity(TownSavedData data) {
        int total = 0;
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            if (building.status() == BuildingStatus.INFRASTRUCTURE_BLOCKED) continue;
            TownBuildingDefinition definition = TownBuildingDefinition.get(building.type());
            if (definition != null) total += definition.populationCapacityForTier(building.tier());
        }
        return Math.min(data.getTownRank().maxOccupancy(), Math.max(0, total));
    }

    private static int countEquivalent(TownSavedData data, String buildingType) {
        if ("barracks".equals(buildingType) || "guard_post".equals(buildingType)) {
            return data.countBuildings("guard_post") + data.countBuildings("barracks");
        }
        return data.countBuildings(buildingType);
    }

    private static boolean contributes(RegisteredTownBuilding building) {
        return building != null && building.status() != BuildingStatus.INFRASTRUCTURE_BLOCKED;
    }

    private static int clampTier(int tier) {
        return Math.max(1, Math.min(3, tier));
    }
}
