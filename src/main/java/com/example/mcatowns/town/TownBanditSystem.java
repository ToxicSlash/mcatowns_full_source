package com.example.mcatowns.town;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * First-release Bandit Activity backend. It owns pressure/timing/tagging only; physical band placement and custom
 * raid integration are intentionally separate so they can be tested in-game later.
 */
public final class TownBanditSystem {
    public static final int MAX_ACTIVITY = 120;
    public static final int OFFLINE_ACTIVITY_CAP = 99;
    public static final int ACTIVITY_INCREASE = 5;
    public static final int BANDIT_KILL_REDUCTION = 4;
    public static final int CAMP_CLEAR_REDUCTION = 20;

    public static final long ACTIVITY_INTERVAL_TICKS = 20L * 60L * 20L; // 20 minutes
    public static final long BAND_ATTEMPT_INTERVAL_TICKS = 5L * 60L * 20L; // 5 minutes
    public static final long CAMP_ATTEMPT_INTERVAL_TICKS = 40L * 60L * 20L; // 40 minutes
    public static final long CAMP_REPLACEMENT_COOLDOWN_TICKS = 80L * 60L * 20L; // 80 minutes
    public static final long UNSPAWNED_WILD_BAND_EXPIRY_TICKS = 15L * 60L * 20L;
    public static final long SPAWNED_WILD_SLOT_RELEASE_TICKS = 10L * 60L * 20L;

    public static final double BAND_ATTEMPT_CHANCE = 0.60D;
    public static final double DEFENCE_CHANCE_REDUCTION_PER_POINT = 0.025D;
    public static final double MIN_ACTIVITY_INCREASE_CHANCE = 0.40D;

    public static final String BANDIT_TAG = "mcatowns_bandit";
    private static final String TOWN_TAG_PREFIX = "mcatowns_bandit_town_";
    private static final String SCORE_TAG_PREFIX = "mcatowns_bandit_score_";

    private TownBanditSystem() { }

    /** Called from the lightweight town tick; no chunk or entity scans are performed. */
    public static void tickActivity(ServerWorld world, TownContext context, TownSavedData townData) {
        if (!"player_created".equals(context.source())) return;

        long now = world.getTime();
        TownBanditSavedData saved = TownBanditSavedData.get(world);
        TownBanditSavedData.State state = saved.getOrCreate(context.townId());
        if (state.lastGrowthTick() < 0L) {
            saved.put(context.townId(), state.withLastGrowthTick(now));
            return;
        }
        if (now - state.lastGrowthTick() < ACTIVITY_INTERVAL_TICKS) return;

        boolean ownerOnline = isTownOwnerOnline(world, context.townId());
        int defence = TownDefenceInfrastructure.defence(townData);
        int activity = applyGrowthRoll(state.activity(), defence, ownerOnline, world.getRandom().nextDouble());
        saved.put(context.townId(), state.withActivity(activity).withLastGrowthTick(now));
    }

    public static int applyGrowthRoll(int currentActivity, int defence, boolean playerOnline, double roll) {
        int current = clampActivity(currentActivity);
        int cap = playerOnline ? MAX_ACTIVITY : OFFLINE_ACTIVITY_CAP;
        if (current >= cap) return Math.min(current, cap);
        if (roll >= activityIncreaseChance(defence)) return current;
        return Math.min(cap, current + ACTIVITY_INCREASE);
    }

    public static double activityIncreaseChance(int defence) {
        return Math.max(MIN_ACTIVITY_INCREASE_CHANCE,
                1.0D - Math.max(0, defence) * DEFENCE_CHANCE_REDUCTION_PER_POINT);
    }

    public static BanditActivityTier tier(int activity) {
        return BanditActivityTier.fromActivity(activity);
    }

    public static int clampActivity(int activity) {
        return Math.max(0, Math.min(MAX_ACTIVITY, activity));
    }

    public static int getActivity(ServerWorld world, String townId) {
        return TownBanditSavedData.get(world).getOrCreate(townId).activity();
    }

    public static void setActivity(ServerWorld world, String townId, int activity) {
        TownBanditSavedData saved = TownBanditSavedData.get(world);
        TownBanditSavedData.State state = saved.getOrCreate(townId);
        saved.put(townId, state.withActivity(activity));
    }

    public static void reduceActivity(ServerWorld world, String townId, int amount) {
        if (amount <= 0) return;
        TownBanditSavedData saved = TownBanditSavedData.get(world);
        TownBanditSavedData.State state = saved.getOrCreate(townId);
        saved.put(townId, state.withActivity(state.activity() - amount));
    }

    public static void onBanditBountyCompleted(ServerWorld world, String townId, int extraReduction) {
        reduceActivity(world, townId, Math.max(0, extraReduction));
    }

    public static void onRaidVictory(ServerWorld world, String townId, int reduction) {
        reduceActivity(world, townId, Math.max(0, reduction));
    }

    public static void onCampCleared(ServerWorld world, String townId) {
        TownBanditSavedData saved = TownBanditSavedData.get(world);
        TownBanditSavedData.State state = saved.getOrCreate(townId);
        saved.put(townId, state.withActivity(state.activity() - CAMP_CLEAR_REDUCTION)
                .withCampCooldownUntilTick(world.getTime() + CAMP_REPLACEMENT_COOLDOWN_TICKS));
    }

    /**
     * Reserves the global 5-minute band attempt cadence. A successful result should create at most one missing
     * Wild/Town encounter reservation; it must not fill every empty slot in one roll.
     */
    public static boolean consumeBandSpawnAttempt(ServerWorld world, String townId) {
        TownBanditSavedData saved = TownBanditSavedData.get(world);
        TownBanditSavedData.State state = saved.getOrCreate(townId);
        long now = world.getTime();
        if (state.lastBandAttemptTick() >= 0L && now - state.lastBandAttemptTick() < BAND_ATTEMPT_INTERVAL_TICKS) {
            return false;
        }
        saved.put(townId, state.withLastBandAttemptTick(now));
        return world.getRandom().nextDouble() < BAND_ATTEMPT_CHANCE;
    }

    /** Camp placement policy only. Physical position/structure generation is deliberately not performed here. */
    public static boolean consumeCampAttempt(ServerWorld world, String townId) {
        TownBanditSavedData saved = TownBanditSavedData.get(world);
        TownBanditSavedData.State state = saved.getOrCreate(townId);
        long now = world.getTime();
        if (state.campCooldownUntilTick() > now) return false;
        if (state.lastCampAttemptTick() >= 0L && now - state.lastCampAttemptTick() < CAMP_ATTEMPT_INTERVAL_TICKS) {
            return false;
        }
        BanditActivityTier tier = tier(state.activity());
        saved.put(townId, state.withLastCampAttemptTick(now));
        int chance = tier.campAttemptChancePercent();
        return chance > 0 && world.getRandom().nextInt(100) < chance;
    }

    /** Tags a later-spawned Pillager/custom bandit so only MCA Towns bandits alter the owning town's Activity. */
    public static void tagBandit(LivingEntity entity, String townId, int activityReductionOnKill) {
        if (entity == null || townId == null || townId.isBlank()) return;
        entity.addCommandTag(BANDIT_TAG);
        entity.addCommandTag(TOWN_TAG_PREFIX + townId);
        entity.addCommandTag(SCORE_TAG_PREFIX + Math.max(1, activityReductionOnKill));
    }

    /** Returns true when the death belonged to a tagged MCA Towns bandit and Activity was reduced. */
    public static boolean handleBanditDeath(ServerWorld world, LivingEntity entity) {
        if (entity == null || !entity.getCommandTags().contains(BANDIT_TAG)) return false;
        String townId = null;
        int reduction = BANDIT_KILL_REDUCTION;
        for (String tag : entity.getCommandTags()) {
            if (tag.startsWith(TOWN_TAG_PREFIX)) townId = tag.substring(TOWN_TAG_PREFIX.length());
            if (tag.startsWith(SCORE_TAG_PREFIX)) {
                try {
                    reduction = Math.max(1, Integer.parseInt(tag.substring(SCORE_TAG_PREFIX.length())));
                } catch (NumberFormatException ignored) { }
            }
        }
        if (townId == null || townId.isBlank()) return false;
        reduceActivity(world, townId, reduction);
        return true;
    }

    public static int wildBandCapacity(int activity) {
        return tier(activity).wildBandCapacity();
    }

    public static int townBandCapacity(int activity) {
        return tier(activity).townBandCapacity();
    }

    public static int campCapacity(int activity) {
        return tier(activity).campCapacity();
    }

    private static boolean isTownOwnerOnline(ServerWorld world, String townId) {
        PlayerTownRegistry registry = PlayerTownRegistry.get(world);
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (registry.isOwner(townId, player)) return true;
        }
        return false;
    }
}
