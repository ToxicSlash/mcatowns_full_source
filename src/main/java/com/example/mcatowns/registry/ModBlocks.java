package com.example.mcatowns.registry;

import com.example.mcatowns.MCATowns;
import com.example.mcatowns.block.BarracksBlock;
import com.example.mcatowns.block.MayorDeskBlock;
import com.example.mcatowns.block.SiloBlock;
import com.example.mcatowns.block.TreasuryBlock;
import com.example.mcatowns.block.TownBellBlock;
import com.example.mcatowns.block.StorehouseBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block MAYOR_DESK = register("mayor_desk", new MayorDeskBlock(
            AbstractBlock.Settings.create().mapColor(MapColor.OAK_TAN).sounds(BlockSoundGroup.WOOD).strength(2.5f)
    ));

    public static final Block TREASURY = register("treasury", new TreasuryBlock(
            AbstractBlock.Settings.create().mapColor(MapColor.SPRUCE_BROWN).sounds(BlockSoundGroup.WOOD).strength(3.0f)
    ));

    public static final Block BARRACKS = register("barracks", new BarracksBlock(
            AbstractBlock.Settings.create().mapColor(MapColor.STONE_GRAY).sounds(BlockSoundGroup.STONE).strength(3.5f)
    ));

    public static final Block SILO = register("silo", new SiloBlock(
            AbstractBlock.Settings.create().mapColor(MapColor.PALE_YELLOW).sounds(BlockSoundGroup.WOOD).strength(2.5f)
    ));

    public static final Block TOWN_BELL = register("town_bell", new TownBellBlock(
            AbstractBlock.Settings.create().mapColor(MapColor.GOLD).sounds(BlockSoundGroup.METAL).strength(3.0f)
    ));

    public static final Block STOREHOUSE = register("storehouse", new StorehouseBlock(
            AbstractBlock.Settings.create().mapColor(MapColor.SPRUCE_BROWN).sounds(BlockSoundGroup.WOOD).strength(3.0f)
    ));

    private static Block register(String id, Block block) {
        Registry.register(Registries.BLOCK, new Identifier(MCATowns.MOD_ID, id), block);
        Registry.register(Registries.ITEM, new Identifier(MCATowns.MOD_ID, id), new BlockItem(block, new Item.Settings()));
        return block;
    }

    public static void register() {
    }
}
