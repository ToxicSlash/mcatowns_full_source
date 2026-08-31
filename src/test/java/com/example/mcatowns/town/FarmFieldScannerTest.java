package com.example.mcatowns.town;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
