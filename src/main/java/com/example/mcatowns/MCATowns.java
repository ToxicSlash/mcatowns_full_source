package com.example.mcatowns;

import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.event.BlueprintTownCreationHandler;
import com.example.mcatowns.event.ServerCombatEventsHandler;
import com.example.mcatowns.event.ServerTickEventsHandler;
import com.example.mcatowns.event.VillagerTownInteractionHandler;
import com.example.mcatowns.event.TownBlockEvents;
import com.example.mcatowns.network.ModNetworking;
import com.example.mcatowns.registry.ModBlockEntities;
import com.example.mcatowns.registry.ModBlocks;
import com.example.mcatowns.registry.ModItemGroup;
import com.example.mcatowns.registry.ModItems;
import com.example.mcatowns.registry.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCATowns implements ModInitializer {
    public static final String MOD_ID = "mcatowns";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        MCATownsConfig.load();
        ModItems.register();
        ModBlocks.register();
        ModBlockEntities.register();
        ModScreenHandlers.register();
        ModItemGroup.register();
        ModNetworking.registerC2S();
        ServerTickEventsHandler.register();
        ServerCombatEventsHandler.register();
        BlueprintTownCreationHandler.register();
        VillagerTownInteractionHandler.register();
        TownBlockEvents.register();

        LOGGER.info("MCA Towns initialized");
    }
}
