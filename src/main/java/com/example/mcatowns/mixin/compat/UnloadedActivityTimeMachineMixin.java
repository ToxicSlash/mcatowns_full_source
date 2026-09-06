package com.example.mcatowns.mixin.compat;

import com.example.mcatowns.integration.UnloadedActivityCompat;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs only when Unloaded Activity's TimeMachine class exists. */
@Pseudo
@Mixin(targets = "fabric.lol.zanspace.unloadedactivity.TimeMachine", remap = false)
public abstract class UnloadedActivityTimeMachineMixin {
    @Inject(method = "simulateChunk", at = @At("RETURN"), remap = false, require = 0)
    private static void mcatowns$afterChunkCatchUp(long elapsedTicks, ServerWorld world, WorldChunk chunk,
                                                   int randomTickSpeed, CallbackInfoReturnable<Long> cir) {
        UnloadedActivityCompat.onChunkCatchUp(elapsedTicks, world, chunk, randomTickSpeed);
    }
}
