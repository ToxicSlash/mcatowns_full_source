package com.example.mcatowns.client;

import com.example.mcatowns.network.ModNetworking;
import com.example.mcatowns.network.TownBlueprintView;
import com.example.mcatowns.network.TownManagerView;
import com.example.mcatowns.network.VillagerTownView;
import com.example.mcatowns.screen.TownBlueprintScreen;
import com.example.mcatowns.screen.TownManagerScreen;
import com.example.mcatowns.screen.VillagerTownScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

/** Client-only packet transport and S2C screen opening. */
public final class ClientModNetworking {
    private ClientModNetworking() { }

    public static void register() {
        ModNetworking.installClientPacketSender((id, writer) -> {
            var buf = PacketByteBufs.create();
            writer.accept(buf);
            ClientPlayNetworking.send(id, buf);
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_TOWN_BLUEPRINT, (client, handler, buf, sender) -> {
            TownBlueprintView view;
            try {
                view = TownBlueprintView.read(buf);
            } catch (RuntimeException ignored) {
                return;
            }
            client.execute(() -> {
                if (view.founding()) client.setScreen(new TownBlueprintScreen(view));
                else McaBlueprintScreenBridge.open(view);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_TOWN_MANAGER, (client, handler, buf, sender) -> {
            TownManagerView view;
            try {
                view = TownManagerView.read(buf);
            } catch (RuntimeException ignored) {
                return;
            }
            client.execute(() -> client.setScreen(new TownManagerScreen(view)));
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_VILLAGER_TOWN, (client, handler, buf, sender) -> {
            VillagerTownView view;
            try {
                view = VillagerTownView.read(buf);
            } catch (RuntimeException ignored) {
                return;
            }
            client.execute(() -> client.setScreen(new VillagerTownScreen(view)));
        });
    }
}
