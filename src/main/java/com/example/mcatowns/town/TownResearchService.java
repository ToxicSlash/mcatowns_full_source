package com.example.mcatowns.town;

import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.registry.ModItems;
import com.example.mcatowns.util.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.UUID;

public final class TownResearchService {
    private TownResearchService() {
    }

    public static void research(ServerPlayerEntity player, UUID architectId, String researchId) {
        TownContext town = PlayerTownRegistry.get(player.getServerWorld()).getOwnedTown(player.getUuid()).orElse(null);
        TownResearchDefinition research = TownResearchDefinition.get(researchId);
        if (town == null || research == null) return;
        TownSavedData data = TownSavedData.get(player.getServerWorld(), town.townId());
        if (!"architect".equals(data.getSpecialists().get(architectId)) || data.isBuildingUnlocked(research.buildingId())) {
            return;
        }
        String missingInfrastructure = research.infrastructureThresholds().entrySet().stream()
                .filter(entry -> data.getInfrastructureProvided(entry.getKey()) < entry.getValue())
                .map(entry -> entry.getKey().displayName() + " " + data.getInfrastructureProvided(entry.getKey())
                        + "/" + entry.getValue())
                .findFirst().orElse("");
        if (!missingInfrastructure.isBlank()) {
            player.sendMessage(Text.translatable("text.mcatowns.research_infrastructure_missing",
                    missingInfrastructure), true);
            return;
        }

        MCATownsConfig config = MCATownsConfig.get();
        Item currency = item(config.currencyItemId);
        if (currency == null) currency = item("minecraft:emerald");
        Item essence = config.greatEssenceItemId.isBlank() ? null : item(config.greatEssenceItemId);
        int essenceCost = essence == null ? 0 : research.greatEssence();
        if (!player.getAbilities().creativeMode && (InventoryHelper.count(player.getInventory(), ModItems.BLUEPRINT_SCRAP) < research.scraps()
                || InventoryHelper.count(player.getInventory(), currency) < research.currency()
                || essenceCost > 0 && InventoryHelper.count(player.getInventory(), essence) < essenceCost
                || data.getTownTokens() < research.townTokens())) {
            player.sendMessage(Text.translatable("text.mcatowns.research_cost_missing"), true);
            return;
        }

        if (!data.unlockBuilding(research.buildingId())) return;
        if (!player.getAbilities().creativeMode) {
            InventoryHelper.remove(player.getInventory(), ModItems.BLUEPRINT_SCRAP, research.scraps());
            InventoryHelper.remove(player.getInventory(), currency, research.currency());
            if (essenceCost > 0) InventoryHelper.remove(player.getInventory(), essence, essenceCost);
            data.spendTownTokens(research.townTokens());
        }
        TownBuildingDefinition building = TownBuildingDefinition.get(research.buildingId());
        player.sendMessage(Text.translatable("text.mcatowns.research_complete", building.displayName()), false);
    }

    private static Item item(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null || !Registries.ITEM.containsId(identifier)) return null;
        return Registries.ITEM.get(identifier);
    }
}
