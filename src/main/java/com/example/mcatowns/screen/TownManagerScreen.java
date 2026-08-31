package com.example.mcatowns.screen;

import com.example.mcatowns.network.ModNetworking;
import com.example.mcatowns.network.TownManagerView;
import com.example.mcatowns.town.TownRank;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class TownManagerScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 250;
    private final TownManagerView view;
    private int selectedTab;

    public TownManagerScreen(TownManagerView view) {
        super(Text.translatable("screen.mcatowns.town_manager"));
        this.view = view;
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        String[] tabs = {"overview", "buildings", "residents", "specialists", "requests"};
        for (int i = 0; i < tabs.length; i++) {
            int tabIndex = i;
            ButtonWidget tab = ButtonWidget.builder(
                            Text.translatable("screen.mcatowns.town_manager." + tabs[i]), button -> {
                                selectedTab = tabIndex;
                                clearChildren();
                                init();
                            })
                    .dimensions(left + 8 + i * 51, (height - PANEL_HEIGHT) / 2 + 31, 49, 18)
                    .build();
            tab.active = i != selectedTab;
            addDrawableChild(tab);
        }
        if (view.canRemove()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.mcatowns.town_manager.remove"),
                            button -> confirmRemoval())
                    .dimensions(left + PANEL_WIDTH - 88, (height - PANEL_HEIGHT) / 2 + PANEL_HEIGHT - 25, 80, 18)
                    .build());
        }
        if (view.canManage()) {
            if (selectedTab == 0 && view.rank().next() != null) {
                addDrawableChild(ButtonWidget.builder(Text.translatable("screen.mcatowns.town_manager.advance"),
                                button -> ModNetworking.sendAdvanceTown(view.bellPos()))
                        .dimensions(left + 10, (height - PANEL_HEIGHT) / 2 + PANEL_HEIGHT - 25, 100, 18)
                        .build());
            }
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("MCA Town Map"),
                        button -> ModNetworking.sendOpenTownBlueprint())
                .dimensions(left + 116, (height - PANEL_HEIGHT) / 2 + PANEL_HEIGHT - 25, 96, 18)
                .build());
    }

    private void confirmRemoval() {
        if (client == null) return;
        client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                ModNetworking.sendRemoveTown(view.bellPos());
                client.setScreen(null);
            } else {
                client.setScreen(this);
            }
        }, Text.translatable("screen.mcatowns.town_manager.remove_confirm"),
                Text.translatable("screen.mcatowns.town_manager.remove_warning")));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        context.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xEE252525);
        context.fill(left + 4, top + 4, left + PANEL_WIDTH - 4, top + 28, 0xFF3A3A3A);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(view.name()), width / 2, top + 10, 0xFFFFFF);

        switch (selectedTab) {
            case 0 -> drawOverview(context, left, top);
            case 1 -> drawSimple(context, left, top, "Registered Buildings", view.buildings(),
                    "Use the Blueprint while standing in or looking at a structure.");
            case 2 -> drawSimple(context, left, top, "Residents", view.residentCount(),
                    "Sneak-interact with a friendly MCA villager to invite them.");
            case 3 -> drawSimple(context, left, top, "Specialists", view.specialists(),
                    "Specialists are ordinary MCA villagers with town roles.");
            case 4 -> drawRequests(context, left, top);
            default -> { }
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawOverview(DrawContext context, int left, int top) {
        int x = left + 18;
        int y = top + 61;
        context.drawTextWithShadow(textRenderer,
                Text.translatable("screen.mcatowns.town_manager.rank", view.rank().displayName()), x, y, 0xFFE080);
        context.drawTextWithShadow(textRenderer,
                Text.translatable("screen.mcatowns.town_manager.role", view.canManage() ? "Mayor" : "Visitor"), x, y + 15, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer,
                Text.translatable("screen.mcatowns.town_manager.population", view.population(), view.populationCapacity()), x, y + 35, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer,
                Text.translatable("screen.mcatowns.town_manager.building_count", view.buildings(), view.rank().maxBuildings()), x, y + 50, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer,
                Text.translatable("screen.mcatowns.town_manager.food_capacity", view.food(), view.foodCapacity()), x, y + 65, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Food Status: " + foodStatus()), left + 178, y + 65, 0xCCCCCC);
        context.drawTextWithShadow(textRenderer,
                Text.translatable("screen.mcatowns.town_manager.tokens", view.townTokens()), left + 178, y + 35, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer,
                Text.translatable("screen.mcatowns.town_manager.specialist_count", view.specialists(), view.rank().maxSpecialists()), left + 178, y + 50, 0xFFFFFF);

        drawProsperityBar(context, left + 18, top + 145, PANEL_WIDTH - 36);
        TownRank next = view.rank().next();
        context.drawTextWithShadow(textRenderer,
                next == null
                        ? Text.translatable("screen.mcatowns.town_manager.max_rank")
                        : Text.translatable("screen.mcatowns.town_manager.next_rank", next.displayName()),
                left + 18, top + 165, 0xAAAAAA);
        for (int i = 0; i < Math.min(6, view.checklist().size()); i++) {
            String line = view.checklist().get(i);
            context.drawTextWithShadow(textRenderer, Text.literal(line), left + 18 + (i % 2) * 145,
                    top + 180 + (i / 2) * 13, line.startsWith("✓") ? 0x80D080 : 0xE08080);
        }
    }

    private String foodStatus() {
        if (view.foodCapacity() <= 0) return "No Storage";
        int percent = view.food() * 100 / view.foodCapacity();
        if (percent >= 25) return "Stable";
        if (percent >= 10) return "Rationing";
        if (percent > 0) return "Food Shortage";
        return "Severe Food Shortage";
    }

    private void drawSimple(DrawContext context, int left, int top, String heading, int count, String note) {
        context.drawTextWithShadow(textRenderer, Text.literal(heading + ": " + count), left + 18, top + 68, 0xFFE080);
        context.drawTextWithShadow(textRenderer, Text.literal(note), left + 18, top + 92, 0xCCCCCC);
    }

    private void drawRequests(DrawContext context, int left, int top) {
        context.drawTextWithShadow(textRenderer, Text.literal("Monster Control: " + view.bountyKills() + " / 30"),
                left + 170, top + 66, 0xFFFFFF);
        if (view.requestName().isBlank()) {
            drawSimple(context, left, top, "Town Requests", 0,
                    "Register a Storehouse to begin receiving requests.");
            return;
        }
        long day = client == null || client.world == null ? 0 : client.world.getTimeOfDay() / 24000L;
        context.drawTextWithShadow(textRenderer, Text.literal(view.requestName()), left + 18, top + 66, 0xFFE080);
        context.drawTextWithShadow(textRenderer, Text.literal("Type: " + view.requestType()), left + 18, top + 82, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Needed in: " + Math.max(0, view.requestDueDay() - day) + " Minecraft Days"), left + 18, top + 97, 0xFFFFFF);
        for (int i = 0; i < view.requestRequirements().size(); i++) {
            context.drawTextWithShadow(textRenderer, Text.literal(view.requestRequirements().get(i)), left + 18,
                    top + 120 + i * 14, 0xCCCCCC);
        }
        context.drawTextWithShadow(textRenderer, Text.literal("Reward: +" + view.requestProsperity()
                + " Prosperity, +" + view.requestTokens() + " Town Tokens"), left + 18, top + 190, 0x80D080);
    }

    private void drawProsperityBar(DrawContext context, int x, int y, int width) {
        int max = view.rank().maxProsperity();
        int filled = max == 0 ? 0 : width * Math.min(view.prosperity(), max) / max;
        context.fill(x, y, x + width, y + 13, 0xFF151515);
        if (filled > 0) context.fill(x + 1, y + 1, x + filled, y + 12, 0xFF6FAF4B);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.mcatowns.town_manager.prosperity",
                        view.prosperity(), max, view.prosperityBase()), width / 2, y + 2, 0xFFFFFF);
    }
}
