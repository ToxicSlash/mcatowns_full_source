package com.example.mcatowns;

import com.example.mcatowns.client.ClientModNetworking;
import com.example.mcatowns.registry.ModScreenHandlers;
import com.example.mcatowns.screen.MayorDeskScreen;
import com.example.mcatowns.screen.SiloScreen;
import com.example.mcatowns.screen.StorehouseScreen;
import com.example.mcatowns.screen.TreasuryScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class MCATownsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.MAYOR_DESK, MayorDeskScreen::new);
        HandledScreens.register(ModScreenHandlers.TREASURY, TreasuryScreen::new);
        HandledScreens.register(ModScreenHandlers.SILO, SiloScreen::new);
        HandledScreens.register(ModScreenHandlers.STOREHOUSE, StorehouseScreen::new);
        ClientModNetworking.register();
        MCATowns.LOGGER.info("MCA Towns client initialized");
    }
}
