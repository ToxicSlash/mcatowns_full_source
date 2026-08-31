package com.example.mcatowns.screen;

import com.example.mcatowns.network.ModNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class RenameTownScreen extends Screen {
    private final Screen parent;
    private final BlockPos anchor;
    private final String currentName;
    private TextFieldWidget nameField;

    public RenameTownScreen(Screen parent, BlockPos anchor, String currentName) {
        super(Text.literal("Rename Town"));
        this.parent = parent;
        this.anchor = anchor;
        this.currentName = currentName;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = height / 2 - 10;
        nameField = new TextFieldWidget(textRenderer, x, y, 200, 20, Text.literal("Town Name"));
        nameField.setMaxLength(32);
        nameField.setText(currentName);
        addSelectableChild(nameField);
        setInitialFocus(nameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> {
            ModNetworking.sendRenameTown(anchor, nameField.getText());
            if (client != null) client.setScreen(parent);
        }).dimensions(x, y + 30, 96, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> {
            if (client != null) client.setScreen(parent);
        }).dimensions(x + 104, y + 30, 96, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 38, 0xFFFFFF);
        nameField.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }
}
