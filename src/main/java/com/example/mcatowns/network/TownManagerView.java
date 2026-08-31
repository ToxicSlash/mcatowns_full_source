package com.example.mcatowns.network;

import com.example.mcatowns.town.TownRank;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record TownManagerView(
        String name,
        TownRank rank,
        int prosperity,
        int prosperityFloor,
        int townTokens,
        int food,
        int foodCapacity,
        int population,
        int populationCapacity,
        int buildings,
        int specialists,
        boolean canManage,
        boolean canRemove,
        BlockPos bellPos,
        List<String> checklist,
        int residentCount,
        String requestName,
        String requestType,
        long requestDueDay,
        int requestProsperity,
        int requestTokens,
        List<String> requestRequirements,
        int bountyKills
) {
    public void write(PacketByteBuf buf) {
        buf.writeString(name, 64);
        buf.writeEnumConstant(rank);
        buf.writeVarInt(prosperity);
        buf.writeVarInt(prosperityFloor);
        buf.writeVarInt(townTokens);
        buf.writeVarInt(food);
        buf.writeVarInt(foodCapacity);
        buf.writeVarInt(population);
        buf.writeVarInt(populationCapacity);
        buf.writeVarInt(buildings);
        buf.writeVarInt(specialists);
        buf.writeBoolean(canManage);
        buf.writeBoolean(canRemove);
        buf.writeBlockPos(bellPos);
        buf.writeCollection(checklist, (packet, line) -> packet.writeString(line, 128));
        buf.writeVarInt(residentCount);
        buf.writeString(requestName, 64);
        buf.writeString(requestType, 32);
        buf.writeLong(requestDueDay);
        buf.writeVarInt(requestProsperity);
        buf.writeVarInt(requestTokens);
        buf.writeCollection(requestRequirements, (packet, line) -> packet.writeString(line, 128));
        buf.writeVarInt(bountyKills);
    }

    public static TownManagerView read(PacketByteBuf buf) {
        return new TownManagerView(
                buf.readString(64),
                buf.readEnumConstant(TownRank.class),
                buf.readVarInt(),
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
                buf.readCollection(ArrayList::new, packet -> packet.readString(128)),
                buf.readVarInt(),
                buf.readString(64),
                buf.readString(32),
                buf.readLong(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readCollection(ArrayList::new, packet -> packet.readString(128)),
                buf.readVarInt()
        );
    }
}
