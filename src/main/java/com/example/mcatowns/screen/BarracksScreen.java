package com.example.mcatowns.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class BarracksScreen extends HandledScreen<BarracksScreenHandler> {
    private ButtonWidget searchButton;

    public BarracksScreen(BarracksScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 190;
        this.backgroundHeight = 138;
        this.playerInventoryTitleY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        int left = (width - backgroundWidth) / 2;
        int top = (height - backgroundHeight) / 2;
        searchButton = addDrawableChild(ButtonWidget.builder(Text.literal("Search for Hires"), button ->
                handler.sendSearchForHires()
        ).dimensions(left + 20, top + 96, 150, 20).build());
        updateButton();
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int left = (width - backgroundWidth) / 2;
        int top = (height - backgroundHeight) / 2;
        context.fill(left, top, left + backgroundWidth, top + backgroundHeight, 0xCC2B2B2B);
        context.fill(left + 4, top + 4, left + backgroundWidth - 4, top + 24, 0xCC3A3A3A);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateButton();
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, 8, 8, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.literal("Guards: " + handler.getGuards() + " / " + handler.getGuardCapacity()),
                8, 34, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.literal("Population: " + handler.getPopulation() + " / " + handler.getPopulationCapacity()),
                8, 48, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.literal("Hire cost: " + handler.getCost() + "  (You have " + handler.getPlayerCurrency() + ")"),
                8, 62, 0xFFFFFF, false);
        if (handler.isSearching()) {
            context.drawText(textRenderer, Text.literal("Searching for a guard: " + handler.getRemainingSeconds() + "s"),
                    8, 76, 0xAAAAAA, false);
        } else {
            context.drawText(textRenderer, Text.literal("Each hired guard uses 1 population slot."),
                    8, 76, 0xAAAAAA, false);
        }
    }

    private void updateButton() {
        if (searchButton == null) return;
        if (handler.isSearching()) {
            searchButton.setMessage(Text.literal("Searching... " + handler.getRemainingSeconds() + "s"));
            searchButton.active = false;
            return;
        }
        searchButton.setMessage(Text.literal("Search for Hires"));
        boolean creative = client != null && client.player != null && client.player.getAbilities().creativeMode;
        searchButton.active = handler.getGuards() < handler.getGuardCapacity()
                && handler.getPopulation() < handler.getPopulationCapacity()
                && (creative || handler.getPlayerCurrency() >= handler.getCost());
    }
}
