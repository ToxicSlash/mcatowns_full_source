package com.example.mcatowns.integration;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

public class GuardVillagersIntegration {
    public static int countNearbyGuards(ServerWorld world, BlockPos pos) {
        Box box = new Box(pos).expand(64);
        return world.getEntitiesByClass(LivingEntity.class, box, GuardVillagersIntegration::isGuardEntity).size();
    }

    public static boolean isGuardEntity(LivingEntity entity) {
        Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());
        String className = entity.getClass().getName();
        if ("guardvillagers".equals(entityId.getNamespace())) {
            return true;
        }
        if ("net.mca.entity.VillagerEntityMCA".equals(className)) {
            try {
                Object result = entity.getClass().getMethod("isGuard").invoke(entity);
                return result instanceof Boolean b && b;
            } catch (ReflectiveOperationException ignored) {
                return false;
            }
        }
        return false;
    }
}
