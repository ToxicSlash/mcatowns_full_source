package com.example.mcatowns.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class MayorDeskScreen extends HandledScreen<MayorDeskScreenHandler> {
    private static final int PANEL_WIDTH = 256;
    private static final int PANEL_HEIGHT = 232;
    private static final int LEFT_COLUMN_X = 12;
    private static final int RIGHT_COLUMN_X = 132;
    private static final int TITLE_Y = 10;
    private static final int CONTENT_Y = 38;
    private static final int ROW_STEP = 13;

    private ButtonWidget taxMinusButton;
    private ButtonWidget taxPlusButton;
    private ButtonWidget festivalButton;
    private ButtonWidget foodReliefButton;
    private ButtonWidget barracksButton;
    private ButtonWidget foodViewButton;
    private boolean foodView = false;

    public MayorDeskScreen(MayorDeskScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = PANEL_WIDTH;
        this.backgroundHeight = PANEL_HEIGHT;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int left = (width - backgroundWidth) / 2;
        int top = (height - backgroundHeight) / 2;

        int buttonRow1Y = top + 174;
        int buttonRow2Y = top + 198;

        foodViewButton = addDrawableChild(ButtonWidget.builder(Text.literal("Food"), b -> {
            foodView = !foodView;
            updateButtons();
        }).dimensions(left + backgroundWidth - 58, top + 7, 50, 18).build());

        taxMinusButton = addDrawableChild(ButtonWidget.builder(Text.literal("Tax -"), b ->
                handler.sendSetTaxRate(Math.max(0, handler.getTaxRate() - 1))
        ).dimensions(left + 12, buttonRow1Y, 58, 20).build());

        taxPlusButton = addDrawableChild(ButtonWidget.builder(Text.literal("Tax +"), b ->
                handler.sendSetTaxRate(Math.min(3, handler.getTaxRate() + 1))
        ).dimensions(left + 76, buttonRow1Y, 58, 20).build());

        festivalButton = addDrawableChild(ButtonWidget.builder(Text.literal("Festival"), b ->
                handler.sendFestival()
        ).dimensions(left + 140, buttonRow1Y, 104, 20).build());

        foodReliefButton = addDrawableChild(ButtonWidget.builder(Text.literal("Food Relief"), b ->
                handler.sendEmergencyFoodRelief()
        ).dimensions(left + 12, buttonRow2Y, 122, 20).build());

        barracksButton = addDrawableChild(ButtonWidget.builder(Text.literal("B+"), b ->
                handler.sendBarracksUpgrade()
        ).dimensions(left + 140, buttonRow2Y, 104, 20).build());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int left = (width - backgroundWidth) / 2;
        int top = (height - backgroundHeight) / 2;
        context.fill(left, top, left + backgroundWidth, top + backgroundHeight, 0xCC2B2B2B);
        context.fill(left + 4, top + 4, left + backgroundWidth - 4, top + 28, 0xCC3A3A3A);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        updateButtons();
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        int row = CONTENT_Y;

        context.drawText(textRenderer, title, LEFT_COLUMN_X, TITLE_Y, 0xFFFFFF, false);
        context.drawText(textRenderer, "Population: " + handler.getPopulation(), LEFT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Jobless: " + handler.getJobless(), LEFT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Happiness: " + handler.getHappiness(), LEFT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Food: " + handler.getFood(), LEFT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Defense: " + handler.getDefense(), LEFT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Immigration: " + handler.getImmigration(), LEFT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Treasury: " + handler.getTreasury() + "/" + handler.getTreasuryCap(), LEFT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Weekly Tax: " + handler.getWeeklyTax(), LEFT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Unrest: " + handler.getUnrest(), LEFT_COLUMN_X, row, 0xFFFFFF, false);

        if (foodView) {
            drawFoodView(context);
        } else {
            drawStatsView(context);
        }
    }

    private void drawStatsView(DrawContext context) {
        int row = CONTENT_Y;
        context.drawText(textRenderer, "Tax Rate: " + taxRateName(handler.getTaxRate()), RIGHT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Barracks Lvl: " + handler.getBarracksLevel(), RIGHT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Addon Tax Base: " + handler.getAddonTaxBase(), RIGHT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Food Eaten/Day: " + handler.getDailyFoodConsumed(), RIGHT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Food Gained/Day: " + handler.getDailyFoodProduced(), RIGHT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Workforce: " + handler.getWorkforceAvailable() + "/" + handler.getWorkforceRequired(), RIGHT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Efficiency: " + handler.getWorkforceEfficiency() + "%", RIGHT_COLUMN_X, row, 0xFFFFFF, false);
    }

    private void drawFoodView(DrawContext context) {
        int row = CONTENT_Y;

        context.drawText(textRenderer, "Food Sources", RIGHT_COLUMN_X, row, 0xFFE080, false); row += ROW_STEP;
        context.drawText(textRenderer, "Raw/Day: " + handler.getDailyFoodPotential(), RIGHT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Gained/Day: " + handler.getDailyFoodProduced(), RIGHT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Farm Plans: " + (handler.getCropFarms() + handler.getInvalidCropFarms()), RIGHT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Crop Farms: " + handler.getCropFarms() + " x12", RIGHT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Missing Crops: " + handler.getInvalidCropFarms(), RIGHT_COLUMN_X, row, invalidFarmColor(), false); row += ROW_STEP;
        context.drawText(textRenderer, "Bakeries: " + handler.getActiveBakeries() + " x16", RIGHT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Inactive Bakery: " + handler.getInactiveBakeries(), RIGHT_COLUMN_X, row, inactiveColor(), false); row += ROW_STEP;
        context.drawText(textRenderer, "Butcher/Fisher: " + handler.getButchers() + "/" + handler.getFishermansHuts(), RIGHT_COLUMN_X, row, 0xFFFFFF, false); row += ROW_STEP;
        context.drawText(textRenderer, "Farm Event: " + handler.getFarmOutputPercent() + "%", RIGHT_COLUMN_X, row, eventColor(), false); row += ROW_STEP;
        context.drawText(textRenderer, "Workforce: " + handler.getWorkforceEfficiency() + "%", RIGHT_COLUMN_X, row, 0xFFFFFF, false);
    }

    private int inactiveColor() {
        return handler.getInactiveBakeries() > 0 ? 0xFF7777 : 0xAAAAAA;
    }

    private int invalidFarmColor() {
        return handler.getInvalidCropFarms() > 0 ? 0xFF7777 : 0xAAAAAA;
    }

    private int eventColor() {
        return handler.getFarmOutputPercent() < 100 ? 0xFF7777 : 0xFFFFFF;
    }

    private String taxRateName(int rate) {
        return switch (rate) {
            case 0 -> "Low";
            case 1 -> "Normal";
            case 2 -> "High";
            case 3 -> "Very High";
            default -> "Normal";
        };
    }

    private String bonusSourceText(int flags) {
        StringBuilder sb = new StringBuilder();
        if ((flags & 1) != 0) sb.append("MCA ");
        if ((flags & 2) != 0) sb.append("Treasury ");
        if ((flags & 4) != 0) sb.append("Barracks ");
        return sb.isEmpty() ? "None" : sb.toString().trim();
    }

    private void updateButtons() {
        if (taxMinusButton != null) {
            taxMinusButton.active = handler.getTaxRate() > 0;
        }
        if (taxPlusButton != null) {
            taxPlusButton.active = handler.getTaxRate() < 3;
        }
        if (festivalButton != null) {
            festivalButton.active = handler.canFestival();
        }
        if (foodReliefButton != null) {
            foodReliefButton.active = handler.canFoodRelief();
        }
        if (barracksButton != null) {
            barracksButton.active = handler.canBarracksUpgrade();
            int cost = handler.getNextBarracksCost();
            barracksButton.setMessage(cost >= 0 ? Text.literal("B+ " + cost) : Text.literal("B Max"));
        }
        if (foodViewButton != null) {
            foodViewButton.setMessage(Text.literal(foodView ? "Stats" : "Food"));
        }
    }
}
