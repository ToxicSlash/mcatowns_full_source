package com.example.mcatowns.client;

import com.example.mcatowns.network.TownBlueprintView;
import com.example.mcatowns.town.BuildingFootprint;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

/** Shared map geometry for normal outlines, hover and click selection. */
public final class TownMapFootprint {
    private TownMapFootprint() { }

    private static double scale(int size) { return (size / 2 - 5) / 64.0; }
    private static int project(int origin, int size, double blocks) {
        return origin + size / 2 + (int) Math.round(blocks * scale(size));
    }
    private static boolean inside(int x, int origin, int size) {
        return x >= origin + 1 && x < origin + size - 1;
    }

    public static void draw(DrawContext context, TownBlueprintView.BuildingEntry building,
                            BlockPos center, int mapX, int mapY, int size, int color) {
        if (building.footprint().isEmpty()) {
            // A missing/unloaded/unverified footprint gets a small location
            // marker, never a guessed rectangular building outline.
            int x = project(mapX, size, building.pos().getX() - center.getX());
            int y = project(mapY, size, building.pos().getZ() - center.getZ());
            if (inside(x - 1, mapX, size) && inside(x + 1, mapX, size)
                    && inside(y - 1, mapY, size) && inside(y + 1, mapY, size)) {
                context.fill(x - 1, y - 1, x + 2, y + 2, color);
            }
            return;
        }
        for (var edge : BuildingFootprint.boundary(building.footprint())) {
            int x1 = project(mapX, size, edge.x1() - center.getX() - 0.5);
            int x2 = project(mapX, size, edge.x2() - center.getX() - 0.5);
            int y1 = project(mapY, size, edge.z1() - center.getZ() - 0.5);
            int y2 = project(mapY, size, edge.z2() - center.getZ() - 0.5);
            int left = Math.max(mapX + 1, Math.min(x1, x2));
            int right = Math.min(mapX + size - 2, Math.max(x1, x2));
            int top = Math.max(mapY + 1, Math.min(y1, y2));
            int bottom = Math.min(mapY + size - 2, Math.max(y1, y2));
            if (left <= right && top <= bottom) context.fill(left, top, right + 1, bottom + 1, color);
        }
    }

    public static boolean hit(TownBlueprintView.BuildingEntry building, BlockPos center,
                              int mapX, int mapY, int size, double mouseX, double mouseY) {
        if (mouseX < mapX + 1 || mouseX >= mapX + size - 1
                || mouseY < mapY + 1 || mouseY >= mapY + size - 1) return false;
        if (building.footprint().isEmpty()) {
            int x = project(mapX, size, building.pos().getX() - center.getX());
            int y = project(mapY, size, building.pos().getZ() - center.getZ());
            return Math.abs(mouseX - x) <= 2 && Math.abs(mouseY - y) <= 2;
        }
        int x = center.getX() + (int) Math.floor((mouseX - mapX - size / 2) / scale(size) + 0.5);
        int z = center.getZ() + (int) Math.floor((mouseY - mapY - size / 2) / scale(size) + 0.5);
        return BuildingFootprint.contains(building.footprint(), x, z);
    }
}
