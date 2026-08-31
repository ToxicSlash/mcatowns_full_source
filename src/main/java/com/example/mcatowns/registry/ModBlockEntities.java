package com.example.mcatowns.registry;

import com.example.mcatowns.MCATowns;
import com.example.mcatowns.blockentity.BarracksBlockEntity;
import com.example.mcatowns.blockentity.MayorDeskBlockEntity;
import com.example.mcatowns.blockentity.SiloBlockEntity;
import com.example.mcatowns.blockentity.TreasuryBlockEntity;
import com.example.mcatowns.blockentity.StorehouseBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static BlockEntityType<MayorDeskBlockEntity> MAYOR_DESK;
    public static BlockEntityType<TreasuryBlockEntity> TREASURY;
    public static BlockEntityType<BarracksBlockEntity> BARRACKS;
    public static BlockEntityType<SiloBlockEntity> SILO;
    public static BlockEntityType<StorehouseBlockEntity> STOREHOUSE;

    public static void register() {
        MAYOR_DESK = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MCATowns.MOD_ID, "mayor_desk"),
                FabricBlockEntityTypeBuilder.create(MayorDeskBlockEntity::new, ModBlocks.MAYOR_DESK).build()
        );

        TREASURY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MCATowns.MOD_ID, "treasury"),
                FabricBlockEntityTypeBuilder.create(TreasuryBlockEntity::new, ModBlocks.TREASURY).build()
        );

        BARRACKS = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MCATowns.MOD_ID, "barracks"),
                FabricBlockEntityTypeBuilder.create(BarracksBlockEntity::new, ModBlocks.BARRACKS).build()
        );

        SILO = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MCATowns.MOD_ID, "silo"),
                FabricBlockEntityTypeBuilder.create(SiloBlockEntity::new, ModBlocks.SILO).build()
        );
        STOREHOUSE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MCATowns.MOD_ID, "storehouse"),
                FabricBlockEntityTypeBuilder.create(StorehouseBlockEntity::new, ModBlocks.STOREHOUSE).build()
        );
    }
}
