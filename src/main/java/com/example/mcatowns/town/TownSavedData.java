package com.example.mcatowns.town;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.*;

public class TownSavedData extends PersistentState {
    private String townId = "default";
    private int treasury = 0;
    private int happiness = 50;
    private int foodReserves = 50;
    private int weeklyTaxIncome = 0;
    private int population = 0;
    private int jobless = 0;
    private int defenseRating = 0;
    private int unrest = 0;
    private int barracksLevel = 0;
    private int taxRate = 1;
    private int immigrationChance = 0;
    private int buildingBonusScore = 0;
    private int dailyFoodConsumed = 0;
    private int dailyFoodProduced = 0;
    private int detectedQuarryBuildings = 0;
    private int detectedWorkshopBuildings = 0;
    private int detectedMarketBuildings = 0;
    private int detectedTradingPostBuildings = 0;
    private int detectedCropFarmBuildings = 0;
    private int invalidCropFarmBuildings = 0;
    private int activeBakeryBuildings = 0;
    private int inactiveBakeryBuildings = 0;
    private int detectedButcherBuildings = 0;
    private int detectedFishermansHutBuildings = 0;
    private boolean tradingPostLinked = false;
    private int dailyQuarryYield = 0;
    private int dailyFoodPotential = 0;
    private String activeRandomEvent = "";
    private long activeRandomEventUntilDay = -1L;
    private long lastRandomEventDay = -1L;
    private long nextRandomEventDay = -1L;
    private int eventFarmOutputPercent = 100;
    private int eventQuarryOutputPercent = 100;
    private int eventTaxPercent = 100;
    private int eventWorkforcePercent = 100;
    private int eventDefenseBonus = 0;
    private int eventHappinessModifier = 0;
    private int eventExtraFoodDemand = 0;
    private int refugeePopulationBonus = 0;
    private int workforceAvailable = 0;
    private int workforceRequired = 0;
    private int workforceEfficiencyPercent = 100;
    private int businessActivityScore = 0;
    private int businessTaxBonusPercent = 0;
    private long lastCaravanDay = -1L;
    private long nextCaravanDay = -1L;
    private String lastCaravanType = "";
    private int caravanDefenseBonus = 0;
    private long caravanDefenseUntilDay = -1L;
    private int caravanPopulationBonus = 0;
    private long caravanPopulationUntilDay = -1L;
    private boolean hasTreasuryBuilding = false;
    private int detectedTreasuryBuildings = 0;
    private int detectedBarracksBuildings = 0;
    private int detectedArmoryBuildings = 0;
    private int detectedTownHallBuildings = 0;
    private String townName = "New Settlement";
    private BlockPos townCenter = BlockPos.ORIGIN;
    private TownRank townRank = TownRank.UNRANKED;
    private int prosperity = 0;
    private int prosperityBase = 0;
    private int townTokens = 0;
    private int specialistCount = 0;
    private int populationCapacity = 0;
    private int foodCapacity = 0;
    private final Map<Long, RegisteredTownBuilding> registeredBuildings = new LinkedHashMap<>();
    private final Set<String> researchUnlocks = new HashSet<>();
    private final Set<UUID> residents = new HashSet<>();
    private final Map<UUID, String> specialists = new HashMap<>();
    private long lastProgressionDay = -1L;
    private TownRequest activeRequest;
    private long nextRequestDay = 0L;
    private int bountyKills = 0;
    private int mcaNormalTaxIncome = 0;
    private int weightedMcaTaxIncome = 0;
    private int addonTaxIncomeBase = 0;
    private int weeklyAddonTaxContribution = 0;
    private int weeklyMcaTaxContribution = 0;
    private int pendingEmeraldPayout = 0;
    private int bonusSourceFlags = 0;
    private int lastRaidId = -1;
    private long lastRaidImpactTick = -9999L;
    private long lastTaxCollectionDay = 0L;
    private long lastStatsRefreshDay = 0L;
    private long lastFoodCycleDay = -1L;
    private long lastFestivalDay = -9999L;

    public static TownSavedData create() {
        return new TownSavedData();
    }

    public static TownSavedData fromNbt(NbtCompound nbt) {
        TownSavedData data = new TownSavedData();
        data.townId = nbt.getString("TownId");
        data.treasury = nbt.getInt("Treasury");
        data.happiness = nbt.getInt("Happiness");
        data.foodReserves = nbt.getInt("FoodReserves");
        data.weeklyTaxIncome = nbt.getInt("WeeklyTaxIncome");
        data.population = nbt.getInt("Population");
        data.jobless = nbt.getInt("Jobless");
        data.defenseRating = nbt.getInt("DefenseRating");
        data.unrest = nbt.getInt("Unrest");
        data.barracksLevel = nbt.getInt("BarracksLevel");
        data.taxRate = nbt.getInt("TaxRate");
        data.immigrationChance = nbt.getInt("ImmigrationChance");
        data.buildingBonusScore = nbt.getInt("BuildingBonusScore");
        data.dailyFoodConsumed = nbt.getInt("DailyFoodConsumed");
        data.dailyFoodProduced = nbt.getInt("DailyFoodProduced");
        data.detectedQuarryBuildings = nbt.getInt("DetectedQuarryBuildings");
        data.detectedWorkshopBuildings = nbt.getInt("DetectedWorkshopBuildings");
        data.detectedMarketBuildings = nbt.getInt("DetectedMarketBuildings");
        data.detectedTradingPostBuildings = nbt.getInt("DetectedTradingPostBuildings");
        data.detectedCropFarmBuildings = nbt.getInt("DetectedCropFarmBuildings");
        data.invalidCropFarmBuildings = nbt.getInt("InvalidCropFarmBuildings");
        data.activeBakeryBuildings = nbt.getInt("ActiveBakeryBuildings");
        data.inactiveBakeryBuildings = nbt.getInt("InactiveBakeryBuildings");
        data.detectedButcherBuildings = nbt.getInt("DetectedButcherBuildings");
        data.detectedFishermansHutBuildings = nbt.getInt("DetectedFishermansHutBuildings");
        data.tradingPostLinked = nbt.getBoolean("TradingPostLinked");
        data.dailyQuarryYield = nbt.getInt("DailyQuarryYield");
        data.dailyFoodPotential = nbt.getInt("DailyFoodPotential");
        data.activeRandomEvent = nbt.getString("ActiveRandomEvent");
        data.activeRandomEventUntilDay = nbt.contains("ActiveRandomEventUntilDay") ? nbt.getLong("ActiveRandomEventUntilDay") : -1L;
        data.lastRandomEventDay = nbt.contains("LastRandomEventDay") ? nbt.getLong("LastRandomEventDay") : -1L;
        data.nextRandomEventDay = nbt.contains("NextRandomEventDay") ? nbt.getLong("NextRandomEventDay") : -1L;
        data.eventFarmOutputPercent = nbt.contains("EventFarmOutputPercent") ? nbt.getInt("EventFarmOutputPercent") : 100;
        data.eventQuarryOutputPercent = nbt.contains("EventQuarryOutputPercent") ? nbt.getInt("EventQuarryOutputPercent") : 100;
        data.eventTaxPercent = nbt.contains("EventTaxPercent") ? nbt.getInt("EventTaxPercent") : 100;
        data.eventWorkforcePercent = nbt.contains("EventWorkforcePercent") ? nbt.getInt("EventWorkforcePercent") : 100;
        data.eventDefenseBonus = nbt.getInt("EventDefenseBonus");
        data.eventHappinessModifier = nbt.getInt("EventHappinessModifier");
        data.eventExtraFoodDemand = nbt.getInt("EventExtraFoodDemand");
        data.refugeePopulationBonus = nbt.getInt("RefugeePopulationBonus");
        data.workforceAvailable = nbt.getInt("WorkforceAvailable");
        data.workforceRequired = nbt.getInt("WorkforceRequired");
        data.workforceEfficiencyPercent = nbt.contains("WorkforceEfficiencyPercent") ? nbt.getInt("WorkforceEfficiencyPercent") : 100;
        data.businessActivityScore = nbt.getInt("BusinessActivityScore");
        data.businessTaxBonusPercent = nbt.getInt("BusinessTaxBonusPercent");
        data.lastCaravanDay = nbt.contains("LastCaravanDay") ? nbt.getLong("LastCaravanDay") : -1L;
        data.nextCaravanDay = nbt.contains("NextCaravanDay") ? nbt.getLong("NextCaravanDay") : -1L;
        data.lastCaravanType = nbt.getString("LastCaravanType");
        data.caravanDefenseBonus = nbt.getInt("CaravanDefenseBonus");
        data.caravanDefenseUntilDay = nbt.contains("CaravanDefenseUntilDay") ? nbt.getLong("CaravanDefenseUntilDay") : -1L;
        data.caravanPopulationBonus = nbt.getInt("CaravanPopulationBonus");
        data.caravanPopulationUntilDay = nbt.contains("CaravanPopulationUntilDay") ? nbt.getLong("CaravanPopulationUntilDay") : -1L;
        data.hasTreasuryBuilding = nbt.getBoolean("HasTreasuryBuilding");
        data.detectedTreasuryBuildings = nbt.getInt("DetectedTreasuryBuildings");
        data.detectedBarracksBuildings = nbt.getInt("DetectedBarracksBuildings");
        data.detectedArmoryBuildings = nbt.getInt("DetectedArmoryBuildings");
        data.detectedTownHallBuildings = nbt.getInt("DetectedTownHallBuildings");
        data.townName = nbt.contains("TownName") ? nbt.getString("TownName") : "New Settlement";
        data.townCenter = nbt.contains("TownCenter") ? BlockPos.fromLong(nbt.getLong("TownCenter")) : BlockPos.ORIGIN;
        // Saves made before progression ranks existed started at Camp.
        data.townRank = nbt.contains("TownRank") ? TownRank.fromName(nbt.getString("TownRank")) : TownRank.CAMP;
        data.prosperity = Math.max(0, Math.min(data.townRank.maxProsperity(), nbt.getInt("Prosperity")));
        int savedProsperityBase = nbt.contains("ProsperityBase")
                ? nbt.getInt("ProsperityBase") : nbt.getInt("ProsperityFloor");
        data.prosperityBase = Math.max(0, Math.min(data.townRank.maxProsperity(), savedProsperityBase));
        data.townTokens = Math.max(0, nbt.getInt("TownTokens"));
        data.specialistCount = Math.max(0, nbt.getInt("SpecialistCount"));
        for (long position : nbt.getLongArray("RegisteredBuildings")) {
            data.registeredBuildings.put(position, RegisteredTownBuilding.legacy("legacy", BlockPos.fromLong(position)));
        }
        NbtList buildings = nbt.getList("TypedBuildings", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < buildings.size(); i++) {
            NbtCompound building = buildings.getCompound(i);
            String type = building.getString("Type");
            if (!type.isBlank()) {
                BlockPos anchor = BlockPos.fromLong(building.getLong("Pos"));
                UUID id = building.containsUuid("Id") ? building.getUuid("Id") : UUID.randomUUID();
                int tier = building.contains("Tier") ? building.getInt("Tier") : 1;
                BlockPos min = building.contains("MinPos") ? BlockPos.fromLong(building.getLong("MinPos")) : anchor;
                BlockPos max = building.contains("MaxPos") ? BlockPos.fromLong(building.getLong("MaxPos")) : anchor;
                BuildingStatus status = BuildingStatus.fromName(building.getString("Status"));
                int quality = building.contains("Quality") ? building.getInt("Quality") : 50;
                List<UUID> workers = new ArrayList<>();
                NbtList workerList = building.getList("Workers", NbtElement.COMPOUND_TYPE);
                for (int workerIndex = 0; workerIndex < workerList.size(); workerIndex++) {
                    NbtCompound worker = workerList.getCompound(workerIndex);
                    if (worker.containsUuid("Id")) workers.add(worker.getUuid("Id"));
                }
                long inspected = building.contains("LastInspectionDay") ? building.getLong("LastInspectionDay") : -1L;
                int cropState = building.getInt("CropState");
                data.registeredBuildings.put(anchor.asLong(), new RegisteredTownBuilding(id, type, tier, anchor,
                        min, max, status, quality, workers, inspected, cropState));
            }
        }
        for (String unlock : nbt.getList("ResearchUnlocks", NbtElement.STRING_TYPE).stream()
                .map(NbtElement::asString).toList()) {
            data.researchUnlocks.add(unlock);
        }
        NbtList residents = nbt.getList("Residents", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < residents.size(); i++) {
            NbtCompound resident = residents.getCompound(i);
            if (resident.containsUuid("Id")) data.residents.add(resident.getUuid("Id"));
        }
        NbtList specialists = nbt.getList("TownSpecialists", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < specialists.size(); i++) {
            NbtCompound specialist = specialists.getCompound(i);
            if (specialist.containsUuid("Id") && !specialist.getString("Type").isBlank()) {
                data.specialists.put(specialist.getUuid("Id"), specialist.getString("Type"));
            }
        }
        data.specialistCount = data.specialists.isEmpty() ? data.specialistCount : data.specialists.size();
        data.populationCapacity = Math.max(0, nbt.getInt("PopulationCapacity"));
        data.foodCapacity = Math.max(0, nbt.getInt("FoodCapacity"));
        data.lastProgressionDay = nbt.contains("LastProgressionDay") ? nbt.getLong("LastProgressionDay") : -1L;
        data.nextRequestDay = nbt.getLong("NextRequestDay");
        data.bountyKills = Math.max(0, nbt.getInt("BountyKills"));
        if (nbt.contains("ActiveRequest", NbtElement.COMPOUND_TYPE)) {
            NbtCompound request = nbt.getCompound("ActiveRequest");
            Map<String, Integer> requirements = new LinkedHashMap<>();
            NbtList items = request.getList("Requirements", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < items.size(); i++) {
                NbtCompound item = items.getCompound(i);
                if (!item.getString("Item").isBlank()) requirements.put(item.getString("Item"), item.getInt("Count"));
            }
            try {
                data.activeRequest = new TownRequest(request.getString("Id"), request.getString("Name"),
                        TownRequest.Type.valueOf(request.getString("Type")), requirements, request.getLong("DueDay"),
                        request.getInt("ProsperityReward"), request.getInt("TokenReward"));
            } catch (IllegalArgumentException ignored) { }
        }
        data.mcaNormalTaxIncome = nbt.getInt("McaNormalTaxIncome");
        data.weightedMcaTaxIncome = nbt.getInt("WeightedMcaTaxIncome");
        data.addonTaxIncomeBase = nbt.getInt("AddonTaxIncomeBase");
        data.weeklyAddonTaxContribution = nbt.getInt("WeeklyAddonTaxContribution");
        data.weeklyMcaTaxContribution = nbt.getInt("WeeklyMcaTaxContribution");
        data.pendingEmeraldPayout = nbt.getInt("PendingEmeraldPayout");
        data.bonusSourceFlags = nbt.getInt("BonusSourceFlags");
        data.lastRaidId = nbt.contains("LastRaidId") ? nbt.getInt("LastRaidId") : -1;
        data.lastRaidImpactTick = nbt.contains("LastRaidImpactTick") ? nbt.getLong("LastRaidImpactTick") : -9999L;
        data.lastTaxCollectionDay = nbt.getLong("LastTaxCollectionDay");
        data.lastStatsRefreshDay = nbt.getLong("LastStatsRefreshDay");
        data.lastFoodCycleDay = nbt.contains("LastFoodCycleDay") ? nbt.getLong("LastFoodCycleDay") : -1L;
        data.lastFestivalDay = nbt.getLong("LastFestivalDay");
        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putString("TownId", townId);
        nbt.putInt("Treasury", treasury);
        nbt.putInt("Happiness", happiness);
        nbt.putInt("FoodReserves", foodReserves);
        nbt.putInt("WeeklyTaxIncome", weeklyTaxIncome);
        nbt.putInt("Population", population);
        nbt.putInt("Jobless", jobless);
        nbt.putInt("DefenseRating", defenseRating);
        nbt.putInt("Unrest", unrest);
        nbt.putInt("BarracksLevel", barracksLevel);
        nbt.putInt("TaxRate", taxRate);
        nbt.putInt("ImmigrationChance", immigrationChance);
        nbt.putInt("BuildingBonusScore", buildingBonusScore);
        nbt.putInt("DailyFoodConsumed", dailyFoodConsumed);
        nbt.putInt("DailyFoodProduced", dailyFoodProduced);
        nbt.putInt("DetectedQuarryBuildings", detectedQuarryBuildings);
        nbt.putInt("DetectedWorkshopBuildings", detectedWorkshopBuildings);
        nbt.putInt("DetectedMarketBuildings", detectedMarketBuildings);
        nbt.putInt("DetectedTradingPostBuildings", detectedTradingPostBuildings);
        nbt.putInt("DetectedCropFarmBuildings", detectedCropFarmBuildings);
        nbt.putInt("InvalidCropFarmBuildings", invalidCropFarmBuildings);
        nbt.putInt("ActiveBakeryBuildings", activeBakeryBuildings);
        nbt.putInt("InactiveBakeryBuildings", inactiveBakeryBuildings);
        nbt.putInt("DetectedButcherBuildings", detectedButcherBuildings);
        nbt.putInt("DetectedFishermansHutBuildings", detectedFishermansHutBuildings);
        nbt.putBoolean("TradingPostLinked", tradingPostLinked);
        nbt.putInt("DailyQuarryYield", dailyQuarryYield);
        nbt.putInt("DailyFoodPotential", dailyFoodPotential);
        nbt.putString("ActiveRandomEvent", activeRandomEvent);
        nbt.putLong("ActiveRandomEventUntilDay", activeRandomEventUntilDay);
        nbt.putLong("LastRandomEventDay", lastRandomEventDay);
        nbt.putLong("NextRandomEventDay", nextRandomEventDay);
        nbt.putInt("EventFarmOutputPercent", eventFarmOutputPercent);
        nbt.putInt("EventQuarryOutputPercent", eventQuarryOutputPercent);
        nbt.putInt("EventTaxPercent", eventTaxPercent);
        nbt.putInt("EventWorkforcePercent", eventWorkforcePercent);
        nbt.putInt("EventDefenseBonus", eventDefenseBonus);
        nbt.putInt("EventHappinessModifier", eventHappinessModifier);
        nbt.putInt("EventExtraFoodDemand", eventExtraFoodDemand);
        nbt.putInt("RefugeePopulationBonus", refugeePopulationBonus);
        nbt.putInt("WorkforceAvailable", workforceAvailable);
        nbt.putInt("WorkforceRequired", workforceRequired);
        nbt.putInt("WorkforceEfficiencyPercent", workforceEfficiencyPercent);
        nbt.putInt("BusinessActivityScore", businessActivityScore);
        nbt.putInt("BusinessTaxBonusPercent", businessTaxBonusPercent);
        nbt.putLong("LastCaravanDay", lastCaravanDay);
        nbt.putLong("NextCaravanDay", nextCaravanDay);
        nbt.putString("LastCaravanType", lastCaravanType);
        nbt.putInt("CaravanDefenseBonus", caravanDefenseBonus);
        nbt.putLong("CaravanDefenseUntilDay", caravanDefenseUntilDay);
        nbt.putInt("CaravanPopulationBonus", caravanPopulationBonus);
        nbt.putLong("CaravanPopulationUntilDay", caravanPopulationUntilDay);
        nbt.putBoolean("HasTreasuryBuilding", hasTreasuryBuilding);
        nbt.putInt("DetectedTreasuryBuildings", detectedTreasuryBuildings);
        nbt.putInt("DetectedBarracksBuildings", detectedBarracksBuildings);
        nbt.putInt("DetectedArmoryBuildings", detectedArmoryBuildings);
        nbt.putInt("DetectedTownHallBuildings", detectedTownHallBuildings);
        nbt.putString("TownName", townName);
        nbt.putLong("TownCenter", townCenter.asLong());
        nbt.putString("TownRank", townRank.name());
        nbt.putInt("Prosperity", prosperity);
        nbt.putInt("ProsperityBase", prosperityBase);
        nbt.putInt("TownTokens", townTokens);
        nbt.putInt("SpecialistCount", specialistCount);
        nbt.putLongArray("RegisteredBuildings", registeredBuildings.keySet().stream().mapToLong(Long::longValue).toArray());
        NbtList buildings = new NbtList();
        registeredBuildings.forEach((pos, registered) -> {
            NbtCompound building = new NbtCompound();
            building.putLong("Pos", pos);
            building.putUuid("Id", registered.id());
            building.putString("Type", registered.type());
            building.putInt("Tier", registered.tier());
            building.putLong("MinPos", registered.minPos().asLong());
            building.putLong("MaxPos", registered.maxPos().asLong());
            building.putString("Status", registered.status().name());
            building.putInt("Quality", registered.quality());
            building.put("Workers", writeUuids(registered.workers()));
            building.putLong("LastInspectionDay", registered.lastInspectionDay());
            building.putInt("CropState", registered.cropState());
            buildings.add(building);
        });
        nbt.put("TypedBuildings", buildings);
        NbtList unlocks = new NbtList();
        researchUnlocks.stream().sorted().forEach(unlock -> unlocks.add(net.minecraft.nbt.NbtString.of(unlock)));
        nbt.put("ResearchUnlocks", unlocks);
        nbt.put("Residents", writeUuids(residents));
        NbtList specialistList = new NbtList();
        specialists.forEach((id, type) -> {
            NbtCompound specialist = new NbtCompound();
            specialist.putUuid("Id", id);
            specialist.putString("Type", type);
            specialistList.add(specialist);
        });
        nbt.put("TownSpecialists", specialistList);
        nbt.putInt("PopulationCapacity", populationCapacity);
        nbt.putInt("FoodCapacity", foodCapacity);
        nbt.putLong("LastProgressionDay", lastProgressionDay);
        nbt.putLong("NextRequestDay", nextRequestDay);
        nbt.putInt("BountyKills", bountyKills);
        if (activeRequest != null) {
            NbtCompound request = new NbtCompound();
            request.putString("Id", activeRequest.id());
            request.putString("Name", activeRequest.name());
            request.putString("Type", activeRequest.type().name());
            request.putLong("DueDay", activeRequest.dueDay());
            request.putInt("ProsperityReward", activeRequest.prosperityReward());
            request.putInt("TokenReward", activeRequest.tokenReward());
            NbtList items = new NbtList();
            activeRequest.requirements().forEach((id, count) -> {
                NbtCompound item = new NbtCompound();
                item.putString("Item", id);
                item.putInt("Count", count);
                items.add(item);
            });
            request.put("Requirements", items);
            nbt.put("ActiveRequest", request);
        }
        nbt.putInt("McaNormalTaxIncome", mcaNormalTaxIncome);
        nbt.putInt("WeightedMcaTaxIncome", weightedMcaTaxIncome);
        nbt.putInt("AddonTaxIncomeBase", addonTaxIncomeBase);
        nbt.putInt("WeeklyAddonTaxContribution", weeklyAddonTaxContribution);
        nbt.putInt("WeeklyMcaTaxContribution", weeklyMcaTaxContribution);
        nbt.putInt("PendingEmeraldPayout", pendingEmeraldPayout);
        nbt.putInt("BonusSourceFlags", bonusSourceFlags);
        nbt.putInt("LastRaidId", lastRaidId);
        nbt.putLong("LastRaidImpactTick", lastRaidImpactTick);
        nbt.putLong("LastTaxCollectionDay", lastTaxCollectionDay);
        nbt.putLong("LastStatsRefreshDay", lastStatsRefreshDay);
        nbt.putLong("LastFoodCycleDay", lastFoodCycleDay);
        nbt.putLong("LastFestivalDay", lastFestivalDay);
        return nbt;
    }

    private static NbtList writeUuids(Collection<UUID> ids) {
        NbtList list = new NbtList();
        for (UUID id : ids) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("Id", id);
            list.add(entry);
        }
        return list;
    }

    public static TownSavedData get(ServerWorld world, String townId) {
        TownSavedData data = world.getPersistentStateManager().getOrCreate(
                TownSavedData::fromNbt,
                TownSavedData::create,
                "mcatowns_data_" + sanitizeTownId(townId)
        );
        data.setTownId(townId);
        return data;
    }

    private static String sanitizeTownId(String townId) {
        return townId.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
    }

    public String getTownId() { return townId; }
    public void setTownId(String townId) {
        if (!this.townId.equals(townId)) {
            this.townId = townId;
            markDirty();
        }
    }

    public int getTreasury() { return treasury; }
    public void setTreasury(int treasury) { this.treasury = treasury; markDirty(); }

    public int getHappiness() { return happiness; }
    public void setHappiness(int happiness) { this.happiness = happiness; markDirty(); }

    public int getFoodReserves() { return foodReserves; }
    public void setFoodReserves(int foodReserves) { this.foodReserves = foodReserves; markDirty(); }

    public int getWeeklyTaxIncome() { return weeklyTaxIncome; }
    public void setWeeklyTaxIncome(int weeklyTaxIncome) { this.weeklyTaxIncome = weeklyTaxIncome; markDirty(); }

    public int getPopulation() { return population; }
    public void setPopulation(int population) { this.population = population; markDirty(); }

    public int getJobless() { return jobless; }
    public void setJobless(int jobless) { this.jobless = jobless; markDirty(); }

    public int getDefenseRating() { return defenseRating; }
    public void setDefenseRating(int defenseRating) { this.defenseRating = defenseRating; markDirty(); }

    public int getUnrest() { return unrest; }
    public void setUnrest(int unrest) { this.unrest = unrest; markDirty(); }

    public int getBarracksLevel() { return barracksLevel; }
    public void setBarracksLevel(int barracksLevel) { this.barracksLevel = barracksLevel; markDirty(); }

    public int getTaxRate() { return taxRate; }
    public void setTaxRate(int taxRate) { this.taxRate = taxRate; markDirty(); }

    public int getImmigrationChance() { return immigrationChance; }
    public void setImmigrationChance(int immigrationChance) { this.immigrationChance = immigrationChance; markDirty(); }

    public int getBuildingBonusScore() { return buildingBonusScore; }
    public void setBuildingBonusScore(int buildingBonusScore) { this.buildingBonusScore = buildingBonusScore; markDirty(); }

    public int getDailyFoodConsumed() { return dailyFoodConsumed; }
    public void setDailyFoodConsumed(int dailyFoodConsumed) { this.dailyFoodConsumed = dailyFoodConsumed; markDirty(); }

    public int getDailyFoodProduced() { return dailyFoodProduced; }
    public void setDailyFoodProduced(int dailyFoodProduced) { this.dailyFoodProduced = dailyFoodProduced; markDirty(); }

    public int getDetectedQuarryBuildings() { return detectedQuarryBuildings; }
    public void setDetectedQuarryBuildings(int detectedQuarryBuildings) { this.detectedQuarryBuildings = detectedQuarryBuildings; markDirty(); }

    public int getDetectedWorkshopBuildings() { return detectedWorkshopBuildings; }
    public void setDetectedWorkshopBuildings(int detectedWorkshopBuildings) { this.detectedWorkshopBuildings = detectedWorkshopBuildings; markDirty(); }

    public int getDetectedMarketBuildings() { return detectedMarketBuildings; }
    public void setDetectedMarketBuildings(int detectedMarketBuildings) { this.detectedMarketBuildings = detectedMarketBuildings; markDirty(); }

    public int getDetectedTradingPostBuildings() { return detectedTradingPostBuildings; }
    public void setDetectedTradingPostBuildings(int detectedTradingPostBuildings) { this.detectedTradingPostBuildings = detectedTradingPostBuildings; markDirty(); }

    public int getDetectedCropFarmBuildings() { return detectedCropFarmBuildings; }
    public void setDetectedCropFarmBuildings(int detectedCropFarmBuildings) { this.detectedCropFarmBuildings = detectedCropFarmBuildings; markDirty(); }

    public int getInvalidCropFarmBuildings() { return invalidCropFarmBuildings; }
    public void setInvalidCropFarmBuildings(int invalidCropFarmBuildings) { this.invalidCropFarmBuildings = invalidCropFarmBuildings; markDirty(); }

    public int getActiveBakeryBuildings() { return activeBakeryBuildings; }
    public void setActiveBakeryBuildings(int activeBakeryBuildings) { this.activeBakeryBuildings = activeBakeryBuildings; markDirty(); }

    public int getInactiveBakeryBuildings() { return inactiveBakeryBuildings; }
    public void setInactiveBakeryBuildings(int inactiveBakeryBuildings) { this.inactiveBakeryBuildings = inactiveBakeryBuildings; markDirty(); }

    public int getDetectedButcherBuildings() { return detectedButcherBuildings; }
    public void setDetectedButcherBuildings(int detectedButcherBuildings) { this.detectedButcherBuildings = detectedButcherBuildings; markDirty(); }

    public int getDetectedFishermansHutBuildings() { return detectedFishermansHutBuildings; }
    public void setDetectedFishermansHutBuildings(int detectedFishermansHutBuildings) { this.detectedFishermansHutBuildings = detectedFishermansHutBuildings; markDirty(); }

    public boolean isTradingPostLinked() { return tradingPostLinked; }
    public void setTradingPostLinked(boolean tradingPostLinked) {
        if (this.tradingPostLinked != tradingPostLinked) {
            this.tradingPostLinked = tradingPostLinked;
            markDirty();
        }
    }

    public int getDailyQuarryYield() { return dailyQuarryYield; }
    public void setDailyQuarryYield(int dailyQuarryYield) { this.dailyQuarryYield = dailyQuarryYield; markDirty(); }

    public int getDailyFoodPotential() { return dailyFoodPotential; }
    public void setDailyFoodPotential(int dailyFoodPotential) { this.dailyFoodPotential = dailyFoodPotential; markDirty(); }

    public String getActiveRandomEvent() { return activeRandomEvent; }
    public void setActiveRandomEvent(String activeRandomEvent) { this.activeRandomEvent = activeRandomEvent; markDirty(); }

    public long getActiveRandomEventUntilDay() { return activeRandomEventUntilDay; }
    public void setActiveRandomEventUntilDay(long activeRandomEventUntilDay) { this.activeRandomEventUntilDay = activeRandomEventUntilDay; markDirty(); }

    public long getLastRandomEventDay() { return lastRandomEventDay; }
    public void setLastRandomEventDay(long lastRandomEventDay) { this.lastRandomEventDay = lastRandomEventDay; markDirty(); }

    public long getNextRandomEventDay() { return nextRandomEventDay; }
    public void setNextRandomEventDay(long nextRandomEventDay) { this.nextRandomEventDay = nextRandomEventDay; markDirty(); }

    public int getEventFarmOutputPercent() { return eventFarmOutputPercent; }
    public void setEventFarmOutputPercent(int eventFarmOutputPercent) { this.eventFarmOutputPercent = eventFarmOutputPercent; markDirty(); }

    public int getEventQuarryOutputPercent() { return eventQuarryOutputPercent; }
    public void setEventQuarryOutputPercent(int eventQuarryOutputPercent) { this.eventQuarryOutputPercent = eventQuarryOutputPercent; markDirty(); }

    public int getEventTaxPercent() { return eventTaxPercent; }
    public void setEventTaxPercent(int eventTaxPercent) { this.eventTaxPercent = eventTaxPercent; markDirty(); }

    public int getEventWorkforcePercent() { return eventWorkforcePercent; }
    public void setEventWorkforcePercent(int eventWorkforcePercent) { this.eventWorkforcePercent = eventWorkforcePercent; markDirty(); }

    public int getEventDefenseBonus() { return eventDefenseBonus; }
    public void setEventDefenseBonus(int eventDefenseBonus) { this.eventDefenseBonus = eventDefenseBonus; markDirty(); }

    public int getEventHappinessModifier() { return eventHappinessModifier; }
    public void setEventHappinessModifier(int eventHappinessModifier) { this.eventHappinessModifier = eventHappinessModifier; markDirty(); }

    public int getEventExtraFoodDemand() { return eventExtraFoodDemand; }
    public void setEventExtraFoodDemand(int eventExtraFoodDemand) { this.eventExtraFoodDemand = eventExtraFoodDemand; markDirty(); }

    public int getRefugeePopulationBonus() { return refugeePopulationBonus; }
    public void setRefugeePopulationBonus(int refugeePopulationBonus) { this.refugeePopulationBonus = refugeePopulationBonus; markDirty(); }

    public int getWorkforceAvailable() { return workforceAvailable; }
    public void setWorkforceAvailable(int workforceAvailable) { this.workforceAvailable = workforceAvailable; markDirty(); }

    public int getWorkforceRequired() { return workforceRequired; }
    public void setWorkforceRequired(int workforceRequired) { this.workforceRequired = workforceRequired; markDirty(); }

    public int getWorkforceEfficiencyPercent() { return workforceEfficiencyPercent; }
    public void setWorkforceEfficiencyPercent(int workforceEfficiencyPercent) { this.workforceEfficiencyPercent = workforceEfficiencyPercent; markDirty(); }

    public int getBusinessActivityScore() { return businessActivityScore; }
    public void setBusinessActivityScore(int businessActivityScore) {
        if (this.businessActivityScore != businessActivityScore) {
            this.businessActivityScore = businessActivityScore;
            markDirty();
        }
    }

    public int getBusinessTaxBonusPercent() { return businessTaxBonusPercent; }
    public void setBusinessTaxBonusPercent(int businessTaxBonusPercent) { this.businessTaxBonusPercent = businessTaxBonusPercent; markDirty(); }

    public long getLastCaravanDay() { return lastCaravanDay; }
    public void setLastCaravanDay(long lastCaravanDay) { this.lastCaravanDay = lastCaravanDay; markDirty(); }

    public long getNextCaravanDay() { return nextCaravanDay; }
    public void setNextCaravanDay(long nextCaravanDay) { this.nextCaravanDay = nextCaravanDay; markDirty(); }

    public String getLastCaravanType() { return lastCaravanType; }
    public void setLastCaravanType(String lastCaravanType) { this.lastCaravanType = lastCaravanType; markDirty(); }

    public int getCaravanDefenseBonus() { return caravanDefenseBonus; }
    public void setCaravanDefenseBonus(int caravanDefenseBonus) { this.caravanDefenseBonus = caravanDefenseBonus; markDirty(); }

    public long getCaravanDefenseUntilDay() { return caravanDefenseUntilDay; }
    public void setCaravanDefenseUntilDay(long caravanDefenseUntilDay) { this.caravanDefenseUntilDay = caravanDefenseUntilDay; markDirty(); }

    public int getCaravanPopulationBonus() { return caravanPopulationBonus; }
    public void setCaravanPopulationBonus(int caravanPopulationBonus) { this.caravanPopulationBonus = caravanPopulationBonus; markDirty(); }

    public long getCaravanPopulationUntilDay() { return caravanPopulationUntilDay; }
    public void setCaravanPopulationUntilDay(long caravanPopulationUntilDay) { this.caravanPopulationUntilDay = caravanPopulationUntilDay; markDirty(); }

    public boolean hasTreasuryBuilding() { return hasTreasuryBuilding; }
    public void setHasTreasuryBuilding(boolean hasTreasuryBuilding) { this.hasTreasuryBuilding = hasTreasuryBuilding; markDirty(); }

    public int getDetectedTreasuryBuildings() { return detectedTreasuryBuildings; }
    public void setDetectedTreasuryBuildings(int detectedTreasuryBuildings) { this.detectedTreasuryBuildings = detectedTreasuryBuildings; markDirty(); }

    public int getDetectedBarracksBuildings() { return detectedBarracksBuildings; }
    public void setDetectedBarracksBuildings(int detectedBarracksBuildings) { this.detectedBarracksBuildings = detectedBarracksBuildings; markDirty(); }

    public int getDetectedArmoryBuildings() { return detectedArmoryBuildings; }
    public void setDetectedArmoryBuildings(int detectedArmoryBuildings) { this.detectedArmoryBuildings = detectedArmoryBuildings; markDirty(); }

    public int getDetectedTownHallBuildings() { return detectedTownHallBuildings; }
    public void setDetectedTownHallBuildings(int detectedTownHallBuildings) { this.detectedTownHallBuildings = detectedTownHallBuildings; markDirty(); }

    public TownRank getTownRank() { return townRank; }
    public String getTownName() { return townName; }
    public BlockPos getTownCenter() { return townCenter; }
    public int getProsperity() { return prosperity; }
    public int getProsperityBase() { return prosperityBase; }
    public int getTownTokens() { return townTokens; }
    public int getSpecialistCount() { return specialistCount; }
    public int getRegisteredBuildingCount() { return registeredBuildings.size(); }
    public int getPopulationCapacity() { return populationCapacity; }
    public int getFoodCapacity() { return foodCapacity; }
    public long getLastProgressionDay() { return lastProgressionDay; }
    public TownRequest getActiveRequest() { return activeRequest; }
    public long getNextRequestDay() { return nextRequestDay; }
    public int getBountyKills() { return bountyKills; }
    public Set<String> getResearchUnlocks() { return Collections.unmodifiableSet(researchUnlocks); }
    public Set<UUID> getResidents() { return Collections.unmodifiableSet(residents); }
    public Map<UUID, String> getSpecialists() { return Collections.unmodifiableMap(specialists); }
    public List<RegisteredTownBuilding> getRegisteredBuildings() {
        return List.copyOf(registeredBuildings.values());
    }

    public int countBuildings(String type) {
        return (int) registeredBuildings.values().stream().filter(building -> type.equals(building.type())).count();
    }

    public boolean hasBuilding(String type) { return countBuildings(type) > 0; }

    public boolean isBuildingUnlocked(String type) {
        TownBuildingDefinition definition = TownBuildingDefinition.get(type);
        return definition != null && (definition.foundingKnowledge() || researchUnlocks.contains(type));
    }

    public boolean unlockBuilding(String type) {
        if (TownBuildingDefinition.get(type) == null || isBuildingUnlocked(type)) return false;
        boolean added = researchUnlocks.add(type);
        if (added) markDirty();
        return added;
    }

    public void setProgressionCapacities(int populationCapacity, int foodCapacity) {
        int populationValue = Math.max(0, populationCapacity);
        int foodValue = Math.max(0, foodCapacity);
        if (this.populationCapacity != populationValue || this.foodCapacity != foodValue) {
            this.populationCapacity = populationValue;
            this.foodCapacity = foodValue;
            if (foodReserves > foodValue) foodReserves = foodValue;
            markDirty();
        }
    }

    public void setLastProgressionDay(long day) {
        if (lastProgressionDay != day) {
            lastProgressionDay = day;
            markDirty();
        }
    }

    public void setActiveRequest(TownRequest request) {
        activeRequest = request;
        markDirty();
    }

    public void clearRequest(long nextDay) {
        activeRequest = null;
        nextRequestDay = Math.max(0, nextDay);
        markDirty();
    }

    public boolean recordBountyKill() {
        bountyKills++;
        if (bountyKills >= 30) {
            bountyKills = 0;
            addProsperity(3);
            addTownTokens(3);
            markDirty();
            return true;
        }
        markDirty();
        return false;
    }

    public boolean addResident(UUID id) {
        if (id == null || residents.size() >= populationCapacity) return false;
        boolean added = residents.add(id);
        if (added) markDirty();
        return added;
    }

    public boolean addSpecialist(UUID id, String type) {
        if (id == null || type == null || type.isBlank() || specialists.containsValue(type)
                || townRank == TownRank.UNRANKED && !"architect".equals(type)
                || specialists.size() >= townRank.maxSpecialists()) return false;
        specialists.put(id, type);
        specialistCount = specialists.size();
        residents.add(id);
        markDirty();
        return true;
    }

    public void removeResident(UUID id) {
        boolean changed = residents.remove(id);
        changed |= specialists.remove(id) != null;
        for (Map.Entry<Long, RegisteredTownBuilding> entry : registeredBuildings.entrySet()) {
            RegisteredTownBuilding building = entry.getValue();
            if (building.workers().contains(id)) {
                List<UUID> workers = new ArrayList<>(building.workers());
                workers.remove(id);
                entry.setValue(building.withWorkers(workers, BuildingStatus.UNDERSTAFFED));
                changed = true;
            }
        }
        if (changed) {
            specialistCount = specialists.size();
            population = residents.size();
            markDirty();
        }
    }

    public void addProsperity(int amount) {
        int value = Math.max(0, Math.min(townRank.maxProsperity(), prosperity + amount));
        if (value != prosperity) {
            prosperity = value;
            markDirty();
        }
    }

    public void setProsperityBase(int prosperityBase) {
        int value = Math.max(0, Math.min(townRank.maxProsperity(), prosperityBase));
        if (value != this.prosperityBase) {
            this.prosperityBase = value;
            markDirty();
        }
    }

    public void addTownTokens(int amount) {
        int value = Math.max(0, townTokens + amount);
        if (value != townTokens) {
            townTokens = value;
            markDirty();
        }
    }

    public boolean spendTownTokens(int amount) {
        if (amount < 0 || townTokens < amount) return false;
        if (amount > 0) {
            townTokens -= amount;
            markDirty();
        }
        return true;
    }

    public boolean advanceTo(TownRank rank) {
        if (rank == null || rank.ordinal() <= townRank.ordinal()) return false;
        townRank = rank;
        markDirty();
        return true;
    }

    public void setTownName(String townName) {
        String value = townName == null ? "" : townName.trim();
        if (value.isBlank()) value = "New Settlement";
        if (value.length() > 32) value = value.substring(0, 32);
        if (!value.equals(this.townName)) {
            this.townName = value;
            markDirty();
        }
    }

    public void setTownCenter(BlockPos townCenter) {
        if (townCenter != null && !townCenter.equals(this.townCenter)) {
            this.townCenter = townCenter.toImmutable();
            markDirty();
        }
    }

    public void initializeFoundedTown(int startingProsperity) {
        townRank = TownRank.UNRANKED;
        prosperity = Math.max(0, Math.min(townRank.maxProsperity(), startingProsperity));
        prosperityBase = 0;
        townTokens = 0;
        specialistCount = 0;
        registeredBuildings.clear();
        researchUnlocks.clear();
        residents.clear();
        specialists.clear();
        populationCapacity = 0;
        foodCapacity = 0;
        lastProgressionDay = -1L;
        activeRequest = null;
        nextRequestDay = 0L;
        bountyKills = 0;
        markDirty();
    }

    public void prepareForDeletion() {
        townName = "New Settlement";
        townCenter = BlockPos.ORIGIN;
        initializeFoundedTown(0);
        population = 0;
        foodReserves = 0;
        treasury = 0;
        markDirty();
    }

    public void setSpecialistCount(int specialistCount) {
        int value = Math.max(0, specialistCount);
        if (value != this.specialistCount) {
            this.specialistCount = value;
            markDirty();
        }
    }

    public boolean registerBuilding(String type, BlockPos pos) {
        return registerBuilding(RegisteredTownBuilding.legacy(type, pos));
    }

    public boolean registerBuilding(RegisteredTownBuilding building) {
        String type = building == null ? "" : building.type();
        BlockPos pos = building == null ? null : building.anchor();
        if (TownBuildingDefinition.get(type) == null || pos == null || registeredBuildings.containsKey(pos.asLong())) {
            return false;
        }
        if (registeredBuildings.size() >= townRank.maxBuildings()) {
            return false;
        }
        registeredBuildings.put(pos.asLong(), building);
        markDirty();
        return true;
    }

    public boolean replaceBuilding(RegisteredTownBuilding building) {
        if (building == null || !registeredBuildings.containsKey(building.anchor().asLong())) return false;
        registeredBuildings.put(building.anchor().asLong(), building);
        markDirty();
        return true;
    }

    public boolean unregisterBuilding(BlockPos pos) {
        boolean removed = registeredBuildings.remove(pos.asLong()) != null;
        if (removed) markDirty();
        return removed;
    }

    public int getInfrastructureProvided(InfrastructureType type) {
        return registeredBuildings.values().stream()
                .map(TownSavedData::definition)
                .filter(Objects::nonNull)
                .mapToInt(definition -> definition.providedInfrastructure().getOrDefault(type, 0))
                .sum();
    }

    public int getInfrastructureReserved(InfrastructureType type) {
        return registeredBuildings.values().stream()
                .map(TownSavedData::definition)
                .filter(Objects::nonNull)
                .mapToInt(definition -> definition.reservedInfrastructure().getOrDefault(type, 0))
                .sum();
    }

    public int getInfrastructureAvailable(InfrastructureType type) {
        return Math.max(0, getInfrastructureProvided(type) - getInfrastructureReserved(type));
    }

    private static TownBuildingDefinition definition(RegisteredTownBuilding building) {
        return TownBuildingDefinition.get(building.type());
    }

    public int getMcaNormalTaxIncome() { return mcaNormalTaxIncome; }
    public void setMcaNormalTaxIncome(int mcaNormalTaxIncome) { this.mcaNormalTaxIncome = mcaNormalTaxIncome; markDirty(); }

    public int getWeightedMcaTaxIncome() { return weightedMcaTaxIncome; }
    public void setWeightedMcaTaxIncome(int weightedMcaTaxIncome) { this.weightedMcaTaxIncome = weightedMcaTaxIncome; markDirty(); }

    public int getAddonTaxIncomeBase() { return addonTaxIncomeBase; }
    public void setAddonTaxIncomeBase(int addonTaxIncomeBase) { this.addonTaxIncomeBase = addonTaxIncomeBase; markDirty(); }

    public int getWeeklyAddonTaxContribution() { return weeklyAddonTaxContribution; }
    public void setWeeklyAddonTaxContribution(int weeklyAddonTaxContribution) { this.weeklyAddonTaxContribution = weeklyAddonTaxContribution; markDirty(); }

    public int getWeeklyMcaTaxContribution() { return weeklyMcaTaxContribution; }
    public void setWeeklyMcaTaxContribution(int weeklyMcaTaxContribution) { this.weeklyMcaTaxContribution = weeklyMcaTaxContribution; markDirty(); }

    public int getPendingEmeraldPayout() { return pendingEmeraldPayout; }
    public void setPendingEmeraldPayout(int pendingEmeraldPayout) { this.pendingEmeraldPayout = pendingEmeraldPayout; markDirty(); }

    public int getBonusSourceFlags() { return bonusSourceFlags; }
    public void setBonusSourceFlags(int bonusSourceFlags) { this.bonusSourceFlags = bonusSourceFlags; markDirty(); }

    public int getLastRaidId() { return lastRaidId; }
    public void setLastRaidId(int lastRaidId) { this.lastRaidId = lastRaidId; markDirty(); }

    public long getLastRaidImpactTick() { return lastRaidImpactTick; }
    public void setLastRaidImpactTick(long lastRaidImpactTick) { this.lastRaidImpactTick = lastRaidImpactTick; markDirty(); }

    public long getLastTaxCollectionDay() { return lastTaxCollectionDay; }
    public void setLastTaxCollectionDay(long day) { this.lastTaxCollectionDay = day; markDirty(); }

    public long getLastStatsRefreshDay() { return lastStatsRefreshDay; }
    public void setLastStatsRefreshDay(long day) { this.lastStatsRefreshDay = day; markDirty(); }

    public long getLastFoodCycleDay() { return lastFoodCycleDay; }
    public void setLastFoodCycleDay(long day) { this.lastFoodCycleDay = day; markDirty(); }

    public long getLastFestivalDay() { return lastFestivalDay; }
    public void setLastFestivalDay(long day) { this.lastFestivalDay = day; markDirty(); }

    public int getMaxTreasury() {
        return hasTreasuryBuilding ? 2500 : 1000;
    }
}
