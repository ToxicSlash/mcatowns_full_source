package com.example.mcatowns.command;

import com.example.mcatowns.integration.UnloadedActivityCompat;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

public final class FastForwardCommand {
    private FastForwardCommand() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerRoot(dispatcher));
    }

    private static void registerRoot(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("fastforward")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("duration", StringArgumentType.word())
                        .executes(context -> execute(context.getSource(),
                                StringArgumentType.getString(context, "duration"))));
        dispatcher.register(root);
    }

    private static int execute(ServerCommandSource source, String durationText) {
        final long elapsedTicks;
        try {
            elapsedTicks = FastForwardDuration.parseTicks(durationText);
        } catch (IllegalArgumentException exception) {
            source.sendError(Text.literal(exception.getMessage()));
            return 0;
        }

        if (!UnloadedActivityCompat.isUnloadedActivityLoaded()) {
            source.sendError(Text.literal("Unloaded Activity is not loaded."));
            return 0;
        }
        if (!UnloadedActivityCompat.isChunkSimulationEnabled()) {
            source.sendError(Text.literal("Unloaded Activity chunk simulation is disabled or its 0.6.3 API is unavailable."));
            return 0;
        }

        ServerWorld world = source.getWorld();
        BlockPos pos = BlockPos.ofFloored(source.getPosition());
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ);
        if (chunk == null) {
            source.sendError(Text.literal("The command source chunk is not currently loaded."));
            return 0;
        }

        if (!UnloadedActivityCompat.simulateChunkNow(elapsedTicks, world, chunk)) {
            source.sendError(Text.literal("Unloaded Activity simulation failed. Check the server log for details."));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("Fast-forwarded chunk " + chunkX + ", " + chunkZ
                + " by " + FastForwardDuration.display(elapsedTicks) + " (" + elapsedTicks
                + " ticks) using Unloaded Activity. World time was not changed."), false);
        return 1;
    }
}
