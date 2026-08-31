package com.example.mcatowns.town;

import com.example.mcatowns.blockentity.StorehouseBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TownRequestService {
    private static final List<RequestTemplate> GENERIC = List.of(
            template("timber", "Building Timber", "minecraft:oak_log", 12),
            template("stone", "Masonry Supplies", "minecraft:cobblestone", 20),
            template("coal", "Heating Fuel", "minecraft:coal", 10),
            template("wool", "Warm Bedding", "minecraft:white_wool", 8),
            template("torches", "Street Lighting", "minecraft:torch", 16)
    );

    private TownRequestService() { }

    public static void tickDaily(ServerWorld world, TownContext context, TownSavedData data, long day) {
        if (!data.hasBuilding("storehouse")) return;
        TownRequest request = data.getActiveRequest();
        if (request == null) {
            if (day >= data.getNextRequestDay()) data.setActiveRequest(generate(world, data, day));
            return;
        }
        if (day < request.dueDay()) return;
        StorehouseBlockEntity storehouse = findStorehouse(world, data);
        if (storehouse == null) return; // Never fail a request because its chunk was unloaded.
        if (hasAll(storehouse, request.requirements())) {
            consume(storehouse, request.requirements());
            data.addProsperity(request.prosperityReward());
            data.addTownTokens(request.tokenReward());
        }
        data.clearRequest(day + 1);
    }

    public static List<String> requirementLines(ServerWorld world, TownSavedData data) {
        TownRequest request = data.getActiveRequest();
        if (request == null) return List.of();
        StorehouseBlockEntity storehouse = findStorehouse(world, data);
        List<String> lines = new ArrayList<>();
        request.requirements().forEach((id, needed) -> {
            Item item = item(id);
            int stored = storehouse == null ? 0 : count(storehouse, item);
            lines.add(displayItem(id) + ": " + stored + " / " + needed);
        });
        return lines;
    }

    private static TownRequest generate(ServerWorld world, TownSavedData data, long day) {
        List<RequestTemplate> pool = new ArrayList<>(GENERIC);
        if (data.hasBuilding("blacksmith")) pool.add(new RequestTemplate("smithy", "Smithy Supplies",
                Map.of("minecraft:iron_ingot", 8, "minecraft:coal", 12)));
        if (data.hasBuilding("guard_post")) pool.add(new RequestTemplate("guard", "Guard Equipment",
                Map.of("minecraft:arrow", 16, "minecraft:leather", 8)));
        if (data.hasBuilding("scholar")) pool.add(new RequestTemplate("scholar", "Scholar's Materials",
                Map.of("minecraft:paper", 16, "minecraft:book", 4, "minecraft:ink_sac", 4)));
        if (data.hasBuilding("inn")) pool.add(new RequestTemplate("inn", "Inn Supplies",
                Map.of("minecraft:bread", 10, "minecraft:white_wool", 6, "minecraft:glass_bottle", 4)));

        RequestTemplate selected = pool.get(world.random.nextInt(pool.size()));
        boolean important = data.getTownRank().ordinal() >= TownRank.VILLAGE.ordinal() && world.random.nextInt(5) == 0;
        int scale = 1 + Math.max(0, data.getPopulation()) / 10 + (important ? 1 : 0);
        Map<String, Integer> requirements = new LinkedHashMap<>();
        selected.requirements().forEach((id, count) -> requirements.put(id, count * scale));
        TownRequest.Type type = important ? TownRequest.Type.IMPORTANT : TownRequest.Type.ROUTINE;
        return new TownRequest(selected.id(), selected.name(), type, requirements, day + (important ? 3 : 2),
                important ? 4 : 2, important ? 5 : 2);
    }

    private static StorehouseBlockEntity findStorehouse(ServerWorld world, TownSavedData data) {
        for (RegisteredTownBuilding building : data.getRegisteredBuildings()) {
            if (!building.type().equals("storehouse") || !world.isChunkLoaded(building.pos())) continue;
            if (world.getBlockEntity(building.pos()) instanceof StorehouseBlockEntity storehouse) return storehouse;
        }
        return null;
    }

    private static boolean hasAll(StorehouseBlockEntity inventory, Map<String, Integer> requirements) {
        return requirements.entrySet().stream().allMatch(entry -> count(inventory, item(entry.getKey())) >= entry.getValue());
    }

    private static void consume(StorehouseBlockEntity inventory, Map<String, Integer> requirements) {
        requirements.forEach((id, amount) -> remove(inventory, item(id), amount));
        inventory.markDirty();
    }

    private static int count(StorehouseBlockEntity inventory, Item item) {
        int total = 0;
        for (int i = 0; i < inventory.size(); i++) if (inventory.getStack(i).isOf(item)) total += inventory.getStack(i).getCount();
        return total;
    }

    private static void remove(StorehouseBlockEntity inventory, Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < inventory.size() && remaining > 0; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isOf(item)) continue;
            int remove = Math.min(remaining, stack.getCount());
            stack.decrement(remove);
            remaining -= remove;
        }
    }

    private static Item item(String id) { return Registries.ITEM.get(new Identifier(id)); }
    private static String displayItem(String id) { String path = new Identifier(id).getPath().replace('_', ' '); return Character.toUpperCase(path.charAt(0)) + path.substring(1); }
    private static RequestTemplate template(String id, String name, String item, int count) { return new RequestTemplate(id, name, Map.of(item, count)); }
    private record RequestTemplate(String id, String name, Map<String, Integer> requirements) { }
}
