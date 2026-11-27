package com.example.mcatowns.town;

import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Geometry of verified interior floor cells (or actual farmland tiles), not a roof projection. */
public final class BuildingFootprint {
    private BuildingFootprint() { }

    public record Cell(int x, int z) { }
    public record Edge(int x1, int z1, int x2, int z2) { }

    public static Set<Cell> cells(Collection<BlockPos> footprint) {
        Set<Cell> result = new LinkedHashSet<>();
        for (BlockPos pos : footprint) result.add(new Cell(pos.getX(), pos.getZ()));
        return result;
    }

    public static List<Edge> boundary(Collection<BlockPos> footprint) {
        Set<Cell> cells = cells(footprint);
        List<Edge> edges = new ArrayList<>();
        for (Cell cell : cells) {
            int x = cell.x(), z = cell.z();
            if (!cells.contains(new Cell(x - 1, z))) edges.add(new Edge(x, z, x, z + 1));
            if (!cells.contains(new Cell(x + 1, z))) edges.add(new Edge(x + 1, z, x + 1, z + 1));
            if (!cells.contains(new Cell(x, z - 1))) edges.add(new Edge(x, z, x + 1, z));
            if (!cells.contains(new Cell(x, z + 1))) edges.add(new Edge(x, z + 1, x + 1, z + 1));
        }
        return edges;
    }

    public static boolean contains(Collection<BlockPos> footprint, int x, int z) {
        for (BlockPos pos : footprint) {
            if (pos.getX() == x && pos.getZ() == z) return true;
        }
        return false;
    }
}
