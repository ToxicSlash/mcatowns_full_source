package com.example.mcatowns.registry;

import com.example.mcatowns.MCATowns;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item BLUEPRINT_SCRAP = register("blueprint_scrap", new Item(new Item.Settings()));

    private ModItems() {
    }

    private static Item register(String id, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(MCATowns.MOD_ID, id), item);
    }

    public static void register() {
    }
}
