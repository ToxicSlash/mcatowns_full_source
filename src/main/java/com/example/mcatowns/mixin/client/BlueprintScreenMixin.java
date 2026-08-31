package com.example.mcatowns.mixin.client;

import com.example.mcatowns.client.McaBlueprintScreenBridge;
import com.example.mcatowns.network.ModNetworking;
import com.example.mcatowns.network.TownBlueprintView;
import com.example.mcatowns.screen.RenameTownScreen;
import com.example.mcatowns.town.TownBuildingCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Pseudo
@Mixin(targets = {
        "net.mca.client.gui.BlueprintScreen",
        "fabric.net.mca.client.gui.BlueprintScreen"
})
public abstract class BlueprintScreenMixin extends Screen {
    @Shadow(remap = false)
    private String page;

    @Unique private final List<ButtonWidget> mcatowns$controls = new ArrayList<>();
    @Unique private final Map<ButtonWidget, ItemStack> mcatowns$icons = new LinkedHashMap<>();
    @Unique private TownBuildingCategory mcatowns$category = TownBuildingCategory.RESIDENTIAL;
    @Unique private boolean mcatowns$showSpecialists;
    @Unique private TownBlueprintView.BuildingOption mcatowns$selectedBuilding;
    @Unique private TownBlueprintView.ResidentEntry mcatowns$selectedResident;

    protected BlueprintScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = {"init", "method_25426"}, at = @At("TAIL"), remap = false, require = 0)
    private void mcatowns$initializePage(CallbackInfo ci) {
        mcatowns$addPageControls();
    }

    @Inject(method = "setPage", at = @At("TAIL"), remap = false)
    private void mcatowns$pageChanged(String newPage, CallbackInfo ci) {
        mcatowns$addPageControls();
    }

    @Unique
    private void mcatowns$addPageControls() {
        mcatowns$controls.forEach(this::remove);
        mcatowns$controls.clear();
        mcatowns$icons.clear();
        TownBlueprintView view = McaBlueprintScreenBridge.getView();
        if (view == null || page == null) return;
        mcatowns$addTownHeaderControls(view);
        if (page.equals("catalog")) mcatowns$addCatalogControls();
        if (page.equals("villagers")) mcatowns$addVillagerControls();
    }

    @Unique
    private void mcatowns$addTownHeaderControls(TownBlueprintView view) {
        if (view.canManage()) {
            ButtonWidget rename = ButtonWidget.builder(Text.literal("Edit"), ignored -> {
                        if (client != null) client.setScreen(new RenameTownScreen(this, view.anchorPos(), view.name()));
                    })
                    .dimensions(width / 2 - 246, height / 2 - 68, 42, 16)
                    .tooltip(Tooltip.of(Text.literal("Rename town")))
                    .build();
            mcatowns$controls.add(addDrawableChild(rename));
        }
        if (view.canRemove()) {
            ButtonWidget remove = ButtonWidget.builder(Text.literal("Remove"), ignored -> mcatowns$confirmRemoval(view))
                    .dimensions(width / 2 + 122, height / 2 - 106, 72, 18)
                    .tooltip(Tooltip.of(Text.literal("Remove town")))
                    .build();
            mcatowns$controls.add(addDrawableChild(remove));
        }
    }

    @Unique
    private void mcatowns$confirmRemoval(TownBlueprintView view) {
        if (client == null) return;
        client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                ModNetworking.sendRemoveTown(view.anchorPos());
                client.setScreen(null);
            } else {
                client.setScreen(this);
            }
        }, Text.translatable("screen.mcatowns.town_manager.remove_confirm"),
                Text.translatable("screen.mcatowns.town_manager.remove_warning")));
    }

    @Unique
    private void mcatowns$addCatalogControls() {
        mcatowns$removeMcaCatalogIcons();
        TownBuildingCategory[] categories = TownBuildingCategory.values();
        int startX = width / 2 - 84;
        int y = height / 2 - 78;
        for (int i = 0; i < categories.length; i++) {
            TownBuildingCategory category = categories[i];
            ButtonWidget button = ButtonWidget.builder(Text.literal(mcatowns$title(category.name())), ignored -> {
                        mcatowns$category = category;
                        mcatowns$selectedBuilding = null;
                        mcatowns$addPageControls();
                    })
                    .dimensions(startX + i * 48, y, 46, 18).build();
            button.active = category != mcatowns$category;
            mcatowns$controls.add(addDrawableChild(button));
        }

        List<TownBlueprintView.BuildingOption> options = McaBlueprintScreenBridge.getView().buildingOptions().stream()
                .filter(option -> option.category() == mcatowns$category).toList();
        if (mcatowns$selectedBuilding == null && !options.isEmpty()) mcatowns$selectedBuilding = options.get(0);
        for (int i = 0; i < options.size(); i++) {
            TownBlueprintView.BuildingOption option = options.get(i);
            ButtonWidget button = ButtonWidget.builder(Text.empty(), ignored -> {
                        mcatowns$selectedBuilding = option;
                        mcatowns$addPageControls();
                    })
                    .dimensions(width / 2 - 80 + (i % 5) * 28, height / 2 - 48 + (i / 5) * 30, 24, 24)
                    .tooltip(Tooltip.of(Text.literal(option.name() + "\n" + option.description())))
                    .build();
            button.active = option != mcatowns$selectedBuilding;
            mcatowns$controls.add(addDrawableChild(button));
            mcatowns$icons.put(button, mcatowns$buildingIcon(option.id()));
        }
        if (mcatowns$selectedBuilding != null) {
            TownBlueprintView.BuildingOption option = mcatowns$selectedBuilding;
            ButtonWidget register = ButtonWidget.builder(Text.literal("Register " + option.name()),
                            ignored -> ModNetworking.sendRegisterTownBuilding(option.id()))
                    .dimensions(width / 2 + 34, height / 2 + 70, 145, 18)
                    .tooltip(Tooltip.of(Text.literal(mcatowns$cost(option))))
                    .build();
            register.active = McaBlueprintScreenBridge.getView().canManage() && option.unlocked();
            mcatowns$controls.add(addDrawableChild(register));
        }
    }

    @Unique
    private void mcatowns$addVillagerControls() {
        mcatowns$removeMcaContentButtons();
        int x = width / 2 - 80;
        int y = height / 2 - 82;
        ButtonWidget residents = ButtonWidget.builder(Text.literal("Residents"), ignored -> {
            mcatowns$showSpecialists = false;
            mcatowns$selectedResident = null;
            mcatowns$addPageControls();
        }).dimensions(x, y, 78, 18).build();
        residents.active = mcatowns$showSpecialists;
        mcatowns$controls.add(addDrawableChild(residents));
        ButtonWidget specialists = ButtonWidget.builder(Text.literal("Specialists"), ignored -> {
            mcatowns$showSpecialists = true;
            mcatowns$selectedResident = null;
            mcatowns$addPageControls();
        }).dimensions(x + 82, y, 78, 18).build();
        specialists.active = !mcatowns$showSpecialists;
        mcatowns$controls.add(addDrawableChild(specialists));

        List<TownBlueprintView.ResidentEntry> entries = McaBlueprintScreenBridge.getView().residents().stream()
                .filter(entry -> entry.specialist() == mcatowns$showSpecialists).toList();
        if (mcatowns$selectedResident == null && !entries.isEmpty()) mcatowns$selectedResident = entries.get(0);
        for (int i = 0; i < Math.min(entries.size(), 8); i++) {
            TownBlueprintView.ResidentEntry entry = entries.get(i);
            String label = entry.name() + (entry.specialist() ? " - " + mcatowns$title(entry.specialistType()) : "");
            ButtonWidget button = ButtonWidget.builder(Text.literal(label), ignored -> mcatowns$selectedResident = entry)
                    .dimensions(x, height / 2 - 54 + i * 20, 160, 18)
                    .tooltip(Tooltip.of(Text.literal(entry.id().toString())))
                    .build();
            mcatowns$controls.add(addDrawableChild(button));
        }
    }

    @Unique
    private void mcatowns$removeMcaCatalogIcons() {
        try {
            Field field = this.getClass().getDeclaredField("catalogButtons");
            field.setAccessible(true);
            if (field.get(this) instanceof List<?> buttons) {
                for (Object button : List.copyOf(buttons)) if (button instanceof Element element) remove(element);
                buttons.clear();
            }
        } catch (ReflectiveOperationException ignored) {
            // MCA's page still works if a future release renames this private field.
        }
    }

    @Unique
    private void mcatowns$removeMcaContentButtons() {
        for (Element element : List.copyOf(children())) {
            if (element instanceof ButtonWidget button && button.getX() > width / 2 - 100) remove(button);
        }
    }

    @Inject(method = {"render", "method_25394"}, at = @At(value = "INVOKE",
            target = "Lfabric/net/mca/client/gui/BlueprintScreen;renderBackground(Lnet/minecraft/client/gui/DrawContext;)V",
            shift = At.Shift.AFTER), remap = false, require = 0)
    private void mcatowns$drawFabricDevBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        mcatowns$drawContentBackground(context);
    }

    @Inject(method = {"render", "method_25394"}, at = @At(value = "INVOKE",
            target = "Lfabric/net/mca/client/gui/BlueprintScreen;method_25420(Lnet/minecraft/class_332;)V",
            shift = At.Shift.AFTER), remap = false, require = 0)
    private void mcatowns$drawFabricProductionBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        mcatowns$drawContentBackground(context);
    }

    @Inject(method = {"render", "method_25394"}, at = @At(value = "INVOKE",
            target = "Lnet/mca/client/gui/BlueprintScreen;renderBackground(Lnet/minecraft/client/gui/DrawContext;)V",
            shift = At.Shift.AFTER), remap = false, require = 0)
    private void mcatowns$drawStandardDevBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        mcatowns$drawContentBackground(context);
    }

    @Inject(method = {"render", "method_25394"}, at = @At(value = "INVOKE",
            target = "Lnet/mca/client/gui/BlueprintScreen;method_25420(Lnet/minecraft/class_332;)V",
            shift = At.Shift.AFTER), remap = false, require = 0)
    private void mcatowns$drawStandardProductionBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        mcatowns$drawContentBackground(context);
    }

    @Unique
    private void mcatowns$drawContentBackground(DrawContext context) {
        if (McaBlueprintScreenBridge.getView() == null) return;
        context.fill(width / 2 - 94, height / 2 - 112, width / 2 + 205, height / 2 + 108, 0xD9252525);
        context.fill(width / 2 - 90, height / 2 - 108, width / 2 + 201, height / 2 - 88, 0xFF3A3A3A);
    }

    @Inject(method = {"render", "method_25394"}, at = @At("TAIL"), remap = false, require = 0)
    private void mcatowns$drawTownContent(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TownBlueprintView view = McaBlueprintScreenBridge.getView();
        if (view == null) return;
        mcatowns$drawStatus(context, view);
        if ("rank".equals(page)) {
            mcatowns$clearMainContent(context);
            mcatowns$drawRankChecklist(context, view);
        }
        if ("catalog".equals(page)) {
            mcatowns$clearMainContent(context);
            mcatowns$drawCatalogHeader(context);
            mcatowns$drawCustomButtons(context, mouseX, mouseY);
            mcatowns$drawCatalogDetails(context);
        }
        if ("villagers".equals(page)) mcatowns$drawVillagerDetails(context);
    }

    @Unique
    private void mcatowns$drawStatus(DrawContext context, TownBlueprintView view) {
        int x = width / 2 - 280;
        int y = height / 2 - 104;
        context.fill(x - 4, y - 4, x + 142, y + 52, 0xCC252525);
        context.drawTextWithShadow(textRenderer, Text.literal(view.name()), x, y, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer,
                Text.literal(view.rank().displayName() + "  Prosperity " + view.prosperity() + "/" + view.rank().maxProsperity()
                        + "  Base " + view.prosperityBase()),
                x, y + 12, 0xFFE080);
        context.drawTextWithShadow(textRenderer, Text.literal("Food " + view.food() + "/" + view.foodCapacity()),
                x, y + 24, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer,
                Text.literal("Pop " + view.population() + "/" + view.populationCapacity()), x, y + 36, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer,
                Text.literal("Spec " + view.specialists() + "/" + view.rank().maxSpecialists()), x + 70, y + 36, 0xFFFFFF);
    }

    @Unique
    private void mcatowns$clearMainContent(DrawContext context) {
        context.fill(width / 2 - 90, height / 2 - 108, width / 2 + 201, height / 2 + 104, 0xEE252525);
        context.fill(width / 2 - 86, height / 2 - 104, width / 2 + 197, height / 2 - 86, 0xFF3A3A3A);
    }

    @Unique
    private void mcatowns$drawCatalogHeader(DrawContext context) {
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Building Catalog"),
                width / 2 + 55, height / 2 - 101, 0xFFFFFF);
    }

    @Unique
    private void mcatowns$drawCustomButtons(DrawContext context, int mouseX, int mouseY) {
        for (ButtonWidget button : mcatowns$controls) {
            if (button.visible) button.render(context, mouseX, mouseY, 0.0F);
        }
    }

    @Unique
    private void mcatowns$drawRankChecklist(DrawContext context, TownBlueprintView view) {
        int x = width / 2 - 76;
        int y = height / 2 - 10;
        context.drawTextWithShadow(textRenderer, Text.literal("Town Progression"), x, y, 0xFFE080);
        for (int i = 0; i < Math.min(7, view.rankChecklist().size()); i++) {
            String line = view.rankChecklist().get(i);
            context.drawTextWithShadow(textRenderer, Text.literal(line), x, y + 14 + i * 12,
                    line.startsWith("✓") ? 0x80D080 : 0xE08080);
        }
    }

    @Unique
    private void mcatowns$drawCatalogDetails(DrawContext context) {
        for (Map.Entry<ButtonWidget, ItemStack> entry : mcatowns$icons.entrySet()) {
            context.drawItem(entry.getValue(), entry.getKey().getX() + 4, entry.getKey().getY() + 4);
        }
        if (mcatowns$selectedBuilding == null) return;
        int x = width / 2 + 34;
        int y = height / 2 + 8;
        context.fill(x - 4, y - 4, x + 150, y + 58, 0xCC252525);
        TownBlueprintView.BuildingOption option = mcatowns$selectedBuilding;
        context.drawTextWithShadow(textRenderer, Text.literal(option.name()), x, y, 0xFFE080);
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(option.description()), 145);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            context.drawTextWithShadow(textRenderer, lines.get(i), x, y + 14 + i * 10, 0xDDDDDD);
        }
        context.drawTextWithShadow(textRenderer, Text.literal(option.unlocked() ? "Unlocked" : "Locked"),
                x, y + 42, option.unlocked() ? 0x80D080 : 0xE08080);
        context.drawTextWithShadow(textRenderer, Text.literal(mcatowns$cost(option)), x, y + 52, 0xAAAAAA);
    }

    @Unique
    private void mcatowns$drawVillagerDetails(DrawContext context) {
        int x = width / 2 + 90;
        int y = height / 2 - 50;
        if (mcatowns$selectedResident == null) {
            context.drawTextWithShadow(textRenderer,
                    Text.literal(mcatowns$showSpecialists ? "No recruited specialists" : "No recruited residents"),
                    width / 2 - 80, y, 0xAAAAAA);
            return;
        }
        TownBlueprintView.ResidentEntry entry = mcatowns$selectedResident;
        context.drawTextWithShadow(textRenderer, Text.literal(entry.name()), x, y, 0xFFE080);
        context.drawTextWithShadow(textRenderer,
                Text.literal(entry.specialist() ? "Specialist: " + mcatowns$title(entry.specialistType()) : "Town Resident"),
                x, y + 14, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Recruited to this town"), x, y + 28, 0xAAAAAA);
    }

    @Unique
    private static ItemStack mcatowns$buildingIcon(String id) {
        String itemId = switch (id) {
            case "residence" -> "minecraft:red_bed";
            case "farm" -> "minecraft:composter";
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
        Item item = Registries.ITEM.get(new Identifier(itemId));
        return new ItemStack(Registries.ITEM.getId(item).equals(Registries.ITEM.getDefaultId()) ? Items.PAPER : item);
    }

    @Unique
    private static String mcatowns$cost(TownBlueprintView.BuildingOption option) {
        return option.tokens() + " Tokens, " + option.currency() + " Currency"
                + (option.prosperity() > 0 ? ", " + option.prosperity() + " Prosperity" : "");
    }

    @Unique
    private static String mcatowns$title(String value) {
        String text = value.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
