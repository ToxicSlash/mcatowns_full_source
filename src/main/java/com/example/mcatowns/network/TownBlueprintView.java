package com.example.mcatowns.network;

import com.example.mcatowns.town.TownRank;
import com.example.mcatowns.town.TownBuildingCategory;
import com.example.mcatowns.town.InfrastructureType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record TownBlueprintView(
        boolean founding,
        String name,
        String playerRank,
        TownRank rank,
        int prosperity,
        int prosperityBase,
        List<InfrastructureEntry> infrastructure,
        int happiness,
        int population,
        int populationCapacity,
        int food,
        int foodCapacity,
        int buildings,
        int specialists,
        int taxRate,
        boolean canManage,
        boolean canRemove,
        BlockPos anchorPos,
        BlockPos centerPos,
        BlockPos playerPos,
        boolean hasCrown,
        int blueprintScraps,
        List<BuildingEntry> detectedBuildings,
        String inspectionType,
        boolean inspectionPassed,
        List<RequirementLine> inspectionCost,
        List<RequirementLine> inspectionFurniture,
        List<BuildingOption> buildingOptions,
        List<String> rankChecklist,
        List<ResidentEntry> residents,
        List<BuildingEntry> registeredBuildings
) {
    public void write(PacketByteBuf buf) {
        buf.writeBoolean(founding);
        buf.writeString(name, 64);
        buf.writeString(playerRank, 32);
        buf.writeEnumConstant(rank);
        buf.writeVarInt(prosperity);
        buf.writeVarInt(prosperityBase);
        buf.writeCollection(infrastructure, (packet, entry) -> entry.write(packet));
        buf.writeVarInt(happiness);
        buf.writeVarInt(population);
        buf.writeVarInt(populationCapacity);
        buf.writeVarInt(food);
        buf.writeVarInt(foodCapacity);
        buf.writeVarInt(buildings);
        buf.writeVarInt(specialists);
        buf.writeVarInt(taxRate);
        buf.writeBoolean(canManage);
        buf.writeBoolean(canRemove);
        buf.writeBlockPos(anchorPos);
        buf.writeBlockPos(centerPos);
        buf.writeBlockPos(playerPos);
        buf.writeBoolean(hasCrown);
        buf.writeVarInt(blueprintScraps);
        buf.writeCollection(detectedBuildings, (packet, building) -> building.write(packet));
        buf.writeString(inspectionType, 64);
        buf.writeBoolean(inspectionPassed);
        buf.writeCollection(inspectionCost, (packet, line) -> line.write(packet));
        buf.writeCollection(inspectionFurniture, (packet, line) -> line.write(packet));
        buf.writeCollection(buildingOptions, (packet, option) -> option.write(packet));
        buf.writeCollection(rankChecklist, (packet, line) -> packet.writeString(line, 128));
        buf.writeCollection(residents, (packet, resident) -> resident.write(packet));
        buf.writeCollection(registeredBuildings, (packet, building) -> building.write(packet));
    }

    public static TownBlueprintView read(PacketByteBuf buf) {
        return new TownBlueprintView(
                buf.readBoolean(),
                buf.readString(64),
                buf.readString(32),
                buf.readEnumConstant(TownRank.class),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readCollection(ArrayList::new, InfrastructureEntry::read),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBlockPos(),
                buf.readBlockPos(),
                buf.readBlockPos(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readCollection(ArrayList::new, BuildingEntry::read),
                buf.readString(64),
                buf.readBoolean(),
                buf.readCollection(ArrayList::new, RequirementLine::read),
                buf.readCollection(ArrayList::new, RequirementLine::read),
                buf.readCollection(ArrayList::new, BuildingOption::read),
                buf.readCollection(ArrayList::new, packet -> packet.readString(128)),
                buf.readCollection(ArrayList::new, ResidentEntry::read),
                buf.readCollection(ArrayList::new, BuildingEntry::read)
        );
    }

    public record BuildingOption(
            String id,
            String name,
            TownBuildingCategory category,
            String description,
            boolean unlocked,
            int tokens,
            int currency,
            int prosperity,
            String icon,
            boolean legacy
    ) {
        void write(PacketByteBuf buf) {
            buf.writeString(id, 64);
            buf.writeString(name, 64);
            buf.writeEnumConstant(category);
            buf.writeString(description, 256);
            buf.writeBoolean(unlocked);
            buf.writeVarInt(tokens);
            buf.writeVarInt(currency);
            buf.writeVarInt(prosperity);
            buf.writeString(icon, 64);
            buf.writeBoolean(legacy);
        }

        static BuildingOption read(PacketByteBuf buf) {
            return new BuildingOption(buf.readString(64), buf.readString(64),
                    buf.readEnumConstant(TownBuildingCategory.class), buf.readString(256),
                    buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readString(64), buf.readBoolean());
        }
    }

    public record ResidentEntry(UUID id, String name, String specialistType,
                                UUID assignedBuildingId, String assignedBuildingName) {
        void write(PacketByteBuf buf) {
            buf.writeUuid(id);
            buf.writeString(name, 64);
            buf.writeString(specialistType, 32);
            buf.writeUuid(assignedBuildingId);
            buf.writeString(assignedBuildingName, 64);
        }

        static ResidentEntry read(PacketByteBuf buf) {
            return new ResidentEntry(buf.readUuid(), buf.readString(64), buf.readString(32),
                    buf.readUuid(), buf.readString(64));
        }

        public boolean specialist() {
            return !specialistType.isBlank();
        }
    }

    public record InfrastructureEntry(InfrastructureType type, int provided, int reserved, int available) {
        void write(PacketByteBuf buf) {
            buf.writeEnumConstant(type);
            buf.writeVarInt(provided);
            buf.writeVarInt(reserved);
            buf.writeVarInt(available);
        }

        static InfrastructureEntry read(PacketByteBuf buf) {
            return new InfrastructureEntry(buf.readEnumConstant(InfrastructureType.class), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt());
        }
    }

    public record RequirementLine(String text, int state) {
        void write(PacketByteBuf buf) {
            buf.writeString(text, 128);
            buf.writeVarInt(state);
        }

        static RequirementLine read(PacketByteBuf buf) {
            return new RequirementLine(buf.readString(128), buf.readVarInt());
        }
    }

    public record BuildingEntry(UUID id, String type, String name, BlockPos pos, String icon,
                                BlockPos minPos, BlockPos maxPos, int tier, String status,
                                int quality, int workerCount, int workerSlots, int cropState) {
        void write(PacketByteBuf buf) {
            buf.writeUuid(id);
            buf.writeString(type, 64);
            buf.writeString(name, 64);
            buf.writeBlockPos(pos);
            buf.writeString(icon, 64);
            buf.writeBlockPos(minPos);
            buf.writeBlockPos(maxPos);
            buf.writeVarInt(tier);
            buf.writeString(status, 32);
            buf.writeVarInt(quality);
            buf.writeVarInt(workerCount);
            buf.writeVarInt(workerSlots);
            buf.writeVarInt(cropState);
        }

        static BuildingEntry read(PacketByteBuf buf) {
            return new BuildingEntry(buf.readUuid(), buf.readString(64), buf.readString(64), buf.readBlockPos(),
                    buf.readString(64), buf.readBlockPos(), buf.readBlockPos(), buf.readVarInt(),
                    buf.readString(32), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }
    }
}
