package com.example.mcatowns.integration;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GuardVillagersIntegration {
    private static final ConcurrentMap<Class<?>, Optional<Method>> MCA_IS_GUARD_METHODS = new ConcurrentHashMap<>();

    public static int countNearbyGuards(ServerWorld world, BlockPos pos) {
        Box box = new Box(pos).expand(64);
        return world.getEntitiesByClass(LivingEntity.class, box, GuardVillagersIntegration::isGuardEntity).size();
    }

    public static boolean isGuardEntity(LivingEntity entity) {
        Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());
        if ("guardvillagers".equals(entityId.getNamespace())) {
            return true;
        }
        if (!"net.mca.entity.VillagerEntityMCA".equals(entity.getClass().getName())) {
            return false;
        }

        Optional<Method> method = MCA_IS_GUARD_METHODS.computeIfAbsent(entity.getClass(), type -> {
            try {
                return Optional.of(type.getMethod("isGuard"));
            } catch (ReflectiveOperationException ignored) {
                return Optional.empty();
            }
        });
        if (method.isEmpty()) return false;
        try {
            Object result = method.get().invoke(entity);
            return result instanceof Boolean b && b;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
