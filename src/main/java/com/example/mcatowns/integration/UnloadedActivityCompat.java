package com.example.mcatowns.integration;

import com.example.mcatowns.MCATowns;
import com.example.mcatowns.integration.FarmWeedMaintenanceService.FarmArea;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Optional compatibility for Unloaded Activity 0.6.3 and Immersive Weathering 2.0.5. */
public final class UnloadedActivityCompat {
    private static final TagKey<Block> IW_FERTILE_BLOCKS = TagKey.of(
            RegistryKeys.BLOCK, new Identifier("immersive_weathering", "fertile_blocks"));

    private static volatile boolean iwReflectionAttempted;
    private static volatile boolean iwReflectionAvailable;
    private static Method iwTickBlock;
    private static Object iwBlockTick;
    private static Object iwClearSky;

    private static volatile boolean uaReflectionAttempted;
    private static Field uaConfigField;
    private static Field uaEnableRandomTicksField;
    private static Field uaEnablePrecipitationTicksField;
    private static Field uaGrowCropsField;
    private static Method uaGetRandomPickOdds;
    private static Method uaGetOccurrences;

    private static volatile boolean warnedIwReflection;
    private static volatile boolean warnedUaReflection;

    private UnloadedActivityCompat() { }

    public static boolean isImmersiveWeatheringLoaded() {
        return FabricLoader.getInstance().isModLoaded("immersive_weathering");
    }

    /**
     * Called only from Unloaded Activity's own chunk catch-up method. No MCA-specific chunk timer
     * or last-loaded timestamp is maintained here.
     */
    public static void onChunkCatchUp(long elapsedTicks, ServerWorld world, WorldChunk chunk, int randomTickSpeed) {
        if (elapsedTicks <= 0L || world == null || chunk == null || randomTickSpeed <= 0) return;
        if (!isImmersiveWeatheringLoaded() || !isUaChunkSimulationEnabled() || !ensureIwReflection()) return;

        int attempts = UnloadedActivityCatchUpPolicy.environmentalAttempts(elapsedTicks, randomTickSpeed);
        if (attempts <= 0) return;

        List<FarmArea> maintainedFarms = FarmWeedMaintenanceService.staffedFarmsForChunk(world, chunk.getPos());
        Random random = world.random;
        ChunkPos chunkPos = chunk.getPos();

        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = chunkPos.getStartX() + random.nextInt(16);
            int z = chunkPos.getStartZ() + random.nextInt(16);
            BlockPos probe = new BlockPos(x, world.getBottomY(), z);
            BlockPos pos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, probe).down();
            if (pos.getY() < world.getBottomY() || pos.getY() >= world.getTopY()) continue;

            // The ordinary IW random-block hook handles much of its vegetation/grass spread.
            // Sampling surface positions keeps this bounded while targeting the environmental
            // behaviour this compatibility exists to preserve.
            BlockState state = world.getBlockState(pos);
            invokeIwGrowth(iwBlockTick, state, world, pos);

            // CLEAR_SKY owns IW weed creation/spread. Staffed registered farms suppress only
            // fertile-soil clear-sky attempts; unrelated environmental weathering is untouched.
            state = world.getBlockState(pos);
            if (state.isIn(IW_FERTILE_BLOCKS)
                    && isMaintainedFarmPosition(maintainedFarms, pos)
                    && random.nextDouble() < UnloadedActivityCatchUpPolicy.staffedFarmWeedSuppression()) {
                continue;
            }
            invokeIwGrowth(iwClearSky, state, world, pos);
        }
    }

    /** Used by optional custom-crop mixins for crops that do not inherit UA's vanilla CropBlock support. */
    public static boolean canSimulateCustomCrop() {
        return isUaCropSimulationEnabled();
    }

    /**
     * Uses UA's own elapsed time and occurrence sampler, then caps actual randomTick calls.
     * This is only for custom crop implementations that UA 0.6.3 cannot see through its
     * vanilla CropBlock/SweetBerryBush mixins.
     */
    public static void simulateCustomCrop(Block block, BlockState initialState, ServerWorld world, BlockPos pos,
                                          Random random, long elapsedTicks, int randomTickSpeed) {
        if (block == null || initialState == null || world == null || pos == null || random == null) return;
        if (elapsedTicks <= 0L || randomTickSpeed <= 0 || !isUaCropSimulationEnabled()) return;

        int occurrences = sampleUaRandomTickOccurrences(elapsedTicks, randomTickSpeed, random);
        occurrences = UnloadedActivityCatchUpPolicy.capCustomCropRandomTicks(occurrences);
        for (int i = 0; i < occurrences; i++) {
            BlockState current = world.getBlockState(pos);
            if (!current.isOf(block)) break;
            block.randomTick(current, world, pos, random);
        }
    }

    private static boolean isMaintainedFarmPosition(List<FarmArea> farms, BlockPos pos) {
        for (FarmArea farm : farms) {
            if (farm.containsXZ(pos)) return true;
        }
        return false;
    }

    private static void invokeIwGrowth(Object source, BlockState state, ServerWorld world, BlockPos pos) {
        if (source == null || iwTickBlock == null) return;
        try {
            iwTickBlock.invoke(null, source, state, world, pos);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            iwReflectionAvailable = false;
            if (!warnedIwReflection) {
                warnedIwReflection = true;
                MCATowns.LOGGER.warn("Disabling Immersive Weathering unloaded catch-up after compatibility failure", exception);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static synchronized boolean ensureIwReflection() {
        if (iwReflectionAttempted) return iwReflectionAvailable;
        iwReflectionAttempted = true;
        try {
            Class<?> tickSourceClass = Class.forName("com.ordana.immersive_weathering.data.block_growths.TickSource");
            Class<?> handlerClass = Class.forName("com.ordana.immersive_weathering.data.block_growths.BlockGrowthHandler");
            iwBlockTick = Enum.valueOf((Class<? extends Enum>) tickSourceClass.asSubclass(Enum.class), "BLOCK_TICK");
            iwClearSky = Enum.valueOf((Class<? extends Enum>) tickSourceClass.asSubclass(Enum.class), "CLEAR_SKY");
            iwTickBlock = handlerClass.getMethod("tickBlock", tickSourceClass, BlockState.class, ServerWorld.class, BlockPos.class);
            iwReflectionAvailable = true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            iwReflectionAvailable = false;
            if (!warnedIwReflection) {
                warnedIwReflection = true;
                MCATowns.LOGGER.warn("Immersive Weathering compatibility API was not found; unloaded weathering catch-up is disabled", exception);
            }
        }
        return iwReflectionAvailable;
    }

    private static synchronized void ensureUaReflection() {
        if (uaReflectionAttempted) return;
        uaReflectionAttempted = true;
        try {
            Class<?> uaClass = Class.forName("fabric.lol.zanspace.unloadedactivity.UnloadedActivity");
            Class<?> configClass = Class.forName("fabric.lol.zanspace.unloadedactivity.config.UnloadedActivityConfig");
            Class<?> utilsClass = Class.forName("fabric.lol.zanspace.unloadedactivity.Utils");
            uaConfigField = uaClass.getField("config");
            uaEnableRandomTicksField = configClass.getField("enableRandomTicks");
            uaEnablePrecipitationTicksField = configClass.getField("enablePrecipitationTicks");
            uaGrowCropsField = configClass.getField("growCrops");
            uaGetRandomPickOdds = utilsClass.getMethod("getRandomPickOdds", int.class);
            uaGetOccurrences = utilsClass.getMethod("getOccurrences", long.class, double.class, int.class, Random.class);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!warnedUaReflection) {
                warnedUaReflection = true;
                MCATowns.LOGGER.warn("Unloaded Activity 0.6.3 compatibility API was not found", exception);
            }
        }
    }

    private static boolean isUaChunkSimulationEnabled() {
        ensureUaReflection();
        try {
            if (uaConfigField == null || uaEnableRandomTicksField == null || uaEnablePrecipitationTicksField == null) return false;
            Object config = uaConfigField.get(null);
            return config != null && uaEnableRandomTicksField.getBoolean(config) && uaEnablePrecipitationTicksField.getBoolean(config);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isUaCropSimulationEnabled() {
        ensureUaReflection();
        try {
            if (uaConfigField == null || uaGrowCropsField == null) return false;
            Object config = uaConfigField.get(null);
            return config != null && uaGrowCropsField.getBoolean(config);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return false;
        }
    }

    private static int sampleUaRandomTickOccurrences(long elapsedTicks, int randomTickSpeed, Random random) {
        ensureUaReflection();
        try {
            if (uaGetRandomPickOdds != null && uaGetOccurrences != null) {
                double odds = (double) uaGetRandomPickOdds.invoke(null, randomTickSpeed);
                return (int) uaGetOccurrences.invoke(null, elapsedTicks, odds,
                        UnloadedActivityCatchUpPolicy.MAX_CUSTOM_CROP_RANDOM_TICKS, random);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Fall through to the same random-pick probability UA 0.6.3 uses.
        }

        double odds = 1.0D - Math.pow(0.999755859375D, randomTickSpeed);
        double expected = elapsedTicks * odds;
        int whole = (int) Math.min(UnloadedActivityCatchUpPolicy.MAX_CUSTOM_CROP_RANDOM_TICKS, Math.floor(expected));
        if (whole < UnloadedActivityCatchUpPolicy.MAX_CUSTOM_CROP_RANDOM_TICKS
                && random.nextDouble() < expected - Math.floor(expected)) {
            whole++;
        }
        return whole;
    }
}
