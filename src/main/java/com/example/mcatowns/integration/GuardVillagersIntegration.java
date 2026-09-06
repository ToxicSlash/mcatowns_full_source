package com.example.mcatowns.integration;

import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/** Optional Guard Villagers integration used for standard town guards. */
public final class GuardVillagersIntegration {
    private GuardVillagersIntegration() { }

    public static int countNearbyGuards(ServerWorld world, BlockPos pos) {
        Box box = new Box(pos).expand(64);
        return world.getEntitiesByClass(LivingEntity.class, box, GuardVillagersIntegration::isGuardEntity).size();
    }

    public static boolean isGuardEntity(LivingEntity entity) {
        Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());
        return "guardvillagers".equals(entityId.getNamespace());
    }
}
