package com.example.mcatowns.registry;

import com.example.mcatowns.MCATowns;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroup {
    public static final ItemGroup GROUP = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(MCATowns.MOD_ID, "group"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModBlocks.MAYOR_DESK))
                    .displayName(Text.translatable("itemGroup.mcatowns"))
                    .entries((context, entries) -> {
                        entries.add(ModBlocks.MAYOR_DESK);
                        entries.add(ModBlocks.TREASURY);
                        entries.add(ModBlocks.BARRACKS);
                        entries.add(ModBlocks.SILO);
                        entries.add(ModBlocks.TOWN_BELL);
                        entries.add(ModBlocks.STOREHOUSE);
                        entries.add(ModItems.BLUEPRINT_SCRAP);
                    })
                    .build()
    );

    public static void register() {
    }
}
