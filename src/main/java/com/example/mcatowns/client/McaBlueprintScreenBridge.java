package com.example.mcatowns.client;

import com.example.mcatowns.network.TownBlueprintView;
import com.example.mcatowns.screen.TownBlueprintScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.lang.reflect.Method;

public final class McaBlueprintScreenBridge {
    private static TownBlueprintView view;
    private static String nextPage = "";

    private McaBlueprintScreenBridge() {
    }

    public static void open(TownBlueprintView townView) {
        view = townView;
        MinecraftClient.getInstance().setScreen(new TownBlueprintScreen(townView, nextPage));
        nextPage = "";
    }

    public static TownBlueprintView getView() {
        return view;
    }

    public static void openNextOn(String page) {
        nextPage = page == null ? "" : page;
    }

    public static void openNative(String page) {
        TownBlueprintView saved = view;
        view = null;
        MinecraftClient client = MinecraftClient.getInstance();
        for (String className : new String[]{
                "net.mca.client.gui.BlueprintScreen",
                "fabric.net.mca.client.gui.BlueprintScreen"
        }) {
            try {
                Object screen = Class.forName(className).getConstructor().newInstance();
                if (page != null && !page.isBlank()) {
                    Method setPage = screen.getClass().getDeclaredMethod("setPage", String.class);
                    setPage.setAccessible(true);
                    setPage.invoke(screen, page);
                }
                client.setScreen((Screen) screen);
                return;
            } catch (ReflectiveOperationException | ClassCastException ignored) {
                view = saved;
            }
        }
        view = saved;
        client.setScreen(new TownBlueprintScreen(saved));
    }
}
