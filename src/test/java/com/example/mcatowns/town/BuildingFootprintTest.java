package com.example.mcatowns.town;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class BuildingFootprintTest {
    private static BlockPos p(int x, int z) { return new BlockPos(x, 64, z); }

    @Test
    void rectangleOnlyHasExteriorEdges() {
        List<BlockPos> cells = new ArrayList<>();
        for (int x = 0; x < 2; x++) for (int z = 0; z < 3; z++) cells.add(p(x, z));
        var edges = BuildingFootprint.boundary(cells);
        assertEquals(10, edges.size());
        assertFalse(edges.contains(new BuildingFootprint.Edge(1, 0, 1, 1)));
    }

    @Test
    void lShapeDoesNotDrawOrSelectTheMissingCorner() {
        var cells = List.of(p(0, 0), p(1, 0), p(0, 1));
        assertEquals(8, BuildingFootprint.boundary(cells).size());
        assertFalse(BuildingFootprint.contains(cells, 1, 1));
        assertTrue(BuildingFootprint.boundary(cells).contains(new BuildingFootprint.Edge(1, 1, 1, 2)));
    }

    @Test
    void uShapeKeepsItsCourtyardOpen() {
        var cells = List.of(p(0, 0), p(0, 1), p(0, 2), p(1, 2), p(2, 2), p(2, 1), p(2, 0));
        assertEquals(16, BuildingFootprint.boundary(cells).size());
        assertFalse(BuildingFootprint.contains(cells, 1, 0));
        assertFalse(BuildingFootprint.contains(cells, 1, 1));
    }

    @Test
    void duplicateVerticalCellsDoNotCreateDuplicateEdges() {
        assertEquals(4, BuildingFootprint.boundary(List.of(p(0, 0), p(0, 0).up())).size());
    }
}
