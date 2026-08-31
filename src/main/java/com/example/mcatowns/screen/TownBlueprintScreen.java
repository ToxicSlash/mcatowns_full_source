package com.example.mcatowns.screen;

import com.example.mcatowns.client.McaBlueprintScreenBridge;
import com.example.mcatowns.event.BlueprintTownCreationHandler;
import com.example.mcatowns.network.ModNetworking;
import com.example.mcatowns.network.TownBlueprintView;
import com.example.mcatowns.town.TownBuildingDefinition;
import com.example.mcatowns.town.TownBuildingCategory;
import com.example.mcatowns.util.TownTextHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class TownBlueprintScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 220;
    private static final int TAB_WIDTH = 92;
    private final TownBlueprintView view;
    private final Map<ButtonWidget, ItemStack> icons = new LinkedHashMap<>();
    private TownBuildingCategory category = TownBuildingCategory.RESIDENTIAL;
    private TownBlueprintView.BuildingOption selectedBuilding;
    private TownBlueprintView.ResidentEntry selectedResident;
    private List<TownBlueprintView.ResidentEntry> visibleResidents = List.of();
    private boolean showSpecialists;
    private boolean advancedControls;
    private String page = "map";

    public TownBlueprintScreen(TownBlueprintView view) {
        super(Text.translatable(view.founding() ? "screen.mcatowns.blueprint.start_title" : "screen.mcatowns.blueprint.town_title"));
        this.view = view;
    }

    public TownBlueprintScreen(TownBlueprintView view, String initialPage) {
        this(view);
        if (initialPage != null && !initialPage.isBlank()) this.page = initialPage;
    }

    @Override
    protected void init() {
        icons.clear();
        int left = panelLeft();
        int top = panelTop();
        if (view.founding()) {
            ButtonWidget start = ButtonWidget.builder(Text.translatable("screen.mcatowns.blueprint.start"), button -> ModNetworking.sendFoundTown())
                    .dimensions(left + 90, top + 162, 120, 20)
                    .tooltip(Tooltip.of(Text.translatable("screen.mcatowns.blueprint.start_cost", BlueprintTownCreationHandler.FOUNDING_SCRAP_COST)))
                    .build();
            start.active = view.hasCrown() && view.blueprintScraps() >= BlueprintTownCreationHandler.FOUNDING_SCRAP_COST;
            addDrawableChild(start);
            addBackButton(left, top);
            return;
        }

        addBackButton(left, top);
        addTabs(left, top);
        if (page.equals("catalog")) addCatalogButtons(left, top);
        if (page.equals("villagers")) addVillagerButtons(left, top);
        if (page.equals("rules")) addRuleButtons(left, top);
        if (page.equals("map")) addMapButtons(left, top);
        if (page.equals("rank")) addRankButtons(left, top);
        if (view.canManage()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("E"), button -> client.setScreen(new RenameTownScreen(this, view.anchorPos(), view.name())))
                    .dimensions(left + 8, top + 7, 16, 14)
                    .tooltip(Tooltip.of(Text.literal("Rename town")))
                    .build());
        }
        if (view.canRemove()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.mcatowns.town_manager.remove"), button -> confirmRemoval())
                    .dimensions(Math.min(width - 84, left + PANEL_WIDTH + 8), top + 2, 76, 18)
                    .build());
        }
    }

    private void addBackButton(int left, int top) {
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close())
                .dimensions(left - TAB_WIDTH - 12, top + 8, TAB_WIDTH, 18)
                .tooltip(Tooltip.of(Text.literal("Close")))
                .build());
    }

    private void addTabs(int left, int top) {
        String[] pages = {"map", "rank", "catalog", "villagers", "rules", "refresh"};
        for (int i = 0; i < pages.length; i++) {
            String target = pages[i];
            ButtonWidget button = ButtonWidget.builder(Text.literal(titleCase(target)), ignored -> {
                        if (target.equals("refresh")) {
                            McaBlueprintScreenBridge.openNextOn(page);
                            ModNetworking.sendOpenTownBlueprint();
                            return;
                        }
                        page = target;
                        clearChildren();
                        init();
                    })
                    .dimensions(left - TAB_WIDTH - 12, top + 58 + i * 22, TAB_WIDTH, 18)
                    .build();
            button.active = !target.equals(page);
            addDrawableChild(button);
        }
    }

    private void addCatalogButtons(int left, int top) {
        TownBuildingCategory[] categories = TownBuildingCategory.values();
        for (int i = 0; i < categories.length; i++) {
            TownBuildingCategory value = categories[i];
            ButtonWidget button = ButtonWidget.builder(Text.literal(shortCategory(value)), ignored -> {
                        category = value;
                        selectedBuilding = null;
                        clearChildren();
                        init();
                    })
                    .dimensions(left + 12 + i * 48, top + 42, 46, 18)
                    .build();
            button.active = value != category;
            addDrawableChild(button);
        }

        List<TownBlueprintView.BuildingOption> options = view.buildingOptions().stream()
                .filter(option -> option.category() == category).toList();
        if (selectedBuilding == null && !options.isEmpty()) selectedBuilding = options.get(0);
        for (int i = 0; i < options.size(); i++) {
            TownBlueprintView.BuildingOption option = options.get(i);
            ButtonWidget button = ButtonWidget.builder(Text.empty(), ignored -> selectedBuilding = option)
                    .dimensions(left + 16 + (i % 5) * 28, top + 72 + (i / 5) * 30, 24, 24)
                    .tooltip(Tooltip.of(Text.literal(option.name())))
                    .build();
            button.active = option != selectedBuilding;
            addDrawableChild(button);
            icons.put(button, buildingIcon(option));
        }
        if (selectedBuilding != null) {
            ButtonWidget inspect = ButtonWidget.builder(Text.literal("Inspect"),
                            ignored -> {
                                McaBlueprintScreenBridge.openNextOn("catalog");
                                ModNetworking.sendInspectTownBuilding(selectedBuilding.id());
                            })
                    .dimensions(left + 190, top + 162, 72, 18)
                    .build();
            inspect.active = view.canManage() && !selectedBuilding.legacy();
            addDrawableChild(inspect);

            ButtonWidget register = ButtonWidget.builder(Text.literal(selectedBuilding.legacy() ? "MCA Building" : "Register"),
                            ignored -> {
                                McaBlueprintScreenBridge.openNextOn("catalog");
                                ModNetworking.sendRegisterTownBuilding(selectedBuilding.id());
                            })
                    .dimensions(left + 268, top + 162, 74, 18)
                    .tooltip(Tooltip.of(Text.literal(cost(selectedBuilding))))
                    .build();
            register.active = view.canManage() && selectedBuilding.unlocked() && !selectedBuilding.legacy()
                    && selectedBuilding.id().equals(view.inspectionType()) && view.inspectionPassed();
            addDrawableChild(register);
        }
    }

    private void addVillagerButtons(int left, int top) {
        ButtonWidget residents = ButtonWidget.builder(Text.literal("Residents"), ignored -> {
            showSpecialists = false;
            selectedResident = null;
            clearChildren();
            init();
        }).dimensions(left + 16, top + 44, 86, 18).build();
        residents.active = showSpecialists;
        addDrawableChild(residents);

        ButtonWidget specialists = ButtonWidget.builder(Text.literal("Specialists"), ignored -> {
            showSpecialists = true;
            selectedResident = null;
            clearChildren();
            init();
        }).dimensions(left + 108, top + 44, 86, 18).build();
        specialists.active = !showSpecialists;
        addDrawableChild(specialists);

        visibleResidents = view.residents().stream()
                .filter(entry -> entry.specialist() == showSpecialists).limit(7).toList();
        List<TownBlueprintView.ResidentEntry> entries = visibleResidents;
        if (selectedResident == null || !entries.contains(selectedResident)) {
            selectedResident = entries.isEmpty() ? null : entries.get(0);
        }
        for (int i = 0; i < entries.size(); i++) {
            TownBlueprintView.ResidentEntry entry = entries.get(i);
            ButtonWidget select = ButtonWidget.builder(Text.literal(entry.name()), ignored -> {
                        selectedResident = entry;
                        clearChildren();
                        init();
                    })
                    .dimensions(left + 16, top + 68 + i * 18, 166, 16)
                    .tooltip(Tooltip.of(Text.literal("Assigned: " + entry.assignedBuildingName())))
                    .build();
            select.active = !entry.equals(selectedResident);
            addDrawableChild(select);
        }

        ButtonWidget autoAssign = ButtonWidget.builder(Text.literal("Auto Assign"), ignored -> {
                    McaBlueprintScreenBridge.openNextOn("villagers");
                    ModNetworking.sendAutoAssignTownWorkers();
                })
                .dimensions(left + 256, top + 44, 86, 18)
                .tooltip(Tooltip.of(Text.literal("Fill empty worker slots without changing valid assignments.")))
                .build();
        autoAssign.active = view.canManage() && !view.residents().isEmpty();
        addDrawableChild(autoAssign);

        if (selectedResident == null) return;
        ButtonWidget unassign = ButtonWidget.builder(Text.literal("Unassign"), ignored -> {
                    McaBlueprintScreenBridge.openNextOn("villagers");
                    ModNetworking.sendAssignTownWorker(selectedResident.id(), new java.util.UUID(0L, 0L));
                })
                .dimensions(left + 192, top + 104, 72, 16)
                .build();
        unassign.active = view.canManage() && selectedResident.assignedBuildingId().getLeastSignificantBits() != 0L;
        addDrawableChild(unassign);

        List<TownBlueprintView.BuildingEntry> workplaces = view.registeredBuildings().stream()
                .filter(building -> building.workerSlots() > 0).limit(5).toList();
        for (int i = 0; i < workplaces.size(); i++) {
            TownBlueprintView.BuildingEntry building = workplaces.get(i);
            String label = building.name() + " " + building.workerCount() + "/" + building.workerSlots();
            ButtonWidget assign = ButtonWidget.builder(Text.literal(label), ignored -> {
                        McaBlueprintScreenBridge.openNextOn("villagers");
                        ModNetworking.sendAssignTownWorker(selectedResident.id(), building.id());
                    })
                    .dimensions(left + 270, top + 104 + i * 18, 72, 16)
                    .tooltip(Tooltip.of(Text.literal("Assign to " + building.name())))
                    .build();
            assign.active = view.canManage() && !building.id().equals(selectedResident.assignedBuildingId())
                    && building.workerCount() < building.workerSlots();
            addDrawableChild(assign);
        }
    }

    private void addRuleButtons(int left, int top) {
        ButtonWidget down = ButtonWidget.builder(Text.literal("-"), button ->
                ModNetworking.sendBlueprintTaxRate(view.anchorPos(), view.taxRate() - 1))
                .dimensions(left + 18, top + 82, 22, 18)
                .build();
        down.active = view.canManage() && view.taxRate() > 0;
        addDrawableChild(down);

        ButtonWidget up = ButtonWidget.builder(Text.literal("+"), button ->
                ModNetworking.sendBlueprintTaxRate(view.anchorPos(), view.taxRate() + 1))
                .dimensions(left + 118, top + 82, 22, 18)
                .build();
        up.active = view.canManage() && view.taxRate() < 3;
        addDrawableChild(up);
    }

    private void addMapButtons(int left, int top) {
        ButtonWidget detect = ButtonWidget.builder(Text.literal("Detect"), button -> {
                    McaBlueprintScreenBridge.openNextOn("map");
                    ModNetworking.sendDetectTownBuilding();
                })
                .dimensions(left + 188, top + 58, 74, 18)
                .tooltip(Tooltip.of(Text.literal("Detect the current building or room for inspection.")))
                .build();
        detect.active = view.canManage();
        addDrawableChild(detect);

        ButtonWidget advanced = ButtonWidget.builder(Text.literal(advancedControls ? "Basic" : "Advanced"), button -> {
                    advancedControls = !advancedControls;
                    clearChildren();
                    init();
                })
                .dimensions(left + 188, top + 148, 76, 18)
                .build();
        addDrawableChild(advanced);

        if (!advancedControls) return;

        ButtonWidget refresh = ButtonWidget.builder(Text.literal("Refresh"), button -> {
                    McaBlueprintScreenBridge.openNextOn("map");
                    ModNetworking.sendOpenTownBlueprint();
                })
                .dimensions(left + 268, top + 148, 74, 18)
                .build();
        addDrawableChild(refresh);

        ButtonWidget remove = ButtonWidget.builder(Text.literal("Remove Building"), button -> {
                    McaBlueprintScreenBridge.openNextOn("map");
                    ModNetworking.sendRemoveTownBuilding();
                })
                .dimensions(left + 188, top + 172, 100, 18)
                .tooltip(Tooltip.of(Text.literal("Removes the nearest registered building within 12 blocks.")))
                .build();
        remove.active = view.canManage();
        addDrawableChild(remove);

        ButtonWidget rankUp = ButtonWidget.builder(Text.literal("Advance Tier"), button -> ModNetworking.sendAdvanceTown(view.anchorPos()))
                .dimensions(left + 292, top + 172, 50, 18)
                .tooltip(Tooltip.of(Text.literal("Advances the town if the next tier requirements are met.")))
                .build();
        rankUp.active = view.canManage() && view.rank().next() != null;
        addDrawableChild(rankUp);
    }

    private void addRankButtons(int left, int top) {
        ButtonWidget upgrade = ButtonWidget.builder(Text.literal("Upgrade"), button -> { })
                .dimensions(left + 130, top + 190, 100, 18)
                .tooltip(Tooltip.of(Text.literal("Upgrade action pending.")))
                .build();
        upgrade.active = false;
        addDrawableChild(upgrade);
    }

    private void confirmRemoval() {
        if (client == null) return;
        client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                ModNetworking.sendRemoveTown(view.anchorPos());
                client.setScreen(null);
            } else {
                client.setScreen(this);
            }
        }, Text.translatable("screen.mcatowns.town_manager.remove_confirm"), Text.translatable("screen.mcatowns.town_manager.remove_warning")));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int left = panelLeft();
        int top = panelTop();
        context.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xEE252525);
        context.fill(left + 4, top + 4, left + PANEL_WIDTH - 4, top + 28, 0xFF3A3A3A);

        if (view.founding()) drawFounding(context, left, top);
        else drawEstablished(context, left, top);
        super.render(context, mouseX, mouseY, delta);
        if (page.equals("catalog")) icons.forEach((button, stack) -> context.drawItem(stack, button.getX() + 4, button.getY() + 4));
        drawRequirementTooltip(context, mouseX, mouseY);
    }

    private void drawFounding(DrawContext context, int left, int top) {
        context.drawCenteredTextWithShadow(textRenderer, title, left + PANEL_WIDTH / 2, top + 10, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.mcatowns.blueprint.start_description"), left + PANEL_WIDTH / 2, top + 55, 0xDDDDDD);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.mcatowns.blueprint.start_result"), left + PANEL_WIDTH / 2, top + 75, 0xAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.mcatowns.blueprint.inventory", view.blueprintScraps()), left + PANEL_WIDTH / 2, top + 102, 0xFFFFFF);
    }

    private void drawEstablished(DrawContext context, int left, int top) {
        context.drawTextWithShadow(textRenderer, Text.literal(view.name()).styled(style -> style.withBold(true)), left + 28, top + 8, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Prosperity " + view.prosperity() + "/" + view.rank().maxProsperity()
                + "  Base " + view.prosperityBase()), left + 10, top + 19, 0xFFE080);
        context.drawTextWithShadow(textRenderer, Text.literal("Food " + view.food() + "/" + view.foodCapacity()), left + 110, top + 8, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Population " + view.population() + "/" + view.populationCapacity()), left + 166, top + 8, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Specialists " + view.specialists() + "/" + view.rank().maxSpecialists()), left + 244, top + 8, 0xFFFFFF);

        switch (page) {
            case "rank" -> drawRank(context, left, top);
            case "villagers" -> drawVillagers(context, left, top);
            case "catalog" -> drawCatalog(context, left, top);
            case "rules" -> drawRules(context, left, top);
            default -> drawMap(context, left, top);
        }
    }

    private void drawRank(DrawContext context, int left, int top) {
        String nextTier = view.rank().next() == null ? "Max" : view.rank().next().displayName();
        context.drawTextWithShadow(textRenderer, Text.literal("Player Rank: " + view.playerRank()), left + 18, top + 48, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer,
                Text.literal("Tier: " + view.rank().displayName() + " > " + nextTier), left + 18, top + 64, 0xFFE080);
        for (int i = 0; i < view.infrastructure().size(); i++) {
            TownBlueprintView.InfrastructureEntry entry = view.infrastructure().get(i);
            String line = entry.type().displayName() + ": " + entry.available() + " free ("
                    + entry.provided() + " provided, " + entry.reserved() + " used)";
            context.drawTextWithShadow(textRenderer, Text.literal(line), left + 18, top + 78 + i * 12,
                    entry.available() > 0 ? 0xB8D8FF : 0xAAAAAA);
        }
        int checklistY = top + 82 + view.infrastructure().size() * 12;
        for (int i = 0; i < Math.min(5, view.rankChecklist().size()); i++) {
            String line = view.rankChecklist().get(i);
            context.drawTextWithShadow(textRenderer, Text.literal(line), left + 18, checklistY + i * 13,
                    line.startsWith("✓") || line.startsWith("âœ“") ? 0x80D080 : 0xE08080);
        }
    }

    private void drawCatalog(DrawContext context, int left, int top) {
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Building Catalog"), left + PANEL_WIDTH / 2, top + 32, 0xFFFFFF);
        if (selectedBuilding == null) return;
        int x = left + 192;
        int y = top + 74;
        context.drawTextWithShadow(textRenderer, Text.literal(selectedBuilding.name()), x, y, 0xFFE080);
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(selectedBuilding.description()), 146);
        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            context.drawTextWithShadow(textRenderer, lines.get(i), x, y + 14 + i * 11, 0xDDDDDD);
        }
        if (selectedBuilding.legacy()) {
            context.drawTextWithShadow(textRenderer, Text.literal("MCA building type"), x, y + 62, 0xAAAAAA);
            return;
        }
        context.drawTextWithShadow(textRenderer, Text.literal(selectedBuilding.unlocked() ? "Unlocked" : "Locked"), x, y + 62, selectedBuilding.unlocked() ? 0x80D080 : 0xE08080);
        context.drawTextWithShadow(textRenderer, Text.literal("Cost Requirements"), x, y + 70, requirementColor(selectedBuilding, true));
        context.drawTextWithShadow(textRenderer, Text.literal("Furniture Requirements"), x, y + 86, requirementColor(selectedBuilding, false));
    }

    private void drawVillagers(DrawContext context, int left, int top) {
        List<TownBlueprintView.ResidentEntry> entries = visibleResidents;
        if (selectedResident == null && !entries.isEmpty()) selectedResident = entries.get(0);
        int x = left + 192;
        int y = top + 74;
        if (selectedResident == null) {
            context.drawTextWithShadow(textRenderer, Text.literal(showSpecialists ? "No specialists recruited" : "No residents recruited"), x, y, 0xAAAAAA);
            return;
        }
        context.drawTextWithShadow(textRenderer, Text.literal(selectedResident.name()), x, y, 0xFFE080);
        context.drawTextWithShadow(textRenderer, Text.literal(selectedResident.specialist() ? "Specialist: " + titleCase(selectedResident.specialistType()) : "Resident"), x, y + 14, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Work: " + selectedResident.assignedBuildingName()), x, y + 28, 0xB8D8FF);
    }

    private void drawMap(DrawContext context, int left, int top) {
        int mapX = left + 24;
        int mapY = top + 48;
        int size = 136;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Town Map"), mapX + size / 2, top + 34, 0xFFE080);
        context.fill(mapX, mapY, mapX + size, mapY + size, 0xFF151515);
        context.drawBorder(mapX, mapY, size, size, 0xFFFFFF80);
        int centerX = mapX + size / 2;
        int centerY = mapY + size / 2;
        context.drawItem(itemStack("minecraft:bell"), centerX - 8, centerY - 8);
        context.drawBorder(centerX - 10, centerY - 10, 20, 20, 0xFFFFE080);
        for (TownBlueprintView.BuildingEntry building : view.registeredBuildings()) {
            drawBuildingFootprint(context, building, mapX, mapY, size, centerX, centerY, 0xFF80D080);
        }
        for (TownBlueprintView.BuildingEntry building : view.detectedBuildings()) {
            drawBuildingFootprint(context, building, mapX, mapY, size, centerX, centerY, 0xFFE0C060);
        }
        int playerX = centerX + clampMapOffset(view.playerPos().getX() - view.centerPos().getX(), size);
        int playerY = centerY + clampMapOffset(view.playerPos().getZ() - view.centerPos().getZ(), size);
        context.fill(playerX - 2, playerY - 2, playerX + 3, playerY + 3, 0xFF40A0FF);
        context.drawBorder(playerX - 3, playerY - 3, 6, 6, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Center " + posText(view.centerPos())), mapX + size / 2, mapY + size + 4, 0xFFE080);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Player " + posText(view.playerPos())), mapX + size / 2, mapY + size + 15, 0x40A0FF);
        context.drawTextWithShadow(textRenderer, Text.literal("Buildings: " + view.registeredBuildings().size()), left + 190, top + 90, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Detected: " + view.detectedBuildings().size()), left + 190, top + 104, 0xE0C060);
        for (int i = 0; i < Math.min(4, view.registeredBuildings().size()); i++) {
            TownBlueprintView.BuildingEntry building = view.registeredBuildings().get(i);
            String detail = building.name() + " T" + building.tier() + " " + titleCase(building.status())
                    + " " + building.workerCount() + "/" + building.workerSlots() + "w Q" + building.quality()
                    + ("farm".equals(building.type()) ? " C" + building.cropState() : "");
            context.drawTextWithShadow(textRenderer, Text.literal(detail), left + 190, top + 120 + i * 13,
                    "ACTIVE".equals(building.status()) ? 0x80D080 : 0xE0A060);
        }
        if (advancedControls) {
            drawWrapped(context, "Advanced controls include direct unregister for nearby buildings.", left + 190, top + 178, 150, 2, 0xCCCCCC);
        }
    }

    private void drawBuildingFootprint(DrawContext context, TownBlueprintView.BuildingEntry building,
                                       int mapX, int mapY, int size, int centerX, int centerY, int color) {
        int x1 = centerX + clampMapOffset(building.minPos().getX() - view.centerPos().getX(), size);
        int x2 = centerX + clampMapOffset(building.maxPos().getX() - view.centerPos().getX(), size);
        int y1 = centerY + clampMapOffset(building.minPos().getZ() - view.centerPos().getZ(), size);
        int y2 = centerY + clampMapOffset(building.maxPos().getZ() - view.centerPos().getZ(), size);
        int left = Math.max(mapX + 1, Math.min(x1, x2));
        int right = Math.min(mapX + size - 1, Math.max(x1, x2));
        int top = Math.max(mapY + 1, Math.min(y1, y2));
        int bottom = Math.min(mapY + size - 1, Math.max(y1, y2));
        int width = Math.max(6, right - left + 1);
        int height = Math.max(6, bottom - top + 1);
        if (left + width > mapX + size - 1) left = mapX + size - 1 - width;
        if (top + height > mapY + size - 1) top = mapY + size - 1 - height;
        context.drawBorder(left, top, width, height, color);
        int x = centerX + clampMapOffset(building.pos().getX() - view.centerPos().getX(), size);
        int y = centerY + clampMapOffset(building.pos().getZ() - view.centerPos().getZ(), size);
        context.fill(x - 1, y - 1, x + 2, y + 2, color);
    }

    private void drawRules(DrawContext context, int left, int top) {
        context.drawTextWithShadow(textRenderer, Text.literal("Town Rules"), left + 18, top + 52, 0xFFE080);
        context.drawTextWithShadow(textRenderer, Text.literal("Tax Rate"), left + 18, top + 70, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(TownTextHelper.taxRateName(view.taxRate())),
                left + 79, top + 87, 0xFFE080);
        drawWrapped(context, "Tax rate affects weekly taxes, happiness, and unrest.", left + 18, top + 114, 300, 3, 0xCCCCCC);
    }

    private void drawPlaceholder(DrawContext context, int left, int top, String heading, String line) {
        context.drawTextWithShadow(textRenderer, Text.literal(heading), left + 18, top + 52, 0xFFE080);
        context.drawTextWithShadow(textRenderer, Text.literal(line), left + 18, top + 72, 0xCCCCCC);
    }

    private void drawWrapped(DrawContext context, String text, int x, int y, int width, int maxLines, int color) {
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(text), width);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            context.drawTextWithShadow(textRenderer, lines.get(i), x, y + i * 11, color);
        }
    }

    private List<TownBlueprintView.RequirementLine> inspectionLines(TownBlueprintView.BuildingOption option, boolean cost) {
        if (option.id().equals(view.inspectionType())) {
            return cost ? view.inspectionCost() : view.inspectionFurniture();
        }
        TownBuildingDefinition definition = TownBuildingDefinition.get(option.id());
        if (definition == null) return List.of();
        return (cost ? neutralCostLines(definition) : neutralFurnitureLines(definition)).stream()
                .map(line -> new TownBlueprintView.RequirementLine(line.text(), line.state()))
                .toList();
    }

    private List<com.example.mcatowns.town.BlueprintSessionService.RequirementLine> neutralCostLines(TownBuildingDefinition definition) {
        return com.example.mcatowns.town.TownBuildingService.neutralCostRequirementLines(definition);
    }

    private List<com.example.mcatowns.town.BlueprintSessionService.RequirementLine> neutralFurnitureLines(TownBuildingDefinition definition) {
        return com.example.mcatowns.town.TownBuildingService.neutralFurnitureRequirementLines(definition);
    }

    private int requirementColor(TownBlueprintView.BuildingOption option, boolean cost) {
        if (!option.id().equals(view.inspectionType())) return 0xAAAAAA;
        List<TownBlueprintView.RequirementLine> lines = cost ? view.inspectionCost() : view.inspectionFurniture();
        return lines.stream().anyMatch(line -> line.state() < 0) ? 0xE08080 : 0x80D080;
    }

    private static int colorForState(int state) {
        if (state > 0) return 0x80D080;
        if (state < 0) return 0xE08080;
        return 0xAAAAAA;
    }

    private void drawRequirementTooltip(DrawContext context, int mouseX, int mouseY) {
        if (!page.equals("catalog") || view.founding() || selectedBuilding == null || selectedBuilding.legacy()) return;
        int left = panelLeft();
        int top = panelTop();
        drawRequirementTooltip(context, mouseX, mouseY, left + 192, top + 144, 150, 12, selectedBuilding, true);
        drawRequirementTooltip(context, mouseX, mouseY, left + 192, top + 160, 150, 12, selectedBuilding, false);
    }

    private void drawRequirementTooltip(DrawContext context, int mouseX, int mouseY, int x, int y, int width, int height,
                                        TownBlueprintView.BuildingOption option, boolean cost) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return;
        List<OrderedText> lines = new ArrayList<>();
        for (TownBlueprintView.RequirementLine line : inspectionLines(option, cost)) {
            lines.add(Text.literal(line.text()).styled(style -> style.withColor(colorForState(line.state()))).asOrderedText());
        }
        context.drawTooltip(textRenderer, lines, HoveredTooltipPositioner.INSTANCE, mouseX, mouseY);
    }

    private int panelLeft() {
        return Math.max(128, width / 2 - 180);
    }

    private int panelTop() {
        return (height - PANEL_HEIGHT) / 2;
    }

    private static String shortCategory(TownBuildingCategory category) {
        return switch (category) {
            case RESIDENTIAL -> "Homes";
            case COMMUNITY -> "Social";
            default -> titleCase(category.name());
        };
    }

    private static ItemStack buildingIcon(TownBlueprintView.BuildingOption option) {
        if (!option.icon().isBlank()) return itemStack(option.icon());
        String itemId = switch (option.id()) {
            case "residence" -> "minecraft:red_bed";
            case "farm" -> "minecraft:wheat";
            case "granary" -> "mcatowns:silo";
            case "campfire" -> "minecraft:campfire";
            case "park" -> "minecraft:oak_sapling";
            case "inn" -> "minecraft:barrel";
            case "storehouse" -> "mcatowns:storehouse";
            case "bounty_board" -> "bountiful:bountyboard";
            case "civic_office" -> "mcatowns:mayor_desk";
            case "guard_post" -> "mcatowns:barracks";
            case "blacksmith" -> "minecraft:anvil";
            case "jeweler" -> "minecraft:smithing_table";
            case "scholar" -> "minecraft:lectern";
            default -> "minecraft:paper";
        };
        return itemStack(itemId);
    }

    private static String posText(net.minecraft.util.math.BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static int clampMapOffset(int blocks, int mapSize) {
        int clamped = Math.max(-64, Math.min(64, blocks));
        return clamped * (mapSize / 2 - 5) / 64;
    }

    private static ItemStack itemStack(String itemId) {
        Item item = Registries.ITEM.get(new Identifier(itemId));
        return new ItemStack(Registries.ITEM.getId(item).equals(Registries.ITEM.getDefaultId()) ? Items.PAPER : item);
    }

    private static String cost(TownBlueprintView.BuildingOption option) {
        return option.tokens() + " Tokens, " + option.currency() + " Currency"
                + (option.prosperity() > 0 ? ", " + option.prosperity() + " Prosperity" : "");
    }

    private static String titleCase(String value) {
        String text = value.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
