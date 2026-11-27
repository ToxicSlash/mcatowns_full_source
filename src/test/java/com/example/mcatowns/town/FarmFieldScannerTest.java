package com.example.mcatowns.town;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FarmFieldScannerTest {
    @Test
    void tiersUseTheDocumentedConnectedCropBoundaries() {
        assertEquals(1, FarmFieldScanner.tierForCropCount(25));
        assertEquals(2, FarmFieldScanner.tierForCropCount(26));
        assertEquals(2, FarmFieldScanner.tierForCropCount(64));
        assertEquals(3, FarmFieldScanner.tierForCropCount(65));
        assertEquals(3, FarmFieldScanner.tierForCropCount(144));
    }

    @Test
    void eachTierHasABoundedCropContribution() {
        assertEquals(25, FarmFieldScanner.cropCapForTier(1));
        assertEquals(64, FarmFieldScanner.cropCapForTier(2));
        assertEquals(144, FarmFieldScanner.cropCapForTier(3));
    }

    @Test
    void irregularRowsAcrossOneBlockChannelsIncludeAllTilesFromEitherEnd() {
        Set<BlockPos> field = new HashSet<>();
        Set<BlockPos> gaps = new HashSet<>();
        for (int z = 0; z < 7; z++) {
            field.add(new BlockPos(0, 64, z));
            field.add(new BlockPos(2, 64, z));
            gaps.add(new BlockPos(1, 64, z));
        }
        field.add(new BlockPos(3, 65, 6));
        field.add(new BlockPos(3, 64, 6));
        for (BlockPos seed : Set.of(new BlockPos(0, 64, 0), new BlockPos(3, 65, 6))) {
            var result = FarmFieldScanner.scanConnected(seed, field::contains, field::contains, gaps::contains);
            assertEquals(field, new HashSet<>(result.tiles()));
            assertEquals(16, result.crops());
            assertTrue(result.valid());
            assertFalse(result.scanLimited());
        }
    }

    @Test
    void wallsAndTwoBlockGapsDoNotMergeFields() {
        BlockPos a = new BlockPos(0, 64, 0);
        BlockPos b = new BlockPos(2, 64, 0);
        BlockPos c = new BlockPos(5, 64, 0);
        Set<BlockPos> field = Set.of(a, b, c);
        assertEquals(1, FarmFieldScanner.scanConnected(a, field::contains, field::contains, p -> false).crops());
        assertEquals(2, FarmFieldScanner.scanConnected(a, field::contains, field::contains, p -> true).crops());
    }

    @Test
    void emptyFarmlandConnectsRowsButDoesNotCountAsCrops() {
        Set<BlockPos> field = new HashSet<>();
        for (int x = 0; x < 15; x++) field.add(new BlockPos(x, 64, 0));
        var result = FarmFieldScanner.scanConnected(new BlockPos(0, 64, 0), field::contains,
                p -> p.getX() != 7, p -> false);
        assertEquals(15, result.tiles().size());
        assertEquals(14, result.crops());
    }

    @Test
    void capsContributionAndReportsOnlyRealTruncation() {
        Set<BlockPos> field = new HashSet<>();
        for (int x = 0; x < 256; x++) field.add(new BlockPos(x, 64, 0));
        BlockPos seed = new BlockPos(0, 64, 0);
        var exact = FarmFieldScanner.scanConnected(seed, field::contains, field::contains, p -> false);
        assertEquals(256, exact.tiles().size());
        assertEquals(144, exact.contributingCrops());
        assertFalse(exact.scanLimited());
        field.add(new BlockPos(256, 64, 0));
        assertTrue(FarmFieldScanner.scanConnected(seed, field::contains, field::contains, p -> false).scanLimited());
    }
}
