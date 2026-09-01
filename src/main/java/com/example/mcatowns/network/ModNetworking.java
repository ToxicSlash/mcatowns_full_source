package com.example.mcatowns.network;

import com.example.mcatowns.MCATowns;
import com.example.mcatowns.event.BlueprintTownCreationHandler;
import com.example.mcatowns.event.TownRemovalHandler;
import com.example.mcatowns.registry.ModBlocks;
import com.example.mcatowns.screen.MayorDeskScreenHandler;
import com.example.mcatowns.screen.SiloScreenHandler;
import com.example.mcatowns.screen.StorehouseScreenHandler;
import com.example.mcatowns.screen.TreasuryScreenHandler;
import com.example.mcatowns.town.BlueprintSessionService;
import com.example.mcatowns.town.PlayerTownRegistry;
import com.example.mcatowns.town.TownBuildingService;
import com.example.mcatowns.town.TownFoodSystem;
import com.example.mcatowns.town.TownProgressionService;
import com.example.mcatowns.town.TownResearchService;
import com.example.mcatowns.town.VillagerRecruitmentService;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Shared packet identifiers plus server-side networking.
 * Client transport is installed by the client entrypoint so this class remains safe to load on a dedicated server.
 */
public final class ModNetworking {
    private static final long MIN_PACKET_INTERVAL_NANOS = 50_000_000L;
    private static final Map<UUID, Long> LAST_PACKET_NANOS = new ConcurrentHashMap<>();
    private static volatile ClientPacketSender clientPacketSender;

    public static final Identifier SET_TAX_RATE = id("set_tax_rate");
    public static final Identifier FESTIVAL = id("festival");
    public static final Identifier FOOD_RELIEF = id("food_relief");
    public static final Identifier BARRACKS_UPGRADE = id("barracks_upgrade");
    public static final Identifier TREASURY_DEPOSIT = id("treasury_deposit");
    public static final Identifier TREASURY_RETRIEVE = id("treasury_retrieve");
    public static final Identifier SILO_DEPOSIT = id("silo_deposit");
    public static final Identifier REQUEST_TOWN_BLUEPRINT = id("request_town_blueprint");
    public static final Identifier OPEN_TOWN_BLUEPRINT = id("open_town_blueprint");
    public static final Identifier OPEN_TOWN_MANAGER = id("open_town_manager");
    public static final Identifier FOUND_TOWN = id("found_town");
    public static final Identifier REMOVE_TOWN = id("remove_town");
    public static final Identifier RENAME_TOWN = id("rename_town");
    public static final Identifier SET_BLUEPRINT_TAX_RATE = id("set_blueprint_tax_rate");
    public static final Identifier REGISTER_TOWN_BUILDING = id("register_town_building");
    public static final Identifier REMOVE_TOWN_BUILDING = id("remove_town_building");
    public static final Identifier DETECT_TOWN_BUILDING = id("detect_town_building");
    public static final Identifier INSPECT_TOWN_BUILDING = id("inspect_town_building");
    public static final Identifier STOREHOUSE_FOOD = id("storehouse_food");
    public static final Identifier ADVANCE_TOWN = id("advance_town");
    public static final Identifier OPEN_VILLAGER_TOWN = id("open_villager_town");
    public static final Identifier RECRUIT_VILLAGER = id("recruit_villager");
    public static final Identifier RESEARCH = id("research");
    public static final Identifier ASSIGN_TOWN_WORKER = id("assign_town_worker");
    public static final Identifier AUTO_ASSIGN_TOWN_WORKERS = id("auto_assign_town_workers");

    private ModNetworking() { }

    private static Identifier id(String path) {
        return new Identifier(MCATowns.MOD_ID, path);
    }

    public static void registerC2S() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.player.getUuid();
            LAST_PACKET_NANOS.remove(playerId);
            BlueprintSessionService.clear(playerId);
        });

        ServerPlayNetworking.registerGlobalReceiver(SET_TAX_RATE, (server, player, handler, buf, sender) -> {
            if (!acceptPacket(player)) return;
            BlockPos pos;
            int rate;
            try {
                pos = buf.readBlockPos();
                rate = buf.readVarInt();
            } catch (RuntimeException ignored) {
                return;
            }
            server.execute(() -> {
                if (canUseDesk(player, pos)) MayorDeskActions.setTaxRate(player, pos, rate);
            });
        });

        registerPositionPacket(FESTIVAL, ModNetworking::canUseDesk, MayorDeskActions::holdFestival);
        registerPositionPacket(FOOD_RELIEF, ModNetworking::canUseDesk, MayorDeskActions::emergencyFoodRelief);
        registerPositionPacket(BARRACKS_UPGRADE, ModNetworking::canUseDesk, MayorDeskActions::buyBarracksUpgrade);
        registerPositionPacket(TREASURY_DEPOSIT, ModNetworking::canUseTreasury, TreasuryActions::deposit);
        registerPositionPacket(TREASURY_RETRIEVE, ModNetworking::canUseTreasury, TreasuryActions::retrieve);
        registerPositionPacket(SILO_DEPOSIT, ModNetworking::canUseSilo, SiloActions::deposit);
        registerEmptyPacket(REQUEST_TOWN_BLUEPRINT, BlueprintTownCreationHandler::openRequestedBlueprint);
        registerEmptyPacket(FOUND_TOWN, BlueprintTownCreationHandler::foundTown);
        registerPositionPacket(REMOVE_TOWN, TownRemovalHandler::canRemove, TownRemovalHandler::remove);

        ServerPlayNetworking.registerGlobalReceiver(RENAME_TOWN, (server, player, handler, buf, sender) -> {
            if (!acceptPacket(player)) return;
            BlockPos pos;
            String name;
            try {
                pos = buf.readBlockPos();
                name = buf.readString(64);
            } catch (RuntimeException ignored) {
                return;
            }
            server.execute(() -> {
                if (TownRemovalHandler.canRename(player, pos)) {
                    PlayerTownRegistry.get(player.getServerWorld()).getTownAt(pos).ifPresent(town ->
                            com.example.mcatowns.town.TownSavedData.get(player.getServerWorld(), town.townId()).setTownName(name));
                    BlueprintTownCreationHandler.openRequestedBlueprint(player);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SET_BLUEPRINT_TAX_RATE, (server, player, handler, buf, sender) -> {
            if (!acceptPacket(player)) return;
            BlockPos pos;
            int rate;
            try {
                pos = buf.readBlockPos();
                rate = buf.readVarInt();
            } catch (RuntimeException ignored) {
                return;
            }
            server.execute(() -> PlayerTownRegistry.get(player.getServerWorld()).getTownAt(pos)
                    .filter(town -> PlayerTownRegistry.get(player.getServerWorld()).isOwner(town.townId(), player))
                    .ifPresent(town -> {
                        com.example.mcatowns.town.TownSavedData data = com.example.mcatowns.town.TownSavedData.get(player.getServerWorld(), town.townId());
                        data.setTaxRate(Math.max(0, Math.min(3, rate)));
                        BlueprintTownCreationHandler.openRequestedBlueprint(player);
                    }));
        });

        ServerPlayNetworking.registerGlobalReceiver(REGISTER_TOWN_BUILDING, (server, player, handler, buf, sender) -> {
            if (!acceptPacket(player)) return;
            String type;
            try {
                type = buf.readString(64);
            } catch (RuntimeException ignored) {
                return;
            }
            server.execute(() -> {
                TownBuildingService.register(player, type);
                BlueprintTownCreationHandler.openRequestedBlueprint(player);
            });
        });

        registerEmptyPacket(REMOVE_TOWN_BUILDING, player -> {
            TownBuildingService.unregisterNearest(player);
            BlueprintTownCreationHandler.openRequestedBlueprint(player);
        });
        registerEmptyPacket(DETECT_TOWN_BUILDING, player -> {
            TownBuildingService.detect(player);
            BlueprintTownCreationHandler.openRequestedBlueprint(player);
        });

        ServerPlayNetworking.registerGlobalReceiver(INSPECT_TOWN_BUILDING, (server, player, handler, buf, sender) -> {
            if (!acceptPacket(player)) return;
            String type;
            try {
                type = buf.readString(64);
            } catch (RuntimeException ignored) {
                return;
            }
            server.execute(() -> {
                TownBuildingService.inspect(player, type);
                BlueprintTownCreationHandler.openRequestedBlueprint(player);
            });
        });

        registerPositionPacket(STOREHOUSE_FOOD, ModNetworking::canUseStorehouse, TownFoodSystem::contributeFromInventory);
        registerPositionPacket(ADVANCE_TOWN, ModNetworking::canManageTownBell, TownProgressionService::advance);

        ServerPlayNetworking.registerGlobalReceiver(RECRUIT_VILLAGER, (server, player, handler, buf, sender) -> {
            if (!acceptPacket(player)) return;
            UUID id;
            try {
                id = buf.readUuid();
            } catch (RuntimeException ignored) {
                return;
            }
            server.execute(() -> VillagerRecruitmentService.recruit(player, id));
        });

        ServerPlayNetworking.registerGlobalReceiver(RESEARCH, (server, player, handler, buf, sender) -> {
            if (!acceptPacket(player)) return;
            UUID architect;
            String research;
            try {
                architect = buf.readUuid();
                research = buf.readString(64);
            } catch (RuntimeException ignored) {
                return;
            }
            server.execute(() -> TownResearchService.research(player, architect, research));
        });

        ServerPlayNetworking.registerGlobalReceiver(ASSIGN_TOWN_WORKER, (server, player, handler, buf, sender) -> {
            if (!acceptPacket(player)) return;
            UUID resident;
            UUID building;
            try {
                resident = buf.readUuid();
                building = buf.readUuid();
            } catch (RuntimeException ignored) {
                return;
            }
            server.execute(() -> com.example.mcatowns.town.TownWorkforceActions.assign(player, resident, building));
        });
        registerEmptyPacket(AUTO_ASSIGN_TOWN_WORKERS, com.example.mcatowns.town.TownWorkforceActions::autoAssign);
    }

    private static void registerEmptyPacket(Identifier id, Consumer<ServerPlayerEntity> action) {
        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler, buf, sender) -> {
            if (acceptPacket(player)) server.execute(() -> action.accept(player));
        });
    }

    private static void registerPositionPacket(Identifier id, BiPredicate<ServerPlayerEntity, BlockPos> validator,
                                               BiConsumer<ServerPlayerEntity, BlockPos> action) {
        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler, buf, sender) -> {
            if (!acceptPacket(player)) return;
            BlockPos pos = readBlockPos(buf);
            if (pos == null) return;
            server.execute(() -> {
                if (validator.test(player, pos)) action.accept(player, pos);
            });
        });
    }

    private static BlockPos readBlockPos(PacketByteBuf buf) {
        try {
            return buf.readBlockPos();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean acceptPacket(ServerPlayerEntity player) {
        long now = System.nanoTime();
        Long previous = LAST_PACKET_NANOS.put(player.getUuid(), now);
        return previous == null || now - previous >= MIN_PACKET_INTERVAL_NANOS;
    }

    private static boolean canUseDesk(ServerPlayerEntity player, BlockPos pos) {
        if (!player.getServerWorld().isChunkLoaded(pos)
                || !player.getServerWorld().getBlockState(pos).isOf(ModBlocks.MAYOR_DESK)
                || !(player.currentScreenHandler instanceof MayorDeskScreenHandler deskHandler)
                || !deskHandler.getPos().equals(pos)) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    private static boolean canUseTreasury(ServerPlayerEntity player, BlockPos pos) {
        if (!player.getServerWorld().isChunkLoaded(pos)
                || !player.getServerWorld().getBlockState(pos).isOf(ModBlocks.TREASURY)
                || !(player.currentScreenHandler instanceof TreasuryScreenHandler treasuryHandler)
                || !treasuryHandler.getPos().equals(pos)) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    private static boolean canUseSilo(ServerPlayerEntity player, BlockPos pos) {
        if (!player.getServerWorld().isChunkLoaded(pos)
                || !player.getServerWorld().getBlockState(pos).isOf(ModBlocks.SILO)
                || !(player.currentScreenHandler instanceof SiloScreenHandler siloHandler)
                || !siloHandler.getPos().equals(pos)) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    private static boolean canUseStorehouse(ServerPlayerEntity player, BlockPos pos) {
        return player.getServerWorld().isChunkLoaded(pos)
                && player.getServerWorld().getBlockState(pos).isOf(ModBlocks.STOREHOUSE)
                && player.currentScreenHandler instanceof StorehouseScreenHandler storehouse
                && storehouse.getPos().equals(pos)
                && player.squaredDistanceTo(pos.toCenterPos()) <= 64.0;
    }

    private static boolean canManageTownBell(ServerPlayerEntity player, BlockPos pos) {
        return player.squaredDistanceTo(pos.toCenterPos()) <= 64.0
                && PlayerTownRegistry.get(player.getServerWorld()).getTownAt(pos)
                .filter(town -> PlayerTownRegistry.get(player.getServerWorld()).isOwner(town.townId(), player))
                .isPresent();
    }

    public static void openTownBlueprint(ServerPlayerEntity player, TownBlueprintView view) {
        PacketByteBuf buf = PacketByteBufs.create();
        view.write(buf);
        ServerPlayNetworking.send(player, OPEN_TOWN_BLUEPRINT, buf);
    }

    public static void openTownManager(ServerPlayerEntity player, TownManagerView view) {
        PacketByteBuf buf = PacketByteBufs.create();
        view.write(buf);
        ServerPlayNetworking.send(player, OPEN_TOWN_MANAGER, buf);
    }

    public static void openVillagerTown(ServerPlayerEntity player, VillagerTownView view) {
        PacketByteBuf buf = PacketByteBufs.create();
        view.write(buf);
        ServerPlayNetworking.send(player, OPEN_VILLAGER_TOWN, buf);
    }

    /** Installed only by the client entrypoint; deliberately contains no client-only type in this class. */
    public static void installClientPacketSender(ClientPacketSender sender) {
        clientPacketSender = Objects.requireNonNull(sender, "sender");
    }

    public static void sendSetTaxRate(BlockPos pos, int rate) {
        sendClientPacket(SET_TAX_RATE, buf -> {
            buf.writeBlockPos(pos);
            buf.writeVarInt(rate);
        });
    }

    public static void sendFestival(BlockPos pos) { sendPositionPacket(FESTIVAL, pos); }
    public static void sendEmergencyFoodRelief(BlockPos pos) { sendPositionPacket(FOOD_RELIEF, pos); }
    public static void sendBarracksUpgrade(BlockPos pos) { sendPositionPacket(BARRACKS_UPGRADE, pos); }
    public static void sendTreasuryDeposit(BlockPos pos) { sendPositionPacket(TREASURY_DEPOSIT, pos); }
    public static void sendTreasuryRetrieve(BlockPos pos) { sendPositionPacket(TREASURY_RETRIEVE, pos); }
    public static void sendSiloDeposit(BlockPos pos) { sendPositionPacket(SILO_DEPOSIT, pos); }
    public static void sendOpenTownBlueprint() { sendEmptyPacket(REQUEST_TOWN_BLUEPRINT); }
    public static void sendFoundTown() { sendEmptyPacket(FOUND_TOWN); }
    public static void sendRemoveTown(BlockPos pos) { sendPositionPacket(REMOVE_TOWN, pos); }

    public static void sendRenameTown(BlockPos pos, String name) {
        sendClientPacket(RENAME_TOWN, buf -> {
            buf.writeBlockPos(pos);
            buf.writeString(name, 64);
        });
    }

    public static void sendBlueprintTaxRate(BlockPos pos, int rate) {
        sendClientPacket(SET_BLUEPRINT_TAX_RATE, buf -> {
            buf.writeBlockPos(pos);
            buf.writeVarInt(rate);
        });
    }

    public static void sendRegisterTownBuilding(String type) {
        sendClientPacket(REGISTER_TOWN_BUILDING, buf -> buf.writeString(type, 64));
    }

    public static void sendRemoveTownBuilding() { sendEmptyPacket(REMOVE_TOWN_BUILDING); }
    public static void sendDetectTownBuilding() { sendEmptyPacket(DETECT_TOWN_BUILDING); }
    public static void sendInspectTownBuilding(String type) {
        sendClientPacket(INSPECT_TOWN_BUILDING, buf -> buf.writeString(type, 64));
    }
    public static void sendStorehouseFood(BlockPos pos) { sendPositionPacket(STOREHOUSE_FOOD, pos); }
    public static void sendAdvanceTown(BlockPos pos) { sendPositionPacket(ADVANCE_TOWN, pos); }

    public static void sendRecruitVillager(UUID id) {
        sendClientPacket(RECRUIT_VILLAGER, buf -> buf.writeUuid(id));
    }

    public static void sendResearch(UUID architect, String research) {
        sendClientPacket(RESEARCH, buf -> {
            buf.writeUuid(architect);
            buf.writeString(research, 64);
        });
    }

    public static void sendAssignTownWorker(UUID resident, UUID building) {
        sendClientPacket(ASSIGN_TOWN_WORKER, buf -> {
            buf.writeUuid(resident);
            buf.writeUuid(building);
        });
    }

    public static void sendAutoAssignTownWorkers() { sendEmptyPacket(AUTO_ASSIGN_TOWN_WORKERS); }

    private static void sendEmptyPacket(Identifier id) {
        sendClientPacket(id, buf -> { });
    }

    private static void sendPositionPacket(Identifier id, BlockPos pos) {
        sendClientPacket(id, buf -> buf.writeBlockPos(pos));
    }

    private static void sendClientPacket(Identifier id, Consumer<PacketByteBuf> writer) {
        ClientPacketSender sender = clientPacketSender;
        if (sender != null) sender.send(id, writer);
    }

    @FunctionalInterface
    public interface ClientPacketSender {
        void send(Identifier id, Consumer<PacketByteBuf> writer);
    }
}
