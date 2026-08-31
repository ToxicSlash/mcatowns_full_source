package com.example.mcatowns.integration;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.util.UUID;

public final class QuestIntegration {
    private static final Identifier INTRODUCTIONS = new Identifier("mcatowns", "specialist_introductions");

    private QuestIntegration() {
    }

    public static void assignIntroductions(Entity villager) {
        if (!FabricLoader.getInstance().isModLoaded("mebahelrpgrquest")) return;
        try {
            invoke(villager, "mebahelrpgrquest$setQuestId", new Class<?>[]{Identifier.class}, INTRODUCTIONS);
            invoke(villager, "mebahelrpgrquest$setQuestRollDone", new Class<?>[]{boolean.class}, true);
            invoke(villager, "mebahelrpgrquest$setQuestClaimed", new Class<?>[]{boolean.class}, false);
        } catch (ReflectiveOperationException ignored) { }
    }

    public static boolean introductionsComplete(ServerPlayerEntity player, UUID villagerId) {
        if (!FabricLoader.getInstance().isModLoaded("mebahelrpgrquest")) return true;
        try {
            Object progress = invoke(player, "mebahelrpgrquest$getQuestFromGiver", new Class<?>[]{UUID.class}, villagerId);
            if (progress == null) return false;
            Object questId = invoke(progress, "getQuestId", new Class<?>[0]);
            return INTRODUCTIONS.equals(questId) && (boolean) invoke(progress, "isClaimed", new Class<?>[0]);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args)
            throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
