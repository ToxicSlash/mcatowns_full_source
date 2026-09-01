package com.example.mcatowns.mixin.client;

import com.example.mcatowns.network.TownBlueprintView;
import com.example.mcatowns.screen.TownBlueprintScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;

/** UI polish for the standalone MCA Towns blueprint screen. */
@Mixin(value = TownBlueprintScreen.class, remap = false)
public abstract class TownBlueprintScreenMixin extends Screen {
    @Shadow(remap = false) private TownBlueprintView view;
    @Shadow(remap = false) private TownBlueprintView.BuildingEntry selectedRegisteredBuilding;
    @Shadow(remap = false) private boolean showOutputBonuses;
    @Shadow(remap = false) private String page;

    @Unique private static final int MCATOWNS_VISIBLE_BUILDINGS = 10;
    @Unique private int mcatowns$buildingScroll;

    protected TownBlueprintScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "addBuildingButtons", at = @At("HEAD"), cancellable = true, remap = false)
    private void mcatowns$scrollableBuildingButtons(int left, int top, CallbackInfo ci) {
        ci.cancel();
        List<TownBlueprintView.BuildingEntry> all = view.registeredBuildings();
        int maxScroll = Math.max(0, all.size() - MCATOWNS_VISIBLE_BUILDINGS);
        mcatowns$buildingScroll = Math.max(0, Math.min(maxScroll, mcatowns$buildingScroll));

        if (selectedRegisteredBuilding == null || !all.contains(selectedRegisteredBuilding)) {
            selectedRegisteredBuilding = all.isEmpty() ? null : all.get(0);
        }
        if (selectedRegisteredBuilding != null) {
            int selectedIndex = all.indexOf(selectedRegisteredBuilding);
            if (selectedIndex < mcatowns$buildingScroll) mcatowns$buildingScroll = selectedIndex;
            if (selectedIndex >= mcatowns$buildingScroll + MCATOWNS_VISIBLE_BUILDINGS) {
                mcatowns$buildingScroll = selectedIndex - MCATOWNS_VISIBLE_BUILDINGS + 1;
            }
            mcatowns$buildingScroll = Math.max(0, Math.min(maxScroll, mcatowns$buildingScroll));
        }

        int end = Math.min(all.size(), mcatowns$buildingScroll + MCATOWNS_VISIBLE_BUILDINGS);
        List<TownBlueprintView.BuildingEntry> visible = all.subList(mcatowns$buildingScroll, end);
        for (int i = 0; i < visible.size(); i++) {
            TownBlueprintView.BuildingEntry building = visible.get(i);
            ButtonWidget button = ButtonWidget.builder(Text.literal(building.name()), ignored -> {
                        selectedRegisteredBuilding = building;
                        showOutputBonuses = false;
                        mcatowns$reinitialize();
                    })
                    .dimensions(left + 16, top + 46 + i * 16, 150, 15)
                    .tooltip(Tooltip.of(Text.literal(mcatowns$outputLine(building))))
                    .build();
            button.active = !building.equals(selectedRegisteredBuilding);
            addDrawableChild(button);
        }

        if (selectedRegisteredBuilding != null) {
            ButtonWidget bonuses = ButtonWidget.builder(Text.literal(showOutputBonuses ? "Overview" : "Bonuses"), ignored -> {
                        showOutputBonuses = !showOutputBonuses;
                        mcatowns$reinitialize();
                    })
                    .dimensions(left + 250, top + 174, 82, 18).build();
            addDrawableChild(bonuses);
        }
    }

    @Inject(method = "drawBuildings", at = @At("TAIL"), remap = false)
    private void mcatowns$drawBuildingPolish(DrawContext context, int left, int top, CallbackInfo ci) {
        List<TownBlueprintView.BuildingEntry> all = view.registeredBuildings();
        if (all.size() > MCATOWNS_VISIBLE_BUILDINGS) {
            int maxScroll = all.size() - MCATOWNS_VISIBLE_BUILDINGS;
            int trackX = left + 170;
            int trackY = top + 46;
            int trackHeight = 159;
            context.fill(trackX, trackY, trackX + 3, trackY + trackHeight, 0xFF3A3A3A);
            int handleHeight = Math.max(18, trackHeight * MCATOWNS_VISIBLE_BUILDINGS / all.size());
            int handleTravel = trackHeight - handleHeight;
            int handleY = trackY + (maxScroll == 0 ? 0 : handleTravel * mcatowns$buildingScroll / maxScroll);
            context.fill(trackX, handleY, trackX + 3, handleY + handleHeight, 0xFFAAAAAA);
            int first = mcatowns$buildingScroll + 1;
            int last = Math.min(all.size(), mcatowns$buildingScroll + MCATOWNS_VISIBLE_BUILDINGS);
            context.drawTextWithShadow(textRenderer, Text.literal(first + "-" + last + " / " + all.size()), left + 18, top + 207, 0xAAAAAA);
        }

        if (!showOutputBonuses || selectedRegisteredBuilding == null) return;
        TownBlueprintView.BuildingEntry building = selectedRegisteredBuilding;
        int x = left + 188;
        int y = top + 52;
        context.fill(x - 2, y + 78, left + 356, top + 170, 0xEE252525);
        context.drawTextWithShadow(textRenderer, Text.literal("Output breakdown"), x, y + 82, 0xFFE080);
        context.drawTextWithShadow(textRenderer, Text.literal("Base output: 100%"), x, y + 96, 0xDDDDDD);
        context.drawTextWithShadow(textRenderer, Text.literal("Staffing: " + building.staffingPercent() + "% (" + building.workerCount() + "/" + building.workerSlots() + ")"), x, y + 108, 0xDDDDDD);
        context.drawTextWithShadow(textRenderer, Text.literal("Tier modifier: +" + building.tierBonus() + "%"), x, y + 120, 0xDDDDDD);
        context.drawTextWithShadow(textRenderer, Text.literal("Furniture: +" + building.furnitureBonus() + "% (" + building.furnitureCount() + ")"), x, y + 132, 0xDDDDDD);
        context.drawTextWithShadow(textRenderer, Text.literal("Synergy: +" + building.synergyBonus() + "% (" + building.synergyCount() + ")"), x, y + 144, 0xDDDDDD);
        context.drawTextWithShadow(textRenderer, Text.literal("Final output: " + building.output() + "%"), x, y + 156, 0x80D080);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if ("buildings".equals(page) && view.registeredBuildings().size() > MCATOWNS_VISIBLE_BUILDINGS) {
            int left = mcatowns$panelLeft();
            int top = mcatowns$panelTop();
            if (mouseX >= left + 12 && mouseX <= left + 178 && mouseY >= top + 40 && mouseY <= top + 210) {
                int maxScroll = view.registeredBuildings().size() - MCATOWNS_VISIBLE_BUILDINGS;
                int next = Math.max(0, Math.min(maxScroll, mcatowns$buildingScroll + (amount < 0 ? 1 : -1)));
                if (next != mcatowns$buildingScroll) {
                    mcatowns$buildingScroll = next;
                    mcatowns$reinitialize();
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && "map".equals(page) && !view.registeredBuildings().isEmpty()) {
            int mapX = mcatowns$panelLeft() + 24;
            int mapY = mcatowns$panelTop() + 48;
            int size = 136;
            if (mouseX >= mapX && mouseX <= mapX + size && mouseY >= mapY && mouseY <= mapY + size) {
                TownBlueprintView.BuildingEntry hit = view.registeredBuildings().stream()
                        .filter(building -> mcatowns$footprint(building, mapX, mapY, size).contains(mouseX, mouseY))
                        .min(Comparator.comparingDouble(building -> {
                            Rect rect = mcatowns$footprint(building, mapX, mapY, size);
                            double dx = mouseX - (rect.left + rect.right) / 2.0;
                            double dy = mouseY - (rect.top + rect.bottom) / 2.0;
                            return dx * dx + dy * dy;
                        }))
                        .orElse(null);
                if (hit != null) {
                    selectedRegisteredBuilding = hit;
                    showOutputBonuses = false;
                    int index = view.registeredBuildings().indexOf(hit);
                    int maxScroll = Math.max(0, view.registeredBuildings().size() - MCATOWNS_VISIBLE_BUILDINGS);
                    mcatowns$buildingScroll = Math.max(0, Math.min(maxScroll, index - MCATOWNS_VISIBLE_BUILDINGS / 2));
                    page = "buildings";
                    mcatowns$reinitialize();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void mcatowns$highlightHoveredMapBuilding(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!"map".equals(page)) return;
        int mapX = mcatowns$panelLeft() + 24;
        int mapY = mcatowns$panelTop() + 48;
        int size = 136;
        if (mouseX < mapX || mouseX > mapX + size || mouseY < mapY || mouseY > mapY + size) return;
        view.registeredBuildings().stream()
                .filter(building -> mcatowns$footprint(building, mapX, mapY, size).contains(mouseX, mouseY))
                .findFirst().ifPresent(building -> {
                    Rect rect = mcatowns$footprint(building, mapX, mapY, size);
                    context.drawBorder(rect.left - 1, rect.top - 1, rect.width() + 2, rect.height() + 2, 0xFFFFFFFF);
                });
    }

    @Unique private void mcatowns$reinitialize() { clearChildren(); init(); }

    @Unique
    private String mcatowns$outputLine(TownBlueprintView.BuildingEntry building) {
        String line = "Output: " + building.output() + "%";
        if ("farm".equals(building.type())) {
            int base = Math.max(3, building.tier() * 3);
            int food = Math.max(0, Math.round(base * building.output() / 100.0f));
            line += " (+" + food + " Food/Day)";
        }
        return line;
    }

    @Unique
    private Rect mcatowns$footprint(TownBlueprintView.BuildingEntry building, int mapX, int mapY, int size) {
        int centerX = mapX + size / 2;
        int centerY = mapY + size / 2;
        int x1 = centerX + mcatowns$clampMapOffset(building.minPos().getX() - view.centerPos().getX(), size);
        int x2 = centerX + mcatowns$clampMapOffset(building.maxPos().getX() - view.centerPos().getX(), size);
        int y1 = centerY + mcatowns$clampMapOffset(building.minPos().getZ() - view.centerPos().getZ(), size);
        int y2 = centerY + mcatowns$clampMapOffset(building.maxPos().getZ() - view.centerPos().getZ(), size);
        int left = Math.max(mapX + 1, Math.min(x1, x2));
        int right = Math.min(mapX + size - 1, Math.max(x1, x2));
        int top = Math.max(mapY + 1, Math.min(y1, y2));
        int bottom = Math.min(mapY + size - 1, Math.max(y1, y2));
        int width = Math.max(6, right - left + 1);
        int height = Math.max(6, bottom - top + 1);
        if (left + width > mapX + size - 1) left = mapX + size - 1 - width;
        if (top + height > mapY + size - 1) top = mapY + size - 1 - height;
        return new Rect(left, top, left + width, top + height);
    }

    @Unique private static int mcatowns$clampMapOffset(int blocks, int mapSize) {
        int clamped = Math.max(-64, Math.min(64, blocks));
        return clamped * (mapSize / 2 - 5) / 64;
    }

    @Unique private int mcatowns$panelLeft() { return Math.max(128, width / 2 - 180); }
    @Unique private int mcatowns$panelTop() { return (height - 220) / 2; }

    @Unique
    private record Rect(int left, int top, int right, int bottom) {
        boolean contains(double x, double y) { return x >= left && x <= right && y >= top && y <= bottom; }
        int width() { return Math.max(1, right - left); }
        int height() { return Math.max(1, bottom - top); }
    }
}
