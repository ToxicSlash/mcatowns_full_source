package com.example.mcatowns.town;

import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;
import com.example.mcatowns.config.MCATownsConfig;

public class TownFoodSystem {
    public static void applyDailyCycle(ServerWorld world, TownContext context, TownSavedData data) {
        TownBuildingService.refreshDerivedValues(data);
        int consumed = Math.max(0, data.getResidents().size() * MCATownsConfig.get().foodPerResidentPerDay);
        int farmers = countFarmers(world, data);
        int produced = data.hasBuilding("farm") ? Math.min(data.countBuildings("farm") * 3, farmers * 2) : 0;
        data.setDailyFoodConsumed(consumed);
        data.setDailyFoodProduced(produced);
        int next = Math.max(0, data.getFoodReserves() - consumed + produced);
        next = Math.min(data.getFoodCapacity(), next);
        data.setFoodReserves(next);
    }

    private static int countFarmers(ServerWorld world, TownSavedData data) {
        int farmers = 0;
        for (java.util.UUID id : data.getResidents()) {
            Entity entity = world.getEntity(id);
            if (entity instanceof VillagerEntity villager
                    && villager.getVillagerData().getProfession() == VillagerProfession.FARMER) farmers++;
        }
        return farmers;
    }

    public static void contributeFromInventory(ServerPlayerEntity player, BlockPos storehousePos) {
        TownContext town = TownManager.resolveTownContext(player.getServerWorld(), storehousePos);
        TownSavedData data = TownSavedData.get(player.getServerWorld(), town.townId());
        boolean registered = data.getRegisteredBuildings().stream()
                .anyMatch(building -> (building.type().equals("storehouse") || building.type().equals("granary"))
                        && building.pos().equals(storehousePos));
        if (!registered || data.getFoodCapacity() <= data.getFoodReserves()) return;

        int added = 0;
        for (int slot = 0; slot < player.getInventory().size() && data.getFoodReserves() + added < data.getFoodCapacity(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            FoodComponent food = stack.getItem().getFoodComponent();
            if (stack.isEmpty() || food == null || food.getHunger() <= 0) continue;
            int room = data.getFoodCapacity() - data.getFoodReserves() - added;
            int items = Math.min(stack.getCount(), (room + food.getHunger() - 1) / food.getHunger());
            if (items <= 0) continue;
            stack.decrement(items);
            added += Math.min(room, items * food.getHunger());
        }
        if (added > 0) {
            data.setFoodReserves(data.getFoodReserves() + added);
            player.sendMessage(Text.translatable("text.mcatowns.food_contributed", added), true);
        }
    }

    public static int foodPercent(TownSavedData data) {
        return data.getFoodCapacity() <= 0 ? 0 : data.getFoodReserves() * 100 / data.getFoodCapacity();
    }
}
