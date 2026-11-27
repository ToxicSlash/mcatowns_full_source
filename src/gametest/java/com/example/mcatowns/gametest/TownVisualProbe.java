package com.example.mcatowns.gametest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TownVisualProbe implements ClientModInitializer {
    private int ticks;
    private boolean finished;

    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("town.test.visual")) return;
        if (!FabricLoader.getInstance().isModLoaded("mca") || !FabricLoader.getInstance().isModLoaded("mcatowns")) {
            throw new IllegalStateException("MCA and Towns must both load");
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (finished) return;
            if (!(client.currentScreen instanceof TitleScreen)) {
                ticks = 0;
                return;
            }
            if (++ticks < 40) return;
            finished = true;
            Path dir = client.runDirectory.toPath();
            try (NativeImage image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
                image.writeTo(dir.resolve("town-title.png"));
                Files.writeString(dir.resolve("town-title-ready.txt"),
                        "TOWN_MCA_CLIENT_TITLE_OK " + image.getWidth() + "x" + image.getHeight());
                System.out.println("TOWN_MCA_CLIENT_TITLE_OK");
                client.scheduleStop();
            } catch (Exception e) {
                throw new IllegalStateException("Town visual screenshot probe failed", e);
            }
        });
    }
}
