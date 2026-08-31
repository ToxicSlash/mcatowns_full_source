package com.example.mcatowns.integration;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.UUID;

public class GuardStatIntegration {
    private static final UUID HEALTH_UUID = UUID.fromString("d57f9c3a-b8c3-4ce8-8d56-5b36d4b1f1a1");
    private static final UUID ARMOR_UUID = UUID.fromString("12633275-f22f-4fd5-9df6-1e5480c8d27c");
    private static final UUID DAMAGE_UUID = UUID.fromString("9c1b8ae4-f60b-4ee8-8e4b-a1662395ae2e");

    public static void applyBarracksBonuses(ServerWorld world, BlockPos anchor, int barracksLevel, int armories, int radius) {
        double armoryArmorBonus = Math.min(6.0, Math.max(0, armories) * 1.5);
        Box box = new Box(anchor).expand(radius);
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, GuardVillagersIntegration::isGuardEntity)) {
            applyAttributeBonus(entity, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_UUID, "mcatowns_barracks_health", switch (barracksLevel) {
                case 1 -> 2.0;
                case 2 -> 4.0;
                case 3 -> 8.0;
                default -> 0.0;
            });
            applyAttributeBonus(entity, EntityAttributes.GENERIC_ARMOR, ARMOR_UUID, "mcatowns_barracks_armor", switch (barracksLevel) {
                case 1 -> 1.0 + armoryArmorBonus;
                case 2 -> 2.0 + armoryArmorBonus;
                case 3 -> 4.0 + armoryArmorBonus;
                default -> armoryArmorBonus;
            });
            applyAttributeBonus(entity, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE_UUID, "mcatowns_barracks_damage", switch (barracksLevel) {
                case 1 -> 0.5;
                case 2 -> 1.5;
                case 3 -> 3.0;
                default -> 0.0;
            });
            if (barracksLevel > 0 && entity instanceof PathAwareEntity pathAware) {
                applyEquipment(pathAware, barracksLevel);
            }
            entity.setHealth(Math.min(entity.getHealth(), entity.getMaxHealth()));
        }
    }

    private static void applyAttributeBonus(LivingEntity entity, net.minecraft.entity.attribute.EntityAttribute attribute, UUID uuid, String name, double amount) {
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        EntityAttributeModifier existing = instance.getModifier(uuid);
        if (existing != null) {
            if (Double.compare(existing.getValue(), amount) == 0) {
                return;
            }
            instance.removeModifier(existing);
        }
        if (amount > 0.0) {
            instance.addPersistentModifier(new EntityAttributeModifier(uuid, name, amount, EntityAttributeModifier.Operation.ADDITION));
        }
    }

    private static void applyEquipment(PathAwareEntity entity, int level) {
        if (level >= 1) {
            ensureMainHand(entity, Items.IRON_SWORD, Items.BOW);
            ensureArmor(entity, EquipmentSlot.CHEST, Items.IRON_CHESTPLATE);
            ensureArmor(entity, EquipmentSlot.LEGS, Items.IRON_LEGGINGS);
            ensureArmor(entity, EquipmentSlot.FEET, Items.IRON_BOOTS);
        }
        if (level >= 2) {
            ensureOffHand(entity, Items.SHIELD);
            ensureArmor(entity, EquipmentSlot.HEAD, Items.IRON_HELMET);
        }
        if (level >= 3) {
            ensureMainHand(entity, Items.DIAMOND_SWORD, Items.CROSSBOW);
            ensureArmor(entity, EquipmentSlot.CHEST, Items.DIAMOND_CHESTPLATE);
            ensureArmor(entity, EquipmentSlot.HEAD, Items.DIAMOND_HELMET);
        }
    }

    private static void ensureMainHand(PathAwareEntity entity, Item melee, Item ranged) {
        Item current = entity.getMainHandStack().getItem();
        if (current == Items.BOW || current == Items.CROSSBOW) {
            if (current != ranged) {
                entity.equipStack(EquipmentSlot.MAINHAND, ranged.getDefaultStack());
            }
            return;
        }
        if (current == Items.AIR || current == Items.WOODEN_SWORD || current == Items.STONE_SWORD || current == Items.IRON_SWORD) {
            entity.equipStack(EquipmentSlot.MAINHAND, melee.getDefaultStack());
        }
    }

    private static void ensureOffHand(PathAwareEntity entity, Item offhand) {
        Item mainHand = entity.getMainHandStack().getItem();
        boolean holdingSupportedMeleeWeapon = mainHand == Items.WOODEN_SWORD
                || mainHand == Items.STONE_SWORD
                || mainHand == Items.IRON_SWORD
                || mainHand == Items.GOLDEN_SWORD
                || mainHand == Items.DIAMOND_SWORD
                || mainHand == Items.NETHERITE_SWORD;

        // TACZ and other two-handed weapons need an empty offhand for their holding pose.
        // Only remove the vanilla shield that this upgrade supplies; preserve all other items.
        if (!holdingSupportedMeleeWeapon) {
            if (entity.getOffHandStack().isOf(Items.SHIELD)) {
                entity.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
            return;
        }

        if (entity.getOffHandStack().isEmpty()) {
            entity.equipStack(EquipmentSlot.OFFHAND, offhand.getDefaultStack());
        }
    }

    private static void ensureArmor(PathAwareEntity entity, EquipmentSlot slot, Item item) {
        if (entity.getEquippedStack(slot).isEmpty()) {
            entity.equipStack(slot, item.getDefaultStack());
        }
    }
}
