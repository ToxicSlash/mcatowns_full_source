package com.example.mcatowns.gametest;

import com.example.mcatowns.client.McaBlueprintScreenBridge;
import com.example.mcatowns.network.ModNetworking;
import com.example.mcatowns.screen.TownBlueprintScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TownConnectedClientProbe implements ClientModInitializer {
    private String current = "";
    private int ticks;
    private boolean sent;
    private boolean done;

    @Override public void onInitializeClient() {
        String path = System.getProperty("town.test.connected");
        if (path == null) return;
        Path shared = Path.of(path);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (done || client.player == null || client.world == null || client.getOverlay() != null) return;
            try {
                if (!Files.exists(shared.resolve("phase"))) return;
                String phase = Files.readString(shared.resolve("phase")).trim();
                if (phase.isEmpty()) return;
                if (!phase.equals(current)) {
                    current = phase;
                    ticks = 0;
                    sent = false;
                }
                if (++ticks < 20) return;
                if (!sent) {
                    switch (phase) {
                        case "deny", "found" -> ModNetworking.sendFoundTown();
                        case "inspect", "farm_inspect", "duplicate_inspect" -> ModNetworking.sendInspectTownBuilding("farm");
                        case "blocked_register", "register", "duplicate_register" -> ModNetworking.sendRegisterTownBuilding("farm");
                        case "campfire_inspect" -> ModNetworking.sendInspectTownBuilding("campfire");
                        case "campfire_register" -> ModNetworking.sendRegisterTownBuilding("campfire");
                        case "map" -> ModNetworking.sendOpenTownBlueprint();
                        case "complete" -> {
                            done = true;
                            client.scheduleStop();
                            return;
                        }
                        default -> throw new IllegalStateException("Unexpected phase " + phase);
                    }
                    sent = true;
                    System.out.println("TOWN_CONNECTED_PACKET " + phase);
                    if (!phase.equals("map")) Files.writeString(shared.resolve("ack-" + phase), "sent");
                    return;
                }
                if (phase.equals("map") && ticks >= 60 && !Files.exists(shared.resolve("ack-map"))
                        && client.currentScreen instanceof TownBlueprintScreen) {
                    var view = McaBlueprintScreenBridge.getView();
                    if (view == null || view.founding() || view.registeredBuildings().size() != 2) return;
                    var farm = view.registeredBuildings().stream().filter(x -> x.type().equals("farm")).findFirst().orElseThrow();
                    if (!farm.type().equals("farm") || farm.enclosed() || farm.cropState() != 18
                            || farm.footprint().size() != 18 || !view.canManage()) {
                        throw new IllegalStateException("Server-fed farm view wrong: " + farm);
                    }
                    try (NativeImage image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
                        int background = 0;
                        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                            if ((image.getColor(x, y) & 0xFFFFFF) == 0x151515) background++;
                        }
                        image.writeTo(shared.resolve("town-connected-map.png"));
                        if (background < 500) throw new IllegalStateException("Connected map not visible");
                    }
                    Files.writeString(shared.resolve("ack-map"), "verified");
                    System.out.println("TOWN_CONNECTED_CLIENT_MAP_OK");
                }
            } catch (Exception e) {
                throw new IllegalStateException("Connected client phase failed " + current, e);
            }
        });
    }
}
