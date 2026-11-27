package com.example.mcatowns.client;

import com.example.mcatowns.network.TownBlueprintView;
import com.example.mcatowns.screen.TownBlueprintScreen;
import com.example.mcatowns.town.TownBuildingCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.EnumMap;
import java.util.Map;
import java.lang.reflect.Method;

public final class McaBlueprintScreenBridge {
    private static TownBlueprintView view;
    private static String nextPage = "";
    private static TownBuildingCategory nextCategory;
    private static final Map<TownBuildingCategory, String> catalogSelections = new EnumMap<>(TownBuildingCategory.class);
    private static boolean mapAdvancedControls;

    private McaBlueprintScreenBridge() {
    }

    public static void open(TownBlueprintView townView) {
        view = townView;
        MinecraftClient.getInstance().setScreen(new TownBlueprintScreen(townView, nextPage, nextCategory));
        nextPage = "";
        nextCategory = null;
    }

    public static TownBlueprintView getView() {
        return view;
    }

    public static void rememberCatalogSelection(TownBuildingCategory category, String buildingId) {
        if (category != null && buildingId != null && !buildingId.isBlank()) {
            catalogSelections.put(category, buildingId);
        }
    }

    public static String rememberedCatalogSelection(TownBuildingCategory category) {
        return category == null ? null : catalogSelections.get(category);
    }

    public static void rememberMapAdvancedControls(boolean expanded) {
        mapAdvancedControls = expanded;
    }

    public static boolean mapAdvancedControls() {
        return mapAdvancedControls;
    }

    public static void openNextOn(String page) {
        nextPage = page == null ? "" : page;
    }

    public static void openNextOn(String page, TownBuildingCategory category) {
        nextPage = page == null ? "" : page;
        nextCategory = category;
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
