package com.example.mcatowns.integration;

import com.example.mcatowns.town.TownDefenceInfrastructure;
import com.example.mcatowns.town.TownSavedData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.UUID;

/** Applies dynamic town Defence bonuses to explicitly affiliated Guard Villagers. */
public final class GuardStatIntegration {
    private static final UUID HEALTH_UUID = UUID.fromString("d57f9c3a-b8c3-4ce8-8d56-5b36d4b1f1a1");
    private static final UUID ARMOR_UUID = UUID.fromString("12633275-f22f-4fd5-9df6-1e5480c8d27c");
    private static final UUID DAMAGE_UUID = UUID.fromString("9c1b8ae4-f60b-4ee8-8e4b-a1662395ae2e");

    private GuardStatIntegration() { }

    public static void applyTownDefenceBonuses(ServerWorld world, BlockPos anchor, TownSavedData data, int radius) {
        double healthBonus = TownDefenceInfrastructure.guardHealthBonus(data);
        double armorBonus = TownDefenceInfrastructure.guardArmourBonus(data);
        double damageBonus = TownDefenceInfrastructure.guardAttackBonus(data);

        Box box = new Box(anchor).expand(radius);
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, GuardVillagersIntegration::isGuardEntity)) {
            if (!data.getResidents().contains(entity.getUuid())) continue;
            applyAttributeBonus(entity, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_UUID,
                    "mcatowns_defence_health", healthBonus);
            applyAttributeBonus(entity, EntityAttributes.GENERIC_ARMOR, ARMOR_UUID,
                    "mcatowns_defence_armor", armorBonus);
            applyAttributeBonus(entity, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE_UUID,
                    "mcatowns_defence_damage", damageBonus);
            if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
        }
    }

    public static void clearTownBonuses(LivingEntity entity) {
        if (entity == null) return;
        removeModifier(entity, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_UUID);
        removeModifier(entity, EntityAttributes.GENERIC_ARMOR, ARMOR_UUID);
        removeModifier(entity, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE_UUID);
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
    }

    private static void applyAttributeBonus(LivingEntity entity, EntityAttribute attribute, UUID uuid,
                                            String name, double amount) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance == null) return;
        EntityAttributeModifier existing = instance.getModifier(uuid);
        if (existing != null) {
            if (Double.compare(existing.getValue(), amount) == 0) return;
            instance.removeModifier(existing);
        }
        if (amount > 0.0D) {
            instance.addPersistentModifier(new EntityAttributeModifier(
                    uuid, name, amount, EntityAttributeModifier.Operation.ADDITION));
        }
    }

    private static void removeModifier(LivingEntity entity, EntityAttribute attribute, UUID uuid) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance == null) return;
        EntityAttributeModifier existing = instance.getModifier(uuid);
        if (existing != null) instance.removeModifier(existing);
    }
}
