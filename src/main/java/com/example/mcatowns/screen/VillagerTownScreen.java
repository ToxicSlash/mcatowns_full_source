package com.example.mcatowns.screen;

import com.example.mcatowns.network.ModNetworking;
import com.example.mcatowns.network.VillagerTownView;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class VillagerTownScreen extends Screen {
    private static final int WIDTH = 330;
    private static final int HEIGHT = 240;
    private final VillagerTownView view;

    public VillagerTownScreen(VillagerTownView view) {
        super(Text.literal(view.name()));
        this.view = view;
    }

    @Override
    protected void init() {
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        if (!view.employed()) {
            ButtonWidget invite = ButtonWidget.builder(Text.translatable("screen.mcatowns.villager.invite"),
                            ignored -> ModNetworking.sendRecruitVillager(view.villagerId()))
                    .dimensions(left + 105, top + 190, 120, 20)
                    .tooltip(Tooltip.of(Text.translatable("screen.mcatowns.villager.invite_cost", view.recruitmentCost())))
                    .build();
            invite.active = view.canInvite();
            addDrawableChild(invite);
        } else {
            for (int i = 0; i < view.research().size(); i++) {
                var research = view.research().get(i);
                ButtonWidget button = ButtonWidget.builder(Text.literal(research.name() + (research.unlocked() ? " ✓" : "")),
                                ignored -> ModNetworking.sendResearch(view.villagerId(), research.id()))
                        .dimensions(left + 14 + (i % 2) * 154, top + 95 + (i / 2) * 29, 148, 20)
                        .tooltip(Tooltip.of(Text.literal("Cost: " + research.scraps() + " Scraps, "
                                + research.essence() + " Great Essence, " + research.currency()
                                + " Currency, " + research.tokens() + " Town Tokens"))).build();
                button.active = !research.unlocked();
                addDrawableChild(button);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        context.fill(left, top, left + WIDTH, top + HEIGHT, 0xEE252525);
        context.fill(left + 4, top + 4, left + WIDTH - 4, top + 28, 0xFF3A3A3A);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 10, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal(view.specialist().isBlank() ? "Resident" : view.specialist()), left + 16, top + 42, 0xFFE080);
        context.drawTextWithShadow(textRenderer, Text.literal("Friendship Hearts: " + view.hearts()), left + 16, top + 58, 0xFFFFFF);
        if (!view.specialist().isBlank()) context.drawTextWithShadow(textRenderer,
                Text.literal("Introductions: " + (view.introductionsComplete() ? "Complete" : "Incomplete")), left + 16, top + 73, 0xFFFFFF);
        if (view.employed()) context.drawTextWithShadow(textRenderer, Text.literal("Town status: Employed"), left + 190, top + 42, 0x80D080);
        super.render(context, mouseX, mouseY, delta);
    }
}
