package com.example.mcatowns.registry;

import com.example.mcatowns.MCATowns;
import com.example.mcatowns.screen.MayorDeskScreenHandler;
import com.example.mcatowns.screen.SiloScreenHandler;
import com.example.mcatowns.screen.TreasuryScreenHandler;
import com.example.mcatowns.screen.StorehouseScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static ScreenHandlerType<MayorDeskScreenHandler> MAYOR_DESK;
    public static ScreenHandlerType<TreasuryScreenHandler> TREASURY;
    public static ScreenHandlerType<SiloScreenHandler> SILO;
    public static ScreenHandlerType<StorehouseScreenHandler> STOREHOUSE;

    public static void register() {
        MAYOR_DESK = Registry.register(
                Registries.SCREEN_HANDLER,
                new Identifier(MCATowns.MOD_ID, "mayor_desk"),
                new ExtendedScreenHandlerType<>(MayorDeskScreenHandler::new)
        );
        TREASURY = Registry.register(
                Registries.SCREEN_HANDLER,
                new Identifier(MCATowns.MOD_ID, "treasury"),
                new ExtendedScreenHandlerType<>(TreasuryScreenHandler::new)
        );
        SILO = Registry.register(
                Registries.SCREEN_HANDLER,
                new Identifier(MCATowns.MOD_ID, "silo"),
                new ExtendedScreenHandlerType<>(SiloScreenHandler::new)
        );
        STOREHOUSE = Registry.register(
                Registries.SCREEN_HANDLER,
                new Identifier(MCATowns.MOD_ID, "storehouse"),
                new ExtendedScreenHandlerType<>(StorehouseScreenHandler::new)
        );
    }
}
