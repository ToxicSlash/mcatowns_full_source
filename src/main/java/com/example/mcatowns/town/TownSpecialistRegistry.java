package com.example.mcatowns.town;

import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.integration.QuestIntegration;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TownSpecialistRegistry extends PersistentState {
    private final Map<UUID, SpecialistType> candidates = new HashMap<>();

    public static TownSpecialistRegistry get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                TownSpecialistRegistry::fromNbt, TownSpecialistRegistry::new, "mcatowns_specialists");
    }

    public static TownSpecialistRegistry fromNbt(NbtCompound nbt) {
        TownSpecialistRegistry registry = new TownSpecialistRegistry();
        NbtList list = nbt.getList("Candidates", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            if (!entry.containsUuid("Id")) continue;
            UUID id = entry.getUuid("Id");
            try {
                registry.candidates.put(id, SpecialistType.valueOf(entry.getString("Type")));
            } catch (IllegalArgumentException ignored) { }
        }
        return registry;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        candidates.forEach((id, type) -> {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("Id", id);
            entry.putString("Type", type.name());
            list.add(entry);
        });
        nbt.put("Candidates", list);
        return nbt;
    }

    public void inspect(Entity entity) {
        if (!(entity instanceof VillagerEntity villager) || villager.isBaby()) return;
        if (villager.getVillagerData().getProfession() != VillagerProfession.NONE) return;
        if (candidates.containsKey(entity.getUuid())) return;
        int chance = MCATownsConfig.get().specialistChancePercent;
        int roll = Math.floorMod(entity.getUuid().hashCode(), 100);
        if (roll >= chance) return;
        SpecialistType[] types = SpecialistType.values();
        SpecialistType type = types[Math.floorMod(entity.getUuid().hashCode() / 100, types.length)];
        candidates.put(entity.getUuid(), type);
        markDirty();
        QuestIntegration.assignIntroductions(entity);
    }

    public Optional<SpecialistType> get(UUID id) {
        return Optional.ofNullable(candidates.get(id));
    }
}
