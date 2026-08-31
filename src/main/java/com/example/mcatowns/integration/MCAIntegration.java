package com.example.mcatowns.integration;

import com.example.mcatowns.MCATowns;
import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.town.TownBuildingSnapshot;
import com.example.mcatowns.town.TownContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.OptionalInt;

public class MCAIntegration {
    private static final String MCA_ID = "mca";
    private static final String[] MCA_TOWN_CENTER_TYPES = {"town_center", "village_center", "townhall", "mcatowns_town_center"};
    private static final String[] MCA_CLASS_PREFIXES = {
            "net.mca.",
            "fabric.net.mca.",
            "quilt.net.mca.",
            "forge.net.mca."
    };

    public static boolean isMcaLoaded() {
        return FabricLoader.getInstance().isModLoaded(MCA_ID);
    }

    public static boolean hasMayorRank(PlayerEntity player) {
        if (!MCATownsConfig.get().requireMayorRank) {
            return true;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer) || !isMcaLoaded()) {
            return true;
        }

        try {
            Class<?> villageManagerClass = resolveMcaClass("server.world.data.VillageManager");
            Object manager = invokeStatic(villageManagerClass, "get", new Class<?>[]{ServerWorld.class}, serverPlayer.getServerWorld());
            Optional<?> villageOpt = castOptional(invoke(manager, "findNearestVillage", new Class<?>[]{net.minecraft.entity.Entity.class}, serverPlayer));
            if (villageOpt.isEmpty()) {
                return false;
            }

            Class<?> tasksClass = resolveMcaClass("resources.Tasks");
            Object rank = invokeStatic(tasksClass, "getRank", new Class<?>[]{resolveMcaClass("server.world.data.Village"), ServerPlayerEntity.class}, villageOpt.get(), serverPlayer);
            Class<?> rankClass = resolveMcaClass("resources.Rank");
            Object mayorRank = Enum.valueOf((Class<Enum>) rankClass.asSubclass(Enum.class), "MAYOR");
            return (boolean) invoke(rank, "isAtLeast", new Class<?>[]{rankClass}, mayorRank);
        } catch (Throwable t) {
            MCATowns.LOGGER.debug("Failed MCA mayor-rank lookup, falling back to allow", t);
            return true;
        }
    }

    public static int getHearts(ServerPlayerEntity player, Entity villager) {
        if (!isMcaLoaded()) return 0;
        try {
            Object brain = invoke(villager, "getVillagerBrain", new Class<?>[0]);
            Object memories = invoke(brain, "getMemoriesForPlayer", new Class<?>[]{PlayerEntity.class}, player);
            return (int) invoke(memories, "getHearts", new Class<?>[0]);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static boolean recruitResident(ServerPlayerEntity player, Entity villager, BlockPos townCenter) {
        try {
            Object residency = invoke(villager, "getResidency", new Class<?>[0]);
            invoke(residency, "leaveHome", new Class<?>[0]);
            villager.requestTeleport(townCenter.getX() + 1.5, townCenter.getY() + 1.0, townCenter.getZ() + 0.5);
            invoke(residency, "setHome", new Class<?>[]{ServerPlayerEntity.class}, player);
            return true;
        } catch (Throwable t) {
            MCATowns.LOGGER.debug("Could not assign MCA residency for {}", villager.getUuid(), t);
            return false;
        }
    }

    public static void modifyResidentMoods(ServerWorld world, Set<UUID> residents, int amount) {
        if (!isMcaLoaded() || amount == 0) return;
        for (UUID id : residents) {
            Entity entity = world.getEntity(id);
            if (entity == null) continue;
            try {
                Object brain = invoke(entity, "getVillagerBrain", new Class<?>[0]);
                invoke(brain, "modifyMoodValue", new Class<?>[]{int.class}, amount);
            } catch (Throwable ignored) { }
        }
    }

    public static OptionalInt averageResidentMood(ServerWorld world, Set<UUID> residents) {
        int total = 0;
        int count = 0;
        for (UUID id : residents) {
            Entity entity = world.getEntity(id);
            if (entity == null) continue;
            try {
                Object brain = invoke(entity, "getVillagerBrain", new Class<?>[0]);
                total += (int) invoke(brain, "getMoodValue", new Class<?>[0]);
                count++;
            } catch (Throwable ignored) { }
        }
        return count == 0 ? OptionalInt.empty() : OptionalInt.of(Math.max(0, Math.min(100, 50 + total / count)));
    }

    public static boolean hasMayorRank(PlayerEntity player, BlockPos pos) {
        if (!MCATownsConfig.get().requireMayorRank) {
            return true;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer) || !isMcaLoaded()) {
            return true;
        }

        try {
            Optional<?> village = findNearestVillage(serverPlayer.getServerWorld(), pos, 64);
            if (village.isEmpty()) {
                return false;
            }
            Class<?> tasksClass = resolveMcaClass("resources.Tasks");
            Object rank = invokeStatic(tasksClass, "getRank", new Class<?>[]{resolveMcaClass("server.world.data.Village"), ServerPlayerEntity.class}, village.get(), serverPlayer);
            Class<?> rankClass = resolveMcaClass("resources.Rank");
            Object mayorRank = Enum.valueOf((Class<Enum>) rankClass.asSubclass(Enum.class), "MAYOR");
            return (boolean) invoke(rank, "isAtLeast", new Class<?>[]{rankClass}, mayorRank);
        } catch (Throwable t) {
            MCATowns.LOGGER.debug("Failed MCA mayor-rank lookup for position {}", pos, t);
            return true;
        }
    }

    public static Optional<TownContext> findNearestTown(ServerWorld world, BlockPos pos, int margin) {
        if (!isMcaLoaded()) {
            return Optional.empty();
        }

        try {
            Optional<?> villageOpt = findNearestVillage(world, pos, margin);
            if (villageOpt.isEmpty()) {
                return Optional.empty();
            }
            Object village = villageOpt.get();
            int id = (int) invoke(village, "getId", new Class<?>[0]);
            Vec3i center = (Vec3i) invoke(village, "getCenter", new Class<?>[0]);
            boolean isVillage = (boolean) invoke(village, "isVillage", new Class<?>[0]);
            if (!isVillageEligibleForBinding(village, isVillage)) {
                return Optional.empty();
            }
            BlockPos anchor = new BlockPos(center.getX(), center.getY(), center.getZ());
            return Optional.of(new TownContext("mca_" + id, anchor, anchor, "mca", true));
        } catch (Throwable t) {
            MCATowns.LOGGER.debug("Failed MCA nearest-town lookup at {}", pos, t);
            return Optional.empty();
        }
    }

    /** Registers a point-of-interest building through MCA's own village manager. */
    public static Optional<TownContext> registerBuilding(ServerWorld world, BlockPos pos) {
        if (!isMcaLoaded()) return Optional.empty();
        try {
            Class<?> managerClass = resolveMcaClass("server.world.data.VillageManager");
            Object manager = invokeStatic(managerClass, "get", new Class<?>[]{ServerWorld.class}, world);
            invoke(manager, "processBuilding", new Class<?>[]{BlockPos.class}, pos);
            return findNearestTown(world, pos, 16);
        } catch (Throwable t) {
            MCATowns.LOGGER.warn("MCA could not register the Town Bell at {}", pos, t);
            return Optional.empty();
        }
    }

    public static void removeTown(ServerWorld world, String mcaTownId) {
        if (!isMcaLoaded() || mcaTownId == null || !mcaTownId.startsWith("mca_")) return;
        try {
            int id = Integer.parseInt(mcaTownId.substring(4));
            Class<?> managerClass = resolveMcaClass("server.world.data.VillageManager");
            Object manager = invokeStatic(managerClass, "get", new Class<?>[]{ServerWorld.class}, world);
            invoke(manager, "removeVillage", new Class<?>[]{int.class}, id);
        } catch (Throwable t) {
            MCATowns.LOGGER.warn("MCA could not remove linked town {}", mcaTownId, t);
        }
    }

    public static String getLinkDebug(ServerWorld world, BlockPos pos, int margin) {
        if (!isMcaLoaded()) {
            return "mcaLoaded=false";
        }
        try {
            Class<?> villageManagerClass = resolveMcaClass("server.world.data.VillageManager");
            Object manager = invokeStatic(villageManagerClass, "get", new Class<?>[]{ServerWorld.class}, world);
            int villages = 0;
            if (manager instanceof Iterable<?> iterable) {
                for (Object ignored : iterable) {
                    villages++;
                }
            }

            Optional<?> villageOpt = findNearestVillage(world, pos, margin);
            if (villageOpt.isEmpty()) {
                return "mcaLoaded=true strict=" + MCATownsConfig.get().requireStrictTownCenter
                        + " villages=" + villages + " nearestVillage=none";
            }

            Object village = villageOpt.get();
            int id = (int) invoke(village, "getId", new Class<?>[0]);
            Vec3i center = (Vec3i) invoke(village, "getCenter", new Class<?>[0]);
            boolean isVillage = (boolean) invoke(village, "isVillage", new Class<?>[0]);
            boolean hasCenter = hasAnyTownCenter(village);
            double dist = Math.sqrt(pos.getSquaredDistance(center.getX(), center.getY(), center.getZ()));
            return "mcaLoaded=true strict=" + MCATownsConfig.get().requireStrictTownCenter
                    + " villages=" + villages
                    + " nearestId=" + id
                    + " dist=" + String.format(java.util.Locale.ROOT, "%.1f", dist)
                    + " center=" + center.getX() + "," + center.getY() + "," + center.getZ()
                    + " isVillage=" + isVillage
                    + " hasTownCenter=" + hasCenter;
        } catch (Throwable t) {
            return "mcaLoaded=true debugError=" + t.getClass().getSimpleName();
        }
    }

    public static List<TownContext> getAllKnownTowns(ServerWorld world) {
        if (!isMcaLoaded()) {
            return List.of();
        }
        try {
            Class<?> villageManagerClass = resolveMcaClass("server.world.data.VillageManager");
            Object manager = invokeStatic(villageManagerClass, "get", new Class<?>[]{ServerWorld.class}, world);
            List<TownContext> result = new ArrayList<>();
            if (manager instanceof Iterable<?> iterable) {
                for (Object village : iterable) {
                    int id = (int) invoke(village, "getId", new Class<?>[0]);
                    Vec3i center = (Vec3i) invoke(village, "getCenter", new Class<?>[0]);
                    boolean isVillage = (boolean) invoke(village, "isVillage", new Class<?>[0]);
                    if (isVillage && isValidVillageForContext(village)) {
                        BlockPos pos = new BlockPos(center.getX(), center.getY(), center.getZ());
                        result.add(new TownContext("mca_" + id, pos, pos, "mca", true));
                    }
                }
            }
            return result;
        } catch (Throwable t) {
            MCATowns.LOGGER.debug("Failed MCA town iteration", t);
            return List.of();
        }
    }

    public static Optional<TownContext> getTownById(ServerWorld world, String townId) {
        if (!isMcaLoaded() || townId == null || !townId.startsWith("mca_")) {
            return Optional.empty();
        }
        try {
            int id = Integer.parseInt(townId.substring("mca_".length()));
            Class<?> villageManagerClass = resolveMcaClass("server.world.data.VillageManager");
            Object manager = invokeStatic(villageManagerClass, "get", new Class<?>[]{ServerWorld.class}, world);
            Optional<?> villageOpt = castOptional(invoke(manager, "getOrEmpty", new Class<?>[]{int.class}, id));
            if (villageOpt.isEmpty()) {
                return Optional.empty();
            }
            Object village = villageOpt.get();
            boolean isVillage = (boolean) invoke(village, "isVillage", new Class<?>[0]);
            if (!isVillageEligibleForBinding(village, isVillage)) {
                return Optional.empty();
            }
            Vec3i center = (Vec3i) invoke(village, "getCenter", new Class<?>[0]);
            BlockPos pos = new BlockPos(center.getX(), center.getY(), center.getZ());
            return Optional.of(new TownContext(townId, pos, pos, "mca", true));
        } catch (Throwable t) {
            MCATowns.LOGGER.debug("Failed MCA town-id lookup for {}", townId, t);
            return Optional.empty();
        }
    }

    public static TownBuildingSnapshot scanBuildings(ServerWorld world, BlockPos pos) {
        if (!isMcaLoaded()) {
            return new TownBuildingSnapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        try {
            Optional<?> villageOpt = findNearestVillage(world, pos, 64);
            if (villageOpt.isEmpty()) {
                return new TownBuildingSnapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            }
            Object village = villageOpt.get();
            Map<?, ?> buildings = castMap(invoke(village, "getBuildings", new Class<?>[0]));

            int townCenter = 0;
            int townHall = 0;
            int storage = 0;
            int library = 0;
            int armory = 0;
            int blacksmith = 0;
            int inn = 0;
            int graveyard = 0;
            int house = 0;
            int farm = 0;
            int dailyFoodProduction = 0;
            int cropFarms = 0;
            int invalidCropFarms = 0;
            int bakeries = 0;
            int butchers = 0;
            int fishermansHuts = 0;
            int quarry = 0;
            int workshop = 0;
            int market = 0;
            int tradingPost = 0;

            for (Object building : buildings.values()) {
                String type = String.valueOf(invoke(building, "getType", new Class<?>[0]));
                if ("town_center".equals(type) || "townhall".equals(type) || "village_center".equals(type)) townCenter++;
                if ("mcatowns_town_hall".equals(type)) townHall++;
                if ("storage".equals(type) || "warehouse".equals(type)) storage++;
                if ("library".equals(type)) library++;
                if ("armory".equals(type) || "barracks".equals(type) || "guard_tower".equals(type)) armory++;
                if ("blacksmith".equals(type)) blacksmith++;
                if ("inn".equals(type) || "tavern".equals(type)) inn++;
                if ("graveyard".equals(type) || "cemetery".equals(type)) graveyard++;
                if ("house".equals(type) || "home".equals(type)) house++;
                if ("mcatowns_quarry".equals(type) || "quarry".equals(type)) quarry++;
                if ("mcatowns_workshop".equals(type) || "workshop".equals(type)) workshop++;
                if ("mcatowns_market".equals(type) || "market".equals(type)) market++;
                if ("mcatowns_trading_post".equals(type) || "trading_post".equals(type)) tradingPost++;
                if ("farm".equals(type)
                        || "field".equals(type)) {
                    farm++;
                    cropFarms++;
                    dailyFoodProduction += 12;
                }
                if ("bakery".equals(type)
                        || "mcatowns_bakery".equals(type)) {
                    bakeries++;
                }
                if ("butcher".equals(type)
                        || "mcatowns_butcher".equals(type)) {
                    farm++;
                    butchers++;
                    dailyFoodProduction += 6;
                }
                if ("fishermans_hut".equals(type)
                        || "mcatowns_fishermans_hut".equals(type)) {
                    farm++;
                    fishermansHuts++;
                    dailyFoodProduction += 6;
                }
                if ("mcatowns_farm".equals(type)) {
                    Vec3i center = (Vec3i) invoke(building, "getCenter", new Class<?>[0]);
                    BlockPos c = new BlockPos(center.getX(), center.getY(), center.getZ());
                    if (countCropsNear(world, c, 20, 12) >= 12) {
                        farm++;
                        cropFarms++;
                        dailyFoodProduction += 12;
                    } else {
                        invalidCropFarms++;
                    }
                }
            }

            int activeBakeries = Math.min(bakeries, cropFarms);
            farm += activeBakeries;
            dailyFoodProduction += activeBakeries * 16;

            return new TownBuildingSnapshot(
                    townCenter, townHall, storage, library, armory, blacksmith, inn, graveyard, house, farm, dailyFoodProduction,
                    cropFarms, invalidCropFarms, activeBakeries, Math.max(0, bakeries - activeBakeries), butchers, fishermansHuts,
                    quarry, workshop, market, tradingPost
            );
        } catch (Throwable t) {
            MCATowns.LOGGER.debug("Failed MCA building scan", t);
            return new TownBuildingSnapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    public static int getPopulation(ServerWorld world, BlockPos pos) {
        if (!isMcaLoaded()) {
            return 0;
        }
        try {
            Optional<?> villageOpt = findNearestVillage(world, pos, 64);
            return villageOpt.map(v -> {
                try {
                    return (int) invoke(v, "getPopulation", new Class<?>[0]);
                } catch (Throwable ignored) {
                    return 0;
                }
            }).orElse(0);
        } catch (Throwable t) {
            MCATowns.LOGGER.debug("Failed MCA population lookup", t);
            return 0;
        }
    }

    public static int getJoblessPopulation(ServerWorld world, BlockPos pos) {
        if (!isMcaLoaded()) {
            return -1;
        }
        try {
            Optional<?> villageOpt = findNearestVillage(world, pos, 64);
            if (villageOpt.isEmpty()) {
                return -1;
            }
            Object village = villageOpt.get();
            Vec3i center = (Vec3i) invoke(village, "getCenter", new Class<?>[0]);
            BlockPos c = new BlockPos(center.getX(), center.getY(), center.getZ());
            Box box = new Box(c).expand(192.0D, 96.0D, 192.0D);

            Class<?> villagerClass = resolveMcaClass("entity.VillagerEntityMCA");
            @SuppressWarnings("unchecked")
            List<net.minecraft.entity.Entity> villagers = (List<net.minecraft.entity.Entity>) (List<?>) world.getEntitiesByClass(
                    (Class) villagerClass,
                    box,
                    e -> true
            );

            int unemployed = 0;
            for (net.minecraft.entity.Entity entity : villagers) {
                boolean inVillage = (boolean) invoke(village, "isWithinBorder", new Class<?>[]{net.minecraft.entity.Entity.class}, entity);
                if (!inVillage) {
                    continue;
                }
                boolean isBaby = (boolean) invoke(entity, "isBaby", new Class<?>[0]);
                if (isBaby) {
                    continue;
                }
                Object professionObj = invoke(entity, "getProfession", new Class<?>[0]);
                if (!(professionObj instanceof VillagerProfession profession)) {
                    continue;
                }
                String id = Registries.VILLAGER_PROFESSION.getId(profession).getPath();
                if ("none".equals(id) || "nitwit".equals(id)) {
                    unemployed++;
                }
            }
            return unemployed;
        } catch (Throwable t) {
            return -1;
        }
    }

    public static int getTownRankValue(ServerWorld world, BlockPos pos) {
        if (!isMcaLoaded()) {
            return 0;
        }
        try {
            Optional<?> villageOpt = findNearestVillage(world, pos, 64);
            if (villageOpt.isEmpty()) {
                return 0;
            }
            Object village = villageOpt.get();
            Class<?> tasksClass = resolveMcaClass("resources.Tasks");

            int bestOrdinal = 0;
            for (ServerPlayerEntity player : world.getPlayers()) {
                boolean inVillage = (boolean) invoke(village, "isWithinBorder", new Class<?>[]{net.minecraft.util.math.BlockPos.class, int.class}, player.getBlockPos(), 32);
                if (!inVillage) {
                    continue;
                }
                Object rank = invokeStatic(tasksClass, "getRank", new Class<?>[]{resolveMcaClass("server.world.data.Village"), ServerPlayerEntity.class}, village, player);
                bestOrdinal = Math.max(bestOrdinal, ((Enum<?>) rank).ordinal());
            }
            return bestOrdinal * 5;
        } catch (Throwable t) {
            MCATowns.LOGGER.debug("Failed MCA rank-value lookup", t);
            return 0;
        }
    }

    public static String getPlayerRankName(ServerPlayerEntity player, BlockPos pos) {
        if (!isMcaLoaded()) {
            return "Mayor";
        }
        try {
            Optional<?> village = findNearestVillage(player.getServerWorld(), pos, 64);
            if (village.isEmpty()) {
                return "Visitor";
            }
            Class<?> tasksClass = resolveMcaClass("resources.Tasks");
            Object rank = invokeStatic(tasksClass, "getRank",
                    new Class<?>[]{resolveMcaClass("server.world.data.Village"), ServerPlayerEntity.class},
                    village.get(), player);
            String name = ((Enum<?>) rank).name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
            String[] words = name.split(" ");
            StringBuilder label = new StringBuilder();
            for (int i = 0; i < words.length; i++) {
                if (words[i].isBlank()) continue;
                if (label.length() > 0) label.append(' ');
                label.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
            }
            return label.isEmpty() ? "Visitor" : label.toString();
        } catch (Throwable t) {
            MCATowns.LOGGER.debug("Failed MCA rank-name lookup", t);
            return hasMayorRank(player, pos) ? "Mayor" : "Visitor";
        }
    }

    public static int getMcaNormalTaxIncome(ServerWorld world, BlockPos pos) {
        return getMcaProjectedTaxIncome(world, pos);
    }

    public static int getMcaProjectedTaxIncome(ServerWorld world, BlockPos pos) {
        if (!isMcaLoaded()) {
            return 0;
        }
        try {
            Optional<?> villageOpt = findNearestVillage(world, pos, 64);
            if (villageOpt.isEmpty()) {
                return 0;
            }
            Object village = villageOpt.get();

            double taxes = ((Number) invoke(village, "getTaxes", new Class<?>[0])).doubleValue();
            int population = (int) invoke(village, "getPopulation", new Class<?>[0]);

            // MCA's item-tax system starts from this value before converting into items.
            Class<?> configClass = resolveMcaClass("Config");
            Object config = invokeStatic(configClass, "getInstance", new Class<?>[0]);
            double taxesFactor = ((Number) configClass.getField("taxesFactor").get(config)).doubleValue();

            double taxValue = taxesFactor * population * taxes;
            boolean hasLibrary = (boolean) invoke(village, "hasBuilding", new Class<?>[]{String.class}, "library");
            if (hasLibrary) {
                taxValue *= 1.5D;
            }

            return Math.max(0, (int) Math.round(taxValue));
        } catch (Throwable t) {
            MCATowns.LOGGER.debug("Failed MCA normal-tax lookup", t);
            return 0;
        }
    }

    public static List<ItemStack> collectVillagerTradeTaxItems(ServerWorld world, BlockPos pos) {
        if (!isMcaLoaded()) {
            return List.of();
        }
        try {
            Optional<?> villageOpt = findNearestVillage(world, pos, 64);
            if (villageOpt.isEmpty()) {
                return List.of();
            }
            Object village = villageOpt.get();
            Vec3i center = (Vec3i) invoke(village, "getCenter", new Class<?>[0]);
            BlockPos c = new BlockPos(center.getX(), center.getY(), center.getZ());
            Box box = new Box(c).expand(192.0D, 96.0D, 192.0D);

            List<MerchantEntity> merchants = world.getEntitiesByClass(MerchantEntity.class, box, e -> {
                String n = e.getClass().getName();
                return n.contains("Villager");
            });
            if (merchants.isEmpty()) {
                return List.of();
            }

            List<ItemStack> out = new ArrayList<>();
            for (MerchantEntity merchant : merchants) {
                if (out.size() >= 24) {
                    break;
                }
                boolean inVillage;
                try {
                    inVillage = (boolean) invoke(village, "isWithinBorder", new Class<?>[]{net.minecraft.entity.Entity.class}, merchant);
                } catch (Throwable ignored) {
                    inVillage = merchant.getBlockPos().getSquaredDistance(c) <= 256.0D * 256.0D;
                }
                if (!inVillage) {
                    continue;
                }

                TradeOfferList offers = merchant.getOffers();
                if (offers == null || offers.isEmpty()) {
                    continue;
                }

                int addedForVillager = 0;
                for (TradeOffer offer : offers) {
                    if (addedForVillager >= 2 || out.size() >= 24) {
                        break;
                    }
                    if (offer == null || offer.isDisabled()) {
                        continue;
                    }
                    ItemStack taxStack = createTradeTaxStack(offer.getSellItem());
                    if (taxStack.isEmpty()) {
                        taxStack = createTradeTaxStack(offer.getOriginalFirstBuyItem());
                    }
                    if (taxStack.isEmpty()) {
                        taxStack = createTradeTaxStack(offer.getSecondBuyItem());
                    }
                    if (!taxStack.isEmpty()) {
                        out.add(taxStack);
                        addedForVillager++;
                    }
                }
            }
            return out;
        } catch (Throwable t) {
            return List.of();
        }
    }

    public static Optional<String> getBuildingTypeAt(ServerWorld world, BlockPos pos) {
        if (!isMcaLoaded()) {
            return Optional.empty();
        }
        try {
            Optional<?> villageOpt = findNearestVillage(world, pos, Math.max(64, MCATownsConfig.get().mcaVillageSearchMargin));
            if (villageOpt.isEmpty()) {
                return Optional.empty();
            }
            Object village = villageOpt.get();
            Optional<?> buildingOpt = castOptional(invoke(village, "getBuildingAt", new Class<?>[]{Vec3i.class}, pos));
            if (buildingOpt.isEmpty()) {
                return Optional.empty();
            }
            Object building = buildingOpt.get();
            String type = String.valueOf(invoke(building, "getType", new Class<?>[0]));
            return Optional.ofNullable(type);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    public static Optional<BlockPos> getBuildingCenterAt(ServerWorld world, BlockPos pos) {
        try {
            Object building = findBuildingAt(world, pos).orElse(null);
            if (building == null) return Optional.empty();
            Vec3i center = (Vec3i) invoke(building, "getCenter", new Class<?>[0]);
            return Optional.of(new BlockPos(center.getX(), center.getY(), center.getZ()));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    public static Optional<BuildingBounds> getBuildingBoundsAt(ServerWorld world, BlockPos pos) {
        try {
            Object building = findBuildingAt(world, pos).orElse(null);
            if (building == null) return Optional.empty();
            Map<?, ?> blocks = castMap(invoke(building, "getBlocks", new Class<?>[0]));
            BlockPos min = null;
            BlockPos max = null;
            for (Object value : blocks.values()) {
                if (!(value instanceof Iterable<?> positions)) continue;
                for (Object entry : positions) {
                    if (!(entry instanceof Vec3i vec)) continue;
                    BlockPos block = new BlockPos(vec.getX(), vec.getY(), vec.getZ());
                    min = min == null ? block : new BlockPos(
                            Math.min(min.getX(), block.getX()),
                            Math.min(min.getY(), block.getY()),
                            Math.min(min.getZ(), block.getZ()));
                    max = max == null ? block : new BlockPos(
                            Math.max(max.getX(), block.getX()),
                            Math.max(max.getY(), block.getY()),
                            Math.max(max.getZ(), block.getZ()));
                }
            }
            return min == null ? Optional.empty() : Optional.of(new BuildingBounds(min, max));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    public static int countBuildingBlocks(ServerWorld world, BlockPos pos, Set<String> blockIds) {
        try {
            Object building = findBuildingAt(world, pos).orElse(null);
            if (building == null) return 0;
            Map<?, ?> blocks = castMap(invoke(building, "getBlocks", new Class<?>[0]));
            int count = 0;
            for (Map.Entry<?, ?> entry : blocks.entrySet()) {
                if (blockIds.contains(String.valueOf(entry.getKey())) && entry.getValue() instanceof List<?> positions) {
                    count += positions.size();
                }
            }
            return count;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public record BuildingBounds(BlockPos min, BlockPos max) { }

    private static Optional<?> findBuildingAt(ServerWorld world, BlockPos pos) throws Exception {
        if (!isMcaLoaded()) return Optional.empty();
        Optional<?> village = findNearestVillage(world, pos, Math.max(64, MCATownsConfig.get().mcaVillageSearchMargin));
        if (village.isEmpty()) return Optional.empty();
        return castOptional(invoke(village.get(), "getBuildingAt", new Class<?>[]{Vec3i.class}, pos));
    }

    public static List<BlockPos> getNearestTownStorageCenters(ServerWorld world, BlockPos pos) {
        if (!isMcaLoaded()) {
            return List.of();
        }
        try {
            Optional<?> villageOpt = findNearestVillage(world, pos, 64);
            if (villageOpt.isEmpty()) {
                return List.of();
            }
            Object village = villageOpt.get();
            Map<?, ?> buildings = castMap(invoke(village, "getBuildings", new Class<?>[0]));
            List<BlockPos> result = new ArrayList<>();
            for (Object building : buildings.values()) {
                String type = String.valueOf(invoke(building, "getType", new Class<?>[0]));
                if (!"storage".equals(type) && !"warehouse".equals(type)) {
                    continue;
                }
                Vec3i center = (Vec3i) invoke(building, "getCenter", new Class<?>[0]);
                result.add(new BlockPos(center.getX(), center.getY(), center.getZ()));
            }
            return result;
        } catch (Throwable t) {
            return List.of();
        }
    }

    private static ItemStack createTradeTaxStack(ItemStack source) {
        if (source == null || source.isEmpty() || source.isOf(Items.EMERALD)) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = source.copy();
        int count = Math.max(1, copy.getCount() / 4);
        count = Math.min(count, copy.getMaxCount() > 1 ? 8 : 1);
        copy.setCount(count);
        return copy;
    }

    private static int countCropsNear(ServerWorld world, BlockPos center, int radius, int limit) {
        int found = 0;
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minY = Math.max(world.getBottomY(), center.getY() - 2);
        int maxY = Math.min(world.getTopY() - 1, center.getY() + 2);
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        int radiusSq = radius * radius;
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int dx = x - center.getX();
                    int dz = z - center.getZ();
                    if (dx * dx + dz * dz > radiusSq) {
                        continue;
                    }
                    p.set(x, y, z);
                    if (world.getBlockState(p).isIn(BlockTags.CROPS)) {
                        found++;
                        if (found >= limit) {
                            return found;
                        }
                    }
                }
            }
        }
        return found;
    }

    private static Optional<?> findNearestVillage(ServerWorld world, BlockPos pos, int margin) throws Exception {
        Class<?> villageManagerClass = resolveMcaClass("server.world.data.VillageManager");
        Object manager = invokeStatic(villageManagerClass, "get", new Class<?>[]{ServerWorld.class}, world);
        int start = Math.max(16, margin);
        Optional<?> nearest = castOptional(invoke(manager, "findNearestVillage", new Class<?>[]{BlockPos.class, int.class}, pos, start));
        if (nearest.isPresent()) {
            return nearest;
        }

        int configured = Math.max(16, MCATownsConfig.get().mcaVillageSearchMargin);
        int maxRadius = Math.max(256, configured);
        for (int probe = Math.min(256, start * 2); probe <= maxRadius; probe *= 2) {
            nearest = castOptional(invoke(manager, "findNearestVillage", new Class<?>[]{BlockPos.class, int.class}, pos, probe));
            if (nearest.isPresent()) {
                return nearest;
            }
        }

        // Bootstrap MCA village discovery around bells when MCA has not scanned this area yet.
        if (!MCATownsConfig.get().requireStrictTownCenter) {
            tryBootstrapVillageDiscovery(world, pos, manager);
            nearest = castOptional(invoke(manager, "findNearestVillage", new Class<?>[]{BlockPos.class, int.class}, pos, maxRadius));
            if (nearest.isPresent()) {
                return nearest;
            }
        }

        // Fallback for worlds where MCA borders are not yet expanded around the desk position:
        // pick the nearest known village by geometric center distance.
        if (manager instanceof Iterable<?> iterable) {
            Object nearestVillage = null;
            double bestDistance = Double.MAX_VALUE;
            int hardCap = Math.max(maxRadius, 2048);
            double hardCapSq = (double) hardCap * hardCap;
            for (Object village : iterable) {
                Vec3i center = (Vec3i) invoke(village, "getCenter", new Class<?>[0]);
                double distSq = pos.getSquaredDistance(center.getX(), center.getY(), center.getZ());
                if (distSq <= hardCapSq && distSq < bestDistance) {
                    bestDistance = distSq;
                    nearestVillage = village;
                }
            }
            if (nearestVillage != null) {
                return Optional.of(nearestVillage);
            }
        }

        return Optional.empty();
    }

    private static Class<?> resolveMcaClass(String relativeName) throws ClassNotFoundException {
        ClassNotFoundException last = null;
        for (String prefix : MCA_CLASS_PREFIXES) {
            try {
                return Class.forName(prefix + relativeName);
            } catch (ClassNotFoundException e) {
                last = e;
            }
        }
        throw last != null ? last : new ClassNotFoundException(relativeName);
    }

    private static void tryBootstrapVillageDiscovery(ServerWorld world, BlockPos origin, Object villageManager) {
        try {
            Method processBuilding = villageManager.getClass().getMethod("processBuilding", BlockPos.class);
            processBuilding.setAccessible(true);
            int radius = 24;
            BlockPos.Mutable cursor = new BlockPos.Mutable();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -8; dy <= 8; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                        if (world.getBlockState(cursor).isOf(Blocks.BELL)) {
                            processBuilding.invoke(villageManager, cursor.toImmutable());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // Best-effort only; standard lookup paths still apply.
        }
    }

    private static boolean hasAnyTownCenter(Object village) throws Exception {
        for (String type : MCA_TOWN_CENTER_TYPES) {
            if ((boolean) invoke(village, "hasBuilding", new Class<?>[]{String.class}, type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidVillageForContext(Object village) throws Exception {
        if (!MCATownsConfig.get().requireStrictTownCenter) {
            return true;
        }
        return hasAnyTownCenter(village);
    }

    private static boolean isVillageEligibleForBinding(Object village, boolean isVillage) throws Exception {
        if (!MCATownsConfig.get().requireStrictTownCenter) {
            return true;
        }
        return isVillage && isValidVillageForContext(village);
    }

    private static Object invoke(Object target, String method, Class<?>[] args, Object... values) throws Exception {
        Method m = target.getClass().getMethod(method, args);
        m.setAccessible(true);
        return m.invoke(target, values);
    }

    private static Object invokeStatic(Class<?> clazz, String method, Class<?>[] args, Object... values) throws Exception {
        Method m = clazz.getMethod(method, args);
        m.setAccessible(true);
        return m.invoke(null, values);
    }

    @SuppressWarnings("unchecked")
    private static Optional<?> castOptional(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional;
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static Map<?, ?> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        return Collections.emptyMap();
    }
}
