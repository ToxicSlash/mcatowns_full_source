package com.example.mcatowns.town;

import com.example.mcatowns.config.MCATownsConfig;
import com.example.mcatowns.integration.MCAIntegration;
import com.example.mcatowns.integration.QuestIntegration;
import com.example.mcatowns.network.ModNetworking;
import com.example.mcatowns.network.VillagerTownView;
import com.example.mcatowns.util.InventoryHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.UUID;

public final class VillagerRecruitmentService {
    private VillagerRecruitmentService() {
    }

    public static void open(ServerPlayerEntity player, VillagerEntity villager) {
        TownContext town = PlayerTownRegistry.get(player.getServerWorld()).getOwnedTown(player.getUuid()).orElse(null);
        if (town == null) return;
        TownSavedData data = TownSavedData.get(player.getServerWorld(), town.townId());
        SpecialistType specialist = TownSpecialistRegistry.get(player.getServerWorld()).get(villager.getUuid()).orElse(null);
        boolean employed = data.getSpecialists().containsKey(villager.getUuid());
        boolean introduced = specialist == null || QuestIntegration.introductionsComplete(player, villager.getUuid());
        int hearts = MCAIntegration.getHearts(player, villager);
        int cost = specialist == null ? MCATownsConfig.get().residentRecruitmentCost
                : MCATownsConfig.get().specialistRecruitmentCost;
        boolean canInvite = !data.getResidents().contains(villager.getUuid())
                && data.getResidents().size() < data.getPopulationCapacity()
                && hearts >= MCATownsConfig.get().residentFriendshipHearts
                && introduced
                && (specialist == null || data.hasBuilding(specialist.workplace())
                && (data.getTownRank() != TownRank.UNRANKED || specialist == SpecialistType.ARCHITECT)
                && data.getSpecialists().size() < data.getTownRank().maxSpecialists()
                && !data.getSpecialists().containsValue(specialist.id()));
        List<VillagerTownView.ResearchOption> research = employed && specialist == SpecialistType.ARCHITECT
                ? TownResearchDefinition.ALL.stream().map(definition -> {
                    TownBuildingDefinition building = TownBuildingDefinition.get(definition.buildingId());
                    return new VillagerTownView.ResearchOption(definition.id(), building.displayName(),
                            data.isBuildingUnlocked(definition.buildingId()), definition.scraps(), definition.greatEssence(),
                            definition.currency(), definition.townTokens());
                }).toList() : List.of();
        ModNetworking.openVillagerTown(player, new VillagerTownView(villager.getUuid(), villager.getDisplayName().getString(),
                hearts, specialist == null ? "" : specialist.displayName(), introduced, employed, canInvite, cost, research));
    }

    public static void recruit(ServerPlayerEntity player, UUID villagerId) {
        Entity entity = player.getServerWorld().getEntity(villagerId);
        if (!(entity instanceof VillagerEntity villager) || player.squaredDistanceTo(villager) > 36.0) return;
        TownContext town = PlayerTownRegistry.get(player.getServerWorld()).getOwnedTown(player.getUuid()).orElse(null);
        if (town == null) return;
        TownSavedData data = TownSavedData.get(player.getServerWorld(), town.townId());
        SpecialistType specialist = TownSpecialistRegistry.get(player.getServerWorld()).get(villagerId).orElse(null);
        int hearts = MCAIntegration.getHearts(player, villager);
        int cost = specialist == null ? MCATownsConfig.get().residentRecruitmentCost
                : MCATownsConfig.get().specialistRecruitmentCost;
        Item currency = currencyItem();
        boolean specialistReady = specialist == null || QuestIntegration.introductionsComplete(player, villagerId)
                && data.hasBuilding(specialist.workplace())
                && (data.getTownRank() != TownRank.UNRANKED || specialist == SpecialistType.ARCHITECT)
                && data.getSpecialists().size() < data.getTownRank().maxSpecialists()
                && !data.getSpecialists().containsValue(specialist.id());
        if (data.getResidents().contains(villagerId) || data.getResidents().size() >= data.getPopulationCapacity()
                || hearts < MCATownsConfig.get().residentFriendshipHearts || !specialistReady
                || !player.getAbilities().creativeMode && InventoryHelper.count(player.getInventory(), currency) < cost) {
            player.sendMessage(Text.translatable("text.mcatowns.recruitment_requirements_missing"), true);
            return;
        }
        if (!MCAIntegration.recruitResident(player, villager, town.center())) {
            player.sendMessage(Text.translatable("text.mcatowns.recruitment_failed"), true);
            return;
        }
        boolean added = specialist == null ? data.addResident(villagerId) : data.addSpecialist(villagerId, specialist.id());
        if (!added) return;
        if (!player.getAbilities().creativeMode) InventoryHelper.remove(player.getInventory(), currency, cost);
        data.setPopulation(data.getResidents().size());
        player.sendMessage(Text.translatable("text.mcatowns.villager_recruited", villager.getDisplayName()), false);
    }

    private static Item currencyItem() {
        Identifier id = Identifier.tryParse(MCATownsConfig.get().currencyItemId);
        return id == null ? Registries.ITEM.get(new Identifier("minecraft", "emerald")) : Registries.ITEM.get(id);
    }
}
