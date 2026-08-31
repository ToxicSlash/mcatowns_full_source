package com.example.mcatowns.network;

import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record VillagerTownView(
        UUID villagerId,
        String name,
        int hearts,
        String specialist,
        boolean introductionsComplete,
        boolean employed,
        boolean canInvite,
        int recruitmentCost,
        List<ResearchOption> research
) {
    public void write(PacketByteBuf buf) {
        buf.writeUuid(villagerId);
        buf.writeString(name, 64);
        buf.writeVarInt(hearts);
        buf.writeString(specialist, 32);
        buf.writeBoolean(introductionsComplete);
        buf.writeBoolean(employed);
        buf.writeBoolean(canInvite);
        buf.writeVarInt(recruitmentCost);
        buf.writeCollection(research, (packet, option) -> option.write(packet));
    }

    public static VillagerTownView read(PacketByteBuf buf) {
        return new VillagerTownView(buf.readUuid(), buf.readString(64), buf.readVarInt(), buf.readString(32),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt(),
                buf.readCollection(ArrayList::new, ResearchOption::read));
    }

    public record ResearchOption(String id, String name, boolean unlocked, int scraps, int essence,
                                 int currency, int tokens) {
        void write(PacketByteBuf buf) {
            buf.writeString(id, 64); buf.writeString(name, 64); buf.writeBoolean(unlocked);
            buf.writeVarInt(scraps); buf.writeVarInt(essence); buf.writeVarInt(currency); buf.writeVarInt(tokens);
        }
        static ResearchOption read(PacketByteBuf buf) {
            return new ResearchOption(buf.readString(64), buf.readString(64), buf.readBoolean(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
        }
    }
}
