package com.example.mcatowns.gametest;

import com.example.mcatowns.network.TownBlueprintView;
import com.example.mcatowns.screen.TownBlueprintScreen;
import com.example.mcatowns.town.TownRank;
import com.example.mcatowns.client.TownMapFootprint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.math.BlockPos;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TownVisualProbe implements ClientModInitializer {
    private int ticks;
    private int stage;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("town.test.visual")) return;
        if (!FabricLoader.getInstance().isModLoaded("mca") || !FabricLoader.getInstance().isModLoaded("mcatowns")) {
            throw new IllegalStateException("MCA and Towns must both load");
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (stage == 2) return;
            boolean ready = stage == 0 ? client.currentScreen instanceof TitleScreen
                    : client.currentScreen instanceof TownBlueprintScreen;
            if (!ready || client.getOverlay() != null) { ticks = 0; return; }
            if (++ticks < 40) return;
            ticks = 0;
            Path dir = client.runDirectory.toPath();
            try (NativeImage image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
                if (stage == 0) {
                    image.writeTo(dir.resolve("town-title.png"));
                    Files.writeString(dir.resolve("town-title-ready.txt"),
                            "TOWN_MCA_CLIENT_TITLE_OK " + image.getWidth() + "x" + image.getHeight());
                    System.out.println("TOWN_MCA_CLIENT_TITLE_OK");
                    stage = 1;
                    client.setScreen(new TownBlueprintScreen(fixture()));
                } else {
                    // Screen state alone can pass while the loading overlay still covers the framebuffer.
                    int mapBackground = 0, outline = 0;
                    for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                        int rgb = image.getColor(x, y) & 0xFFFFFF;
                        if (rgb == 0x151515) mapBackground++;
                        if (rgb == 0xD0D8D8 || rgb == 0x60C0E0) outline++;
                    }
                    image.writeTo(dir.resolve("town-map.png"));
                    if (mapBackground < 500 || outline < 20) {
                        throw new IllegalStateException("Blueprint not visible in framebuffer: background=" + mapBackground + " outline=" + outline);
                    }
                    System.out.println("TOWN_MAP_PIXELS " + mapBackground + " " + outline);
                    Files.writeString(dir.resolve("town-map-ready.txt"),
                            "TOWN_BLUEPRINT_MAP_OK " + image.getWidth() + "x" + image.getHeight());
                    System.out.println("TOWN_BLUEPRINT_MAP_OK");
                    stage = 2;
                    client.scheduleStop();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Town visual screenshot probe failed", e);
            }
        });
    }

    private static TownBlueprintView fixture() {
        BlockPos center = new BlockPos(0, 64, 0);
        List<BlockPos> rectangle = new ArrayList<>(), concave = new ArrayList<>(), field = new ArrayList<>();
        for (int x = -48; x < -32; x++) for (int z = -28; z < -14; z++) rectangle.add(new BlockPos(x, 64, z));
        for (int x = 16; x < 36; x++) for (int z = -30; z < -10; z++) {
            if (x < 24 || z >= -18) concave.add(new BlockPos(x, 64, z));
        }
        for (int x = -40; x < -23; x += 3) for (int z = 16; z < 34; z++) field.add(new BlockPos(x, 64, z));
        var house = entry("residence", "L-shaped residence", concave, true);
        // Hit testing must reject the empty corner and accept the occupied arm.
        if (TownMapFootprint.hit(house, center, 0, 0, 136, 98, 44)
                || !TownMapFootprint.hit(house, center, 0, 0, 136, 88, 44)) {
            throw new IllegalStateException("Concave map hit test failed");
        }
        return new TownBlueprintView(false, "CI wall footprint fixture", "Mayor", TownRank.CAMP, 10, 0,
                List.of(), 70, 3, 6, 10, 20, 3, 0, 10, false, false,
                center, center, center.add(4, 0, 18), true, 0,
                List.of(entry("residence", "Detected rectangle", rectangle, true)), "", false,
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(house, entry("farm", "Irrigated farm", field, false)),
                "", "", 0L, 0, 0, List.of(), "", "", 0L, 0L, false, 0, "", 0L);
    }

    private static TownBlueprintView.BuildingEntry entry(String type, String name, List<BlockPos> cells, boolean enclosed) {
        BlockPos first = cells.get(0);
        // Deliberately exaggerated old bounding box: the rendered outline must use cells.
        return new TownBlueprintView.BuildingEntry(UUID.randomUUID(), type, name, first, "minecraft:paper",
                first.add(-4, 0, -4), first.add(28, 4, 28), 1, "ACTIVE",
                100, 1, 1, enclosed ? 0 : 108, 100, 0, 0, 0, 0, 0, enclosed, List.copyOf(cells));
    }
}
