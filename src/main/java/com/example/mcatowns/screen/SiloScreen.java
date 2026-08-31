package com.example.mcatowns.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class SiloScreen extends HandledScreen<SiloScreenHandler> {
    private ButtonWidget depositButton;

    public SiloScreen(SiloScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 130;
        this.playerInventoryTitleY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        int left = (width - backgroundWidth) / 2;
        int top = (height - backgroundHeight) / 2;
        depositButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.mcatowns.silo.deposit"), b ->
                handler.sendDeposit()
        ).dimensions(left + 38, top + 80, 100, 20).build());
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
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, 8, 8, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.translatable("screen.mcatowns.silo.food", handler.getFood()), 8, 34, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.translatable("screen.mcatowns.silo.inventory_bread", handler.getPlayerBread()), 8, 48, 0xFFFFFF, false);
        context.drawText(textRenderer, Text.translatable("screen.mcatowns.silo.stored_bread_eq", handler.getSiloBreadEquivalent()), 8, 62, 0xFFFFFF, false);
    }
}
